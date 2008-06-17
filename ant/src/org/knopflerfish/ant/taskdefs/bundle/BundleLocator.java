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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

  // Implements Task
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
    }
    // bundleName -> location of the matching bundle with the highest version.
    Map map = buildBundleVersionMap();

    if (null!=bundleName) {
      log("Searching for a versioned bundle named '" +bundleName +"'.",
          Project.MSG_VERBOSE);
      BundleInfo bi = (BundleInfo) map.get(bundleName);

      if (bi!=null && property!=null) {
        project.setProperty(property,bi.file.getAbsolutePath());
        log("Selected " +bi.file, Project.MSG_VERBOSE);
      }
    }

    if (classPathRef!=null && newClassPathId!=null) {
      Path newPath = new Path(project);
      log("Updating bundle paths in class path reference '"
          +classPathRef +"'.",
          Project.MSG_VERBOSE);
      try {
        Path path = (Path) classPathRef.getReferencedObject();
        for (Iterator it=path.iterator(); it.hasNext(); ){
          Resource resource = (Resource) it.next();
          boolean added = false;
          log("resource:"+resource,Project.MSG_INFO);
          if (resource instanceof FileResource) {
            FileResource fr = (FileResource) resource;
            if (!fr.isExists()) {
              String fileName = fr.getName();
              log("Found non existing file resource on path: " +fileName,
                  Project.MSG_DEBUG);
              if (fileName.endsWith("-N.N.N.jar")) {
                String key = fileName.substring(0,fileName.length() -10);
                BundleInfo bi = (BundleInfo) map.get(key);
                if (bi!=null) {
                  newPath.add(new FileResource(bi.file));
                  added = true;
                  log(" => " +bi.file.getAbsolutePath(),
                      Project.MSG_DEBUG);
                }
              }
            }
          }
          if (!added) {
            newPath.add(resource);
          }
        }
        log("Expanded path named '" +newClassPathId +"' is " +newPath,
            Project.MSG_VERBOSE);
      } catch (BuildException e) {
        // Unsatisfied ref in the given path; can not expand.
        // Make the new path a reference to the old one.
        newPath.setRefid(classPathRef);
      } catch (Exception e) {
        log("Got unexpected exception '" +e.getClass() +"': "+e,
            Project.MSG_WARN);
      }
      project.addReference(newClassPathId, newPath);
    }

  }


  // bundleName -> location of the matching bundle with the highest version.
  Map buildBundleVersionMap()
  {
    Map map = new HashMap();

    try {
      for (int i = 0; i < filesets.size(); i++) {
        FileSet          fs      = (FileSet) filesets.elementAt(i);
        DirectoryScanner ds      = fs.getDirectoryScanner(project);
        File             projDir = fs.getDir(project);

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

  static class BundleInfo
  {
    public String  name;
    public Version version;
    public File file;
  }
}
