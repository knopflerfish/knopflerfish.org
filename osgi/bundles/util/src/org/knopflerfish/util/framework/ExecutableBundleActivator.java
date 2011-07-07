/*
 * Copyright (c) 2007-2008, KNOPFLERFISH project
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

package org.knopflerfish.util.framework;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Hashtable;
import java.util.HashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import org.knopflerfish.util.Text;

/**
 */
public class ExecutableBundleActivator implements BundleActivator {
  protected BundleContext bc;
  protected String        startName;
  protected String        stopName;
  protected File          startFile;
  protected File          stopFile;
  protected Process       runProcess;
  protected boolean       bProcessExitMeansStopBundle = true;
  protected String        baseDirName                 = "exefiles";

  /**
   * Manifest header (named &quot;Bundle-Start-Executable&quot;) identifying a
   * number of hardware environments and the native language code executables
   * that the bundle is carrying for each of these environments.
   *
   * <p>
   * If a matching executable is found, the executable file will be copied
   * to the OS file system and started in the start() method
   * </p>
   */
  public static final String BUNDLE_START_EXECUTABLE = "Bundle-Start-Executable";
  /**
   * Manifest header (named &quot;Bundle-Start-Executable-Args&quot;)
   * specifying the (space-separated) process arguments to the executable
   * specified by {@link ExecutableBundleActivator#BUNDLE_START_EXECUTABLE}.
   */
  public static final String BUNDLE_START_EXECUTABLE_ARGS = "Bundle-Start-Executable-Args";

  /**
   * Manifest header (named &quot;Bundle-Start-Executable-Args&quot;)
   * specifying the (space-separated) process arguments to the executable
   * specified by {@link ExecutableBundleActivator#BUNDLE_START_EXECUTABLE}.
   */
  public static final String BUNDLE_STOP_EXECUTABLE_ARGS = "Bundle-Stop-Executable-Args";

  /**
   * Manifest header (named &quot;Bundle-Stop-Executable&quot;) identifying a
   * number of hardware environments and the native language code executables
   * that the bundle is carrying for each of these environments.
   *
   * <p>
   * If a matching executable is found, the executable file will be copied
   * to the OS file system and started in the stop() method.
   * </p>
   */
  public static final String BUNDLE_STOP_EXECUTABLE = "Bundle-Stop-Executable";

  /**
   * Manifest header (named &quot;Bundle-Extract-Files&quot;) identifying a
   * set of resources that should be copied to the OS file system before
   * executing the start or stop executable is run.
   */
  public static final String BUNDLE_EXTRACT_FILES   = "Bundle-Extract-Files";

  /**
   * Manifest header (named
   * &quot;Bundle-Start-Executable-Exit-Means-Bundle-Stop"&quot;)
   * specifying of the the process exit of the start executable should
   * stop the bundle too. Set to "true" if the bundle should be stopped.
   * Default is "true"
   *
   * <p>
   * </p>
   */
  public static final String BUNDLE_START_EXECUTABLE_EXIT_MEANS_BUNDLESTOP =
    "Bundle-Start-Executable-Exit-Means-Bundle-Stop";

  public ExecutableBundleActivator() {
  }

  public void start(BundleContext bc) throws BundleException {
    this.bc = bc;
    debug("start " + this);
    try {
      initFiles();

      if(startFile != null) {
        String args = (String)bc.getBundle().getHeaders().get(BUNDLE_START_EXECUTABLE_ARGS);
        runProcess = runFile(startFile, args, false,
                             isProcessExitMeansStopBundle());
      }
    } catch (IOException e) {
      throw new BundleException("Failed to init", e);
    }
  }


  public void stop(BundleContext bc) throws BundleException {
    try {
      debug("stop " + this);

      String stopS = (String)bc.getBundle().getHeaders().get(BUNDLE_START_EXECUTABLE_EXIT_MEANS_BUNDLESTOP);

      bProcessExitMeansStopBundle = stopS == null || "true".equals(stopS);

      if(runProcess != null) {
        runProcess.destroy();
        runProcess = null;
      }

      if(stopFile != null) {
        String args = (String)bc.getBundle().getHeaders().get(BUNDLE_STOP_EXECUTABLE_ARGS);
        runFile(stopFile, args, true, false);
      }
    } catch (Exception e) {
      throw new BundleException("Failed to stop bundle process", e);
    } finally {
      this.bc = null;
    }
  }

  public String getBaseDirName() {
    return baseDirName;
  }

  public void setBaseDirName(String s) {
    baseDirName = s;
  }

