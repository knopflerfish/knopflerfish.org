/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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

package org.knopflerfish.ant.taskdefs.bundle;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

public class BIndexTask extends Task {

  private Vector    filesets = new Vector();

  private File   baseDir             = new File("");
  private String baseURL             = "";
  private String repoName            = null;
  private String outFile             = "bindex.xml";

  /**
   * The working directory of the BIndex process. The bundle URLs will
   * be on the form
   * <tt><i>BaseURL</i>/<i>&lt;relative path to JAR&gt;</i></tt>
   * where the relative path to JAR is computed relative to this base
   * dir.
   * @param f The base dir for relative part of the generated URLs.
   */
  public void setBaseDir(File f) {
    this.baseDir = f;
  }

  public void setBaseURL(String s) {
    this.baseURL = s;
  }

  public void setOutFile(String s) {
    this.outFile = s;
  }

  public void setRepoName(String s) {
    this.repoName = s;
  }

  public void addFileset(FileSet set) {
    filesets.addElement(set);
  }

  private String styleSheet;

  /**
   * URL or relative path of the style sheet that the generated XML-file points
   * to for HTML conversion.
   *
   * @param styleSheet
   */
  public void setStyleSheet(final String styleSheet) {
    this.styleSheet = styleSheet;
  }

  // Implements Task
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
    }

    Set jarSet = new HashSet();

    log("loading bundle info...", Project.MSG_VERBOSE);

    try {
      for (int i = 0; i < filesets.size(); i++) {
        FileSet          fs      = (FileSet) filesets.elementAt(i);
        DirectoryScanner ds      = fs.getDirectoryScanner(getProject());
        File             projDir = fs.getDir(getProject());

        String[] srcFiles = ds.getIncludedFiles();

        for (int j = 0; j < srcFiles.length ; j++) {
          File file = new File(projDir, srcFiles[j]);
          if(file.getName().endsWith(".jar")) {
            jarSet.add(file);
          }
        }
      }

      Set removeSet = new HashSet();
      for(Iterator it = jarSet.iterator(); it.hasNext();) {
        File   file = (File)it.next();
        String name = file.getAbsolutePath();
        if(-1 != name.indexOf("_all-")) {
          File f2 = new File(Util.replace(name, "_all-", "-"));
          removeSet.add(f2);
          log("skip " + f2, Project.MSG_VERBOSE);
        }
      }

      if(removeSet.size() > 0) {
        log("skipping " + removeSet.size() + " bundles", Project.MSG_INFO);
      }

      for(Iterator it = removeSet.iterator(); it.hasNext();) {
        File f = (File)it.next();
        jarSet.remove(f);
      }

      log("writing bundle repository to " + outFile, Project.MSG_VERBOSE);
      List cmdList = new ArrayList( 10 + jarSet.size() );

      cmdList.add("-t");
      cmdList.add(baseURL +"/%p/%f ");

      if (null!=styleSheet) {
        cmdList.add("-stylesheet");
        cmdList.add(styleSheet);
      }

      // -d <rootFile> here

      cmdList.add("-r");
      cmdList.add(outFile);

      // Don't print the resulting XML document on System.out.
      cmdList.add("-q");

      if (null!=repoName && repoName.length()>0) {
        cmdList.add("-n");
        cmdList.add(repoName);
      }
      for (Iterator iter = jarSet.iterator(); iter.hasNext(); ) {
        String file = ((File) iter.next()).getAbsolutePath();
        cmdList.add(file);
      }

      try {
        // Call org.osgi.impl.bundle.bindex.Index.main(args) to
        // generate the bindex.xml file.
        Class bIndexClazz = Class.forName("org.osgi.impl.bundle.bindex.Index");

        // Prepend the -d <rootFile> option
        cmdList.add(2, baseDir.getAbsolutePath());
        cmdList.add(2, "-d");
        try {
          // Hack for older BIndex without -d option. Use reflection
          // to set org.osgi.impl.bundle.bindex.Index.rootFile to
          // baseDir to get correctly computed paths in the bundle
          // URLs.
          Field rootFileField = bIndexClazz.getDeclaredField("rootFile");
          if (null!=rootFileField) {
            rootFileField.setAccessible(true);
            rootFileField.set(null, baseDir.getAbsoluteFile());
          }
        } catch (NoSuchFieldException _nsfe) {
        }

        // Call the main method
        String[] args = (String[]) cmdList.toArray(new String[cmdList.size()]);
        Method mainMethod = bIndexClazz
          .getDeclaredMethod("main", new Class[]{args.getClass()});
        {
          StringBuffer argSb = new StringBuffer();
          for (int ix=0; ix<args.length; ix++){
            argSb.append(" \"").append(args[ix]).append("\"");
          }
          log("Calling bindex with args: "+argSb, Project.MSG_VERBOSE);
        }
        mainMethod.invoke(null, new Object[]{args});
      } catch (Exception e) {
        log("Failed to execute BIndex: " +e.getMessage(), Project.MSG_ERR);
        e.printStackTrace();
      }

    } catch (Exception e) { e.printStackTrace(); }
  }

}
