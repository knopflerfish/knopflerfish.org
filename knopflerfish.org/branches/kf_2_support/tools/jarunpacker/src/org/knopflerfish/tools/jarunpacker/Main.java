/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

package org.knopflerfish.tools.jarunpacker;

import java.util.*;
import java.io.*;
import java.util.jar.*;
import java.util.zip.*;
import java.lang.reflect.*;

public class Main {

  public static Main   theMain;

  String version = "unknown";


  public static void main(String[] argv) {

    theMain = new Main();

    theMain.unpack(argv);
  }


  boolean bSilent      = false;
  int     verbosity    = 0;
  String  destdirname   = "";
  String  postHookName = null;
  String  preHookName  = null;
  String  iconLeft     = null;
  String  iconPath     = null;
  String  licenseResName = null;
  String  licenseTitle   = null;
  String  windowTitle    = null;

  String  optButtons   = "base source htdocs";

  InstallUI ui = null;
  File     openDir  = null;

  String[] excludePrefix = new String[] {
    "META-INF",
    "fish16x16.gif",
    "ant.gif",
    "fish32x32.gif",
    "fish200x300.gif",
    "kf_16x16.gif",
    "kf_32x32.gif",
    "knopflerfish_red400pxl.gif",
    "license.txt",
    "org",
    "pspbrwse.jbf",
    "strings.properties",
    "noia"
  };


  boolean bBatch = false;
  boolean bText  = false;

  void printHelp() {
    System.out.println("Usage: jarunpacker [options]\n\n" +
                       " -help         print this page\n" +
                       " -batch        do not use swing or ask for options\n" +
                       " -silent       be silent\n" +
                       " -destdir dir  set destination dir to destdir\n" +
                       ""
                       );
  }