  /**
   * Get the base directory for extract files to be executed.
   *
   * <p>
   * Override to change.
   * Default is <tt>BundleContext.getDataFile(getBaseDirName())</tt>
   * </p>
   */
  public File getBaseDir() {
    return bc.getDataFile(getBaseDirName());
  }


  /**
   * Return true if exit of start process should stop bundle too.  The
   * initial value of this is set by reading the {@link
   * #BUNDLE_START_EXECUTABLE_EXIT_MEANS_BUNDLESTOP} manifest header.
   */
  public boolean isProcessExitMeansStopBundle() {
    return bProcessExitMeansStopBundle;
  }


  /**
   * Set if exit of start process should stop bundle too.
   */
  public void setProcessExitMeansStopBundle(boolean b) {
    bProcessExitMeansStopBundle = b;
  }


  /**
   * Flag which controls if logging should be done.
   * The default implementation returns true.
   * Override if necessary.
   */
  public boolean isDebug() {
    return true;
  }

  /**
   * Method used for debug logging. Calls debug(s, null)
   */
  public void debug(String s) {
    debug(s, null);
  }

  /**
   * Method used for debug logging. The default implementation
   * writes to stdout. Override if necessary.
   *
   * @param s message string to log
   * @param e exception to log
   */
  public void debug(String s, Exception e) {
    System.out.println("ExecutableBundleActivator: " + s);
    if(e != null) {
      e.printStackTrace();
    }
  }


  /**
   * Start a new process from a file.
   *
   * @param f file to run
   * @param args space-separated process arguments
   * @param bWaitForExit if true, wait for exit before returning
   * @param bStopBundle if true, stop bundle after process has exited.
   * @return the created process instance
   */
  protected Process runFile(File f,
                            String args,
                            boolean bWaitForExit,
                            boolean bStopBundle) {

    debug("runFile " + f + ", wait=" + bWaitForExit);

    Process p = null;

    try {
      File     runDir   = f.getParentFile();
      String   filePath = f.getAbsolutePath();
      String   dirPath  = f.getParentFile().getAbsolutePath();
      String[] argv     = null;
      String[] cmd      = new String[1];

      if(args != null) {
        argv = Text.splitwords(args, " ");
        cmd  = new String[argv.length + 1];
        for(int i = 0; i < argv.length; i++) {
          String s = argv[i];
          s = Text.replace(s, "${absfile}",  filePath);
          s = Text.replace(s, "${absdir}",   dirPath);
          s = Text.replace(s, "${file.sep}", File.separator);
          s = Text.replace(s, "${path.sep}", File.pathSeparator);
          cmd[i+1] = s;
        }
      }
      cmd[0] = f.getAbsolutePath();

      if(isDebug()) {
        for(int i = 0; i < cmd.length; i++) {
          debug("cmd[" + i + "]=" + cmd[i]);
        }
      }

      p = Runtime.getRuntime().exec(cmd, null, runDir);

      String baseName = bc.getBundle().getBundleId() + ":" + getName(f.getAbsolutePath());

      ProcessThread t = new ProcessThread(baseName,
                                          p,
                                          bWaitForExit,
                                          bStopBundle);

      t.start();
      return p;
    } catch (Exception e) {
      if(p != null) {
        p.destroy();
      }
      throw new RuntimeException("failed to start " + f, e);
    }
  }

  void initFiles() throws IOException {
    initFiles((String)bc.getBundle().getHeaders().get(BUNDLE_START_EXECUTABLE),
              (String)bc.getBundle().getHeaders().get(BUNDLE_STOP_EXECUTABLE),
              (String)bc.getBundle().getHeaders().get(BUNDLE_EXTRACT_FILES)
              );
  }

  protected void initFiles(String startChoices,
                           String stopChoices,
                           String extractNames) throws IOException {
    String startName = null;
    Collection map = getNativeCode(startChoices);
    // debug("startMap=" + map);
    if(map != null && map.size() > 0) {
      startName = (String)map.iterator().next();
    }

    String stopName = null;
    map = getNativeCode(stopChoices);
    // debug("stopMap=" + map);
    if(map != null && map.size() > 0) {
      stopName = (String)map.iterator().next();
    }

    initFiles2(startName, stopName, extractNames);
  }


  public void initFiles2(String startName,
                         String stopName,
                         String extractNames) throws IOException {
    debug("startName=" + startName + ", stopName=" + stopName);
    this.startName = startName;
    this.stopName  = stopName;


    if(extractNames != null) {
      String[] names = Text.splitwords(extractNames, ",", '\"');

      debug(extractNames + "->" + names.length);
      for(int i = 0; i < names.length; i++) {
        extractResource(names[i]);
      }
    }

    if(startName != null) {
      startFile = extractResource(startName);
      setExecutable(startFile);
    }

    if(stopName != null) {
      stopFile = extractResource(stopName);
      setExecutable(stopFile);
    }
  }

