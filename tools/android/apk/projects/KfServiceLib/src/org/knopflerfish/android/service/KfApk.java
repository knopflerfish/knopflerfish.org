package org.knopflerfish.android.service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.knopflerfish.framework.Main;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

import android.content.res.AssetManager;
import android.util.Log;

/**
 * 'Main' class for Knopflerfish packed as an Android application (.apk).
 * <p>
 * Bundle Jar files in the "assets/jars" folder will be installed and
 * started, framework and system properties in the "assets/props.xargs"
 * file will be set as framework/system properties.
 * <b>
 * The "jars" folder should contain one or more sub-folders. Each
 * sub-folder will be processed in turn in ascending order (based
 * on sub-folder name). For each sub-folder, first every bundle is
 * installed and then every bundle is stated (or resolved if the bundle
 * is a fragment bundle). Bundle Jar files in a sub-folder have no order.
 */
public class KfApk {
  protected final static String JAR_BASE = "jars";
  protected final static String PROP_FILE = "props.xargs";
  protected final static String FWDIR = "fwdir";
  protected final static String RUNLEVEL = "runlevel";
  
  protected static Map<String,String> config;
  /**
   * Create a new framework, initialize and start it. If the init
   * parameter is true, or if there is no stored framework state, bundle
   * Jar file data will be read from the "jars" folder of the provided
   * AssetManager. If the init parameter is false and there is a stored
   * framework state, the framework will be started and its state will
   * be the stored state.
   * 
   * @param localStorage Path to application's storage location in the
   *        file system, framework state will be stored here
   * @param init If true, any stored framework state is ignored.
   * @param am AssetManager for the application, will be used to read
   *        bundle Jar file data when the framework is started without
   *        using a stored state.
   * @return a framework instance or null if the framework could not be
   *         started or initialized.
   * @throws IOException if there was a problem reading bundle Jar file
   *         data or framework/system properties using the AssestManager.
   */
  public static Framework newFramework(String localStorage,
                                       boolean init,
                                       AssetManager am)
      throws IOException
  {
    config = getConfiguration(am);
    String fwDir = localStorage + File.separator + FWDIR;
    config.put(Constants.FRAMEWORK_STORAGE, fwDir);

    boolean doInit = init || !new File(fwDir).exists();
    
    if (doInit) {
      config.put(Constants.FRAMEWORK_STORAGE_CLEAN,
                 Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
    }

    Framework fw = new Main().getFrameworkFactory().newFramework(config);
    try {
      fw.init();
      int runlevel;
      if (doInit) {
        runlevel = scanBundles(fw, am);
        saveRunlevel(runlevel, fwDir);
      } else {
        runlevel = loadRunlevel(fwDir);
      }
      fw.start();
      // set target start level for framework start-up
      final FrameworkStartLevel fsl = fw.adapt(FrameworkStartLevel.class);
      if (fsl!=null) {
        fsl.setStartLevel(runlevel);
      }      
    } catch (BundleException be) {
      Log.e(KfApk.class.getName(), "Failed to init/start framework", be);
      return null;
    }
    
    return fw;
  }
  
  /**
   * Get the framework properties that was read from the props.xargs
   * assets file.
   * 
   * @return a map with framework properties, or null if the framework
   * has not been started yet.
   */
  public static Map<String,String> getFrameworkProperties() {
    return config;
  }
  
  /**
   * Method that can be used to wait for a started framework to be
   * shut down.
   * 
   * @param fw the framework to wait on.
   */
  public static void waitForStop(Framework fw) {
    while (true) { // Ignore interrupted exception.
      try {
        FrameworkEvent stopEvent = fw.waitForStop(0L);
        switch (stopEvent.getType()) {
        case FrameworkEvent.STOPPED:
          // FW terminated, Main is done!
          Log.i(KfApk.class.getName(), "Framework terminated");
          return;
        case FrameworkEvent.STOPPED_UPDATE:
          // Automatic FW restart, wait again.
          break;
        case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
          // A manual FW restart with new boot class path is needed.
          return; // TODO
        case FrameworkEvent.ERROR:
          // Stop failed or other error, give up.
          Log.i(KfApk.class.getName(), "Fatal framework error, terminating.",
                stopEvent.getThrowable());
          return;
        case FrameworkEvent.WAIT_TIMEDOUT:
          // Should not happen with timeout==0, give up.
          Log.i(KfApk.class.getName(), "Framework waitForStop(0) timed out!",
                stopEvent.getThrowable());
          break;
        }
      } catch (final InterruptedException ie) { }
    }
  }

  
  protected static int scanBundles(Framework fw, AssetManager am)
      throws IOException
  {
    int runlevel = 1;
    BundleContext bc = fw.getBundleContext();
    String[] folders = am.list(JAR_BASE);
    for (String folder : folders) {
      int startlevel;
      try {
        startlevel = Integer.parseInt(folder);
      } catch (NumberFormatException nfe) {
        // can not use folder name as startlevel, skip
        continue;
      }
      if (startlevel > runlevel) {
        runlevel = startlevel;
      }
      
      final FrameworkStartLevel fsl = fw.adapt(FrameworkStartLevel.class);
      if (fsl!=null) {
        fsl.setInitialBundleStartLevel(startlevel);
      }
      
      String fPath = JAR_BASE + File.separator + folder;
      String[] jars = am.list(fPath);
      Bundle[] installed = new Bundle[jars.length];
      // install bundles in this folder
      for (int i = 0; i < jars.length; i++) {
        String jPath = fPath + File.separator + jars[i];
        try {
          Log.i(KfApk.class.getName(), "Installing bundle "+jPath);
          installed[i] = bc.installBundle("apk_asset:" + jPath, am.open(jPath));
        } catch (BundleException be) {
          Log.e(KfApk.class.getName(), "Could not install bundle "+jPath, be);
        }
      }
      // start (or resolve) installed bundles
      for (Bundle b : installed) {
        if (b != null) {
          BundleRevision br = b.adapt(BundleRevision.class);
          boolean frag = (br.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
          if (!frag) {
            try {
              Log.i(KfApk.class.getName(),"Starting bundle "+b.getLocation());
              b.start(Bundle.START_ACTIVATION_POLICY); // mark for start
            } catch (BundleException be) {
              Log.e(KfApk.class.getName(), "Could not start bundle "
                  + b.getLocation(), be);            
            }
          }
        } else {
          // bundle did not install,
        }
      }
    }
    
    return runlevel;
  }
  
  protected static Map<String,String> getConfiguration(AssetManager am)
      throws IOException
  {
    List<String> fwProps = new ArrayList<String>();
    List<String> sysProps = new ArrayList<String>();
    
    loadProps(am, fwProps, sysProps);
    
    // set system properties
    Map<String,String> map = getAsMap(sysProps);
    final Properties p = System.getProperties();
    p.putAll(map);
    System.setProperties(p);
    
    // return framework properties
    return getAsMap(fwProps);
  }
  
  protected static Map<String,String> getAsMap(List<String> lines)
      throws IndexOutOfBoundsException
  {
    Map<String,String> map = new HashMap<String,String>();
    for (String line : lines) {
      // split at '=' strip -F/-D from key
      int i = line.indexOf('=');
      map.put(line.substring(2, i), line.substring(i+1));
    }
    return map;
  }
  
  protected static void loadProps(AssetManager am,
                                  List<String> fwProps,
                                  List<String> sysProps)
    throws IOException 
  {
    BufferedReader in = null;
    try {
      final InputStream is = am.open(PROP_FILE);
      in = new BufferedReader(new InputStreamReader(is));

      StringBuilder contLine = new StringBuilder();
      String line = null;
      String tmpline  = null;
      for(tmpline = in.readLine(); tmpline != null; tmpline = in.readLine()) {
        tmpline = tmpline.trim();

        // check for line continuation char and
        // build up line until a line without such a mark is found.
        if(tmpline.endsWith("\\")) {
          // found continuation mark, store actual line to
          // buffered continuation line
          tmpline = tmpline.substring(0, tmpline.length() - 1);
          if(contLine == null) {
            contLine = new StringBuilder(tmpline);
          } else {
            contLine.append(tmpline);
          }
          // read next line
          continue;
        } else {
          // No continuation mark, gather stored line + newly read line
          if(contLine != null) {
            contLine.append(tmpline);
            line = contLine.toString();
            contLine = null;
          } else {
            // this is the normal case if no continuation char is found
            // or any buffered line is found
            line = tmpline;
          }
        }

        if(line.startsWith("-D")) {
          // Preserve System property
          sysProps.add(line);
        } else if(line.startsWith("-F")) {
          // Preserve framework property
          fwProps.add(line);
        } else if(line.length() == 0 || line.startsWith("#")) {
          // Ignore empty lines and comments
        } else {
          throw new IllegalArgumentException("Illegal xargs line '" + line + "'");
        }
      }
    } catch (FileNotFoundException fnfe) {
      // no props.xargs file, ok
    } finally {
      if (null!=in) {
        try {
          in.close();
        } catch (final IOException ignore) { }
      }
    }
  }
  
    private static int loadRunlevel(String fwDir) throws IOException
  {
    DataInputStream dis = null;
    try {
      String path = fwDir + File.separator + RUNLEVEL;
      dis = new DataInputStream(new FileInputStream(path));
      return dis.readInt();
    } finally {
      if (dis != null) {
        dis.close();
      }
    }
  }


  private static void saveRunlevel(int runlevel, String fwDir)
      throws IOException
  {
    DataOutputStream dos = null;
    try {
      String path = fwDir + File.separator + RUNLEVEL;
      dos = new DataOutputStream(new FileOutputStream(path));
      dos.writeInt(runlevel);
    } finally {
      if (dos != null) {
        dos.close();
      }
    }
  }

}
