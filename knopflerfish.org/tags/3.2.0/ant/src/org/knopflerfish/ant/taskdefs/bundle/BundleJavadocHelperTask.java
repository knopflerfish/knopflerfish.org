/*
 * Copyright (c) 2008-2009, KNOPFLERFISH project
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

/**
 * Task that helps building arguments to javadoc.
 *
 * Loads source paths from a file removes duplicates and adds them to
 * a path structure.
 *
 * Loads export package definitions from a file removes duplicates and
 * OSGi specific annotation, then adds them to a comma separated string.
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
 *   <td valign=top>srcRootsFile</td>
 *   <td valign=top>The file to read source tree root directories from.
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>srcPropertyName</td>
 *   <td valign=top>Name of property that the resulting
 *       java source roots are appended to.
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>srcPathId</td>
 *   <td valign=top>Id of a path like structure to append the source
 *       root dirs to. The structure is created if needed.
 *   </td>
 *   <td valign=top>No.<br>No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>exportPkgsFile</td>
 *   <td valign=top>The file to read export package definitions from.
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>pkgPropertyName</td>
 *   <td valign=top>Name of property that the resulting
 *       java package list is appended to.
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 * </table>
 *
 * <h3>Examples</h3>
 */
public class BundleJavadocHelperTask extends Task {

  private File   exportPkgsFile;
  private String pkgPropertyName;

  private File   srcRootsFile;
  private String srcPropertyName;
  private String srcPathId;

  public BundleJavadocHelperTask() {
  }

  /**
   * Set property receiving the file to load export package definitions from.
   */
  public void setExportPkgsFile(File f) {
    this.exportPkgsFile = f;
    log("exportPkgsFile="+exportPkgsFile, Project.MSG_INFO);
  }

  /**
   * Set property receiving the bundle class path pattern.
   */
  public void setPkgPropertyName(String s) {
    if (s!=null && 0==s.length() ) {
      this.pkgPropertyName = null;
    } else {
      this.pkgPropertyName = s;
    }
    log("pkgPropertyName=" +this.pkgPropertyName, Project.MSG_INFO);
  }

  /**
   * Set property receiving the file to load source root directory
   * names from.
   */
  public void setSrcRootsFile(File f) {
    this.srcRootsFile = f;
    log("srcRootsFile="+srcRootsFile, Project.MSG_DEBUG);
  }

  /**
   * Set property receiving the comma separated string with source
   * root directories.
   */
  public void setSrcPropertyName(String s) {
    if (s!=null && 0==s.length()) {
      this.srcPropertyName = null;
    } else {
      this.srcPropertyName = s;
    }
    log("srcPropertyName="+srcPropertyName, Project.MSG_DEBUG);
  }

  /**
   * Set id of path like structure receiving to add source root
   * directories to.
   */
  public void setSrcPathId(String s) {
    if (s!=null && 0==s.length() ) {
      this.srcPathId = null;
    } else {
      this.srcPathId = s;
    }
    log("srcPathId=" +this.srcPathId, Project.MSG_DEBUG);
  }


  // Implements Task
  //
  public void execute() throws BuildException {
    if (null==exportPkgsFile && null!=pkgPropertyName ) {
      throw new BuildException
        ("exportPkgsFile must be set when pkgPropertyName is set.");
    }
    if (null==srcRootsFile &&
        (null!=srcPropertyName || null!=srcPathId)) {
      throw new BuildException
        ("srcRootsFile must be set when srcPropertyName or srcPathId is set.");
    }

    try {
      processSrcRootsFile();
      processExportPkgsFile();
    } catch (Exception e) {
      throw new BuildException(e);
    }
  }

  private void processSrcRootsFile()
    throws FileNotFoundException, IOException
  {
    if (null==srcRootsFile) return;

    BufferedReader in = new BufferedReader(new FileReader(srcRootsFile));
    Set srcRoots = new TreeSet();

    String line = in.readLine();
    while (null!=line) {
      srcRoots.add(line.trim());
      line = in.readLine();
    }
    Project proj = getProject();
    if (null!=srcPropertyName) {
      String sourcepath = proj.getProperty(srcPropertyName);
      if (null==sourcepath) sourcepath = "";

      StringBuffer sb
        = new StringBuffer(sourcepath.length() +50*srcRoots.size());
      sb.append(sourcepath);

      for (Iterator it = srcRoots.iterator(); it.hasNext(); ) {
        if (sb.length()>0) sb.append(",");
        sb.append(it.next());
      }
      proj.setProperty(srcPropertyName, sb.toString());
    }
    if (null!=srcPathId) {
      Path path = new Path(proj);
      for (Iterator it = srcRoots.iterator(); it.hasNext(); ) {
        path.setLocation(new File( (String) it.next() ));
      }
      log("Created path: "+path,Project.MSG_DEBUG);
      Path oldPath = (Path) proj.getReference(srcPathId);
      if (null!=oldPath) {
        oldPath.add(path);
        log(srcPathId +" after extension: "+oldPath, Project.MSG_VERBOSE);
      } else {
        proj.addReference(srcPathId, path);
        log("Created \"" +srcPathId +"\": "+path, Project.MSG_VERBOSE);
      }
    }
  }

  private void processExportPkgsFile()
    throws FileNotFoundException, IOException
  {
    if (null==exportPkgsFile) return;

    BufferedReader in = new BufferedReader(new FileReader(exportPkgsFile));
    Set pkgs = new TreeSet();

    String line = in.readLine();
    while (null!=line) {
      if (!BundleManifestTask.BUNDLE_EMPTY_STRING.equals(line)) {
        Iterator expIt = Util.parseEntries
          ("export.package", line.trim(), true, true, false );
        while (expIt.hasNext()) {
          Map expEntry = (Map) expIt.next();
          String exPkg = (String) expEntry.get("$key");
          pkgs.add(exPkg);
        }
      }
      line = in.readLine();
    }
    Project proj = getProject();
    if (null!=pkgPropertyName) {
      String packagenames = proj.getProperty(pkgPropertyName);
      if (null==packagenames) packagenames = "";

      StringBuffer sb
        = new StringBuffer(packagenames.length() +50*pkgs.size());
      sb.append(packagenames);

      for (Iterator it =pkgs.iterator(); it.hasNext(); ) {
        if (sb.length()>0) sb.append(",");
        sb.append(it.next());
      }
      proj.setProperty(pkgPropertyName, sb.toString());
      log("Setting property '" +pkgPropertyName +"' -> " +sb.toString(),
          Project.MSG_VERBOSE);
    }
  }


}