  void setExecutable(File f) {
    File cmdFile = findOSCommand("chmod");
    if(cmdFile == null) {
      debug("No chmod command found, ignoring setExecutable");
      return;
    }
    String[] cmd = new String[] {
	  cmdFile.getAbsolutePath(),
	  "a+rx",
	  f.getAbsolutePath(),
    };
    Process p = null;
    try {
	  p = Runtime.getRuntime().exec(cmd, null, null);
	  p.waitFor();
    } catch (Exception e) {
      debug("failed to set executable " + f, e);
    } finally {
	  // try { p.destroy(); } catch(Exception ignored) {}
    }
  }

  File findOSCommand(String cmd) {
	String[] paths = new String[] {
      "/bin", "/usr/bin", "/bin/local", "/usr/bin/local",
	};

	for(int i = 0; i <paths.length; i++) {
	    File f = new File(new File(paths[i]), cmd);
	    if(f.exists()) {
		return f;
	    }
	}
	return null;
  }


  /**
   * Extract a bundle resource to a file in the
   * bundle storage area.
   */
  File extractResource(String name) throws IOException {
    debug("extractResource " + name);
    URL    url   = bc.getBundle().getResource(name);
    String fname = name;
    File baseDir = getBaseDir();
    File f       = new File(baseDir, fname);
    File dir     = f.getParentFile();

    // debug("mkdir " + dir.getAbsolutePath());
    dir.mkdirs();

    BufferedInputStream bin = null;
    BufferedOutputStream bout = null;

    debug("extract " + name + " to " + f.getAbsolutePath());

    try {
      bin  = new BufferedInputStream(url.openStream());
      bout = new BufferedOutputStream(new FileOutputStream(f));
      byte[] buf = new byte[1024 * 10];
      int n;
      while(-1 != (n = bin.read(buf))) {
        bout.write(buf, 0, n);
      }
      bout.flush();
      return f;
    } finally {
      try { bin.close(); } catch (Exception ignored) { }
      try { bout.close(); } catch (Exception ignored) { }
    }
  }

  String getName(String s) {
    int ix = s.lastIndexOf("/");
    if(ix == -1) {
      ix = s.lastIndexOf("\\");
    }
    if(ix != -1) {
      return s.substring(ix+1);
    }
    return s;
  }



