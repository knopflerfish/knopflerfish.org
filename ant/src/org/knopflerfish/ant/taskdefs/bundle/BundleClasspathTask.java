/*
 * Copyright (c) 2006-2011, KNOPFLERFISH project
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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;

/**
 * Task that translates the value of the OSGi specified manifest
 * header <tt>Bundle-Classpath</tt> into either a file set or pattern
 * suitable for use as the includes attribute in a file set that will
 * find all classes and jars that the framework may use given the
 * specified Bundle-Classpath manifest attribute.
 *
 * <p>This task may also be used as a nested element in the bundle
 * info task. In that case it will be used to generate a list of file
 * sets, one for each entry in the given bundle classpath. If the
 * excludes attribute is given the file sets will use that as exclude
 * pattern otherwise the includes attriubte is used as includes
 * pattern. If the bundle classpath entry is a jar-file then a
 * ZipFileSet will be created for it with the jar-file as source and
 * the excludes (or includes) attribute as excludes (includes)
 * pattern.</p>
 *
 * <h3>Parameters</h3>
 *
 * <table border=>
 *  <tr>
 *   <td valign=top><b>Attribute</b></td>
 *   <td valign=top><b>Description</b></td>
 *   <td valign=top><b>Required</b></td>
 *  </tr>
 *  <tr>
 *   <td valign=top>BundleClasspath</td>
 *   <td valign=top>The bundle class path to convert into an includes
 *       pattern.
 *       <p>
 *       If unset, set to empty string, or set to the special empty
 *       value <code>[bundle.emptystring]</code> the default bundle
 *       classpath, "." will be used.
 *       </p>
 *       <p>
 *       Note: The current value of this property will be overwritten
 *       by the derived pattern.
 *       </p>
 *   </td>
 *   <td valign=top>No.<br> Default value is "."</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>propertyName</td>
 *   <td valign=top>Name of property that will receive the resulting
 *       pattern.
 *   </td>
 *   <td valign=top>Yes.<br> No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>dir</td>
 *   <td valign=top>The directory to use as root directory in the
 *       created fileset.
 *   </td>
 *   <td valign=top>Yes.<br> No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>filesetId</td>
 *   <td valign=top>Id of a file set with include patterns based on
 *       the given <tt>BundleClasspath</tt> and base directory given
 *       by <tt>dir</tt>. If <tt>dir</tt> is not given or
 *       non-existing then an empty file set is created.
 *   </td>
 *   <td valign=top>No.<br>No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>includes</td>
 *   <td valign=top>Includes pattern to apply to each file set created
 *       from the entries in the bundle classpath when building a list
 *       of file sets.
 *   </td>
 *   <td valign="top">At least one of includes and excludes must be
 *       given when building a list of file sets from the bundle
 *       classpath.<br>
 *       No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign="top">excludes</td>
 *   <td valign=top>Excludes pattern to apply to each file set created
 *       from the entires in the bundle classpath when building a list
 *       of file sets. Note: This attribute is only used when the
 *       &lt;bundleclasspath&gt; is used as a nested element inside a
 *       &lt;bundleinfo&gt; element.
 *   </td>
 *   <td valign=top>At least one of includes and excludes must be
 *       given when building a list of file sets from the bundle
 *       classpath.<br>
 *       No default value.</td>
 *  </tr>
 * </table>
 *
 * <h3>Examples</h3>
 * The table below shows how different bundle class path entries are
 * translated int patterns.
 * <table border="1">
 *  <tr><th>Entry</th><th>Pattern</th></tr>
 *  <tr><td>.</td><td>&#x2A;&#x2A;/&#x2A;.class</td></tr>
 *  <tr><td> rxtx</td><td>rxtx/&#x2A;&#x2A;/&#x2A;.class</td></tr>
 *  <tr><td>/rxtx</td><td>rxtx/&#x2A;&#x2A;/&#x2A;.class</td></tr>
 *  <tr><td>required.jar</td><td>required.jar</td></tr>
 *  <tr><td>xx/required.jar</td><td>xx/required.jar</td></tr>
 *  <tr><td>/xx/required.jar</td><td>xx/required.jar</td></tr>
 * </table>
 */
public class BundleClasspathTask extends Task {

  private File   dir;
  private String filesetId;
  private String bundleClasspath = ".";
  private String propertyName;
  private String includes = null;
  private String excludes = null;

  public BundleClasspathTask() {
  }

  /**
   * Set bundle class path to create a pattern for.
   */
  public void setBundleClasspath(String s) {
    this.bundleClasspath
      = (BundleManifestTask.BUNDLE_EMPTY_STRING.equals(s)) ? "." : s;
    log("bundleClasspath="+bundleClasspath, Project.MSG_DEBUG);
  }

  /**
   * Set property receiving the bundle class path pattern.
   */
  public void setPropertyName(String s) {
    this.propertyName  = s;
    log("propertyName="+propertyName, Project.MSG_DEBUG);
  }

  /**
   * Set property receiving the file set root directory.
   */
  public void setDir(File f) {
    this.dir = f;
    log("dir="+dir, Project.MSG_DEBUG);
  }

  /**
   * Set property receiving the file set id.
   */
  public void setFilesetId(String s) {
    this.filesetId = s;
    log("filesetId="+filesetId, Project.MSG_DEBUG);
  }

