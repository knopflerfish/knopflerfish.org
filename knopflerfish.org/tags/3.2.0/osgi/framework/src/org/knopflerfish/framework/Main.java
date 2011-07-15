/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.framework;

import java.io.*;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.service.startlevel.StartLevel;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * This is the main startup code for the framework and enables
 * basic operations as install, start, stop, uninstall
 * and update.
 *
 * @author Jan Stein, Erik Wistrand, Mats-Ola Persson, Gunnar Ekolin
 */
public class Main
{
  // Main object
  static Main main;

  // Verbosity level of printouts. 0 is low.
  int verbosity;

  // Will be filled in from manifest entry during startup
  String version = "<unknown>";

  // Top directory (excluding trailing /)
  String topDir = "";

  // Set to true if JRE is started without any arguments
  boolean bZeroArgs;

  // will be initialized by main() - up for anyone for grabbing
  public String bootText;

  // Framwork properties, i.e., configuration properties from -Fkey=value
  Map fwProps/* <String, String> */ = new HashMap/*<String, String>*/();

  // System properties, i.e., configuration properties from -Dkey=value
  Map sysProps/* <String, String> */ = new HashMap/*<String, String>*/();

  static public final String JARDIR_PROP    = "org.knopflerfish.gosg.jars";
  static public final String JARDIR_DEFAULT = "file:";

  static public final String XARGS_INIT     = "init.xargs";
  static public final String XARGS_RESTART  = "restart.xargs";
  static public final String FWPROPS_XARGS  = "fwprops.xargs";

  static public final String CMDIR_PROP    = "org.knopflerfish.bundle.cm.store";
  static public final String CMDIR_DEFAULT = "cmdir";

  static public final String VERBOSITY_PROP    = "org.knopflerfish.framework.main.verbosity";
  static public final String VERBOSITY_DEFAULT = "0";

   /**
   * Name of framework property controlling wheather to write an
   * FWPROPS_XARGS file or not at startup.
   */
  static public final String WRITE_FWPROPS_XARGS_PROP =
    "org.knopflerfish.framework.main.write.fwprops.xargs";

  static public final String XARGS_WRITESYSPROPS_PROP =
    "org.knopflerfish.framework.main.xargs.writesysprops";

  static public final String XARGS_DEFAULT     = "default";

  static public final String PRODVERSION_PROP     = "org.knopflerfish.prodver";

  /**
   * Default values for some framework properties.
   */
  Map defaultFwProps = new HashMap() {{
    put(CMDIR_PROP,    CMDIR_DEFAULT);
  }};


  FrameworkFactory ff;
  Framework framework;

  /**
   * Help class for starting the OSGi framework.
   */
  public static void main(String[] args) {
    main = new Main();
    main.start(args);
    System.exit(0);
  }


  public Main() {
    try { // Set the initial verbosity level.
      final String vpv = (String) System.getProperty(VERBOSITY_PROP);
      verbosity = Integer.parseInt(null==vpv ? VERBOSITY_DEFAULT: vpv);
    } catch (Exception ignored) { }
    populateSysProps();
  }


  protected void start(String[] args) {
    version = readVersion();

    bootText =
      "Knopflerfish OSGi framework, version " + version + "\n" +
      "Copyright 2003-2011 Knopflerfish. All Rights Reserved.\n\n" +
      "See http://www.knopflerfish.org for more information.";

    System.out.println(bootText);

    // Check if framework is started with no args at all.
    // This typically happens when starting with "java -jar framework.jar"
    // or similar (e.g by double-clicking on framework.jar)
    bZeroArgs = (args.length == 0);
    if (!bZeroArgs) {// Also true if started with only -D/-F args
      bZeroArgs = true;
      for (int i=0; bZeroArgs && i<args.length; i++) {
        // -Dx=y, -Fx=y and -init does not count as args
        bZeroArgs = args[i].startsWith("-D")
          || args[i].startsWith("-F")
          || "-init".equals(args[i]);
      }
    }

    processProperties(args);

    if (bZeroArgs) {// Add default xargs file to command line
      // To determine the fwdir we must process all -D/-F definitions
      // on the current command line.
      String xargsPath = getDefaultXArgs();
      if(xargsPath != null) {
        if (0==args.length) {
          args = new String[] {"-xargs", xargsPath};
        } else {
          final String[] newArgs = new String[args.length +2];
          newArgs[0] = "-xargs";
          newArgs[1] = xargsPath;
          System.arraycopy(args, 0, newArgs, 2, args.length);
          args = newArgs;
        }
      }
    }

    args = expandArgs(args);
    handleArgs(args);
  }


  /**
   * Shall framework properties be exported as system properties?
   */
  private boolean writeSysProps() {
    Object val = fwProps.get(XARGS_WRITESYSPROPS_PROP);
    if (val == null) {
      val = sysProps.get(XARGS_WRITESYSPROPS_PROP);
    }
   
    println("writeSysProps? '" + val + "'", 2);
    return "true".equals(val);
  }


  public FrameworkFactory getFrameworkFactory() {
    try {
      String factoryS = new String(Util.readResource("/META-INF/services/org.osgi.framework.launch.FrameworkFactory"), "UTF-8");
      String factoryClassName = FrameworkFactoryImpl.class.getName();
      String[] w = Util.splitwords(factoryS, "\n\r");
      for(int i = 0; i < w.length; i++) {
        if(w[i].length() > 0 && !w[i].startsWith("#")) {
          factoryClassName = w[i].trim();
          break;
        }
      }

      return getFrameworkFactory(factoryClassName);
    } catch (Exception e) {
      error("failed to getFrameworkFactory", e);
      throw new RuntimeException("failed to getFrameworkFactory: " + e);
    }
  }


