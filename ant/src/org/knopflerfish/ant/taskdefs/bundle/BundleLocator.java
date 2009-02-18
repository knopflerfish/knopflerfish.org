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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Version;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
//import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * Determines a set of matching bundles from a given file set. The
 * resulting bundle set will only contain the highest version of each
 * matching bundle. A bundle matches if its file name starts with a
 * bundle name followed by a dash, a valid OSGi version number and
 * ends with <tt>.jar</tt>. That is the bundle name must be on the
 * form
 * <tt>bundleName&#x2011;<it>Major</it>.<it>Minor</it>.<it>Micro</it>.<it>sub</it>.jar</tt>,
 * where the minor, micro, and sub parts are optional.
 *
 * <p>
 *
 * Given a partial bundle name and a fileset with bundles, this task
 * may be used to select the bundle with the highest version number
 * and name that matches. E.g.,
 *
 * <pre>
 *   &lt;bundle_locator bundleName="http" property="http.path"&gt;
 *     &lt;fileset dir="${jars.dir}"&gt;
 *       &lt;include name="**&#x002f;*.jar"/&gt;
 *     &lt;/fileset&gt;
 *   &lt;/bundle_locator&gt;
 * </pre>
 *
 * will set the project property <tt>http.path</tt> to the absolute
 * path of the highest version of the bundle named <tt>http</tt>
 * within the given file set.
 *
 * <p>
 *
 * This task can also iterate over a path and replace all
 * non-exisiting file resources in it that has a name ending with
 * <tt>-N.N.N.jar</tt> with the corresponding bundle with the highest
 * version from the given file set.
 *
 * <pre>
 *   &lt;bundle_locator classPathRef="bundle.path"
 *                   newClassPathId="bundle.path.Expanded"
 *                   failOnMissingBundles="true"&gt;
 *     &lt;fileset dir="${jars.dir}"&gt;
 *       &lt;include name="**&#x002f;*.jar"/&gt;
 *     &lt;/fileset&gt;
 *   &lt;/bundle_locator&gt;
 * </pre>
 *
 * this will build a new path added to the project with the id
 * <tt>bundle.path.Expanded</tt>. The new path will be a copy of the
 * original, <tt>bundle.path</tt>, but with all path elements with a
 * name ending in <tt>-N.N.N.jar</tt> replaced with the corresponding
 * match or removed if no match was found.
 *
 * <p>
 *
 * Another usage of this task is to ensure that only the highest
 * version of a bundle is matched by a certain pattern set.
 *
 * <pre>
 *   &lt;bundle_locator patternSetId="my.ps.exact"&gt;
 *     &lt;fileset dir="${jars.dir}"&gt;
 *       &lt;patternset refid="my.ps"/&gt;
 *     &lt;/fileset&gt;
 *   &lt;/bundle_locator&gt;
 * </pre>
 *
 * Here the original pattern set <tt>my.ps</tt> is used to find
 * bundles in the directory <tt>jars.dir</tt>, if more than one
 * version of a bundle matches then only the one with the highest
 * version (in the bundle name) will be selected. A new pattern set
 * based on the matches is created and saved in the project under the
 * name <tt>my.ps.exact</tt>. This pattern set will contain one
 * include pattern for each matching bundle. The value of the include
 * pattern is the relative path of that bundle (relative to the root
 * directory of the file set that the matching bundle originates
 * from).
 *
 * <p>
 *
 * Finally this task may also be used to create a properties file
 * suitable for using as a replacement filter that will replace bundle
 * names on the form <tt>@name-N.N.N.jar@</tt> with the relative
 * path within the given file set of the bundle with the given name
 * and the highest version.
 *
 * <pre>
 *   &lt;bundle_locator replacefilterfile="my.filter"&gt;
 *     &lt;fileset dir="${jars.dir}"&gt;
 *       &lt;patternset refid="my.ps"/&gt;
 *     &lt;/fileset&gt;
 *   &lt;/bundle_locator&gt;
 * </pre>
 *
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
 *   the last '-'-character (excluded). The property given by
 *   <it>property</it> will hold the resulting location (absolute path).
 *   </td>
 *   <td valign=top>No. No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>bundleNames</td>
 *   <td valign=top>
 *
 *   A comma separated list of bundle names to look for. I.e., the
 *   full name up to the last '-'-character (excluded). The resulting
 *   Bundle Absolute Path is stored in a property named
 *   <tt>bap.<it>bundleName</it></tt>.
 *
 *   <p>
 *
 *   If <tt>property</tt> is set its value will be used as prefix for
 *   the property names in stead of the default <tt>bap.</tt>
 *
 *   </td>
 *   <td valign=top>No. No default value.</td>
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
 *   <td valign=top>No. No default value.</td>
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
 *   <td valign=top>patternSetId</td>
 *   <td valign=top>
 *     Create a pattern set from the set of bundles that are selected
 *     by the nested file set(s) and add it to the project using this
 *     name (id).
 *   </td>
 *   <td valign=top>No. No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>failOnMissingBundles</td>
 *   <td valign=top>
 *     If a file with name <tt><it>bundleName</it>-N.N.N.jar</tt> is
 *     found on the classpath to transform and there is no matching
 *     bundle in the file set then fail the build if set to
 *     <tt>true</tt>. Same applies if the given <tt>bundleName</tt> or
 *     one of the given <tt>bundleNames</tt> can not be transformed.
 *   </td>
 *   <td valign=top>No.<br>Defaults to <tt>true</tt>.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>extendedReplaceFilter</td>
 *   <td valign=top>
 *
 *     If set to <tt>true</tt> then the replace filter generated by
 *     the <tt>replacefilter</tt> attribute will be extended with the
 *     following set of replacements for each matching bundle.
 *
 *     <table>
 *       <tr><th>Key</th><th>Value</th></tr>
 *       <tr>
 *         <td valign="top">
 *           <tt>@<it>bundleName</it>&#x2011;N.N.N.name@</tt>
 *         </td>
 *         <td valign="top">
 *
 *           The bundle symbolic name from the manifest. For bundles
 *           with manifest version 1 (i.e., pre OSGi R4 bundles) the
 *           bundle name.
 *
 *         </td>
 *       <tr>
 *       <tr>
 *         <td valign="top">
 *           <tt>@<it>bundleName</it>&#x2011;N.N.N.version@</tt>
 *         </td>
 *         <td valign="top">
 *
 *           The bundle version from the manifest.
 *
 *         </td>
 *       <tr>
 *       <tr>
 *         <td valign="top">
 *           <tt>@<it>bundleName</it>&#x2011;N.N.N.location@</tt>
 *         </td>
 *         <td valign="top">
 *
 *           The absolute path of the bundle.
 *
 *         </td>
 *       <tr>
 *     </table>
 *
 *   </td>
 *   <td valign=top>No.<br>Defaults to <tt>false</tt>.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>replacefilterfile</td>
 *   <td valign=top>
 *
 *     Creates a property file suitable for use as the
 *     replacefilterfile argument in the replace-task. The generated
 *     file will contain one entry for each matching bundle.
 *
 *     <table>
 *       <tr><th>Key</th><th>Value</th></tr>
 *       <tr>
 *         <td valign="top">
 *           <tt>@<it>bundleName</it>&#x2011;N.N.N.jar@</tt>
 *         </td>
 *         <td valign="top">
 *
 *           The relative path to the bundle from the root-directory
 *           of the file set that the matching bundle orginates from.
 *
 *         </td>
 *       <tr>
 *     </table>
 *
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

  private final static String PROPS_PREFIX = "bap.";

  private final Vector    filesets = new Vector();
  private final FileUtils fileUtils;

  private String    bundleName           = null;
  private String    bundleNames          = null;
  private String    property             = null;
  private Reference classPathRef         = null;
  private String    newClassPathId       = null;
  private String    patternSetId         = null;
  private boolean   failOnMissingBundles = true;
  private File      replacefilterfile    = null;
  private boolean   extendedReplaceFilter= false;

  public BundleLocator() {
    fileUtils = FileUtils.newFileUtils();
  }

  public void setProperty(String s) {
    this.property = s;
  }

  public void setBundleName(String s) {
    this.bundleName = s;
  }

  public void setBundleNames(String s) {
    this.bundleNames = s;
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

  public void setPatternSetId(String s) {
    patternSetId = s;
  }

  public void setFailOnMissingBundles(boolean b) {
    failOnMissingBundles = b;
  }

  public void setExtendedReplaceFilter(boolean b) {
    extendedReplaceFilter = b;
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

    if (null!=bundleNames) {
      setProperties(bundleMap);
    }

    if (classPathRef!=null && newClassPathId!=null) {
      transformPath(bundleMap);
    }

    if (patternSetId!=null) {
      createPatternSet(bundleMap);
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
                log("Found bundle " +fileName, Project.MSG_DEBUG);
              } else if (newVersion.compareTo(bi.version)>0) {
                bi.version = newVersion;
                bi.relPath = srcFiles[j];
                bi.file = file;
                log("Found better version " +fileName, Project.MSG_DEBUG);
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

  private void setProperty(final Map bundleMap,
                           final String bn,
                           final String propName)
  {
    log("Searching for a versioned bundle with name '" +bn +"'.",
        Project.MSG_DEBUG);
    final BundleInfo bi = (BundleInfo) bundleMap.get(bn);

    if (bi!=null) {
      project.setProperty(propName, bi.file.getAbsolutePath());
      log(propName +" = " +bi.file, Project.MSG_VERBOSE);
    } else {
      final int logLevel = failOnMissingBundles
        ? Project.MSG_ERR : Project.MSG_INFO;
      log("No versioned bundle with name '" +bn +"' found.",   logLevel);
      log("Known versioned bundles are: " +bundleMap.keySet(), logLevel);
      if (failOnMissingBundles) {
        throw new BuildException
          ("No versioned bundle with name '" +bn+"' found.");
      }
    }
  }

  private void setProperty(final Map bundleMap)
  {
    if (null!=property) {
      setProperty(bundleMap, bundleName, property);
    } else {
      throw new BuildException
        ("The attribute 'bundleName' requires 'property'");
    }
  }

  private void setProperties(final Map bundleMap)
  {
    final String prefix = null==property ? PROPS_PREFIX : property;

    final StringTokenizer st = new StringTokenizer(bundleNames, ",");
    while (st.hasMoreTokens()) {
      final String bn = st.nextToken().trim();

      setProperty(bundleMap, bn, prefix +bn);
    }
  }

  private void transformPath(Map bundleMap)
  {
    final Path newPath = new Path(project);
    log("Updating bundle paths in class path reference '" +classPathRef +"'.",
        Project.MSG_DEBUG);

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


  private void createPatternSet(Map bundleMap)
  {
    final PatternSet patternSet = new PatternSet();

    log("Creating a patternset for the bundles in.", Project.MSG_WARN);

    for (Iterator it=bundleMap.values().iterator(); it.hasNext();) {
      BundleInfo bi = (BundleInfo) it.next();
      patternSet.setIncludes(bi.relPath);
    }
    project.addReference(patternSetId, patternSet);
  } // end of create pattern set for bundles


  private void writeReplaceFilterFile(Map bundleMap)
  {
    final Properties props = new Properties();
    for (Iterator it = bundleMap.values().iterator(); it.hasNext();) {
      final BundleInfo bi = (BundleInfo) it.next();
      //Note: since the path is an URL we must ensure that '/' is used.
      props.put("@" +bi.name +"-N.N.N.jar@", bi.relPath.replace('\\','/'));
      if (extendedReplaceFilter) {
        addExtendedProps(props, bi);
      }
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
        log("Created: "+replacefilterfile, Project.MSG_VERBOSE);
      }
    }
  }

  private void addExtendedProps(final Properties props, final BundleInfo bi)
  {
    final String prefix = "@" +bi.name +"-N.N.N.";
    props.put(prefix +"location@", bi.file.getAbsolutePath());
    JarFile bundle = null;
    try {
      bundle = new JarFile(bi.file);
      final Manifest manifest = bundle.getManifest();
      final Attributes mainAttributes = manifest.getMainAttributes();
      addNameProperty(props, prefix, mainAttributes);
      addVersionProperty(props, prefix, mainAttributes);
    } catch (IOException ioe) {
      log("Failed to create extended filter properties, "
          +"could not read manifest from " +bi.file.getAbsolutePath()
          +", reason: "+ioe,
          Project.MSG_ERR);
      throw new BuildException("Failed to create extended replacefilterfile",
                               ioe);
    } finally {
      if (null!=bundle) {
        try { bundle.close(); } catch (IOException _ioe) {}
      }
    }
  }

  private static String replace(String source,
                                String target,
                                String replacement)
  {
    int index = source.indexOf(target);
    if (index == -1) return source;

    int targetLength = target.length();
    StringBuffer sb = new StringBuffer();

    int position = 0;
    do {
      sb.append(source.substring(position, index));
      sb.append(replacement);
      position = index + targetLength;
      index = source.indexOf(target, position);
    } while (index != -1);
    sb.append(source.substring(position));

    return sb.toString();
  }

  /**
   * Add the bundle's symbolic name to the filter.
   *
   * If Bundle-SymbolicName is present in the manifest use it,
   * otherwise use the Bundle-Name.
   * Any ':' in the name is replaced by a '.' and any ' ' whitespace
   * with a '_'.
   *
   * @param props The properties object to add filter term to.
   * @param prefix The initial part of the properties key to add.
   * @param attributes The main attributes from the bundle's manifest.
   */
  private void addNameProperty(final Properties props,
                               final String     prefix,
                               final Attributes attributes)
  {
    String name = attributes.getValue("Bundle-SymbolicName");
    if (null==name) name = attributes.getValue("Bundle-Name");

    if (null!=name) {
      name = name.replace(':', '.');
      name = name.replace(' ', '_');
      props.put(prefix +"name@", name);
    }
  }

  /**
   * Add version to the props map. The version number is unified to
   * the format <tt>Major.Minor.Micro</tt> by adding as many ".0" as
   * needed.
   *
   * @param props The properties object to add filter term to.
   * @param prefix The initial part of the properties key to add.
   * @param attributes The main attributes from the bundle's manifest.
   */
  private void addVersionProperty(final Properties props,
                                  final String     prefix,
                                  final Attributes attributes)
  {
    String version = attributes.getValue("Bundle-Version");
    { // Ugly hack to unify version format
      if (version == null) {
        version = "0.0.0";
      } else {
        int count = 0;
        int index = 0;
        do {
          count++;
          index = version.indexOf('.', index) + 1;
        } while (index > 0);
        if (version.charAt(version.length() - 1) == '.') {
          version = version + "0";
        }
        while (count++ < 3) {
          version = version + ".0";
        }
      }
    }
    props.put(prefix +"version@", version);
  }

}
