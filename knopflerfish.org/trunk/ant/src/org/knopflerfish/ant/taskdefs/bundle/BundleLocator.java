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
import org.apache.tools.ant.types.resources.FileResource;
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
 *     <tt><it>bundleName</it>-N.N.N.jar</tt>, values is the expanded
 *     file path relative to the root-directory of the file set that
 *     the expansion orginates from.
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

  private Vector    filesets = new Vector();
  private FileUtils fileUtils;

  private String bundleName = null;
  private String property   = null;
  private Reference classPathRef = null;
  private String newClassPathId = null;
  private boolean failOnMissingBundles = true;
  private File replacefilterfile = null;

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
    // bundleName -> location of the matching bundle with the highest version.
    Map bundleMap = buildBundleVersionMap();

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

  // bundleName -> location of the matching bundle with the highest version.
  Map buildBundleVersionMap()
  {
    Map map = new HashMap();

    try {
      for (int i = 0; i < filesets.size(); i++) {
        FileSet          fs      = (FileSet) filesets.elementAt(i);
        File             projDir = fs.getDir(project);
        if (!projDir.exists()) {
          log("Skipping nested file set rooted at '" +projDir
              +"' since that directory does not exist.",
              Project.MSG_WARN);
          continue;
        }
        DirectoryScanner ds      = fs.getDirectoryScanner(project);

        String[] srcFiles = ds.getIncludedFiles();
        String[] srcDirs  = ds.getIncludedDirectories();

        for (int j = 0; j < srcFiles.length ; j++) {
          File file = new File(projDir, srcFiles[j]);
          String fileName = file.getName();
          if(file.getName().endsWith(".jar")) {
            log( "Found candidate " +file, Project.MSG_DEBUG);
            int ix = fileName.lastIndexOf('-');
            if (0<ix) {
              String bundleName = fileName.substring(0,ix);
              String versionS = fileName.substring(ix+1,fileName.length()-4);
              Version newVersion = new Version(versionS);
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
    BundleInfo bi = (BundleInfo) bundleMap.get(bundleName);

    if (bi!=null && property!=null) {
      project.setProperty(property,bi.file.getAbsolutePath());
      log("Selected " +bi.file, Project.MSG_VERBOSE);
    }
  }

  private void transformPath(Map bundleMap)
  {
    Path newPath = new Path(project);
    log("Updating bundle paths in class path reference '"
        +classPathRef +"'.",
        Project.MSG_VERBOSE);

    Iterator pathIt = null;
    try {
      Path path = (Path) classPathRef.getReferencedObject();
      pathIt = path.iterator();
    } catch (BuildException e) {
      // Unsatisfied ref in the given path; can not expand.
      // Make the new path a reference to the old one.
      log("Unresolvable reference in '" +classPathRef
          +"' can not expand bundle names in it.",
          Project.MSG_WARN);
      newPath.setRefid(classPathRef);
    }

    if (null!=pathIt) {
      while( null!=pathIt && pathIt.hasNext()) {
        Resource resource = (Resource) pathIt.next();
        boolean added = false;
        log("resource:"+resource,Project.MSG_DEBUG);
        if (resource instanceof FileResource) {
          FileResource fr = (FileResource) resource;
          if (!fr.isExists()) {
            String fileName = fr.getName();
            log("Found non existing file resource on path: " +fileName,
                Project.MSG_DEBUG);
            if (fileName.endsWith("-N.N.N.jar")) {
              String key = fileName.substring(0,fileName.length() -10);
              BundleInfo bi = (BundleInfo) bundleMap.get(key);
              if (bi!=null) {
                newPath.add(new FileResource(bi.file));
                added = true;
                log(" => " +bi.file.getAbsolutePath(),
                    Project.MSG_DEBUG);
              } else {
                int logLevel = failOnMissingBundles
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
        }
        if (!added) {
          newPath.add(resource);
        }
      }
      log(newClassPathId +" = " +newPath, Project.MSG_INFO);
    }
    project.addReference(newClassPathId, newPath);
  } // end of transform path structure.


  private void writeReplaceFilterFile(Map bundleMap)
  {
    Properties props = new Properties();
    for (Iterator it = bundleMap.values().iterator(); it.hasNext();) {
      BundleInfo bi = (BundleInfo) it.next();
      props.put(bi.name +"-N.N.N.jar", bi.relPath);
    }
    OutputStream out = null;
    try {
      out= new FileOutputStream(replacefilterfile);
      props.store(out, "bundle version replace filter");
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