  /**
   * Set the includes pattern to use in the collection of file sets
   * returned by {@link #getFileSets(boolean)}.
   */
  public void setIncludes(String s) {
    this.includes = s;
    log("includes="+this.includes, Project.MSG_DEBUG);
  }

  /**
   * Set the excludes pattern to use in the collection of file sets
   * returned by {@link #getFileSets(boolean)}.
   */
  public void setExcludes(String s) {
    this.excludes = s;
    log("excludes="+this.excludes, Project.MSG_DEBUG);
  }

  /**
   * Get a collection of file sets selecting all classes in the bundle
   * class path that matches the given pattern.
   *
   * @return A list with file sets, one file set for each entry on the
   *         bundle class path. If the entry is for a Jar/Zip file
   *         then its list item will be a zip file set.
   */
  public List getFileSets(boolean failOnClassPath)
  {
    final List res = new ArrayList();
    final Project proj = getProject();

    if (dir==null) {
      throw new BuildException("The dir attribute (root of the bundle "
                               +"class path) is required.");
    } else if (!dir.exists()) {
      log("Bundle class path root dir '" +dir
          +"' does not exist, returning empty list of file sets.",
          Project.MSG_VERBOSE);
      return res;
    }

    if (null==bundleClasspath || 0==bundleClasspath.length() ) {
      // Use the default bundle class path
      bundleClasspath = ".";
    }

    // Convert path entries to file sets.
    final StringTokenizer st = new StringTokenizer(bundleClasspath, ",");
    while (st.hasMoreTokens()) {
      String entry = st.nextToken().trim();
      if (entry.startsWith("/")) {
        // Entry is a relative path, must not start with a '/', fix it.
        entry = entry.substring(1);
      }

      FileSet fileSet = null;
      File src= new File(dir, entry);

      // Bundle class path entries are either directories or jar/zip-files!
      if (src.isDirectory()) {
        fileSet = new FileSet();
        fileSet.setDir(src);
        fileSet.setProject(getProject());
      } else if (src.exists()) {
        fileSet = new ZipFileSet();
        ((ZipFileSet) fileSet).setSrc(src);
      } else {
        final StringBuffer msg = new StringBuffer();
        msg.append("The following entry in the Bundle-ClassPath")
          .append(" header doesn't exist in the bundle: ")
          .append(entry)
          .append(".");
        if (failOnClassPath) {
          log(msg.toString(), Project.MSG_ERR);
          throw new BuildException(msg.toString(), getLocation());
        } else {
          log(msg.toString(), Project.MSG_WARN);
          continue;
        }
      }

      fileSet.setProject(proj);
      if (null!=includes) {
        fileSet.setIncludes(includes);
      }
      if (null!=excludes) {
        fileSet.setExcludes(excludes);
      }
      res.add(fileSet);
      log("Added FileSet with root '" +src +"', includes: '" +includes
          +"', excludes: '" +excludes +"'.", Project.MSG_DEBUG);
    }

    return res;
  }


  // Implements Task
  //
  public void execute() throws BuildException {
    if (   (null==propertyName || 0==propertyName.length())
           && (null==filesetId    || 0==filesetId.length()) ) {
      throw new BuildException
        ("Either propertyName or filesetId must be given.");
    }
    if (null!=filesetId && dir==null) {
      throw new BuildException
        ("dir is required when filesetId is given.");
    }

    if (null==bundleClasspath || 0==bundleClasspath.length() )
      bundleClasspath = ".";

    final StringBuffer sb = new StringBuffer(100);

    // Convert path entries to patterns.
    final StringTokenizer st = new StringTokenizer(bundleClasspath, ",");
    while (st.hasMoreTokens()) {
      String entry = st.nextToken().trim();
      if (entry.startsWith("/")) {
        // Entry is a relative path, must not start with a '/', fix it.
        entry = entry.substring(1);
      }

      if (".".equals(entry)) {
        sb.append("**/*.class");
      } else if (entry.endsWith(".jar")) {
        sb.append(entry);
      } else {
        sb.append(entry + "/**/*.class");
      }
      if (st.hasMoreTokens())
        sb.append(",");
    }

    final Project proj = getProject();

    // Conversion done - write back properties
    if (null!=propertyName) {
      proj.setProperty(propertyName, sb.toString());
      log("Converted \"" +bundleClasspath +"\" to pattern \""
          +sb.toString() +"\"",
          Project.MSG_VERBOSE);
    }

    if (null!=filesetId) {
      final FileSet fileSet = new FileSet();
      fileSet.setProject(proj);
      if (dir.exists()) {
        fileSet.setDir(dir);
        fileSet.setIncludes(sb.toString());
      } else {
        log("Bundle class path root dir '" +dir
            +"' does not exist, returning empty file set.",
            Project.MSG_DEBUG);
        fileSet.setDir(new File("."));
        fileSet.setExcludes("**/*");
      }
      proj.addReference(filesetId, fileSet);
      log("Converted bundle class path \"" +bundleClasspath
          +"\" to file set with id '" +filesetId
          +"' and files \"" +fileSet +"\"",
          Project.MSG_VERBOSE);
    }
  }
}
