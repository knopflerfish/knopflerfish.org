/*
 * Copyright (c) 2003, KNOPFLERFISH project
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
import java.util.Vector;
import java.util.StringTokenizer;
import java.security.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import org.osgi.framework.*;

import java.lang.reflect.Constructor;

/**
 * This is the main startup code for the framework and enables
 * basic operations as install, start, stop, uninstall
 * and update.
 *
 * @author Jan Stein
 */
public class Main {

  static Framework framework;

  static long bootMgr = 0;

  // Verbosity level of printouts. 0 is low.
  static int verbosity = 0;

  // Will be filled in from manifest entry during startup
  static String version = "<unknown>";

  // Top directory (including any trailing /)
  static String topDir              = "";

  // Base config directory which will be used if zero args are supplied
  // to main(). If this file does not exist, continue as usual.
  //  static String defaultInstDir      = topDir + "inst" + File.separator + "gdsp";
  static String defaultInstDir      = ".";

  // Xargs to use for FW init
  static String defaultXArgsInit    = "init.xargs";

  // Xargs to use for FW init
  static String defaultXArgsInit2   = "remote-init.xargs";

  // Xargs to use for FW restart
  static String defaultXArgsStart   = "restart.xargs";


  // Set to true if JVM is started without any arguments
  static boolean bZeroArgs          = false;

  // will be initialized by main() - up for anyone for grabbing
  public static String bootText = "";

  /**
   * Help class for starting the OSGi framework.
   */
  public static void main(String[] args) {
    try { 
      verbosity = 
	Integer.parseInt(System.getProperty("org.knopflerfish.verbosity", "0"));
    } catch (Exception ignored) { }

    version = readVersion();

    bootText = 
      "Knopflerfish OSGi framework, version " + version + "\n" + 
      "Copyright 2003-2004 Knopflerfish. All Rights Reserved.\n\n" + 
      "See http://www.knopflerfish.org for more information.";

    System.out.println(bootText);

    // Check if framework is started with no args at all.
    // This typically happens when starting with "java -jar framework.jar"
    // or similar (e.g by double-clicking on framework.jar)
    bZeroArgs = (args.length == 0);

    // Check if there is a default xargs file
    // Uses "init" variant if fwdir exists, otherwise
    // uses "start" variant.
    if(bZeroArgs) {
      args = tryDefaultXArgs();
    }

    // Support for arguments located in separate file
    if(args.length >= 2 && args[0].equals("-xargs")) {
      args = loadArgs(args);
    }

    // redo this since it might have changed
    try { verbosity = Integer.parseInt(System.getProperty("org.knopflerfish.verbosity", "0"));
    } catch (Exception ignored) { }

    if(bZeroArgs) {
      // Make sure we have a minimal setup of args
      args = sanityArgs(args);

    }

    // Set default values to something reasonable if not supplied
    // on the command line (or in xargs)
    setDefaultSysProps();

    String jars = System.getProperty("org.knopflerfish.gosg.jars", "file:");

    String[] base = Util.splitwords(jars, ";", '\"');
    for (int i=0; i<base.length; i++) {
      try {
	base[i] = new URL(base[i]).toString();
      } catch (Exception ignored) {
      }
      println("jars base[" + i + "]=" + base[i], 3); 
    }
    

    int i = 0;
    for (; i < args.length; i++) {
      if ("-help".equals(args[i])) {
	printHelp();
	
	// exit() added by EW because conflict with default
	// values and "expected" behavoir of -help
	System.exit(0);
      } else if ("-init".equals(args[i])) {
	String d = System.getProperty("org.osgi.framework.dir");
	
	FileTree dir = (d != null) ? new FileTree(d) : null;
	if (dir != null) {
	  dir.delete();
	}
	println("Removed all existing bundles.", 0);
      } else {
	break;
      }
    }

    try {
      framework = new Framework(new Main());
    } catch (Exception e) {
      e.printStackTrace();
      error("New Framework failed!");
    }
    
    // If no run commands assume launch
    if (args.length == i) {
      args = new String [] { "-launch" };
      i = 0;
    }

    // Save these for possible restart()
    initArgs    = args;
    initOffset  = i;
    initBase    = base;

    handleArgs(args, i, base);
  }