  // This is more or less copied from BundleArchiveImpl
  // It should rather be moved to the Util class
  private Collection getNativeCode(String bnc)   {
    if (bnc != null) {
      Bundle b = bc.getBundle();
      Hashtable fwprops = new Hashtable();
      fwprops.put(Constants.FRAMEWORK_PROCESSOR,
                bc.getProperty(Constants.FRAMEWORK_PROCESSOR));
      fwprops.put(Constants.FRAMEWORK_OS_NAME,
                bc.getProperty(Constants.FRAMEWORK_OS_NAME));
      fwprops.put(Constants.FRAMEWORK_OS_VERSION,
                bc.getProperty(Constants.FRAMEWORK_OS_VERSION));
      fwprops.put(Constants.FRAMEWORK_LANGUAGE,
                bc.getProperty(Constants.FRAMEWORK_LANGUAGE));

      String  proc  = bc.getProperty(Constants.FRAMEWORK_PROCESSOR);
      String  os    = bc.getProperty(Constants.FRAMEWORK_OS_NAME);
      Version osVer = new Version(bc.getProperty(Constants.FRAMEWORK_OS_VERSION));
      String osLang = bc.getProperty(Constants.FRAMEWORK_LANGUAGE);
      boolean optional = false;
      List best = null;
      VersionRange bestVer = null;
      boolean bestLang = false;

      // debug("getNativeCode bnc=" + bnc + ", proc=" + proc + ", os=" + os + ", osVer=" + osVer);

      for (Iterator i = Text.parseEntries(Constants.BUNDLE_NATIVECODE, bnc, false, false, false); i.hasNext(); ) {
        VersionRange matchVer = null;
        boolean matchLang = false;
        Map params = (Map)i.next();

        List keys = (List)params.get("keys");
        if (keys.size() == 1 && "*".equals(keys.get(0)) && !i.hasNext()) {
          optional = true;
          break;
        }

        List pl = (List)params.get(Constants.BUNDLE_NATIVECODE_PROCESSOR);
        if (pl != null) {
          if (!Text.containsIgnoreCase(pl, Alias.unifyProcessor(proc))) {
            continue;
          }
        } else {
          // NYI! Handle null
          continue;
        }

        List ol = (List)params.get(Constants.BUNDLE_NATIVECODE_OSNAME);
        if (ol != null) {
          if (!Text.containsIgnoreCase(ol, Alias.unifyOsName(os))) {
            continue;
          }
        } else {
          // NYI! Handle null
          continue;
        }

        List ver = (List)params.get(Constants.BUNDLE_NATIVECODE_OSVERSION);
        if (ver != null) {
          boolean okVer = false;
          for (Iterator v = ver.iterator(); v.hasNext(); ) {
            // NYI! Handle format Exception
            matchVer = new VersionRange((String)v.next());
            if (matchVer.withinRange(osVer)) {
              okVer = true;
              break;
            }
          }
          if (!okVer) {
            continue;
          }
        }

        List lang = (List)params.get(Constants.BUNDLE_NATIVECODE_LANGUAGE);
        if (lang != null) {
          for (Iterator l = lang.iterator(); l.hasNext(); ) {
            if (osLang.equalsIgnoreCase((String)l.next())) {
              // Found specfied language version, search no more
              matchLang = true;
              break;
            }
          }
          if (!matchLang) {
            continue;
          }
        }

        List sf = (List)params.get(Constants.SELECTION_FILTER_ATTRIBUTE);
        if (sf != null) {
          if (sf.size() == 1) {
            Filter filter = null;
            try {
              filter = bc.createFilter((String)sf.get(0));
            } catch (InvalidSyntaxException e) {
              // I really hate checked exceptions
              throw new RuntimeException("wtf", e);
            }
            if (!filter.match(fwprops)) {
              continue;
            }
          } else {
            //NYI! complain about faulty selection
          }
        }

        // Compare to previous best
        if (best != null) {
          boolean verEqual = false;
          if (bestVer != null) {
            if (matchVer == null) {
              continue;
            }
            int d = bestVer.compareTo(matchVer);
            if (d == 0) {
              verEqual = true;
            } else if (d > 0) {
              continue;
            }
          } else if (matchVer == null) {
            verEqual = true;
          }
          if (verEqual && (!matchLang || bestLang)) {
            continue;
          }
        }
        best = keys;
        bestVer = matchVer;
        bestLang = matchLang;
      }
      return best;
    }  else {
      // No native code in this bundle
      return null;
    }
  }

  class ProcessThread extends Thread {
    String  baseName;
    Process p;
    int     exitCode;
    boolean bDone = false;
    boolean bWait = false;
    boolean bStopBundle = false;
    Thread  stdoutThread;
    Thread  stderrThread;

    ProcessThread(String name, Process p, boolean bWait, boolean bStopBundle) {
      super(name + "::wait");

      this.baseName    = name;
      this.p           = p;
      this.bWait       = bWait;
      this.bStopBundle = bStopBundle;
    }

    public void run() {
      try {
        debug("waitFor " + getName());
        exitCode = p.waitFor();
        debug("done waitFor " + getName());
      } catch (InterruptedException e) {
        bDone = true;
        exitCode = -1;
      } finally {
        bDone = true;
        if(bStopBundle) {
          if(bc != null) {
            debug("stopping bundle");
            try {
              bc.getBundle().stop();
            } catch (Exception e) {
              throw new RuntimeException("Failed to stop bundle", e);
            }
          }
        }
      }
    }

    public void start() {
      bDone = false;

      stdoutThread = gobble(baseName + "::stdout", p.getInputStream());
      stderrThread = gobble(baseName + "::stderr", p.getErrorStream());

      super.start();

      if(bWait) {
        while(!bDone) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            bDone = true;
          }
        }
      }
    }

    public void close() {
      bDone = true;
      try {
        join(2000);
      } catch (Exception e) {
        throw new RuntimeException("Failed to close " + getName(), e);
      }
    }

    /**
     * Create a thread that reads all data from a given stream.
     *
     * @param name thread name
     * @param is stream to read
     */
    Thread gobble(final String name,
                  final InputStream is) {
      Thread t = new Thread(name) {
          public void run() {
            debug("start gobble " + name);
            byte[] buf = new byte[1024];
            try {
              while(!bDone) {
                try {
                  int n = is.read(buf);
                  if(isDebug()) {
                    if(n > 0) {
                      String s = new String(buf, 0, n);
                      debug(name + ": " + s);
                    }
                  }
                } catch (Exception e) {
                  if(isDebug()) {
                    debug("exception in gobble: " + name, e);
                  }
                  throw new RuntimeException("read done. " + name, e);
                }
              }
            } finally {
              if(isDebug()) {
                debug("end gobble " + name);
              }
            }
          }
        };
      t.start();
      return t;
    }
  }
}
