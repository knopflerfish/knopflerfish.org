/*
 * Copyright (c) 2008-2013, KNOPFLERFISH project
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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.OrSelector;

import org.osgi.framework.Constants;

import org.knopflerfish.ant.taskdefs.bundle.Util.HeaderEntry;

/**
 * Task that helps building arguments to javadoc.
 *
 * <ul>
 *   <li>Loads source paths from a file removes duplicates and adds them to
 *       a path structure.
 *
 *   <li>Loads export package definitions from a file removes
 *       duplicates and OSGi specific annotation, then adds them to a
 *       comma separated string that will be set as the value of a
 *       named property.
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
 *   <td valign=top>exportPkgsValue</td>
 *   <td valign=top>A single Export-Package header value to convert.
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>pkgPropertyName</td>
 *   <td valign=top>Name of property that the resulting
 *       java package list is appended to.
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>srcDir</td>
 *   <td valign=top>
 *
 *     Path to the root of a Java source tree. Used together with
 *     {@code exportPkgsValue} to check if there are source files
 *     availble for any of the exported packages. The result is
 *     assigned to the property named by {@code
 *     pkgSrcAvailPropertyName}.
 *
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>pkgSrcAvailPropertyName</td>
 *   <td valign=top>
 *
 *      Name of property that will be set to {@code true} if any of
 *      the packages present in the value of {@code exportPkgsValue}
 *      has a source file (*.java or packages.html) in the Java source
 *      tree specified by {@code srcDir}.
 *
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>pkgWithSourcePropertyName</td>
 *   <td valign=top>
 *
 *     Name of property that will be set to the sub-list of packages
 *     with source code of the java package list saved in {@code
 *     pkgPropertyName}.
 *
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *
 * </table>
 *
 * <h3>Examples</h3>
 *
 * Create a comma separated list of package names (without attributes,
 * and directives) and save the list as the value of the property with
 * name {@code javadoc.packages}. The package names are read from the
 * file that is the value of the property {@code exported.file}. This
 * file contains the value of one Export-Package definition per line.
 * <pre>
 * &lt;bundle_javadoc_helper exportPkgsFile="${exported.file}"
 *                           pkgPropertyName="javadoc.packages"/&gt;
 * </pre>
 *
 */
public class BundleJavadocHelperTask extends Task {

  private File   exportPkgsFile;
  private String exportPkgsValue;
  private String pkgPropertyName;

