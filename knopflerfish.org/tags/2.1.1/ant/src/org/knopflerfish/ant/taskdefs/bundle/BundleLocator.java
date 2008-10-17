/*
 * Copyright (c) 2008-2008, KNOPFLERFISH project
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.osgi.framework.Version;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
//import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * A task that given a partial bundle name and a fileset with
 * bundles, will select the bundle with the highest version number and
 * a name that matches. Bundles names are supposed to be on the form
 * <tt>name-<it>Major</it>.<it>Minor</it>.<it>Micro</it>.<it>sub</it>.jar</tt>,
 * i.e., a name followed by '-' then a valid OSGi version number.
 *
 * This task can also iterate over a path and replace all
 * non-exisiting file resources in it that has a name ending with
 * <tt>-N.N.N.jar</tt> with the corresponding bundle with the highest
 * version from the given file set.
 *
 * <h3>Parameters</h3>
 *
 * <table border=>
 *  <tr>
 *   <td valign=top><b>Attribute</b></td>
 *   <td valign=top><b>Description</b></td>
 *   <td valign=top><b>Required</b></td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>bundleName</td>
 *   <td valign=top>
 *   The name of the bundle to look for. I.e., the full name up to
 *   the last '-'-character (excluded).
 *   </td>
 *   <td valign=top>Either <tt>bundleName</tt> or
 *   <tt>classPathRef</tt> must be given.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>property</td>
 *   <td valign=top>
 *     The name of a project property to be assigned the location of
 *     the matching bundle.
 *   </td>
 *   <td valign=top>Yes when <tt>bundleName</tt> is specified.<br>
 *                  No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>classPathRef</td>
 *   <td valign=top>
 *   The reference name (id) of a path-structure to transform.
 *   </td>
 *   <td valign=top>Either <tt>bundleName</tt> or
 *   <tt>classPathRef</tt> must be given.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>newClassPathId</td>
 *   <td valign=top>
 *     The transformed path-structure will be added to the current
 *     project using this name (id).
 *   </td>
 *   <td valign=top>Yes when <tt>classPathRef</tt> is specified.<br>
 *                  No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>failOnMissingBundles</td>
 *   <td valign=top>
 *     If a file with name <tt><it>bundleName</it>-N.N.N.jar</tt> is
 *     found on the classpath to transform and there is no matching
 *     bundle in the file set then fail the build if set to
 *     <tt>true</tt>. Same applies if the given <tt>bundleName</tt>
 *     can not be transformed.
 *   </td>
 *   <td valign=top>No.<br>Defaults to <tt>true</tt>.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>replacefilterfile</td>
 *   <td valign=top>
 *     Creates a property file suitable for use as the
 *     replacefilterfile argument in the replace-task. Keys in the
 *     property file will be the bundle name on the form
 *     <tt><it>@bundleName</it>-N.N.N.jar@</tt>, values is the expanded
 *     file path relative to the root-directory of the file set that
 *     the expansion orginates from. The "@" surrounding the key are
 *     needede to avoid problem if the name of one bundle is contained
 *     within the name of another bundle. E.g.,
 *     <tt>kxml-N.N.N.jar</tt> and <tt>xml-N.N.N.jar</tt>.
 *   </td>
 *   <td valign=top>No.<br>No default value.</td>
 *  </tr>
 *
 * </table>
 *
 * <h3>Parameters specified as nested elements</h3>
 * <h4>fileset</h4>
 *
 * (required)<br>
 * <p>
 * The jar files to match against must be specified as a fileset.
 * </p>
 *
 */
public class BundleLocator extends Task {

  private final Vector    filesets = new Vector();
  private final FileUtils fileUtils;

  private String    bundleName           = null;
  private String    property             = null;
  private Reference classPathRef         = null;
  private String    newClassPathId       = null;
  private boolean   failOnMissingBundles = true;
  private File      replacefilterfile    = null;

  public BundleLocator() {
    fileUtils = FileUtils.newFileUtils();
  }

  public void setProperty(String s) {
    this.property = s;
  }

  public void setBundleName(String s) {
    this.bundleName = s;
  }

  public void addFileset(FileSet set) {
    filesets.addElement(set);
  }

  public void setClassPathRef(Reference r) {
    classPathRef = r;
  }

  public void setNewClassPathId(String s) {
    newClassPathId = s;
  }

  public void setFailOnMissingBundles(boolean b) {
    failOnMissingBundles = b;
  }

  public void setReplacefilterfile(File f) {
    replacefilterfile = f;
  }