  void unpack(String[] argv) {
    int i = 0;
    while(i < argv.length) {
      if("-batch".equals(argv[i])) {
        bBatch = true;
      } else if("-help".equals(argv[i])) {
        printHelp();
        exit("", null);
      } else if("-silent".equals(argv[i])) {
        bSilent = true;
      } else if("-destdir".equals(argv[i]) && i+1 < argv.length) {
        i++;
        destdirname = argv[i];
      } else {
        System.out.println("Unknown option '" + argv[i] + "'\n");
        printHelp();
        exit("", null);
      }
      i++;
    }

    try {
      verbosity = Integer.parseInt(System.getProperty
                      ("org.knopflerfish.tools.jarunpacker.verbosity"));
    } catch (Exception ignored) {
    }


    log("unpacking", 2);

    String classPath = System.getProperty("java.class.path");
    if(classPath == null) {
      exit("No valid jar file to unpack on classpath", null);
      System.exit(0);
    }
    String jarfilename = "";
    StringTokenizer st = new StringTokenizer(classPath,
                                             java.io.File.pathSeparator);
    while ( st.hasMoreTokens() && !jarfilename.endsWith(".jar")) {
      jarfilename = st.nextToken();
    }
    if(!jarfilename.endsWith(".jar")) {
      exit("No valid jar file to unpack on classpath", null);
      System.exit(0);
    }

    File destDir = new File(destdirname);

    String opendirname = "${destdir}";

    try {

      File file = new File(jarfilename);

      if(!file.exists()) {
        exit("Cannot find '" + jarfilename + "'", null);
      }

      JarFile jarFile = new JarFile(file);

      Manifest mf = jarFile.getManifest();

      if(destdirname.equals("")) {
        try {
          destdirname = mf.getMainAttributes().getValue("jarunpacker-destdir");
        } catch (Exception ignored) {   }
      }
      if(destdirname == null) { destdirname = ".";   }

      try {
        version = mf.getMainAttributes().getValue("knopflerfish-version");
      } catch (Exception ignored) {      }
      if(version == null) {  version = "";   }

      try {
        optButtons = mf.getMainAttributes().getValue("jarunpacker-optbutton");
      } catch (Exception ignored) {  }
      if(optButtons == null) {  optButtons = "base source htdocs";  }

      try {
        opendirname = mf.getMainAttributes().getValue("jarunpacker-opendir");
      } catch (Exception ignored) {     }
      //      if(opendirname == null) { opendirname = ".";   }

      try {
        iconLeft = mf.getMainAttributes().getValue("jarunpacker-iconleft");
      } catch (Exception ignored) {     }

      try {
        iconPath = mf.getMainAttributes().getValue("jarunpacker-iconpath");
      } catch (Exception ignored) {     }

      try {
        licenseResName
          = mf.getMainAttributes().getValue("jarunpacker-licensepath");
        if (null!=licenseResName) {
          licenseResName = licenseResName.trim();
        }
      } catch (Exception ignored) {     }

      try {
        licenseTitle
          = mf.getMainAttributes().getValue("jarunpacker-licensetitle");
        if (null!=licenseTitle) {
          licenseTitle = licenseTitle.trim();
        }
      } catch (Exception ignored) {     }

      try {
        windowTitle
          = mf.getMainAttributes().getValue("jarunpacker-windowtitle");
        if (null!=windowTitle) {
          windowTitle = windowTitle.trim();
        }
      } catch (Exception ignored) {     }


      try {
        preHookName = mf.getMainAttributes().getValue("jarunpacker-prehook");
      } catch (Exception ignored) {      }

      try {
        postHookName = mf.getMainAttributes().getValue("jarunpacker-posthook");
      } catch (Exception ignored) {      }

      destDir = new File(destdirname);
      call(jarFile, destDir, preHookName);

      int nFiles = 0;
      long nBytes = 0;
      for(Enumeration e = jarFile.entries(); e.hasMoreElements(); ) {
        ZipEntry entry = (ZipEntry)e.nextElement();
        if(!isExcluded(entry)) {
          nFiles++;
          long size = entry.getSize();
          if(size != -1) {
            nBytes += size;
          }
        }
      }


      if(!bBatch) {
        try {
          JUnpackWizard wizard = new JUnpackWizard(jarFile, destDir, nBytes, nFiles);
          ui = wizard;

          wizard.start();

          if(!wizard.isFinished()) {
            exit("Wizard not finished", null);
          }

          destDir = wizard.getDestDir();
          if(!wizard.doOpenDir()) {
            opendirname = null;
          }
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }

      if(ui == null) {

        if(!destDir.exists()) {
          destDir.mkdirs();
        }

        if(!destDir.exists()) {
          exit("failed to create destdir " + destDir.getAbsolutePath(), null);
        }

        ui = new TextUI(jarFile, destDir);
      }

      log("extracting " + nFiles + " files to: " + destDir.getAbsolutePath() + ", size=" + (nBytes/1024) + "kb", 0);

      int count = 0;
      for(Enumeration e = jarFile.entries(); !bCancel && e.hasMoreElements(); ) {
        ZipEntry entry = (ZipEntry)e.nextElement();

        if(!isExcluded(entry)) {
          progress(entry.getName(), 100 * count / nFiles);
          if(entry.isDirectory()) {
            makeDir(jarFile, entry, destDir);
          } else {
            copyEntry(jarFile, entry, destDir);
          }
          count++;
        }
      }

      if(ui != null) {
        ui.theEnd();
      }

      call(jarFile, destDir, postHookName);

      if(!bCancel && opendirname != null) {
        opendirname = Strings.replace(opendirname,
                                      "${destdir}",
                                      destDir.getAbsolutePath());
        openDir = new File(opendirname);
      }


      if(openDir != null) {
        doOpenDir(openDir);
      }

    } catch (Exception  e) {
      exit("Failed to unpack " + jarfilename, e);
    }
    if(bCancel) {
      exit("Installation cancelled - some extracted files may be left in\n " +
           destDir.getAbsolutePath(), null);
    } else {
      exit("Installation complete", null);
    }
  }

  long calcSize(ZipFile jarFile) {
    int nFiles = 0;
    long nBytes = 0;
    for(Enumeration e = jarFile.entries(); e.hasMoreElements(); ) {
      ZipEntry entry = (ZipEntry)e.nextElement();
      if(!isExcluded(entry)) {
        nFiles++;
        long size = entry.getSize();
        if(size != -1) {
          nBytes += size;
        }
      }
    }
    return nBytes;
  }


  void doOpenDir(File dir) {
    try {
      if(Util.isWindows() && !bBatch) {
        // Yes, this only works on windows
        String systemBrowser = "explorer.exe";
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(new String[] {
          systemBrowser,
          dir.getAbsolutePath()
        });
      } else {
        log("Installed in " + dir.getAbsolutePath(), 0);
      }
    } catch (Exception e) {
      log("Failed to open dir " + dir + ": " + e, 0);
    }
  }

  String[] sa = new String[0];

  void call(ZipFile file, File destDir, String className) {
    if(className == null || "".equals(className)) {
      return;
    }

    try {
      Class clazz = Class.forName(className);

      Method mainM = clazz.getMethod("main", new Class[] {
        sa.getClass()
      });

      Object obj  = clazz.newInstance();

      mainM.invoke(null, new Object[] { new String[] { file.getName(), destDir.getAbsolutePath() }} );

    } catch (Exception e) {
      e.printStackTrace();
      log("Failed to call " + className, 0);
    }
  }

  boolean isExcluded(ZipEntry entry) {
    for(int i = 0; i < excludePrefix.length; i++) {
      if(entry.getName().startsWith(excludePrefix[i])) {
        return true;
      }
    }

    if(iconPath != null) {
      String s = iconPath;
      if(s.startsWith("/")) {
        s = s.substring(1);
      }
      if(entry.getName().equals(s)) {
        return true;
      }
    }
    if(ui != null && ui.isExcluded(entry.getName())) {
      return true;
    }
    return false;
  }

  void makeDir(ZipFile file, ZipEntry entry, File destDir) {
    File d = new File(destDir, entry.getName());

    if(!bReplaceDirYesAll && d.exists()) {
      String q = Strings.get("q_replace_dir");


      q = Strings.replace(q, "$(name)", d.getName());

      int n = ui.ask(Strings.get("title_replace_dir"),
                           q,
                           Strings.getArray("replace_array4"),
                           InstallUI.OPT_YES,
                           null);
      switch(n) {
      case InstallUI.OPT_YES:
        break;
      case InstallUI.OPT_YESALL:
        bReplaceDirYesAll = true;
        break;
      case InstallUI.OPT_NONE:
      case InstallUI.OPT_NO:
        return;
      case InstallUI.OPT_CANCEL:
        bCancel = true;
        return;
      }
    }

    d.mkdirs();
    log("create:  " + entry.getName(), 2);
  }

  byte[] buf = new byte[1024 * 10];

  boolean bReplaceYesAll    = false;
  boolean bReplaceDirYesAll = false;
  boolean bCancel           = false;

  void copyEntry(ZipFile file, ZipEntry entry, File destDir) throws IOException {

    File d = new File(destDir, entry.getName());

    File dir = d.getParentFile();

    if(!dir.exists()) {
      dir.mkdirs();
    }

    if(!bReplaceYesAll && d.exists()) {
      int n = ui.askFile(Strings.get("title_replace_file"),
                               Strings.getArray("replace_array4"),
                               InstallUI.OPT_YES,
                               d.getAbsolutePath(),
                               d.getName(),
                               new Date(entry.getTime()),
                               entry.getSize());
      switch(n) {
      case InstallUI.OPT_YES:
        break;
      case InstallUI.OPT_YESALL:
        bReplaceYesAll = true;
        break;
      case InstallUI.OPT_NONE:
      case InstallUI.OPT_NO:
        return;
      case InstallUI.OPT_CANCEL:
        bCancel = true;
        return;
      }
    }

    log("extract: " + entry.getName(), 2);

    BufferedInputStream in = new BufferedInputStream(file.getInputStream(entry));
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(d));
    try {
      in  = new BufferedInputStream(file.getInputStream(entry));
      out = new BufferedOutputStream(new FileOutputStream(d));
      int n;
      int total = 0;
      while ((n = in.read(buf)) > 0) {
        out.write(buf, 0, n);
        total += n;
      }
    } finally {
      try { in.close(); } catch (Exception ignored) { }
      try { out.close(); } catch (Exception ignored) { }
    }
  }

  void exit(String msg, Exception e) {
    System.out.println(msg);
    if(e != null) {
      e.printStackTrace();
    }
    System.exit(0);
  }

  void progress(String msg, int perc) {
    if(ui != null) {
      ui.updateProgress(msg, perc);
    }
  }


  void log(String msg, int level) {
    if(verbosity >= level) {
      System.out.println(msg);
    }
  }
}
