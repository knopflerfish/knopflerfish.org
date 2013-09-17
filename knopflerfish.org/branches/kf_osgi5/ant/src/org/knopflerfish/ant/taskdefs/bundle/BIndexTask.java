/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ResourceCollection;

public class BIndexTask extends Task {

  private final List<ResourceCollection>    filesets = new ArrayList<ResourceCollection>();

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
    filesets.add(set);
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
  @Override
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
    }

    final Set<File> jarSet = new HashSet<File>();

    log("loading bundle info...", Project.MSG_VERBOSE);

    try {
      for (int i = 0; i < filesets.size(); i++) {
        final FileSet          fs      = (FileSet) filesets.get(i);
        final DirectoryScanner ds      = fs.getDirectoryScanner(getProject());
        final File             projDir = fs.getDir(getProject());

        final String[] srcFiles = ds.getIncludedFiles();

        for (final String srcFile : srcFiles) {
          final File file = new File(projDir, srcFile);
          if(file.getName().endsWith(".jar")) {
            jarSet.add(file);
          }
        }
      }

      final Set<File> removeSet = new HashSet<File>();
      for (final Object element : jarSet) {
        final File   file = (File)element;
        final String name = file.getAbsolutePath();
        if(-1 != name.indexOf("_all-")) {
          final File f2 = new File(Util.replace(name, "_all-", "-"));
          removeSet.add(f2);
          log("skip " + f2, Project.MSG_VERBOSE);
        }
      }

      if(removeSet.size() > 0) {
        log("skipping " + removeSet.size() + " bundles", Project.MSG_INFO);
      }

      for (final Object element : removeSet) {
        final File f = (File)element;
        jarSet.remove(f);
      }

      log("writing bundle repository to " + outFile, Project.MSG_VERBOSE);
      final List<String> cmdList = new ArrayList<String>( 10 + jarSet.size() );

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
      for (final Object element : jarSet) {
        final String file = ((File) element).getAbsolutePath();
        cmdList.add(file);
      }

      try {
        // Call org.osgi.impl.bundle.bindex.Index.main(args) to
        // generate the bindex.xml file.
        final Class<?> bIndexClazz = Class.forName("org.osgi.impl.bundle.bindex.Index");

        // Prepend the -d <rootFile> option
        cmdList.add(2, baseDir.getAbsolutePath());
        cmdList.add(2, "-d");
        try {
          // Hack for older BIndex without -d option. Use reflection
          // to set org.osgi.impl.bundle.bindex.Index.rootFile to
          // baseDir to get correctly computed paths in the bundle
          // URLs.
          final Field rootFileField = bIndexClazz.getDeclaredField("rootFile");
          if (null!=rootFileField) {
            rootFileField.setAccessible(true);
            rootFileField.set(null, baseDir.getAbsoluteFile());
          }
        } catch (final NoSuchFieldException _nsfe) {
        }

        // Call the main method
        final String[] args = cmdList.toArray(new String[cmdList.size()]);
        final Method mainMethod = bIndexClazz
          .getDeclaredMethod("main", new Class[]{args.getClass()});
        {
          final StringBuffer argSb = new StringBuffer();
          for (final String arg : args) {
            argSb.append(" \"").append(arg).append("\"");
          }
          log("Calling bindex with args: "+argSb, Project.MSG_VERBOSE);
        }
        mainMethod.invoke(null, new Object[]{args});
      } catch (final Exception e) {
        log("Failed to execute BIndex: " +e.getMessage(), Project.MSG_ERR);
        e.printStackTrace();
      }

    } catch (final Exception e) { e.printStackTrace(); }
  }

}