  // Implements Task
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
    }
    /** Mapping from <tt>bundleName</tt> without the version suffix
     * (<tt>-N.N.N.jar</tt>) to the BundleInfo object of the bundle
     * within the file set with the highest version.
     */
    final Map bundleMap = buildBundleVersionMap();

    if (null!=bundleName) {
      setProperty(bundleMap);
    }

    if (classPathRef!=null && newClassPathId!=null) {
      transformPath(bundleMap);
    }

    if (null!=replacefilterfile) {
      writeReplaceFilterFile(bundleMap);
    }
  }


  /**
   * Holder of the value in the bundle version map.
   */
  static class BundleInfo
  {
    /** The short bundle name. I.e., the name to the left of the last '-'.*/
    public String  name;
    /** The bundles version. I.e., the part to the right of the last '-'.*/
    public Version version;
    /** The relative path from the root of the file set holding the bundle.*/
    public String  relPath;
    /** The absolute path of the bundle. */
    public File file;
  }

  /**
   * Traverses the given file sets, adding an entry to the returned
   * map for each bundle with a name on the form
   * <tt><it>bundleName</it>-<it>Version</it>.jar</tt>. The version
   * must be a valid version according to the OSGi version specifivation.
   *
   * The key of the entry in the returned map is the
   * <tt><it>bundleName</it></tt> and the value is the corresponding
   * <tt>BundleInfo</tt>-object.
   * @return map with the bundles with the highest version.
   */
  Map buildBundleVersionMap()
  {
    final Map map = new HashMap();

    try {
      for (int i = 0; i < filesets.size(); i++) {
        final FileSet          fs      = (FileSet) filesets.elementAt(i);
        final File             projDir = fs.getDir(project);
        if (!projDir.exists()) {
          log("Skipping nested file set rooted at '" +projDir
              +"' since that directory does not exist.",
              Project.MSG_WARN);
          continue;
        }
        final DirectoryScanner ds      = fs.getDirectoryScanner(project);

        final String[] srcFiles = ds.getIncludedFiles();
        final String[] srcDirs  = ds.getIncludedDirectories();

        for (int j = 0; j < srcFiles.length ; j++) {
          final File file = new File(projDir, srcFiles[j]);
          final String fileName = file.getName();
          if(file.getName().endsWith(".jar")) {
            log( "Found candidate " +file, Project.MSG_DEBUG);
            int ix = fileName.lastIndexOf('-');
            if (0<ix) {
              final String bundleName = fileName.substring(0,ix);
              final String versionS
                = fileName.substring(ix+1,fileName.length()-4);
              Version newVersion = Version.emptyVersion;
              try {
                newVersion = new Version(versionS);
              } catch (NumberFormatException nfe) {
                log("Invalid or missing bundle version in bundle name '"
                    +fileName +"' assuming " +newVersion +".",
                    Project.MSG_WARN);
              }
              BundleInfo bi = (BundleInfo) map.get(bundleName);
              if (null==bi) {
                bi = new BundleInfo();
                bi.name = bundleName;
                bi.relPath = srcFiles[j];
                bi.version = newVersion;
                bi.file = file;
                map.put(bundleName, bi);
                log("Found bundle " +fileName, Project.MSG_VERBOSE);
              } else if (newVersion.compareTo(bi.version)>0) {
                bi.version = newVersion;
                bi.relPath = srcFiles[j];
                bi.file = file;
                log("Found better version " +fileName, Project.MSG_VERBOSE);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed locate bundle: " + e, e);
    }
    return map;
  }

  private void setProperty(Map bundleMap)
  {
    log("Searching for a versioned bundle named '" +bundleName +"'.",
        Project.MSG_VERBOSE);
    final BundleInfo bi = (BundleInfo) bundleMap.get(bundleName);

    if (bi!=null && property!=null) {
      project.setProperty(property,bi.file.getAbsolutePath());
      log("Selected " +bi.file, Project.MSG_VERBOSE);
    }
  }

  private void transformPath(Map bundleMap)
  {
    final Path newPath = new Path(project);
    log("Updating bundle paths in class path reference '"
        +classPathRef +"'.",
        Project.MSG_VERBOSE);

    String[] pathElements = null;
    try {
      final Path path = (Path) classPathRef.getReferencedObject();
      pathElements = path.list();
    } catch (BuildException e) {
      // Unsatisfied ref in the given path; can not expand.
      // Make the new path a reference to the old one.
      log("Unresolvable reference in '" +classPathRef
          +"' can not expand bundle names in it.",
          Project.MSG_WARN);
      newPath.setRefid(classPathRef);
    }

    if (null!=pathElements) {
      for (int i=0; i<pathElements.length; i++) {
        final File pathElement = new File(pathElements[i]);
        boolean added = false;
        log("path element: "+pathElement, Project.MSG_DEBUG);
        if (!pathElement.exists()) {
          log("Found non existing path element: " +pathElement,
              Project.MSG_DEBUG);
          final String fileName = pathElement.getName();
          if (fileName.endsWith("-N.N.N.jar")) {
            final String key = fileName.substring(0,fileName.length() -10);
            final BundleInfo bi = (BundleInfo) bundleMap.get(key);
            if (bi!=null) {
              final String filePath = bi.file.getAbsolutePath();
              newPath.setPath(filePath);
              added = true;
              log(fileName +" => " +filePath, Project.MSG_VERBOSE);
            } else {
              final int logLevel = failOnMissingBundles
                ? Project.MSG_ERR : Project.MSG_INFO;
              log("No match for '" +fileName
                  +"' when expanding the path named '"
                  +classPathRef.getRefId() +"'.",
                  logLevel);
              log("Bundles with known version: " +bundleMap.keySet(),
                  logLevel);
              if (failOnMissingBundles) {
                throw new BuildException
                  ("No bundle with name like '" +fileName+"' found.");
              }
              }
          }
        }
        if (!added) {
          newPath.setPath(pathElement.getAbsolutePath());
        }
      }
      log(newClassPathId +" = " +newPath, Project.MSG_INFO);
    }
    project.addReference(newClassPathId, newPath);
  } // end of transform path structure.


  private void writeReplaceFilterFile(Map bundleMap)
  {
    final Properties props = new Properties();
    for (Iterator it = bundleMap.values().iterator(); it.hasNext();) {
      final BundleInfo bi = (BundleInfo) it.next();
      //Note: since the path is an URL we must ensure that '/' is used.
      props.put("@" +bi.name +"-N.N.N.jar@", bi.relPath.replace('\\','/'));
    }
    OutputStream out = null;
    try {
      out= new FileOutputStream(replacefilterfile);
      props.store(out, "Bundle Version Expansion Mapping");
    } catch (IOException ioe) {
      log("Failed to write replacefilterfile, "+replacefilterfile
          +", reason: "+ioe,
          Project.MSG_ERR);
      throw new BuildException("Failed to write replacefilterfile",ioe);
    } finally {
      if (null!=out) {
        try { out.close(); } catch (IOException _ioe) {}
      }
    }
  }

}
