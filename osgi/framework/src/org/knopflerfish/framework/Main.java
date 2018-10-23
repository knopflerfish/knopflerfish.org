/*
 * Copyright (c) 2003-2015, KNOPFLERFISH project
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.FrameworkStartLevel;

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

  // Save beginning start level as long as true
  boolean saveStartLevel = true;

  // will be initialized by main() - up for anyone for grabbing
  public String bootText;

  // Framework properties, i.e., configuration properties from -Fkey=value
  Map<String, String> fwProps = new HashMap<String, String>();

  // System properties, i.e., configuration properties from -Dkey=value
  Map<String, String> sysProps = new HashMap<String, String>();

  static public final String JARDIR_PROP    = "org.knopflerfish.gosg.jars";
  static public final String JARDIR_DEFAULT = "file:jars/;"
      + FWResourceURLStreamHandler.PROTOCOL + ":jars/";

  static public final String XARGS_INIT     = "init.xargs";
  static public final String XARGS_RESTART  = "restart.xargs";
  static public final String FWPROPS_XARGS  = "fwprops.xargs";

  static public final String CMDIR_PROP    = "org.knopflerfish.bundle.cm.store";
  static public final String CMDIR_DEFAULT = "cmdir";

  static public final String VERBOSITY_PROP    = "org.knopflerfish.framework.main.verbosity";
  static public final String VERBOSITY_DEFAULT = "0";

   /**
   * Name of framework property controlling whether to write an
   * FWPROPS_XARGS file or not at startup.
   */
  static public final String WRITE_FWPROPS_XARGS_PROP =
    "org.knopflerfish.framework.main.write.fwprops.xargs";

  static public final String XARGS_WRITESYSPROPS_PROP =
    "org.knopflerfish.framework.main.xargs.writesysprops";

  static public final String XARGS_DEFAULT     = "default";

  static public final String PRODVERSION_PROP     = "org.knopflerfish.prodver";

  static public final String BOOT_TEXT_PROP
    = "org.knopflerfish.framework.main.bootText";

  /**
   * Default values for some framework properties.
   */
  Map<String, String> defaultFwProps = new HashMap<String,String>() {
    private static final long serialVersionUID = 1L;
  {
    put(CMDIR_PROP,    CMDIR_DEFAULT);
  }};


  FrameworkFactory ff;
  Framework framework;

  /**
   * Help class for starting the OSGi framework.
   */
  public static void main(String[] args) {
    main = new Main();

    System.out.println(main.bootText);

    main.start(args);
    System.exit(0);
  }


  public Main() {
    try { // Set the initial verbosity level.
      final String vpv = System.getProperty(VERBOSITY_PROP);
      verbosity = Integer.parseInt(null==vpv ? VERBOSITY_DEFAULT: vpv);
    } catch (final Exception ignored) { }
    populateSysProps();
    // Setup URLStremFactory so that we can use fwresource:
    FrameworkContext.setupURLStreamHandleFactory();
    final String tstampYear = Util.readTstampYear();

    bootText =
      "Knopflerfish OSGi framework launcher, version " + version + "\n" +
      "Copyright 2003-" +tstampYear +" Knopflerfish. All Rights Reserved.\n" +
      "See http://www.knopflerfish.org for more information.\n";
  }


  public Framework start(String[] args) {
    version = Util.readFrameworkVersion();
    // Check if framework is started with no args at all.
    // This typically happens when starting with "java -jar framework.jar"
    // or similar (e.g by double-clicking on framework.jar)
    bZeroArgs = (args.length == 0);
    if (!bZeroArgs) {// Also true if started with only -D/-F/-ff args
      bZeroArgs = true;
      for (int i=0; bZeroArgs && i<args.length; i++) {
        // -Dx=y, -Fx=y -init and -ff class does not count as args
        if ("-ff".equals(args[i])) {
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
        } else {
          bZeroArgs = args[i].startsWith("-D")
            || args[i].startsWith("-F")
            || "-init".equals(args[i]);
        }
      }
    }

    processProperties(args);

    if (bZeroArgs) {// Add default xargs file to command line
      // To determine the fwdir we must process all -D/-F definitions
      // on the current command line.
      if (0==args.length) {
        args = new String[] {"--xargs", XARGS_DEFAULT};
      } else {
        final String[] newArgs = new String[args.length +2];
        newArgs[0] = "--xargs";
        newArgs[1] = XARGS_DEFAULT;
        System.arraycopy(args, 0, newArgs, 2, args.length);
        args = newArgs;
      }
    }

    args = expandArgs(args);
    return handleArgs(args);
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
    String factoryClassName = FrameworkFactoryImpl.class.getName();
    try {
      final String factoryS = new String(Util.readResource("/META-INF/services/org.osgi.framework.launch.FrameworkFactory"), "UTF-8");
      final String[] w = Util.splitwords(factoryS, "\n\r");
      for(int i = 0; i < w.length; i++) {
        if(w[i].length() > 0 && !w[i].startsWith("#")) {
          factoryClassName = w[i].trim();
          break;
        }
      }

    } catch (final Exception e) {
      // META-INF/services may be lost when putting framework in Android .apk
      println("failed to get FrameworkFactory, using default", 6, e);
    }
    return getFrameworkFactory(factoryClassName);
  }


  public FrameworkFactory getFrameworkFactory(String factoryClassName) {
    try {
      println("getFrameworkFactory(" + factoryClassName + ")", 2);
      final Class<?> clazz = Class.forName(factoryClassName);
      final Constructor<?> cons  = clazz.getConstructor(new Class[] { });
      final FrameworkFactory ff = (FrameworkFactory) cons.newInstance(new Object[] { });
      return ff;
    } catch (final Exception e) {
      error("failed to create " + factoryClassName, e);
      throw new RuntimeException("failed to create " + factoryClassName + ": " + e);
    }
  }


  URL[] getJarBase() {
    assertFramework();
    String jars = framework.getBundleContext().getProperty(JARDIR_PROP);
    if(jars == null) {
      jars = JARDIR_DEFAULT;
    }

    final String[] ja = Util.splitwords(jars, ";");
    ArrayList<URL> res = new ArrayList<URL>();
    for (int i=0; i<ja.length; i++) {
      final String u = ja[i].trim();
      try {
        res.add(ReferenceURLStreamHandler.createURL(u));
        println("jar base[" + i + "]=" + u, 3);
      } catch (final MalformedURLException ignored) {
        System.err.println("Skip illegal jar base: " + u);
      }
    }

    return res.toArray(new URL[res.size()]);
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
  void setProperty(String prefix, String arg, Map<String, String> props) {
    final int ix = arg.indexOf("=");
    if(ix != -1) {
      final String key = arg.substring(2, ix);
      final String value = arg.substring(ix + 1);

      println(prefix +key +"=" +value, 1);
      props.put(key,value);

      if (Constants.FRAMEWORK_BEGINNING_STARTLEVEL.equals(key)) {
        saveStartLevel = false;
      }
      if (VERBOSITY_PROP.equals(key)) {
        // redo this here since verbosity level may have changed
        try {
          final int old = verbosity;
          verbosity = Integer.parseInt( value.length()==0
                                        ? VERBOSITY_DEFAULT : value);
          if (old != verbosity) {
            println("Verbosity changed to "+verbosity, 1);
          }
        } catch (final Exception ignored) {}
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
        } else if ("-launch".equals(args[i])) {
          saveStartLevel = false;
        } else if (saveStartLevel && i+1 < args.length && "-startlevel".equals(args[i])) {
          fwProps.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, args[i+1]);
          saveStartLevel = false;
        }
      } catch (final Exception e) {
        e.printStackTrace(System.err);
        error("Command \"" +args[i] +"\" failed, " + e.getMessage());
      }
    }
  }


  /**
   * Save all framework properties as an xargs-file in the framework
   * directory for restarts.
   */
  private void save_restart_props(Map<String, String> props) {
    final String ro = framework.getBundleContext().getProperty(FWProps.READ_ONLY_PROP);
    if ("true".equalsIgnoreCase(ro)) {
      return;
    }
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
      for (final Entry<String, String> entry : props.entrySet()) {
        final String key   = entry.getKey();
        final String value = entry.getValue();

        // Don't save clean onFirstInit
        if (!Constants.FRAMEWORK_STORAGE_CLEAN.equals(key) ||
            !Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT.equals(value)) {
          pr.println("-F" +key +"=" +value);
        }
      }
    } catch (final Exception e) {
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

        // Make the boot text available to any bundle (e.g., desktop).
        // Added it here since we can not save multiline properties.
        fwProps.put(BOOT_TEXT_PROP, bootText);

      } catch (final BundleException be) {
        error("Failed to initialize the framework: " +be.getMessage(), be);
      }
      println("Framework class " + framework.getClass().getName(), 1);
      System.out.println("Created Framework: " + framework.getSymbolicName()
                         + ", version=" + framework.getVersion() + ".");
    }
  }


  /**
   * Handle command line options.
   *
   * @param args argument line
   */
  private Framework handleArgs(String[] args) {
    boolean bLaunched = false;

    // If not init, check if we have saved framework properties
    if (!Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
        .equals(fwProps.get(Constants.FRAMEWORK_STORAGE_CLEAN))) {
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

    boolean bg = false;
    // Process all other options.
    for (int i = 0; i < args.length; i++) {
      try {
        if ("-bg".equals(args[i])) {
          println("Background mode.", 0);
          bg  = true;
        } else if ("-exit".equals(args[i])) {
          println("Exit.", 0);
          System.exit(0);
        } else if (args[i].startsWith("-F")) {
          // Skip, handled in processProperties(String[])
        } else if (args[i].startsWith("-D")) {
          // Skip, handled in processProperties(String[])
        } else if ("-init".equals(args[i])) {
          // Skip, handled in processProperties(String[])
        } else if ("-version".equals(args[i])) {
          System.out.println("Knopflerfish release: " + readRelease());
          System.out.println("Framework version: " + Util.readFrameworkVersion());
          printResource("/tstamp");
          System.exit(0);
        } else if ("-help".equals(args[i])) {
          printResource("/help.txt");
          System.exit(0);
        } else if ("-jvminfo".equals(args[i])) {
          assertFramework();
          printJVMInfo(framework);
          System.exit(0);
        } else if ("-ff".equals(args[i])) {
          // Already handled; skip class name
          i++;
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
          bLaunched = doLaunch();
        } else if ("-shutdown".equals(args[i])) {
          if (i+1 < args.length) {
            i++;
            final long timeout = Long.parseLong(args[i]);
            try {
              if(framework != null) {
                framework.stop();
                final FrameworkEvent stopEvent = framework.waitForStop(timeout);
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
            } catch (final Exception e) {
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
            } catch (final InterruptedException e) {
              error("Sleep interrupted.");
            }
            i++;
          } else {
            error("No time for sleep command");
          }
        } else if ("-start".equals(args[i])) {
          i = doStartBundle(args, i, Bundle.START_ACTIVATION_POLICY, "Started (policy): ");
        } else if ("-start_e".equals(args[i])) {
          i = doStartBundle(args, i, 0, "Started (eager): ");
        } else if ("-start_et".equals(args[i])) {
          i = doStartBundle(args, i, Bundle.START_TRANSIENT, "Started (eager,transient): ");
        } else if ("-start_pt".equals(args[i])) {
          i = doStartBundle(args, i, Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY,
                            "Start (policy,transient): ");
        } else if ("-stop".equals(args[i])) {
          i = doStopBundle(args, i, 0);
        } else if ("-stop_t".equals(args[i])) {
          i = doStopBundle(args, i, Bundle.STOP_TRANSIENT);
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
              final Bundle b = bl[n];
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
            final FrameworkStartLevel fsl = framework.adapt(FrameworkStartLevel.class);
            if (fsl!=null) {
              fsl.setInitialBundleStartLevel(n);
            }
            i++;
          } else {
            error("No integer level for initlevel command");
          }
        } else if ("-startlevel".equals(args[i])) {
          assertFramework();
          if (i+1 < args.length) {
            final int n = Integer.parseInt(args[i+1]);
            if ((Bundle.ACTIVE&framework.getState()) != 0) {
              final FrameworkStartLevel fsl = framework.adapt(FrameworkStartLevel.class);
              if (fsl!=null) {
                fsl.setStartLevel(n);
              }
            } else {
              error("Startlevel command not available before framework started.\n" +
                    "Use property 'org.osgi.framework.startlevel.beginning' instead");
            }
            i++;
          } else {
            error("No integer level for startlevel command");
          }
        } else {
          error("Unknown option: " + args[i] +
                "\nUse option -help to see all options");
        }
      } catch (final BundleException e) {
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
      } catch (final Exception e) {
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
        bLaunched = doLaunch();
      } catch (Throwable t) {
        if (t instanceof BundleException) {
          final BundleException be = (BundleException) t;
          final Throwable ne = be.getNestedException();
          if (ne != null) t = ne;
        }
        error("Framework launch failed, " + t.getMessage(), t);
      }
    }
    if (bg) {
      return framework;
    }
    FrameworkEvent stopEvent = null;
    while (true) { // Ignore interrupted exception.
      try {
        stopEvent = framework.waitForStop(0L);
        switch (stopEvent.getType()) {
        case FrameworkEvent.STOPPED:
          // FW terminated, Main is done!
          println("Framework terminated", 0);
          return framework;
        case FrameworkEvent.STOPPED_UPDATE:
          // Automatic FW restart, wait again.
          break;
        case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
          // A manual FW restart with new boot class path is needed.
          return framework;
        case FrameworkEvent.ERROR:
          // Stop failed or other error, give up.
          error("Fatal framework error, terminating.",
                stopEvent.getThrowable());
          return framework;
        case FrameworkEvent.WAIT_TIMEDOUT:
          // Should not happen with timeout==0, give up.
          error("Framework waitForStop(0) timed out!",
                stopEvent.getThrowable());
          break;
        }
      } catch (final InterruptedException ie) { }
    }
  }


  private int doStopBundle(final String[] args, int i, final int options)
  {
    assertFramework();
    if (i+1 < args.length) {
      final long id = getBundleID(framework, args[i+1]);
      final Bundle b = framework.getBundleContext().getBundle(id);
      try {
        b.stop(options);
        println("Stopped: ", b);
      } catch (final Exception e) {
        error("Failed to stop", e);
      }
      i++;
    } else {
      error("No ID for stop command");
    }
    return i;
  }


  private boolean doLaunch()
      throws BundleException
  {
    boolean bLaunched;
    if (null!=framework && (Bundle.ACTIVE&framework.getState())!=0) {
      throw new IllegalArgumentException
        ("a framework instance is already active.");
    }
    assertFramework();
    framework.start();
    bLaunched = true;
    closeSplash();
    println("Framework launched", 0);
    return bLaunched;
  }


  private int doStartBundle(final String[] args, int i, final int options, final String logMsg)
      throws BundleException
  {
    assertFramework();
    if (i+1 < args.length) {
      final long id = getBundleID(framework, args[i+1]);
      final Bundle b = framework.getBundleContext().getBundle(id);
      b.start(options);
      println(logMsg, b);
      i++;
    } else {
      error("No ID for start command");
    }
    return i;
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
    } catch (final NumberFormatException nfe) {
      final Bundle[] bl = fw.getBundleContext().getBundles();
      final String loc = completeLocation(idLocation);
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
    // Handle file: case where topDir is not ""
    if(location.startsWith("file:jars/") && !topDir.equals("")) {
      location = ("file:" + topDir + "/" + location.substring(5)).replace('\\', '/');
      println("mangled bundle location to " + location, 2);
    }
    final int ic = location.indexOf(":");
    if (ic<2 || ic>location.indexOf("/")) {
      println("location=" + location, 2);
      final URL[] base = getJarBase();
      // URL without protocol complete it.
      for (int i=0; i<base.length; i++) {
        println("base[" + i + "]=" + base[i], 2);
        try {
        	final URL url = new URL(base[i], location);

          println("check " + url, 2);
          if ("file".equals(url.getProtocol())) {
            final File f = new File(url.getFile()).getAbsoluteFile();
            if (!f.exists() || !f.canRead()) {
              continue; // Noope; try next.
            }
          } else if ("http".equals(url.getProtocol())) {
            final HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            uc.connect();
            final int rc = uc.getResponseCode();
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
        } catch (final Exception _ignore) { }
      }
    }
    return location;
  }


  /**
   * Expand all occurrence of <tt>-xarg &lt;URL&gt;</tt> and <tt>--xarg
   * &lt;URL&gt;</tt> into a new array without any <tt>-xargs</tt>,
   * <tt>--xargs</tt>.
   *
   * @param argv array of command line options to expand all
   *             <tt>-xarg &lt;URL&gt;</tt> options in.
   * @return New argv array where all <tt>-xarg &lt;URL&gt;</tt>
   *         options have been expanded.
   */
  String[] expandArgs(String[] argv) {
    final List<String> v = new ArrayList<String>();
    int i = 0;
    while(i < argv.length) {
      if ("-xargs".equals(argv[i]) || "--xargs".equals(argv[i])) {
        // if "--xargs", ignore any load errors of xargs file
        final boolean bIgnoreException = argv[i].equals("--xargs");
        if (i+1 < argv.length) {
          final String   xargsPath = argv[i+1];
          i++;
          try {
            final String[] moreArgs = loadArgs(xargsPath, argv);
            final String[] r = expandArgs(moreArgs);
            for (final String element : r) {
              v.add(element);
            }
          } catch (final RuntimeException e) {
            if(bIgnoreException) {
              println("Failed to load --xargs " + xargsPath, 1, e);
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
    final String[] r = new String[v.size()];

    v.toArray(r);
    return r;
  }


  /**
   * Print help for starting the platform.
   */
  void printResource(String name) {
    try {
      System.out.println(new String(Util.readResource(name)));
    } catch (final Exception e) {
      System.out.println("No resource '" + name + "' available");
    }
  }


  /**
   * Print help for starting the platform.
   */
  void printJVMInfo(Framework framework) {
    try {
      final Properties props = System.getProperties();
      System.out.println("--- System properties ---");
      for(final Enumeration<?> e = props.keys(); e.hasMoreElements(); ) {
        final String key = (String) e.nextElement();
        System.out.println(key + ": " + props.get(key));
      }

      System.out.println("\n");
      System.out.println("--- Framework properties ---");

      final String keyStr = framework.getBundleContext().getProperty("org.knopflerfish.framework.bundleprops.keys");
      final String[] keys = Util.splitwords(keyStr != null ? keyStr : "", ",");

      for (final String key : keys) {
        System.out.println(key + ": " + framework.getBundleContext().getProperty(key));
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }


  // should this be read from the manifest instead?
  static String readRelease() {
    return Util.readResource("/release", "0.0.0.snapshot", "UTF-8");
  }


  /**
   * Helper method which tries to find a default xargs files to use.
   * Note: Make sure that fwProps are up to date by calling
   * processProperties(args) before calling this method.
   *
   * @throws IOException 
   */
  BufferedReader getDefaultXArgs() throws IOException {
    boolean bInit = Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
      .equals(fwProps.get(Constants.FRAMEWORK_STORAGE_CLEAN));

    final String fwDirStr = Util.getFrameworkDir(fwProps);
    // avoid getAbsoluteFile since some profiles don't have this
    final File fwDir      = new File(new File(fwDirStr).getAbsolutePath());
    println("fwdir is " + fwDir, 1);

    if (!bInit) { // Implicit init needed?
      bInit = !(fwDir.exists() && fwDir.isDirectory());
      if (bInit) println("Implicit -init since fwdir does not exist.", 2);
    }
    println("init is "  +bInit, 2);

    // avoid getParentFile since some profiles don't have this
    final String defDirStr = fwDir.getParent();
    final File   defDir    = defDirStr != null ? new File(defDirStr) : null;

    println("defDir=" +defDir, 5);

    final File[] dirs = null!=defDir ?
      new File[]{ fwDir, defDir } : new File[]{ fwDir };

    // Determine the root OSGi dir, a.k.a. topDir
    for (final File dir : dirs) {
      final File jarsDir = new File(dir,"jars");
      if (jarsDir.exists() && jarsDir.isDirectory()) {
        topDir = dir.getAbsolutePath();
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
        final File xargsFile = new File(dirs[i], xargNames[k]);
        println("  trying " +xargsFile.getAbsolutePath(), 5);
        if (xargsFile.exists()) {
          xargsFound = xargsFile.getAbsolutePath();
          break xargsSearch;
        }
      }
    }
    if (xargsFound != null) {
      println("default xargs file is " + xargsFound, 2);
      return getXargsReader(xargsFound);
    } else {
      IOException failure = null;
      for (int i = 0; i<xargNames.length; i++) {
        try {
          return getXargsReader(xargNames[i]);
        } catch (IOException e) {
          failure = e;
        }
      }
      throw failure;
    }
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
    for (final Entry<String, String> entry : defaultFwProps.entrySet()) {
      final String key = entry.getKey();
      if(!fwProps.containsKey(key)) {
        final String val = entry.getValue();
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
    String jars = fwProps.get(JARDIR_PROP);
    if (jars == null) {
      jars = sysProps.get(JARDIR_PROP);
    }

    if(!(jars == null || "".equals(jars))) {
      println("old jars=" + jars, 1);
    } else {
      final String jarBaseDir = topDir + File.separator + "jars";
      println("jarBaseDir=" + jarBaseDir, 2);

      // avoid getAbsoluteFile since some profiles don't have this
      final File jarDir = new File(new File(jarBaseDir).getAbsolutePath());
      if(jarDir.exists() && jarDir.isDirectory()) {

        // avoid FileNameFilter since some profiles don't have it
        final String [] names = jarDir.list();
        final List<String> v = new ArrayList<String>();
        for (final String name : names) {
          final File f = new File(jarDir, name);
          if(f.isDirectory()) {
            v.add(name);
          }
        }
        final String [] subdirs = new String[v.size()];
        v.toArray(subdirs);

        final StringBuffer sb = new StringBuffer();
        sb.append("file:" + jarBaseDir + "/");
        for (final String subdir : subdirs) {
          sb.append(";file:" + jarBaseDir + "/" + subdir + "/");
        }
        sb.append(FWResourceURLStreamHandler.PROTOCOL + ":jars/");
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
    final Enumeration<?> systemPropertiesNames = systemProperties.propertyNames();
    while (systemPropertiesNames.hasMoreElements()) {
      try {
        final String name  = (String) systemPropertiesNames.nextElement();
        final String value = systemProperties.getProperty(name);
        sysProps.put(name, value);
        if (Constants.FRAMEWORK_BEGINNING_STARTLEVEL.equals(name)) {
          saveStartLevel = false;
        }
        println("Initial system property: " +name +"=" +value, 3);
      } catch (final Exception e) {
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
  void mergeSystemProperties(Map<String, String> props) {
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
  void expandPropValues(Map<String, String> toExpand, Map<String, String> fallback)
  {
    Map<String, String> all = null;

    for (final Entry<String, String> entry : toExpand.entrySet()) {
      String value = entry.getValue();

      if(-1 != value.indexOf("${")) {
        if (null==all) {
          all = new HashMap<String, String>();
          if (null!=fallback) {
            all.putAll(fallback);
          }
          all.putAll(toExpand);
        }
        for (final Entry<String, String> allEntry : all.entrySet()) {
          final String rk = "${" + allEntry.getKey() + "}";
          final String rv = allEntry.getValue();
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
  private void addArg(List<String> args, String arg) {
    if (0==args.size()) {
      args.add(arg);
    } else {
      final String lastArg = args.get(args.size()-1);
      if ("-xargs".equals(lastArg) || "--xargs".equals(lastArg)) {
        final String[] exArgs = expandArgs( new String[]{ lastArg, arg } );
        args.remove(args.size()-1);
        for (final String exArg : exArgs) {
          args.add(exArg);
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
    // out result
    final List<String> v = new ArrayList<String>();

    BufferedReader in = null;
    try {
      if (XARGS_DEFAULT.equals(xargsPath)) {
        processProperties(oldArgs);
        in = getDefaultXArgs();
      } else {
        in = getXargsReader(xargsPath);
      }
      StringBuffer contLine = new StringBuffer();
      String       line     = null;
      String       tmpline  = null;
      for(tmpline = in.readLine(); tmpline != null;
          tmpline = in.readLine()) {
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
          final int i = line.indexOf(' ');
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

    } catch (final Exception e) {
      if(e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      throw new IllegalArgumentException("xargs loading failed: " + e);
    } finally {
      if (null!=in) {
        try {
          in.close();
        } catch (final IOException ignore) { }
      }
    }

    final String [] args2 = new String[v.size()];
    v.toArray(args2);

    return args2;
  }


  private BufferedReader getXargsReader(String xargsPath)
      throws IOException {
    BufferedReader in = null;
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

    // 3) Try argument as URL if it contains ':'.
    if (in == null && xargsPath.indexOf(':') != -1) {
      try {
        println(" trying URL " +xargsPath, 5);
        final URL url = new URL(xargsPath);
        println("Loading xargs url " + url, 1);
        in = new BufferedReader(new InputStreamReader(url.openStream()));
      } catch (final MalformedURLException _ignore)  { }
    }
    // 4) Try as resource
    if (null==in) {
      ClassLoader cl = getClass().getClassLoader();
      while (cl != null) {
        InputStream is = cl.getResourceAsStream(xargsPath);
        if (is != null) {
          in = new BufferedReader(new InputStreamReader(is));
          break;
        }
        cl = cl.getParent();
      }
    }
    if (null==in) {
      throw new FileNotFoundException("Didn't find xargs file: " + xargsPath);
    }
    return in;
  }


  // If a splash screen hash been shown, try to close it.
  void closeSplash() {
    // User reflection, and ignore errors since this is only supported
    // in Java SE 6.
    try {
      final Class<?> splashScreenCls = Class.forName("java.awt.SplashScreen");
      final Method getSplashScreenMethod
        = splashScreenCls.getMethod("getSplashScreen", (Class[]) null);
      final Object splashScreen = getSplashScreenMethod.invoke( (Object) null,
                                                                (Object[])null);
      if (null!=splashScreen) {
        final Method closeMethod = splashScreenCls.getMethod("close",
                                                             (Class[]) null);
        closeMethod.invoke(splashScreen, (Object[]) null);
      }
    } catch (final Exception e) {
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