  static String[] initArgs   = null;
  static int      initOffset = 0;
  static String[] initBase   = null;


  /**
   * Handle arg line options.
   *
   * @param args argument line
   * @param startOffset index to start from in argv
   */
  private static void handleArgs(String[] args, 
				 int startOffset,
				 String[] base) {
    for (int i = startOffset; i < args.length; i++) {
      try {
	if ("-exit".equals(args[i])) {
	  println("Exit.", 0);
	  System.exit(0);
	} else if ("-install".equals(args[i])) {
	  if (i+1 < args.length) { 
	    String bundle = args[i+1];
	    long id = framework.installBundle(completeLocation(base,bundle), null);
	    println("Installed: " + framework.getBundleLocation(id) + " (id#" + id + ")", 0);
	    i++;
	  } else {
	    error("No URL for install command");
	  }
	} else if ("-istart".equals(args[i])) {
	  if (i+1 < args.length) { 
	    String bundle = args[i+1];
	    long id = framework.installBundle(completeLocation(base,bundle), null);
	    framework.startBundle(id);
	    println("Installed and started: " + framework.getBundleLocation(id) + " (id#" + id + ")", 0);
	    i++;
	  } else {
	    error("No URL for install command");
	  }
	} else if ("-launch".equals(args[i])) {
	  if (i+1 < args.length && !args[i+1].startsWith("-")) { 
	    bootMgr = Long.parseLong(args[i+1]);
	    framework.launch(bootMgr);
	    i++;
	  } else {
	    framework.launch(0);
	  }
	  println("Framework launched", 0);
	} else if ("-shutdown".equals(args[i])) {
	  framework.shutdown();
	  println("Framework shutdown", 0);
	} else if ("-sleep".equals(args[i])) {
	  if (i+1 < args.length) { 
	    long t = Long.parseLong(args[i+1]);
	    try {
	      println("Sleeping...", 0);
	      Thread.sleep(t * 1000);
	    } catch (InterruptedException e) {
	      error("Sleep interrupted.");
	    }
	    i++;
	  } else {
	    error("No time for sleep command");
	  }
	} else if ("-start".equals(args[i])) {
	  if (i+1 < args.length) { 
	    long id = getBundleID(base,args[i+1]);
	    framework.startBundle(id);
	    println("Started: " + framework.getBundleLocation(id) + " (id#" + id + ")", 0);
	    i++;
	  } else {
	    error("No ID for start command");
	  }
	} else if ("-stop".equals(args[i])) {
	  if (i+1 < args.length) { 
	    long id = getBundleID(base,args[i+1]);
	    framework.stopBundle(id);
	    println("Stopped: " + framework.getBundleLocation(id) + " (id#" + id + ")", 0);
	    i++;
	  } else {
	    error("No ID for stop command");
	  }
	} else if ("-uninstall".equals(args[i])) {
	  if (i+1 < args.length) { 
	    long id = getBundleID(base,args[i+1]);
	    String loc = framework.getBundleLocation(id);
	    framework.uninstallBundle(id);
	    println("Uninstalled: " + loc + " (id#" + id + ")", 0);
	    i++;
	  } else {
	    error("No id for uninstall command");
	  }
	} else if ("-update".equals(args[i])) {
	  if (i+1 < args.length) { 
	    long id = getBundleID(base,args[i+1]);
	    framework.updateBundle(id);
	    println("Updated: " + framework.getBundleLocation(id) + " (id#" + id + ")", 0);

	    i++;
	  } else {
	    error("No id for update command");
	  }
	} else if ("-initlevel".equals(args[i])) {
	  if (i+1 < args.length) { 
	    int n = Integer.parseInt(args[i+1]);
	    if(framework.startLevelService != null) {
	      framework.startLevelService.setInitialBundleStartLevel(n);
	    } else {
	      println("No start level service - ignoring init bundle level " + n, 0);
	    }
	    i++;
	  } else {
	    error("No integer level for initlevel command");
	  }
	} else if ("-startlevel".equals(args[i])) {
	  if (i+1 < args.length) { 
	    int n = Integer.parseInt(args[i+1]);
	    if(framework.startLevelService != null) {
	      framework.startLevelService.setStartLevel(n);
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
	Throwable ne = e.getNestedException();
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
  }


  /**
   * Returns a bundle id from a string. The string is either a number
   * or the location used for the bundle in the
   * "-install bundleLocation" or "-istart" command.
   * @param base Base URL to complete locations with.
   * @param idLocation bundle id or location of the bundle to lookup
   */
  static private long getBundleID(String[] base, String idLocation ) {
    try {
      return Long.parseLong(idLocation);
    } catch (NumberFormatException nfe) {
      long id = framework.getBundleId( completeLocation( base, idLocation ) );
      if (id!=-1) {
	return id;
      } 
      throw new IllegalArgumentException
	("Invalid bundle id/location: " +idLocation);
    }
  }

  /**
   ** Complete location relative to the base Jars URL.
   ** @param base Base URLs to complete with; first match is used.
   ** @param location The location to be completed.
   **/
  static private String completeLocation( String[] base, String location )
  {
    // Handle file: case where topDir is not ""
    if(bZeroArgs && location.startsWith("file:jars/") && !topDir.equals("")) {
      location = ("file:" + topDir + location.substring(5)).replace('\\', '/');
      println("mangled bundle location to " + location, 2);
    }
    int ic = location.indexOf(":");
    if (ic<2 || ic>location.indexOf("/")) {
      println("location=" + location, 2);
      // URL without protocol complete it.
      for (int i=0; i<base.length; i++) {
        try {
          URL url = new URL( new URL(base[i]), location );

          if ("file".equals(url.getProtocol())) {
            File f = new File(url.getFile());
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
          break; // Found.
        } catch (Exception _e) {
        }
      }
    }
    return location;
  }

  public static void frameworkEvent(final FrameworkEvent evt) {
    framework.checkAdminPermission();

    final FrameworkEvent e2 = new FrameworkEvent(evt.getType(),
						 evt.getBundle(),
						 evt.getThrowable());
    AccessController.doPrivileged(new PrivilegedAction() {
	public Object run() {
	  framework.listeners.frameworkEvent(e2);
	  return null;
	}
      });
  }
  
  /**
   * Shutdown framework.
   *
   * <p>
   * This code is called in SystemBundle.stop(), which is the
   * preferred way to shut down the framework.
   * </p>
   */
  static public void shutdown(final int exitcode) {
    framework.checkAdminPermission();
    AccessController.doPrivileged(new PrivilegedAction() {
	public Object run() {
	  Thread t = new Thread() {
	      public void run() {
		if (bootMgr != 0) {
		  try {
		    framework.stopBundle(bootMgr);
		  } catch (BundleException e) {
		    System.err.println("Stop of BootManager failed, " +
				       e.getNestedException());
		  }
		}
		framework.shutdown();
		System.exit(exitcode);
	      }
	    };
	  t.setDaemon(false);
	  t.start();
	  return null;
	}
      });
  }

  /**
   * Restart framework.
   * <p>
   * This code is called in SystemBundle.update()
   * </p>
   */
  static public void restart() {
    framework.checkAdminPermission();

    AccessController.doPrivileged(new PrivilegedAction() {
	public Object run() {
	  Thread t = new Thread() {
	      public void run() {
		if (bootMgr != 0) {
		  try {
		    framework.stopBundle(bootMgr);
		  } catch (BundleException e) {
		    System.err.println("Stop of BootManager failed, " +
				       e.getNestedException());
		  }
		}
		framework.shutdown();

		try {
		  if (bootMgr != 0) {
		    framework.launch(bootMgr);
		  } else {
		    framework.launch(0);
		  }
		} catch (Exception e) {
		  println("Failed to restart framework", 0);
		}
	      }
	    };
	  t.setDaemon(false);
	  t.start();
	  return null;
	}
      });
  }



  /**
   * Print help for starting the platform.
   */
  static void printHelp() {
    try {
      System.out.println(new String(Util.readResource("/help.txt")));
    } catch (Exception e) {
      System.out.println("No help available");
    }
  }

  // Read version info from manifest
  static String readVersion() {
    try {
      return (new String(Util.readResource("/version"))).trim();
    } catch (Exception e) {
      return "<no version found>";
    }
  }

  /**
   * Helper method which tries to find default xargs files.
   */
  static String[] tryDefaultXArgs() {

    // Get starting directory
    String userDir = System.getProperty("user.dir");

    if(userDir != null && userDir.endsWith(File.separator + "lib")) {
      // hmmm...we seem to have started in the lib directory.
      // prefix the default instance dir to ../
      topDir         = ".." + File.separator;
      defaultInstDir = topDir + defaultInstDir;

      println("adjusted defaultInstDir to " + defaultInstDir, 1);
    }

    String[] args   = new String[0];
    File     defDir = new File(defaultInstDir);

    try {
      String osName = Alias.unifyOsName(System.getProperty("os.name"));
      File f = new File("init_" + osName + ".xargs");
      if(f.exists()) {
	defaultXArgsInit = f.getName();
	println("using OS specific xargs=" + defaultXArgsInit, 1);
      }
    } catch (Exception e) {
      
    }
    // Check if default instance dir exists
    if(defDir.exists() && defDir.isDirectory()) {
      String fwDirStr = System.getProperty("org.osgi.framework.dir");
      if(fwDirStr == null || "".equals(fwDirStr)) {
	fwDirStr = (new File(defDir, "fwdir")).getAbsolutePath();
      }
      File fwDir     = new File(fwDirStr);
      File xargsFile = null;

      // ..and select appropiate xargs file
      if(fwDir.exists() && fwDir.isDirectory()) {
	xargsFile = new File(defDir, defaultXArgsStart);
	if(xargsFile.exists()) {
	  println("\n" + 
		  "Using restart xargs file: " + xargsFile + 
		  "\n" + 
		  "To reinitialize, remove the " + fwDir.toString() + 
		  " directory\n", 
		  0);
	} else {
	  File xargsFile2 = new File(defDir, defaultXArgsInit);
	  println("No restart xargs file " + xargsFile + 
			     ", trying " + xargsFile2 + " instead.", 0);
	  xargsFile = xargsFile2;
	}
      } else {
	xargsFile = new File(defDir, defaultXArgsInit);
	if(xargsFile.exists()) {
	  println("\n" + 
		  "Using init xargs file: " + xargsFile + 
		  "\n", 
		  0);
	} else {
	  xargsFile = new File(defDir, defaultXArgsInit2);
	  if(xargsFile.exists()) {
	    println("\n" + 
		    "Using secondary init xargs file: " + xargsFile + 
		    "\n", 
		    0);
	  }
	}
      }

      // ...and return the mangled args array
      if(xargsFile != null && xargsFile.exists()) {
	args = new String[] { "-xargs", xargsFile.toString() };
      } else {
	println("Default xargs file " + xargsFile + " does not exists", 1);
      }
    }
    return args;
  }

  /**
   * Default values for some system properties.
   */
  static String[][] defaultSysProps = new String[][] {
    {"org.osgi.framework.system.packages", "javax.swing,javax.swing.border,javax.swing.event,javax.swing.plaf,javax.swing.plaf.basic,javax.swing.plaf.metal,javax.swing.table,javax.swing.text,javax.swing.tree"},
    {"org.knopflerfish.gosg.name",            "gdsp"},
    {"org.knopflerfish.gdsp.instance.dir",    defaultInstDir}, 
    {"org.osgi.framework.dir",             defaultInstDir + File.separator + "fwdir"},
    {"org.knopflerfish.bundle.cm.store",      defaultInstDir + File.separator + "cmdir"},
  };



  /**
   * Check current system properties and set default values
   * if importand ones are missing. The defualt values 
   * are taken from the <tt>defaultSysProps</tt> variable.
   *
   * <p>
   * The <tt>org.knopflerfish.gosg.jars</tt> property (if not defined)
   * is created by scanning the "jars" directory for subdirs.
   * </p>
   *
   * @see defaultSysProps
   */
  static void setDefaultSysProps() {

    // Make modifications to temporary dictionary and write
    // it back in end of this method
    Properties sysProps = System.getProperties();

    println("setDefaultSysProps", 1);

    for(int i = 0; i < defaultSysProps.length; i++) {
      if(null == System.getProperty(defaultSysProps[i][0])) {
	println("Using default " + defaultSysProps[i][0] + "=" + 
		defaultSysProps[i][1], 1);
	sysProps.put(defaultSysProps[i][0], defaultSysProps[i][1]);
      } else {
	println("system prop " + defaultSysProps[i][0] + "=" + System.getProperty(defaultSysProps[i][0]), 1); 
      }
    }

    // Set version info
    if(null == System.getProperty("org.knopflerfish.gdsp.prodver")) {
      sysProps.put("org.knopflerfish.gdsp.prodver", version);
    }


    // If jar dir is not specified, default to "file:jars/" and its
    // subdirs
    String jars = System.getProperty("org.knopflerfish.gosg.jars");
    
    if(jars == null || "".equals(jars)) {
      String jarBaseDir = topDir + "jars";
      
      println("jarBaseDir=" + jarBaseDir, 1);

      File jarDir = new File(jarBaseDir);
      if(jarDir.exists() && jarDir.isDirectory()) {
	String [] subdirs = jarDir.list(new FilenameFilter() {
	    public boolean accept(File dir, String fname) {
	      File f = new File(dir, fname);
	      return f.isDirectory();
	    }
	  });
	StringBuffer sb = new StringBuffer();
	sb.append("file:" + jarBaseDir + "/");
	for(int i = 0; i < subdirs.length; i++) {
	  sb.append(";file:" + jarBaseDir + "/" + subdirs[i] + "/");
	}
	jars = sb.toString().replace('\\', '/');
	sysProps.put("org.knopflerfish.gosg.jars", jars);
	println("scanned org.knopflerfish.gosg.jars=" + jars, 1);
      }
    }

    // Write back system properties
    System.setProperties(sysProps);
  }

  /**
   * Loop over args array and check that it looks reasonable.
   * If really bad things are found, they might be fixed ;)
   *
   * <p>
   * This method is intended to be called in the "zeroargs" 
   * startup case to preserve backwards compatibility.
   * </p>
   *
   * @return new, fixed args array.
   */
  static String [] sanityArgs(String[] args) {
    Vector v = new Vector();

    // First, clone everything
    for(int i = 0; i < args.length; i++) {
      v.addElement(args[i]);
    }

    // ...since this is really annoying
    if(!v.contains("-launch")) {
      println("adding last -launch command", 1);
      v.addElement("-launch");
    }

    // ...and copy back into array
    String [] arg2 = new String[v.size()];
    v.copyInto(arg2);
    return arg2;
  }


  /**
   * Helper method when OS shell does not allow long command lines. This
   * method has nowadays become the only reasonable way to start the
   * framework due to the amount of properties.
   *
   * <p>
   * Loads file or URL specified in argv[1] (argv[0] is ignored) and 
   * creates a new String array where each entry corresponds to entries
   * in the loaded file.
   * </p>
   * <p>
   * The resulting array consists of the newly created array, appended to 
   * the array consisting of argv[2] to argv[argv.length-1].
   * </p>
   * 
   * <p>
   * File format:<br>
   *
   * <ul>
   *  <li>Each line starting with '-D' and containing an '=' is set as
   *      a system property. 
   *      Example "-Dorg.knopflerfish.test=apa" is equivalent
   *      to <code>System.setProperty("org.knopflerfish.test", "apa");</code>
   *  <li>Each line of length zero is ignored.
   *  <li>Each line starting with '#' is ignored.
   *  <li>Lines starting with '-' is used a command with optional argument
   *      after space.
   *  <li>All other lines is used directly as an entry to the new
   *      command line array.
   * </ul>
   * </p>
   * 
   *
   * @param argv Original command line arguments. These should begin 
   *             with "-xargs" "<file to load>". If argv.length &lt; 2 
   *             return original argv.
   * @return     Original argv + argv loaded from file
   */
  static String [] loadArgs(String []args) {

    if(args.length < 2) return args;
    
    java.util.Vector v = new java.util.Vector();

    try {
      BufferedReader in = null;

      URL url;

      // Check as URL first, then as a plain file
      if(-1 != args[1].indexOf(":")) {
	try {
	  url = new URL(args[1]);
	  
	  in = new BufferedReader(new InputStreamReader(url.openStream()));
	} catch (Exception e) {
	  println("-xargs failed with " + e, 0);
	}
      }

      if(in == null) {
	File f = new File(args[1]);
	if(f.exists()) {
	  in = new BufferedReader(new FileReader(f));
	} else {
	  System.err.println("No xargs file: " + f.getAbsolutePath());
	  return args;
	}
      }

      
      String line = null;
      Properties sysProps = System.getProperties();
      for(line = in.readLine(); line != null; 
	  line = in.readLine()) {
	line = line.trim();
	if(line.startsWith("-D")) {
	  // Set system property
	  int ix = line.indexOf("=");
	  if(ix != -1) {
	    String name = line.substring(2, ix);
	    String val  = line.substring(ix + 1);
	    sysProps.put(name, val);
	  }
	} else if(line.startsWith("#")) {
	  // Ignore comments
	} else if(line.startsWith("-")) {
	  int i = line.indexOf(' ');
	  if (i != -1) {
	    v.addElement(line.substring(0,i));
	    line = line.substring(i).trim();
	    if(line.length() > 0) {
	      v.addElement(line);
	    }
	  } else {
	    v.addElement(line);
	  }
	} else if(line.length() > 0) {
	  // Add argument
	  v.addElement(line);
	}
      }
      setSecurityManager(sysProps);
      System.setProperties(sysProps);
    } catch (Exception e) {
      error("-xargs loading failed: " + e);
    }
    String [] args2 = new String[args.length - 2 + v.size()];
    int n = 0;
    // First, copy in old argv
    for(int i = 2; i < args.length; i++) {
      args2[n++] = args[i];
    } 
    // Then, append new entries
    for(int i = 0; i < v.size(); i++) {
      args2[n++] = (String)v.elementAt(i);
    } 
    
    return args2;
  }

  /**
   * Print string to System.out if level >= current verbosity.
   *
   * @param s String to print.
   * @param level print level.
   */
  static void println(String s, int level) {
    if(verbosity >= level) {
      System.out.println((level > 0 ? ("#" + level + ": ") : "") + s);
    }
  }

  static void setSecurityManager(Properties props) {
    try {
      String manager  = (String)props.get("java.security.manager");      
      String policy   = (String)props.get("java.security.policy");
      

      if(manager != null) {
	if(System.getSecurityManager() == null) {
	  println("Setting security manager=" + manager + 
		  ", policy=" + policy, 1);
	  System.setProperty("java.security.manager", manager);
	  if(policy != null) {
	    System.setProperty("java.security.policy",  policy);
	  }
	  SecurityManager sm = null;
	  if("".equals(manager)) {
	    sm = new SecurityManager();
	  } else {
	    Class       clazz = Class.forName(manager);
	    Constructor cons  = clazz.getConstructor(new Class[0]);

	    sm = (SecurityManager)cons.newInstance(new Object[0]);
	  }
	  System.setSecurityManager(sm);
	}
      }
    } catch (Exception e) {
      error("Failed to set security manager", e);
    }
  }

  /**
   * Report error and exit.
   */
  static void error(String s) {
    error(s, null);
  }

  static void error(String s, Exception e) {
    System.err.println("Error: " + s);
    if(e != null) {
      e.printStackTrace();
    }
    System.exit(1);
  }
}