  public FrameworkFactory getFrameworkFactory(String factoryClassName) {
    try {
      println("getFrameworkFactory(" + factoryClassName + ")", 2);
      Class            clazz = Class.forName(factoryClassName);
      Constructor      cons  = clazz.getConstructor(new Class[] { });
      FrameworkFactory ff    = (FrameworkFactory)cons.newInstance(new Object[] { });
      return ff;
    } catch (Exception e) {
      error("failed to create " + factoryClassName, e);
      throw new RuntimeException("failed to create " + factoryClassName + ": " + e);
    }
  }


  String[] getJarBase() {
    assertFramework();
    String jars = framework.getBundleContext().getProperty(JARDIR_PROP);
    if(jars == null) {
      jars = JARDIR_DEFAULT;
    }

    String[] base = Util.splitwords(jars, ";");
    for (int i=0; i<base.length; i++) {
      try {
        base[i] = new URL(base[i]).toString();
      } catch (Exception ignored) {
      }
      println("jar base[" + i + "]=" + base[i], 3);
    }

    return base;
  }


  /**
   * Process one command line argument for setting a property.
   *
   * If the line contains an '=' then property will be set.
   *
   * Example "org.knopflerfish.test=apa" is equivalent to calling
   * <tt>setProperty("org.knopflerfish.test", "apa")</tt> in the given
   * props object.
   *
   * @param prefix The prefix (<tt>-D</tt>/<tt>-F</tt>) to print in the trace.
   * @param arg    The command line argument to process.
   * @param props  The properties object to add the property to.
   */
  void setProperty(String prefix, String arg, Map props) {
    final int ix = arg.indexOf("=");
    if(ix != -1) {
      final String key = arg.substring(2, ix);
      String value = arg.substring(ix + 1);

      println(prefix +key +"=" +value, 1);
      props.put(key,value);

      if (VERBOSITY_PROP.equals(key)) {
        // redo this here since verbosity level may have changed
        try {
          int old = verbosity;
          verbosity = Integer.parseInt( value.length()==0
                                        ? VERBOSITY_DEFAULT : value);
          if (old != verbosity) {
            println("Verbosity changed to "+verbosity, 1);
          }
        } catch (Exception ignored) {}
      }
    }
  }