  private File   srcDir;
  private String pkgSrcAvailPropertyName;
  private String pkgWithSourcePropertyName;

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
    log("exportPkgsFile="+exportPkgsFile, Project.MSG_DEBUG);
  }

  /**
   * Set property receiving the export package definition to handle.
   */
  public void setExportPkgsValue(String s) {
    this.exportPkgsValue = s;
    log("exportPkgsValue="+exportPkgsValue, Project.MSG_DEBUG);
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
    log("pkgPropertyName=" +this.pkgPropertyName, Project.MSG_DEBUG);
  }

  /**
   * Property receiving the root of the source tree to check for
   * availability of javadoc source files in when setting {@code
   * pkgSrcAvailPropertyName}.
   */
  public void setSrcDir(File f) {
    this.srcDir = f;
    log("srcDir="+srcDir, Project.MSG_DEBUG);
  }

  /**
   * Set name of property to be set if there are javadoc source files
   * for the packages selected by the value of {@code pkgPropertyName}
   * available in {@code srcDir}.
   */
  public void setPkgSrcAvailPropertyName(String s) {
    if (s!=null && 0==s.length() ) {
      this.pkgSrcAvailPropertyName = null;
    } else {
      this.pkgSrcAvailPropertyName = s;
    }
    log("pkgSrcAvailPropertyName=" +this.pkgSrcAvailPropertyName,
        Project.MSG_DEBUG);
  }

  /**
   * Set property receiving the filtered list of exported
   * packages. I.e., the list of exported packages that has at least
   * one source file.
   */
  public void setPkgWithSourcePropertyName(String s) {
    if (s!=null && 0==s.length() ) {
      this.pkgWithSourcePropertyName = null;
    } else {
      this.pkgWithSourcePropertyName = s;
    }
    log("pkgWithSourcePropertyName=" +this.pkgWithSourcePropertyName,
        Project.MSG_DEBUG);
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

  /**
   * The set of java package names for exported packages.
   */
  final TreeSet<String> ePkgs = new TreeSet<String>();


  // Implements Task
  //
  @Override
  public void execute() throws BuildException {
    if ((null==exportPkgsFile&&null==exportPkgsValue)
        && null!=pkgPropertyName ) {
      throw new BuildException
        ("When pkgPropertyName is set, exportPkgsValue or exportPkgsFile "
         +"must also be set.");
    }

    if (null!=pkgSrcAvailPropertyName && null==srcDir) {
      throw new BuildException
        ("When pkgSrcAvailPropertyName is set, srcDir "
         +"must also be set.");
    }

    if (null!=pkgWithSourcePropertyName && null==pkgSrcAvailPropertyName) {
      throw new BuildException
        ("When pkgWithSourcePropertyName is set, pkgSrcAvailPropertyName "
         +"must also be set.");
    }

    if (null==srcRootsFile &&
        (null!=srcPropertyName || null!=srcPathId)) {
      throw new BuildException
        ("srcRootsFile must be set when srcPropertyName or srcPathId is set.");
    }

    try {
      processSrcRootsFile();
      processExportPkgs();
      processPkgSrcsAvailable();
    } catch (final Exception e) {
      throw new BuildException(e);
    }
  }

  private void processSrcRootsFile()
    throws FileNotFoundException, IOException
  {
    if (null==srcRootsFile) {
      return;
    }

    final Set<String> srcRoots = new TreeSet<String>();
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(srcRootsFile));

      String line = in.readLine();
      while (null != line) {
        srcRoots.add(line.trim());
        line = in.readLine();
      }
    } finally {
      if (in != null) {
        in.close();
        in = null;
      }
    }
    final Project proj = getProject();
    if (null!=srcPropertyName) {
      String sourcepath = proj.getProperty(srcPropertyName);
      if (null==sourcepath) {
        sourcepath = "";
      }

      final StringBuilder sb = new StringBuilder(sourcepath.length() + 50 * srcRoots.size());
      sb.append(sourcepath);

      for (final Object element : srcRoots) {
        if (sb.length()>0) {
          sb.append(",");
        }
        sb.append(element);
      }
      proj.setProperty(srcPropertyName, sb.toString());
    }
    if (null!=srcPathId) {
      final Path path = new Path(proj);
      for (final Object element : srcRoots) {
        path.setLocation(new File( (String) element ));
      }
      log("Created path: "+path,Project.MSG_DEBUG);
      final Path oldPath = (Path) proj.getReference(srcPathId);
      if (null!=oldPath) {
        oldPath.add(path);
        log(srcPathId +" after extension: "+oldPath, Project.MSG_VERBOSE);
      } else {
        proj.addReference(srcPathId, path);
        log("Created \"" +srcPathId +"\": "+path, Project.MSG_VERBOSE);
      }
    }
  }

  private void processExportPkgs()
    throws FileNotFoundException, IOException
  {
    // Unconditional process of exportPkgsFile / exportPkgsValue since
    // result is used by other process-methods.
    if (exportPkgsFile != null) {
      BufferedReader in= null;

      try {
        in = new BufferedReader(new FileReader(exportPkgsFile));

        String line = in.readLine();
        while (null!=line) {
          handleOneExportPackageLine(line);
          line = in.readLine();
        }
      } finally {
        if (in != null) {
          in.close();
          in = null;
        }
      }
    }

    if (exportPkgsValue != null) {
      handleOneExportPackageLine(exportPkgsValue);
    }


    if (null!=pkgPropertyName) {
      final Project proj = getProject();
      String packagenames = proj.getProperty(pkgPropertyName);
      if (null==packagenames) {
        packagenames = "";
      }

      final StringBuilder sb = new StringBuilder(packagenames.length() + 50 * ePkgs.size());
      sb.append(packagenames);

      for (final Object element : ePkgs) {
        if (sb.length()>0) {
          sb.append(",");
        }
        sb.append(element);
      }
      proj.setProperty(pkgPropertyName, sb.toString());
      log("Setting property '" +pkgPropertyName +"' -> " +sb.toString(),
          Project.MSG_VERBOSE);
    }
  }

  private void handleOneExportPackageLine(final String line)
  {
    if (!BundleManifestTask.BUNDLE_EMPTY_STRING.equals(line)) {
      final List<HeaderEntry> entries =
        Util.parseManifestHeader(Constants.EXPORT_PACKAGE, line.trim(), false,
                                 true, false);

      for (final HeaderEntry entry : entries) {
        for (final String pkg : entry.getKeys()) {
          ePkgs.add(pkg);
        }
      }
    }
  }

  private void processPkgSrcsAvailable()
  {
    // If no srcDir then there are no source files in it.
    if (srcDir==null || !srcDir.exists()) {
      return;
    }

    final Project proj = getProject();

    final TreeSet<String> pkgNamesWithSource = new TreeSet<String>();
    boolean pkgSrcAvailable = false;

    for (final Object element : ePkgs) {
      final String pkg = (String) element;

      final FileSet fileSet = new FileSet();
      fileSet.setProject(proj);
      fileSet.setDir(srcDir);

      final OrSelector orSelector = new OrSelector();
      fileSet.add(orSelector);

      final FilenameSelector fnsJ = new FilenameSelector();
      fnsJ.setName(pkg.replace('.', File.separatorChar)
                   + File.separatorChar + "*.java");
      orSelector.add(fnsJ);

      final FilenameSelector fnsP = new FilenameSelector();
      fnsP.setName(pkg.replace('.', File.separatorChar)
                  + File.separatorChar + "package.html");
      orSelector.add(fnsP);

      log(" Package '" +pkg +"' has sources: " + fileSet.toString(),
          Project.MSG_DEBUG);

      if (fileSet.size() > 0) {
        pkgSrcAvailable |= true;
        pkgNamesWithSource.add(pkg);
      }
    }

    if (pkgSrcAvailable) {
      proj.setProperty(pkgSrcAvailPropertyName, "true");
      log("Setting property '" +pkgSrcAvailPropertyName +"' -> 'true'",
          Project.MSG_VERBOSE);
    }

    if (pkgWithSourcePropertyName != null) {
      final StringBuilder sb = new StringBuilder(50 * pkgNamesWithSource.size());

      for (final Object element : pkgNamesWithSource) {
        if (sb.length()>0) {
          sb.append(",");
        }
        sb.append(element);
      }

      proj.setProperty(pkgWithSourcePropertyName, sb.toString());
      log("Setting property '" +pkgWithSourcePropertyName +"' -> "
          + sb.toString(), Project.MSG_VERBOSE);
    }
  }

}