  /**
   * Process all property related command line arguments. I.e.,
   * arguments on the form
   * <ul>
   *
   *   <li> <tt>-D<it>*</it></tt> a system property definition.
   *
   *   <li> <tt>-F<it>*</it></tt> a framework property definition.
   *
   *   <li> <tt>-init</it></tt> a request to clear the frameworks
   *        persistent state on the first call to init(). This will
   *        set the framework property named
   *        <tt>org.osgi.framework.storage.clean</tt> to the value
   *        <tt>onFirstInit</tt>.
   *
   * </ul>
   *
   * Example
   * "-Dorg.knopflerfish.test=apa" is equivalent to the code
   * <tt>System.setProperty("org.knopflerfish.test", "apa");</tt>
   *
   * @param args  The command line argument array to process
   */
  void processProperties(String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        if(args[i].startsWith("-D")) { // A system property
          setProperty("-D", args[i], sysProps);
        } else if (args[i].startsWith("-F")) { // A framework property
          setProperty("-F", args[i], fwProps);
        } else if ("-init".equals(args[i])) {
          fwProps.put(Constants.FRAMEWORK_STORAGE_CLEAN,
                      Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        }
      } catch (Exception e) {
        e.printStackTrace(System.err);
        error("Command \"" +args[i] +"\" failed, " + e.getMessage());
      }
    }
  }


  /**
   * Save all framework properties as an xargs-file in the framework
   * directory for restarts.
   */
  private void save_restart_props(Map props) {
    final String xrwp = framework.getBundleContext().getProperty(WRITE_FWPROPS_XARGS_PROP);
    if ("false".equalsIgnoreCase(xrwp)) {
      return;
    }
    PrintWriter pr = null;
    try {
      final String fwDirStr = Util.getFrameworkDir(props);
      final File fwDir      = new File(fwDirStr);
      fwDir.mkdirs();

      final File propsFile = new File(fwDir, Main.FWPROPS_XARGS);
      pr = new PrintWriter(new BufferedWriter(new FileWriter(propsFile)));
      for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
        final Map.Entry entry = (Map.Entry) it.next();
        final String key   = (String) entry.getKey();
        final String value = (String) entry.getValue();
        
        // Don't save clean onFirstInit
        if (!Constants.FRAMEWORK_STORAGE_CLEAN.equals(key) ||
            !Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT.equals(value)) {
          pr.println("-F" +key +"=" +value);
        }
      }
    } catch (Exception e) {
      if(e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      throw new IllegalArgumentException("Failed to create "+ Main.FWPROPS_XARGS
                                         + ": " + e);
    } finally {
      if (null!=pr) {
        pr.close();
      }
    }
  }


  /**
   * Ensure that a framework instance is created and initialized, if
   * not finalize properties and create a framework instance.
   */
  void assertFramework() {
    if(ff == null) {
      ff = getFrameworkFactory();
      println("Created FrameworkFactory " + ff.getClass().getName(), 1);
    }
    if(framework == null) {
      // Expand property values and export them as system properties
      finalizeProperties();

      framework = ff.newFramework(fwProps);
      try {
        framework.init();
        save_restart_props(fwProps);
      } catch (BundleException be) {
        error("Failed to initialize the framework: " +be.getMessage(), be);
      }
      println("Created Framework " + framework.getClass().getName(), 1);
    }
  }


  /**
   * Handle command line options.
   *
   * @param args argument line
   */
  private void handleArgs(String[] args) {
    boolean bLaunched = false;

    // If not init, check if we have saved framework properties
    if (!Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
        .equals((String) fwProps.get(Constants.FRAMEWORK_STORAGE_CLEAN))) {
      final File propsFile = new File(Util.getFrameworkDir(fwProps), Main.FWPROPS_XARGS);
      if (propsFile.exists()) {
        final String[] newArgs = new String[args.length +2];
        newArgs[0] = "-xargs";
        newArgs[1] = propsFile.getAbsolutePath();
        System.arraycopy(args, 0, newArgs, 2, args.length);
        args = expandArgs(newArgs);
      }
    }
    // Since we must have all framework properties in the
    // fwProps-map before creating the framework instance we must
    // first handle all args that define properties. I.e., args
    // starting with '-D' and '-F'.
    processProperties(args);
    if(verbosity > 5) {
      for(int i = 0; i < args.length; i++) {
        println("argv[" + i + "]=" + args[i], 5);
      }
    }

    // Process all other options.
    for (int i = 0; i < args.length; i++) {
      try {
        if ("-exit".equals(args[i])) {
          println("Exit.", 0);
          System.exit(0);
        } else if (args[i].startsWith("-F")) {
          // Skip, handled in processProperties(String[])
        } else if (args[i].startsWith("-D")) {
          // Skip, handled in processProperties(String[])
        } else if ("-init".equals(args[i])) {
          // Skip, handled in processProperties(String[])
        } else if ("-version".equals(args[i])) {
          System.out.println("Knopflerfish version: " +readRelease());
          printResource("/tstamp");
          printResource("/revision");
          System.exit(0);
        } else if ("-help".equals(args[i])) {
          printResource("/help.txt");
          System.exit(0);
        } else if ("-jvminfo".equals(args[i])) {
          assertFramework();
          printJVMInfo(framework);
          System.exit(0);
        } else if ("-ff".equals(args[i])) {
          if (null!=framework) {
            throw new IllegalArgumentException
              ("a framework instance is already created.");
          }
          if (i+1 < args.length) {
            i++;
            ff = getFrameworkFactory(args[i]);
          } else {
            throw new IllegalArgumentException("No framework factory argument");
          }
        } else if ("-create".equals(args[i])) {
          if (null!=framework
              && ((Bundle.RESOLVED)&framework.getState())==0) {
            throw new IllegalArgumentException
              ("a framework instance is already created."
               +" The '-create' command must either be the first command"
               +" or come directly after a '-shutdown mSEC' command.");
          }
          framework = null;
        } else if ("-install".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final String bundle = args[i+1];
            final Bundle b = framework.getBundleContext()
              .installBundle(completeLocation(bundle), null);
            println("Installed: ", b);
            i++;
          } else {
            error("No URL for install command");
          }
        } else if ("-istart".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final String bundle = args[i+1];
            final Bundle b = framework.getBundleContext()
              .installBundle(completeLocation(bundle), null);

            b.start(Bundle.START_ACTIVATION_POLICY);
            println("Installed and started (policy): ", b);
            i++;
          } else {
            error("No URL for install command");
          }
        } else if ("-launch".equals(args[i])) {
          if (null!=framework && (Bundle.ACTIVE&framework.getState())!=0) {
            throw new IllegalArgumentException
              ("a framework instance is already active.");
          }
          assertFramework();
          framework.start();
          bLaunched = true;
          closeSplash();
          println("Framework launched", 0);
        } else if ("-shutdown".equals(args[i])) {
          if (i+1 < args.length) {
            i++;
            long timeout = Long.parseLong(args[i]);
            try {
              if(framework != null) {
                framework.stop();
                FrameworkEvent stopEvent = framework.waitForStop(timeout);
                switch (stopEvent.getType()) {
                case FrameworkEvent.STOPPED:
                  // FW terminated, Main is done!
                  println("Framework terminated", 0);
                  break;
                case FrameworkEvent.STOPPED_UPDATE:
                  // Automatic FW restart, wait again.
                  println("Framework stopped for update", 0);
                  break;
                case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
                  // A manual FW restart with new boot class path is needed.
                  println("Framework stopped for bootclasspath update", 0);
                  break;
                case FrameworkEvent.ERROR:
                  // Stop failed or other error, give up.
                  error("Fatal framework error, terminating.",
                        stopEvent.getThrowable());
                  break;
                case FrameworkEvent.WAIT_TIMEDOUT:
                  // Should not happen with timeout==0, give up.
                  error("Framework waitForStop(" +timeout +") timed out!",
                        stopEvent.getThrowable());
                  break;
                }
                println("Framework shutdown", 0);
              } else {
                throw new IllegalArgumentException("No framework to shutdown");
              }
            } catch (Exception e) {
              error("Failed to shutdown", e);
            }
          } else {
            error("No timout for shutdown command");
          }
        } else if ("-sleep".equals(args[i])) {
          if (i+1 < args.length) {
            final long t = Long.parseLong(args[i+1]);
            try {
              println("Sleeping " + t + " seconds...", 0);
              Thread.sleep(t * 1000);
            } catch (InterruptedException e) {
              error("Sleep interrupted.");
            }
            i++;
          } else {
            error("No time for sleep command");
          }
        } else if ("-start".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final long id = getBundleID(framework, args[i+1]);
            final Bundle b = framework.getBundleContext().getBundle(id);
            b.start(Bundle.START_ACTIVATION_POLICY);
            println("Started (policy): ", b);
            i++;
          } else {
            error("No ID for start command");
          }
        } else if ("-start_e".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final long id = getBundleID(framework, args[i+1]);
            final Bundle b = framework.getBundleContext().getBundle(id);
            b.start(0); // Eager start
            println("Started (eager): ", b);
            i++;
          } else {
            error("No ID for start_e command");
          }
        } else if ("-start_et".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final long id = getBundleID(framework, args[i+1]);
            final Bundle b = framework.getBundleContext().getBundle(id);
            b.start(Bundle.START_TRANSIENT);
            println("Started (eager,transient): ", b);
            i++;
          } else {
            error("No ID for start_et command");
          }
        } else if ("-start_pt".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final long id = getBundleID(framework, args[i+1]);
            final Bundle b = framework.getBundleContext().getBundle(id);
            b.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);
            println("Start (policy,transient): ", b);
            i++;
          } else {
            error("No ID for start_pt command");
          }
        } else if ("-stop".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final long id = getBundleID(framework, args[i+1]);
            final Bundle b = framework.getBundleContext().getBundle(id);
            try {
              b.stop(0);
              println("Stopped: ", b);
            } catch (Exception e) {
              error("Failed to stop", e);
            }
            i++;
          } else {
            error("No ID for stop command");
          }
        } else if ("-stop_t".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final long id = getBundleID(framework, args[i+1]);
            final Bundle b = framework.getBundleContext().getBundle(id);
            try {
              b.stop(Bundle.STOP_TRANSIENT);
              println("Stopped (transient): ", b);
            } catch (Exception e) {
              error("Failed to stop", e);
            }
            i++;
          } else {
            error("No ID for stop_t command");
          }
        } else if ("-uninstall".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final long id = getBundleID(framework, args[i+1]);
            final Bundle b = framework.getBundleContext().getBundle(id);
            b.uninstall();
            println("Uninstalled: ", b);
            i++;
          } else {
            error("No id for uninstall command");
          }
        } else if ("-update".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            Bundle[] bl = null;
            if("ALL".equals(args[i+1])) {
              bl = framework.getBundleContext().getBundles();
            } else {
              bl = new Bundle[] { framework.getBundleContext().getBundle(getBundleID(framework, args[i+1])) };
            }
            for(int n = 0; bl != null && n < bl.length; n++) {
              final Bundle b = bl[i];
              b.update();
              println("Updated: ", b);
            }
            i++;
          } else {
            error("No id for update command");
          }
        } else if ("-initlevel".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final int n = Integer.parseInt(args[i+1]);
            final ServiceReference sr = framework.getBundleContext()
              .getServiceReference(StartLevel.class.getName());

            if(sr != null) {
              final StartLevel ss = (StartLevel) framework.getBundleContext()
                .getService(sr);

              ss.setInitialBundleStartLevel(n);
              framework.getBundleContext().ungetService(sr);
            } else {
              println("No start level service - ignoring init bundle level "
                      + n, 0);
            }
            i++;
          } else {
            error("No integer level for initlevel command");
          }
        } else if ("-startlevel".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final int n = Integer.parseInt(args[i+1]);
            final ServiceReference sr = framework.getBundleContext()
              .getServiceReference(StartLevel.class.getName());

            if(sr != null) {
              final StartLevel ss = (StartLevel) framework.getBundleContext()
                .getService(sr);

              ss.setStartLevel(n);
              framework.getBundleContext().ungetService(sr);
            } else {
              println("No start level service - ignoring start level " + n, 0);
            }
            i++;
          } else {
            error("No integer level for startlevel command");
          }
        } else {
          error("Unknown option: " + args[i] +
                "\nUse option -help to see all options");
        }
      } catch (BundleException e) {
        final Throwable ne = e.getNestedException();
        if (ne != null) {
          e.getNestedException().printStackTrace(System.err);
        } else {
          e.printStackTrace(System.err);
        }
        error("Command \"" + args[i] +
              ((i+1 < args.length && !args[i+1].startsWith("-")) ?
               " " + args[i+1] :
               "") +
              "\" failed, " + e.getMessage());
      } catch (Exception e) {
        e.printStackTrace(System.err);
        error("Command \"" + args[i] +
              ((i+1 < args.length && !args[i+1].startsWith("-")) ?
               " " + args[i+1] :
               "") +
              "\" failed, " + e.getMessage());
      }
    }

    assertFramework();
    if(!bLaunched) {
      try {
        framework.start();
        closeSplash();
        println("Framework launched", 0);
      } catch (Throwable t) {
        if (t instanceof BundleException) {
          BundleException be = (BundleException) t;
          Throwable ne = be.getNestedException();
          if (ne != null) t = ne;
        }
        error("Framework launch failed, " + t.getMessage(), t);
      }
    }
    FrameworkEvent stopEvent = null;
    while (true) { // Ignore interrupted exception.
      try {
        stopEvent = framework.waitForStop(0L);
        switch (stopEvent.getType()) {
        case FrameworkEvent.STOPPED:
          // FW terminated, Main is done!
          println("Framework terminated", 0);
          return;
        case FrameworkEvent.STOPPED_UPDATE:
          // Automatic FW restart, wait again.
          break;
        case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
          // A manual FW restart with new boot class path is needed.
          return; // TODO
        case FrameworkEvent.ERROR:
          // Stop failed or other error, give up.
          error("Fatal framework error, terminating.",
                stopEvent.getThrowable());
          return;
        case FrameworkEvent.WAIT_TIMEDOUT:
          // Should not happen with timeout==0, give up.
          error("Framework waitForStop(0) timed out!",
                stopEvent.getThrowable());
          break;
        }
      } catch (InterruptedException ie) { }
    }
  }


  /**
   * Returns a bundle id from a string. The string is either a number
   * or the location used for the bundle in the
   * "-install bundleLocation" or "-istart" command.
   * @param base Base URL to complete locations with.
   * @param idLocation bundle id or location of the bundle to lookup
   */
  private long getBundleID(Framework fw, String idLocation) {
    try {
      return Long.parseLong(idLocation);
    } catch (NumberFormatException nfe) {
      Bundle[] bl = fw.getBundleContext().getBundles();
      String loc = completeLocation(idLocation);
      for(int i = 0; bl != null && i < bl.length; i++) {
        if(loc.equals(bl[i].getLocation())) {
          return bl[i].getBundleId();
        }
      }
      throw new IllegalArgumentException
        ("Invalid bundle id/location: " +idLocation);
    }
  }


  /**
   * Complete location relative to topDir.
   * @param location The location to be completed.
   */
  private String completeLocation(String location) {
    String[] base = getJarBase();
    // Handle file: case where topDir is not ""
    if(location.startsWith("file:jars/") && !topDir.equals("")) {
      location = ("file:" + topDir + "/" + location.substring(5)).replace('\\', '/');
      println("mangled bundle location to " + location, 2);
    }
    int ic = location.indexOf(":");
    if (ic<2 || ic>location.indexOf("/")) {
      println("location=" + location, 2);
      // URL without protocol complete it.
      for (int i=0; i<base.length; i++) {
        println("base[" + i + "]=" + base[i], 2);
        try {
          URL url = new URL( new URL(base[i]), location );

          println("check " + url, 2);
          if ("file".equals(url.getProtocol())) {
            File f = new File(url.getFile()).getAbsoluteFile();
            if (!f.exists() || !f.canRead()) {
              continue; // Noope; try next.
            }
          } else if ("http".equals(url.getProtocol())) {
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            uc.connect();
            int rc = uc.getResponseCode();
            uc.disconnect();
            if (rc!=HttpURLConnection.HTTP_OK) {
              println("Can't access HTTP bundle: " + url + ", response code=" + rc, 0);
              continue; // Noope; try next.
            }
          } else {
            // Generic case; Check if we can read data from this URL
            InputStream is = null;
            try {
              is = url.openStream();
            } finally {
              if (is!=null) is.close();
            }
          }
          location = url.toString();
          println("found location=" + location, 5);
          break; // Found.
        } catch (Exception _e) {
        }
      }
    }
    return location;
  }


  /**
   * Expand all occurance of <tt>-xarg &lt;URL&gt;</tt> and <tt>--xarg
   * &lt;URL&gt;</tt> into a new array without any <tt>-xargs</tt>,
   * <tt>--xargs</tt>.
   *
   * @param argv array of command line options to expand all
   *             <tt>-xarg &lt;URL&gt;</tt> options in.
   * @return New argv array where all <tt>-xarg &lt;URL&gt;</tt>
   *         options have been expanded.
   */
  String[] expandArgs(String[] argv) {
    List v = new ArrayList();
    int i = 0;
    while(i < argv.length) {
      if ("-xargs".equals(argv[i]) || "--xargs".equals(argv[i])) {
        // if "--xargs", ignore any load errors of xargs file
        boolean bIgnoreException = argv[i].equals("--xargs");
        if (i+1 < argv.length) {
          String   xargsPath = argv[i+1];
          i++;
          try {
            String[] moreArgs = loadArgs(xargsPath, argv);
            String[] r = expandArgs(moreArgs);
            for(int j = 0; j < r.length; j++) {
              v.add(r[j]);
            }
          } catch (RuntimeException e) {
            if(bIgnoreException) {
              println("Failed to load -xargs " + xargsPath, 1, e);
            } else {
              throw e;
            }
          }
        } else {
          throw new IllegalArgumentException("-xargs without argument");
        }
      } else {
        v.add(argv[i]);
      }
      i++;
    }
    String[] r = new String[v.size()];

    v.toArray(r);
    return r;
  }


  /**
   * Print help for starting the platform.
   */
  void printResource(String name) {
    try {
      System.out.println(new String(Util.readResource(name)));
    } catch (Exception e) {
      System.out.println("No resource '" + name + "' available");
    }
  }


  /**
   * Print help for starting the platform.
   */
  void printJVMInfo(Framework framework) {
    try {
      Properties props = System.getProperties();
      System.out.println("--- System properties ---");
      for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
        String key = (String)e.nextElement();
        System.out.println(key + ": " + props.get(key));
      }

      System.out.println("\n");
      System.out.println("--- Framework properties ---");

      String keyStr = framework.getBundleContext().getProperty("org.knopflerfish.framework.bundleprops.keys");
      String[] keys = Util.splitwords(keyStr != null ? keyStr : "", ",");

      for(int i = 0; i < keys.length; i++) {
        System.out.println(keys[i] + ": " + framework.getBundleContext().getProperty(keys[i]));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // should this be read from the manifest instead?
  static String readVersion() {
    return readResource("/version", "<no version found>", "UTF-8");
  }


  // should this be read from the manifest instead?
  static String readRelease() {
    return readResource("/release", "0.0.0.snapshot", "UTF-8");
  }


  // Read version info from manifest
  static String readResource(String file, String defaultValue, String encoding) {
    try {
      return (new String(Util.readResource(file), encoding)).trim();
    } catch (Exception e) {
      return defaultValue;
    }
  }


  /**
   * Helper method which tries to find a default xargs files to use.
   * Note: Make sure that fwProps are up to date by calling
   * processProperties(args) before calling this method.
   */
  String getDefaultXArgs() {
    boolean bInit = Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
      .equals((String) fwProps.get(Constants.FRAMEWORK_STORAGE_CLEAN));

    String fwDirStr = Util.getFrameworkDir(fwProps);
    // avoid getAbsoluteFile since some profiles don't have this
    File fwDir      = new File(new File(fwDirStr).getAbsolutePath());
    println("fwdir is " + fwDir, 1);

    if (!bInit) { // Implicit init needed?
      bInit = !(fwDir.exists() && fwDir.isDirectory());
      if (bInit) println("Implicit -init since fwdir does not exist.", 2);
    }
    println("init is "  +bInit, 2);

    // avoid getParentFile since some profiles don't have this
    String defDirStr = fwDir.getParent();
    File   defDir    = defDirStr != null ? new File(defDirStr) : null;

    File   cwDir = new File(new File("").getAbsolutePath());

    println("defDir=" +defDir, 5);
    println("cwDir="  +cwDir, 5);

    File[] dirs = null!=defDir ?
      new File[]{ fwDir, defDir, cwDir } : new File[]{ fwDir, cwDir };

    // Determine the root OSGi dir, a.k.a. topDir
    for (int i = 0; i<dirs.length; i++) {
      final File jarsDir = new File(dirs[i],"jars");
      if (jarsDir.exists() && jarsDir.isDirectory()) {
        topDir = dirs[i].getAbsolutePath();
        break;
      }
    }
    println("Knopflerfish root directory is " +topDir, 2);

    final String osName = Alias.unifyOsName(System.getProperty("os.name"));
    final String[] xargNames = bInit
      ? new String[]{"init_" +osName +".xargs",
                     XARGS_INIT,
                     "remote-" +XARGS_INIT }
      : new String[]{XARGS_RESTART};

    String xargsFound  = null;
    println("Searching for default xargs file:", 5);
    xargsSearch:
    for (int i = 0; i<dirs.length; i++) {
      for (int k = 0; k<xargNames.length; k++) {
        File xargsFile = new File(dirs[i], xargNames[k]);
        println("  trying " +xargsFile.getAbsolutePath(), 5);
        if (xargsFile.exists()) {
          xargsFound = xargsFile.getAbsolutePath();
          break xargsSearch;
        }
      }
    }
    println("default xargs file is " + xargsFound, 2);

    return xargsFound;
  }


  /**
   * Add default properties and set default values if important ones
   * are missing. The default values are taken from the
   * <tt>defaultFwProps</tt> variable.
   *
   * <p>The <tt>org.knopflerfish.gosg.jars</tt> system property (if
   * not defined) is created by scanning the "jars" directory for
   * subdirs.</p>
   *
   * @see defaultSysProps
   */
  protected void addDefaultProps() {
    for(Iterator it = defaultFwProps.entrySet().iterator(); it.hasNext();) {
      final Map.Entry entry = (Map.Entry) it.next();
      final String key = (String) entry.getKey();
      if(!fwProps.containsKey(key)) {
        final String val = (String) entry.getValue();
        println("Using default " + key + "=" + val, 1);
        fwProps.put(key, val);
      } else {
        println("framework prop " + key + "=" + fwProps.get(key), 1);
      }
    }

    // Set version info
    if(null == fwProps.get(PRODVERSION_PROP)) {
      fwProps.put(PRODVERSION_PROP, version);
    }


    // If jar dir is not specified, default to "file:jars/" and its
    // subdirs
    String jars = (String) fwProps.get(JARDIR_PROP);
    if (jars == null) {
      jars = (String) sysProps.get(JARDIR_PROP);
    }

    if(!(jars == null || "".equals(jars))) {
      println("old jars=" + jars, 1);
    } else {
      String jarBaseDir = topDir + File.separator + "jars";
      println("jarBaseDir=" + jarBaseDir, 2);

      // avoid getAbsoluteFile since some profiles don't have this
      File jarDir = new File(new File(jarBaseDir).getAbsolutePath());
      if(jarDir.exists() && jarDir.isDirectory()) {

        // avoid FileNameFilter since some profiles don't have it
        String [] names = jarDir.list();
        List v = new ArrayList();
        for(int i = 0; i < names.length; i++) {
          File f = new File(jarDir, names[i]);
          if(f.isDirectory()) {
            v.add(names[i]);
          }
        }
        String [] subdirs = new String[v.size()];
        v.toArray(subdirs);

        StringBuffer sb = new StringBuffer();
        sb.append("file:" + jarBaseDir + "/");
        for(int i = 0; i < subdirs.length; i++) {
          sb.append(";file:" + jarBaseDir + "/" + subdirs[i] + "/");
        }
        jars = sb.toString().replace('\\', '/');
        fwProps.put(JARDIR_PROP, jars);
        println("scanned " +JARDIR_PROP +"=" + jars, 2);
      }
    }
  }


  /**
   * Populates the sysProps Map&lt;String,String&gt; with all entries
   * from the system properties object.
   */
  void populateSysProps() {
    final Properties systemProperties = System.getProperties();
    final Enumeration systemPropertiesNames = systemProperties.propertyNames();
    while (systemPropertiesNames.hasMoreElements()) {
      try {
        final String name  = (String) systemPropertiesNames.nextElement();
        final String value = systemProperties.getProperty(name);
        sysProps.put(name, value);
        println("Initial system property: " +name +"=" +value, 3);
      } catch (Exception e) {
        println("Failed to process system property: " +e, 1, e);
      }
    }
  }


  /**
   * Add all entires in the given map to the set of system properties.
   *
   * @param props The map with name value pairs to add to the system
   *              properties.
   */
  void mergeSystemProperties(Map props) {
    final Properties p = System.getProperties();
    p.putAll(props);
    System.setProperties(p);
  }


  /**
   * Do the final processing of framework and system properties before
   * creating the framework instance using them.
   *
   * <ul>
   *   <li>Perform variable expansion on property values.
   *   <li>System properties are then merged into the real system
   *       properties.
   *   <li>If <tt>writeSysProps()</tt> is <tt>true</tt> merge
   *       framework properties into the real system properties.
   *   <li>Add default properties.
   * </ul>
   */
  void finalizeProperties() {
    expandPropValues(sysProps, null);
    expandPropValues(fwProps, sysProps);
    addDefaultProps();

    mergeSystemProperties(sysProps);
    if(writeSysProps()) {
      mergeSystemProperties(fwProps);
      println("merged Framework to System properties " + fwProps, 2);
    }

  }


  /**
   * If any of the values in <tt>toExpand</tt> contains a sub-string
   * like "<tt>${propname}</tt>" replace it with value of the named
   * property in the map if found, else if <tt>fallback</tt> is
   * non-null then replace with the value from that map if any.
   *
   * @param toExpand Map in which to expand variables in the values.
   * @param fallback Optional map to get expansion values from if not
   *                 found in the map to be expanded.
   */
  void expandPropValues(Map toExpand, Map fallback)
  {
    Map all = null;

    for (Iterator eit = toExpand.entrySet().iterator(); eit.hasNext();) {
      final Map.Entry entry = (Map.Entry) eit.next();
      String value = (String) entry.getValue();

      if(-1 != value.indexOf("${")) {
        if (null==all) {
          all = new HashMap();
          if (null!=fallback) {
            all.putAll(fallback);
          }
          all.putAll(toExpand);
        }
        for(Iterator it = all.entrySet().iterator(); it.hasNext();) {
          final Map.Entry allEntry = (Map.Entry) it.next();
          final String rk = "${" + ((String) allEntry.getKey()) + "}";
          final String rv = (String) allEntry.getValue();
          value = Util.replace(value, rk, rv);
        }
        entry.setValue(value);
        println("Expanded property: " +entry.getKey() +"=" +value, 1);
      }
    }
  }


  /**
   * If the last to elements in args "-xargs" or "--xargs" then expand
   * it with arg as argument and replace the last element in args with
   * the expansion. Otherwise just add arg to args.
   * <p>
   * This expansion is necessarry to allow redefinition of a system
   * property after inclusion of xargs-file that sets the same property.
   *
   * @param args The list to add elements to.
   * @param arg  The element to add.
   */
  private void addArg(List args, String arg) {
    if (0==args.size()) {
      args.add(arg);
    } else {
      String lastArg = (String) args.get(args.size()-1);
      if ("-xargs".equals(lastArg) || "--xargs".equals(lastArg)) {
        String[] exArgs = expandArgs( new String[]{ lastArg, arg } );
        args.remove(args.size()-1);
        for (int i=0; i<exArgs.length; i++) {
          args.add(exArgs[i]);
        }
      } else {
        args.add(arg);
      }
    }
  }


  /**
   * Helper method when OS shell does not allow long command lines. This
   * method has now days become the only reasonable way to start the
   * framework due to the amount of properties.
   *
   * <p>
   * Loads a specified file or URL and
   * creates a new String array where each entry corresponds to entries
   * in the loaded file.
   * </p>
   *
   * <p>
   * File format:<br>
   *
   * <ul>
   *  <li>Each line starting with '-D' or '-F' is used dirctly as an
   *      entry in the new command line array.
   *  <li>Each line of length zero is ignored.
   *  <li>Each line starting with '#' is ignored.
   *  <li>Lines starting with '-' is used a command with optional argument
   *      after space.
   *  <li>All other lines is used directly as an entry to the new
   *      command line array.
   * </ul>
   * </p>
   *
   * @param xargsPath The URL to load the xargs-file from. The URL
   *                  protcoll defaults to "file:". File URLs are
   *                  first search for in the parent directory of the
   *                  current FW-dir, then in the current working directory.
   * @param oldArgs   The command line arguments as it looks before
   *                  the file named in <tt>xargsPath</tt> have been
   *                  expanded.
   * @return array with command line options loaded from
   *         <tt>xargsPath</tt> suitable to be merged into
   *         <tt>argv</tt> by the caller.
   */
  String [] loadArgs(String xargsPath, String[] oldArgs) {
    if(XARGS_DEFAULT.equals(xargsPath)) {
      processProperties(oldArgs);
      xargsPath = getDefaultXArgs();
    }

    // out result
    final List v = new ArrayList();

    BufferedReader in = null;
    try {

      // Check as file first, then as a URL
      println("Searching for xargs file with '" +xargsPath +"'.", 2);

      // 1) Search in parent dir of the current framework directory
      final String fwDirStr = Util.getFrameworkDir(fwProps);

      // avoid getAbsoluteFile() since some profiles don't have this
      final File fwDir      = new File(new File(fwDirStr).getAbsolutePath());

      // avoid getParentFile() since some profiles don't have this
      final String defDirStr = fwDir.getParent();
      final File   defDir    = defDirStr != null ? new File(defDirStr) : null;
      if (null!=defDir) {
        // Make the file object absolute before calling exists(), see
        // http://forum.java.sun.com/thread.jspa?threadID=428403&messageID=2595075
        // for details.
        final File f = new File(new File(defDir,xargsPath).getAbsolutePath());
        println(" trying " +f, 5);
        if(f.exists()) {
          println("Loading xargs file " + f, 1);
          in = new BufferedReader(new FileReader(f));
        }
      }

      // 2) Search in the current working directory
      if (null==in) {
        // Make the file object absolute before calling exists(), see
        // http://forum.java.sun.com/thread.jspa?threadID=428403&messageID=2595075
        // for details.
        final File f = new File(new File(xargsPath).getAbsolutePath());
        println(" trying " +f, 5);
        if(f.exists()) {
          println("Loading xargs file " + f, 1);
          in = new BufferedReader(new FileReader(f));
        }
      }

      // 3) Try argument as URL
      if(in == null) {
        try {
          println(" trying URL " +xargsPath, 5);
          final URL url = new URL(xargsPath);
          println("Loading xargs url " + url, 0);
          in = new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (MalformedURLException e)  {
          throw new IllegalArgumentException("Bad xargs URL " + xargsPath +
                                             ": " + e);
        }
      }

      StringBuffer contLine = new StringBuffer();
      String       line     = null;
      String       tmpline  = null;
      int          lineno   = 0;
      for(tmpline = in.readLine(); tmpline != null;
          tmpline = in.readLine()) {
        lineno++;
        tmpline = tmpline.trim();

        // check for line continuation char and
        // build up line until a line without such a mark is found.
        if(tmpline.endsWith("\\")) {
          // found continuation mark, store actual line to
          // buffered continuation line
          tmpline = tmpline.substring(0, tmpline.length() - 1);
          if(contLine == null) {
            contLine = new StringBuffer(tmpline);
          } else {
            contLine.append(tmpline);
          }
          // read next line
          continue;
        } else {
          // No continuation mark, gather stored line + newly read line
          if(contLine != null) {
            contLine.append(tmpline);
            line     = contLine.toString();
            contLine = null;
          } else {
            // this is the normal case if no continuation char is found
            // or any buffered line is found
            line = tmpline;
          }
        }

        if(line.startsWith("-D")) {
          // Preserve System property
          addArg(v,line);
        } else if(line.startsWith("-F")) {
          // Preserve framework property
          addArg(v,line);
        } else if(line.startsWith("#")) {
          // Ignore comments
        } else if(line.startsWith("-")) {
          // Split command that contains a ' ' int two args
          int i = line.indexOf(' ');
          if (i != -1) {
            addArg(v, line.substring(0,i));
            line = line.substring(i).trim();
            if(line.length() > 0) {
              addArg(v, line);
            }
          } else {
            addArg(v, line);
          }
        } else if(line.length() > 0) {
          // Add argument
          addArg(v,line);
        }
      }

      // Write to framework properties. This should be the primary
      // source for all code, including the framework itself.
      // framework.props.setProperties(sysProps);

    } catch (Exception e) {
      if(e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      throw new IllegalArgumentException("xargs loading failed: " + e);
    } finally {
      if (null!=in) {
        try {
          in.close();
        } catch (IOException ignore) { }
      }
    }

    final String [] args2 = new String[v.size()];
    v.toArray(args2);

    return args2;
  }


  // If a splash screen hash been shown, try to close it.
  void closeSplash() {
    // User reflection, and ignore errors since this is only supported
    // in Java SE 6.
    try {
      final Class splashScreenCls = Class.forName("java.awt.SplashScreen");
      final Method getSplashScreenMethod
        = splashScreenCls.getMethod("getSplashScreen", null);
      final Object splashScreen = getSplashScreenMethod.invoke(null,null);
      if (null!=splashScreen) {
        final Method closeMethod = splashScreenCls.getMethod("close", null);
        closeMethod.invoke(splashScreen, null);
      }
    } catch (Exception e) {
      // Ignore any error.
      println("close splash screen: ", 6, e);
    }
  }


  /**
   * Print string to System.out if level >= current verbosity.
   *
   * @param s String to print.
   * @param level print level.
   */
  void println(String s, int level) {
    println(s, level, null);
  }


  void println(String s, Bundle b) {
    println(s + b.getLocation() + " (id#" + b.getBundleId() + ")", 1);
  }


  void println(String s, int level, Exception e) {
    if(verbosity >= level) {
      System.out.println((level > 0 ? ("#" + level + ": ") : "") + s);
      if(e != null) {
        e.printStackTrace();
      }
    }
  }


  /**
   * Report error and exit.
   */
  void error(final String s) {
    error(s, null);
  }


  void error(final String s, final Throwable t) {
    System.err.println("Error: " + s);
    if(t != null) {
      t.printStackTrace();
    }
    System.exit(1);
  }
}
