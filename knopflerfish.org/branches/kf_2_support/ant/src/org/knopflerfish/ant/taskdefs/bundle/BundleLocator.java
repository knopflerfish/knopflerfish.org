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
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;
import org.osgi.framework.Constants;

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
 * Determines a sub-set of bundles from a given file set. The
 * resulting set of bundles will only contain the highest version of
 * each bundle in the original file set. The resulting set of bundles
 * may then be used in several ways.
 *
 * <p>
 *
 * An <em>OSGi version spec</em> used below is a string on the format
 * <tt><em>Major</em>.<em>Minor</em>.<em>Micro</em>.<em>Qualifier</em></tt>
 * where major, minor and micro are integers, and all parts of the
 * version except major are optional. See {@link
 * org.osgi.framework.Version#Version(java.lang.String)
 * org.osgi.framework.Version} for details. The version formatting
 * used by Maven 2 is also recognized, i.e., a '&#x2011;' between the
 * micro and qualifier fields:
 * <tt><em>Major</em>.<em>Minor</em>.<em>Micro</em>&#x2011;<em>Qualifier</em></tt>.
 *
 * <p>
 *
 * Given a partial bundle name and a file set with bundles, this task
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
 * within the given file set. By setting the <tt>bundleName</tt>
 * to <tt>http-1.N.N</tt> the task will select the highest version of
 * the <tt>http</tt>-bundle with the restriction that the Major part
 * of the version number of the selection must be exactly <tt>1</tt>.
 *
 * <p>
 *
 * The bundle locator task can also iterate over a path and replace
 * all non-existing file resources in it that either has a name ending
 * with <tt>-N.N.N.jar</tt> or that is the symbolic name of a bundle
 * with the corresponding bundle with the highest version of the
 * matching bundle from the given file set. Non-existing path entries
 * that does not end in <tt>.jar</tt> or <tt>.zip</tt> that does not
 * match a symbolic bundle name will trigger a build error if
 * <tt>failOnMissingBundles</tt> is set to <tt>true</tt>. The same
 * applies to path entries ending with <tt>-N.N.N.jar</tt> that does
 * not yield a match. The search may be further restricted to specific
 * versions by replacing the <tt>N</tt> in the resource name with a
 * specific version number.
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
 * Another usage of the bundle locator task is to ensure that only the
 * highest version of a bundle is matched by a certain pattern set.
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
 * version will be selected. A new pattern set based on the matches is
 * created and saved in the project under the name
 * <tt>my.ps.exact</tt>. This pattern set will contain one include
 * pattern for each matching bundle. The value of the include pattern
 * is the relative path of that bundle (relative to the root directory
 * of the file set that the matching bundle originates from).
 *
 * <p>
 *
 * Finally this task may also be used to create a properties file
 * suitable for using as a replacement filter that will replace bundle
 * names on the form <tt>@name-N.N.N.jar@</tt> or bundle symbolic
 * names on the form <tt>@bundleSymbolicName.jar@</tt> with the
 * relative path within the given file set of the bundle with the
 * given name and the highest version.
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
 *   The name of the bundle to look for. There are several ways to
 *   specify the bundle name.
 *   <ul>
 *     <li> If the bundle to locate is named like
 *          <tt>name&#x2011;<em>OSGi version spec</em>.jar</tt>, then
 *          the value of the <tt>bundleName</tt>-attribute may be
 *          specified as <tt>name</tt>.
 *     <li> The symbolic name of the bundle.
 *   </ul>
 *
 *   A property with name given by the value of the attribute
 *   <tt>property</tt> with the location (absolute path) of the bundle
 *   as value is added to the project.
 *
 *   </td>
 *   <td valign=top>No. No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>bundleNames</td>
 *   <td valign=top>
 *
 *   A comma separated list of bundle names to look for. There are
 *   several ways to specify the bundle name.
 *   <ul>
 *     <li> If a bundle to locate is named like
 *          <tt>name&#x2011;<em>OSGi version spec</em>.jar</tt>, then
 *          the value of the <tt>bundleName</tt>-attribute may be
 *          specified as <tt>name</tt>.
 *     <li> The symbolic name of the bundle.
 *   </ul>
 *
 *   The absolute path of the matching bundle will be stored in a
 *   project property named <tt>bap.<em>bundleName</em></tt>.
 *
 *   <p>
 *
 *   If the attribute <tt>property</tt> is set its value will be used
 *   as prefix for the property names in stead of the default
 *   <tt>bap.</tt>
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
 *
 *     If an entry with a file name like
 *     <tt><em>bundleName</em>-N.N.N.jar</tt> is found on the
 *     classpath to transform and there is no matching bundle in the
 *     file set then a build failure is triggered if this attribute is
 *     set to <tt>true</tt>. Same applies if the given
 *     <tt>bundleName</tt> or one of the given <tt>bundleNames</tt>
 *     does not yield a match.
 *
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
 *           <tt>@<em>bundleName</em>&#x2011;N.N.N.name@</tt><br>
 *           <tt>@<em>Bundle&#x2011;SymbolicName</em>.name@</tt>
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
 *           <tt>@<em>bundleName</em>&#x2011;N.N.N.version@</tt><br>
 *           <tt>@<em>Bundle&#x2011;SymbolicName</em>.version@</tt>
 *         </td>
 *         <td valign="top">
 *
 *           The bundle version from the manifest.
 *
 *         </td>
 *       <tr>
 *       <tr>
 *         <td valign="top">
 *           <tt>@<em>bundleName</em>&#x2011;N.N.N.location@</tt><br>
 *           <tt>@<em>Bundle&#x2011;SymbolicName</em>.location@</tt>
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
 *     <tt>replacefilterfile</tt> argument in the replace-task. The
 *     generated file will contain two entries for each matching
 *     bundle.
 *
 *     <table>
 *       <tr><th>Key</th><th>Value</th></tr>
 *       <tr>
 *         <td valign="top">
 *           <tt>@<em>bundleName</em>&#x2011;N.N.N.jar@</tt><br>
 *           <tt>@<em>Bundle&#x2011;SymbolicName</em>.jar@</tt>
 *         </td>
 *         <td valign="top">
 *
 *           The relative path to the bundle from the root-directory
 *           of the file set that the matching bundle ordinates from.
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

  /**
   * A list with all BundeInfo-objects that has been created in the
   * order that they are specified through the included file sets.
   */
  final private SortedSet allBis = new TreeSet();

  /**
   * Mapping from bundle symbolic name to a sorted set with all
   * BundleInfo objects of that bundle.
   */
  final private Map bsnMap = new HashMap();

  /**
   * Mapping that for bundles named like <tt>bundleName-N.N.N.jar</tt>
   * maps from the <tt>bundleName</tt> part to a sorted set of
   * BundleInfo objects for all matching bundles.
   */
  final private Map bundleMap = new HashMap();


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
    analyze();

    if (null!=bundleName) {
      setProperty();
    }

    if (null!=bundleNames) {
      setProperties();
    }

    if (classPathRef!=null && newClassPathId!=null) {
      transformPath();
    }

    if (patternSetId!=null) {
      createPatternSet();
    }

    if (null!=replacefilterfile) {
      writeReplaceFilterFile();
    }
  }


  /**
   * Holder of the value in the bundle version map.
   */
  static class BundleInfo implements Comparable
  {
    /** The short bundle name. I.e., the name to the left of the last '-'.*/
    public String  name;
    /** The bundle file name without path.*/
    public String  fileName;
    /** The bundle symbolic name.*/
    public String  bsn;
    /** The bundles version. I.e., the part to the right of the last '-'.*/
    public Version version;
    /** The relative path from the root of the file set holding the bundle.*/
    public String  relPath;
    /** The absolute path of the bundle. */
    public File file;

    /** Sort on bundle symbolic name then on the bundle version. */
    public int compareTo(Object o)
    {
      BundleInfo other = (BundleInfo) o;

      // The bsn may be null for pre OSGi R4 bundles!
      if (null==this.bsn || null==other.bsn) {
        return this.name.compareTo(other.name);
      }

      int res = this.bsn.compareTo(other.bsn);
      return res!=0 ? res : this.version.compareTo(other.version);
    }

    public String toString()
    {
      return "BundleInfo[fileName=" +fileName +", name=" +name
        +", Bundle-SymbolicName=" +bsn +", version=" +version
        +", relPath=" +relPath +", absPath=" +file.getAbsolutePath() +"]";
    }
  }


  private String encodeBundleName(final String bundleName)
  {
    String name = bundleName;
    if (null!=name) {
      name = name.replace(':', '.');
      name = name.replace(' ', '_');
    }
    return name;
  }

  /**
   * Format the OSGi version as Maven 2 does in versioned file names.
   */
  private String toMavenVersion(final Version version)
  {
    final StringBuffer sb = new StringBuffer(40);

    sb.append(String.valueOf(version.getMajor())).append(".");
    sb.append(String.valueOf(version.getMinor())).append(".");
    sb.append(String.valueOf(version.getMicro()));

    final String qualifier = version.getQualifier();
    if (0<qualifier.length()) {
      sb.append("-").append(qualifier);
    }

    return sb.toString();
  }

  /**
   * Get the bundle symbolic name from the manifest. If the
   * <tt>Bundle-SymbolicName</tt> attribute is missing use the
   * <tt>Bundle-Name</tt> but replace all ':' with '.' and all '&nbsp;'
   * with '_' in the returned value.
   *
   * @param attributes The main attributes from the bundle's manifest.
   */
  private String getBundleSymbolicName(final Attributes attributes)
  {
    String name = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
    if (null==name) {
      name = attributes.getValue(Constants.BUNDLE_NAME);
    }
    return encodeBundleName(name);
  }
  /**
   * Get the bundle version from the manifest.
   *
   * @param attributes The main attributes from the bundle's manifest.
   */
  private Version getBundleVersion(final File file,
                                   final Attributes attributes)
    throws NumberFormatException
  {
    final String versionS = attributes.getValue(Constants.BUNDLE_VERSION);
    if (null==versionS) {
      return Version.emptyVersion;
    }

    try {
      return new Version(versionS);
    } catch (NumberFormatException nfe) {
      final String manifestVersion
        = attributes.getValue(Constants.BUNDLE_MANIFESTVERSION);
      if (null==manifestVersion || "1".equals(manifestVersion.trim())) {
        // Pre OSGi R4 bundle with non-standard version format; use
        // the default version.
        return Version.emptyVersion;
      }
      log("Invalid bundle version '" +versionS +"' found in " +file +": "+nfe,
          Project.MSG_ERR);
      throw nfe;
    }
  }

  /**
   * Create a BundleInfo-object for the given file if it is a bundle.
   */
  private BundleInfo createBundleInfo(final File rootDir,
                                      final String relPath)
    throws Exception
  {
    final File   file     = new File(rootDir, relPath);
    final String fileName = file.getName();

    BundleInfo bi = null;
    if(file.getName().endsWith(".jar")) {
      log( "Processing candidate " +file, Project.MSG_DEBUG);
      String bundleName   = null;
      Version nameVersion = null;

      int ix = fileName.lastIndexOf('-');
      if (0<ix) {
        bundleName = fileName.substring(0,ix);
        final String versionS = fileName.substring(ix+1,fileName.length()-4);
        try {
          nameVersion = new Version(versionS);
        } catch (NumberFormatException nfe) {
          bundleName = null; // Not valid due to missing version.
          log("Invalid version in bundle file name '" +versionS +"': "+nfe,
              Project.MSG_VERBOSE);
        }
      }
      final JarFile bundle  = new JarFile(file);
      String  bsn     = null;
      Version version = null;
      try {
        final Manifest   manifest       = bundle.getManifest();
        final Attributes mainAttributes = manifest.getMainAttributes();
        bsn     = getBundleSymbolicName(mainAttributes);
        version = getBundleVersion(file, mainAttributes);
      } finally {
        if (null!=bundle) {
          try { bundle.close(); } catch (IOException _ioe) {}
        }
      }

      if (null!=nameVersion && 0!=nameVersion.compareTo(version)) {
        bundleName = null; // Not valid due to version missmatch.
        log("Found version '" +nameVersion +"' in the file name '"
            +fileName +"', but the version in the bundle's manifest is '"
            +version +"'.",
            Project.MSG_VERBOSE);
      }

      if (0<version.getQualifier().length()) {
        // Maven uses '-' and not '.' as separator for the qualifier
        // in its bundle names. Check if bundleName needs to be
        // updated.
        final String mavenSuffix = "-" +toMavenVersion(version) +".jar";
        if (fileName.endsWith(mavenSuffix)) {
          bundleName
            = fileName.substring(0, fileName.length() -mavenSuffix.length());
        }
      }

      bi = new BundleInfo();
      bi.name     = bundleName;
      bi.bsn      = bsn;
      bi.version  = version;
      bi.fileName = fileName;
      bi.relPath  = relPath;
      bi.file     = file;
      log("Found " +bi, Project.MSG_DEBUG);
    }
    return bi;
  }

  private void updateMap(Map map, String key, BundleInfo bi)
  {
    if (null==key) return;

    SortedSet bis = (SortedSet) map.get(key);
    if (null==bis) {
      bis = new TreeSet();
      map.put(key, bis);
      log("Found bundle '" +key +"'.", Project.MSG_DEBUG);
    }
    if (bis.add(bi)) {
      log("Found bundle '" +key +"' '" +bi.version +"'.", Project.MSG_DEBUG);
    }
  }

  /**
   * Chgeck if the given bundle name contains a wildcard version descriptor.
   *
   *
   * @param name The bundle name to check.
   *
   * @return <code>true</code> if a bundle version with wildcard is
   *         part of the bundle name, <code>false</code> otherwise.
   */
  private boolean isBundleNameWithWildcardVersion(final String name)
  {
    final Pattern pattern = Pattern.compile
      ("^(.+)-(\\d+|N)(?:|\\.(\\d+|N)(?:|\\.(\\d+|N)(?:|\\.([-_0-9a-zA-Z]+))))(?:.jar|.zip)$");
    final Matcher matcher = pattern.matcher(name);
    if (matcher.matches()) {
      for (int i=2; i<6; i++) {
        final String s = matcher.group(i);
        if ("N".equals(s)) return true;
      }
    }
    return false;
  }

  /**
   * Get the bundle info for the given bundle name.
   *
   * <ol>
   *  <li> Remove any <tt>.jar</tt> or <tt>.zip</tt> suffix form the name.
   *  <li> If <code>name</code> is a bundle name prefix return the
   *       highest version of that bundle.
   *  <li> If <code>name</code> is a symbolic bundle name return the
   *       highest version of that bundle.
   *  <li> Try to find a bundle version as part if the bundle name,
   *       use that version to select the bundle.
   * </ol>
   *
   * @param name Name of the bundle to look for. May contain a version
   *             suffix.
   *
   * @return <code>null</code> if no matching bundle info was found.
   */
  private BundleInfo getBundleInfo(String name)
  {
    log("getBundleInfo("+name +")", Project.MSG_DEBUG);
    if (null==name || 0==name.length()) return null;

    if (name.endsWith(".jar")) name = name.substring(0, name.length()-4);
    if (name.endsWith(".zip")) name = name.substring(0, name.length()-4);
    if (name.endsWith("-N.N.N")) name = name.substring(0, name.length() -6);

    SortedSet bis = (SortedSet) bundleMap.get(name);
    if (null==bis) bis = (SortedSet) bsnMap.get(name);
    if (null!=bis) {
      final BundleInfo bi = (BundleInfo) bis.last();
      log("getBundleInfo("+name +")->"+bi, Project.MSG_VERBOSE);
      return bi;
    }

    final Pattern pattern = Pattern.compile
      ("^(.+)-(\\d+|N)(?:|\\.(\\d+|N)(?:|\\.(\\d+|N)(?:|\\.([-_0-9a-zA-Z]+))))$");
    final Matcher matcher = pattern.matcher(name);
    if (matcher.matches()) {
      int groupCount = matcher.groupCount();
      name = matcher.group(1);
      String version = "";
      int level = -1;

      for (int i=2; i<6; i++) {
        final String s = matcher.group(i);
        if (null==s || "N".equals(s)) {// Done;
          break;
        }
        level++;
        if (version.length()>0) version += ".";
        version += s;
      }
      if (level<0) {
        return getBundleInfo(name, null, null);
      } else {
        Version min = new Version(version);
        Version max = null;

        switch(level) {
        case 0:
          max = new Version(min.getMajor()+1, 0, 0,"");
          break;
        case 1:
          max = new Version(min.getMajor(), min.getMinor()+1, 0,"");
          break;
        case 2:
          max = new Version(min.getMajor(), min.getMinor(), min.getMicro()+1,
                            "");
          break;
        default:
          max = min;
        }
        return getBundleInfo(name, min, max);
      }
    } else {
      log("getBundleInfo(" +name +") no valid version found in the name.",
          Project.MSG_VERBOSE);
    }
    return null;
  }

  /**
   * Get the bundle info with the highest version within the given
   * intervall for the given bundle.
   *
   * @param name Name of the bundle to look for. Either the part of
   *             the file name that cames before the file version or
   *             the bundle symbolic name.
   * @param min  The lowest acceptable version number (inclusive).
   * @param max  The highest acceptable version number (exclusive). If
   *             null the highest version of this bundle will be
   *             selected.
   * @return <code>null</code> if no matching bundle info was found.
   *
   */
  private BundleInfo getBundleInfo(final String name,
                                   Version min,
                                   final Version max)
  {
    SortedSet bis = (SortedSet)  bundleMap.get(name);
    if (null==bis) {
      bis = (SortedSet) bsnMap.get(encodeBundleName(name));
    }
    BundleInfo bi = null;

    if (null!=bis) {
      if (null==max) { // Select highest available version
        bi = (BundleInfo) bis.last();
      } else {
        if (null==min) min = Version.emptyVersion;
        for (Iterator it = bis.iterator(); it.hasNext(); ) {
          final BundleInfo candBi = (BundleInfo) it.next();
          if (candBi.version.compareTo(min)<0) continue;
          if (candBi.version.compareTo(max)>=0) break;
          bi = candBi;
        }
      }
    }

    log("getBundleInfo("+name +", " +min +", " +max +")->"+bi,
        Project.MSG_VERBOSE);
    return bi;
  }


  /**
   * Traverses the given file sets, identify files that are bundles
   * and add them to the bundle info maps.
   */
  private void analyze()
  {
    try {
      for (int i = 0; i < filesets.size(); i++) {
        final FileSet          fs      = (FileSet) filesets.elementAt(i);
        final File             projDir = fs.getDir(project);
        if (!projDir.exists()) {
          log("Skipping nested file set rooted at '" +projDir
              +"' since that directory does not exist.",
              Project.MSG_WARN);
          continue;
        } else {
          log( "Processing file set rooted at " +projDir, Project.MSG_DEBUG);
        }
        final DirectoryScanner ds      = fs.getDirectoryScanner(project);

        final String[] srcFiles = ds.getIncludedFiles();
        final String[] srcDirs  = ds.getIncludedDirectories();

        for (int j = 0; j < srcFiles.length ; j++) {
          final BundleInfo bi = createBundleInfo(projDir, srcFiles[j]);
          if (null!=bi) {
            allBis.add(bi);
            updateMap(bundleMap, bi.name, bi);
            updateMap(bsnMap,    bi.bsn,  bi);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed locate bundle: " + e, e);
    }
  }

  private void setProperty(final String bn,
                           final String propName)
  {
    log("Searching for a bundle with name '" +bn +"'.", Project.MSG_DEBUG);
    BundleInfo bi = getBundleInfo(bn);

    if (bi!=null) {
      project.setProperty(propName, bi.file.getAbsolutePath());
      log(propName +" = " +bi.file, Project.MSG_VERBOSE);
    } else {
      final int logLevel = failOnMissingBundles
        ? Project.MSG_ERR : Project.MSG_INFO;
      log("No bundle with name '" +bn +"' found.", logLevel);
      final TreeSet knownNames = new TreeSet(bundleMap.keySet());
      knownNames.addAll(bsnMap.keySet());
      log("Known bundles names: " +knownNames, logLevel);

      if (failOnMissingBundles) {
        throw new BuildException("No bundle with name '" +bn+"' found.");
      }
    }
  }

  private void setProperty()
  {
    if (null!=property) {
      setProperty(bundleName, property);
    } else {
      throw new BuildException
        ("The attribute 'bundleName' requires 'property'");
    }
  }

  private void setProperties()
  {
    final String prefix = null==property ? PROPS_PREFIX : property;

    final StringTokenizer st = new StringTokenizer(bundleNames, ",");
    while (st.hasMoreTokens()) {
      final String bn = st.nextToken().trim();

      setProperty(bn, prefix +bn);
    }
  }

  private void transformPath()
  {
    final Path newPath = new Path(project);
    log("Updating bundle paths in class path reference '"
        +classPathRef.getRefId() +"'.", Project.MSG_DEBUG);

    String[] pathElements = null;
    try {
      final Path path = (Path) classPathRef.getReferencedObject();
      pathElements = path.list();
    } catch (BuildException e) {
      // Unsatisfied ref in the given path; can not expand.
      // Make the new path a reference to the old one.
      log("Unresolvable reference in '" +classPathRef.getRefId()
          +"' can not expand bundle names in it.", Project.MSG_WARN);
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
          final BundleInfo bi = getBundleInfo(fileName);
          if (bi!=null) {
            final String filePath = bi.file.getAbsolutePath();
            newPath.setPath(filePath);
            added = true;
            log(fileName +" => " +filePath, Project.MSG_VERBOSE);
          } else if (isBundleNameWithWildcardVersion(fileName)) {
            final int logLevel = failOnMissingBundles
              ? Project.MSG_ERR : Project.MSG_INFO;
            log("No match for '" +fileName +"' when expanding the path named '"
                +classPathRef.getRefId() +"'.", logLevel);
            TreeSet knownNames = new TreeSet(bundleMap.keySet());
            knownNames.addAll(bsnMap.keySet());
            log("Known bundles names: " +knownNames, logLevel);
            if (failOnMissingBundles) {
              throw new BuildException
                ("No bundle with name like '" +fileName+"' found.");
            }
          } else {
            log("No match for '" +fileName +"' when expanding the path named '"
                +classPathRef.getRefId() +"'.", Project.MSG_VERBOSE);
          }
        }
        if (!added) {
          newPath.setPath(pathElement.getAbsolutePath());
        }
      }
      log(newClassPathId +" = " +newPath, Project.MSG_VERBOSE);
    }
    project.addReference(newClassPathId, newPath);
  } // end of transform path structure.


  private void createPatternSet()
  {
    final PatternSet patternSet = new PatternSet();

    log("Creating a patternset for the bundles with id='" +patternSetId +"'.",
        Project.MSG_DEBUG);

    for (Iterator it=allBis.iterator(); it.hasNext();) {
      BundleInfo bi = (BundleInfo) it.next();
      patternSet.setIncludes(bi.relPath);
      log("Adding includes '" +bi.relPath +"'.", Project.MSG_DEBUG);
    }
    project.addReference(patternSetId, patternSet);
  } // end of create pattern set for bundles


  /**
   * Get the set of version patterns (partial versions) used in the
   * keys in replacement filters for the given version.
   * @param version The version to create patterns for.
   * @return Set of version patterns.
   */
  private String[] getVersionPatterns(Version version)
  {
    if (null==version) {
      return new String[]{"-N.N.N"};
    }

    final String qualifier = version.getQualifier();
    final boolean usesQualifier = null != qualifier && qualifier.length() > 0;
    final String[] res = new String[usesQualifier ? 5 : 4];

    res[0] = "-N.N.N";
    res[1] = "-" +version.getMajor() +".N.N";
    res[2] = "-" +version.getMajor() +"." +version.getMinor() +".N";
    res[3] = "-" +version.getMajor() +"." +version.getMinor()
      +"." +version.getMicro();
    if (usesQualifier) {
      res[4] = "-" +version.getMajor() +"." +version.getMinor()
        +"." +version.getMicro() +"." +version.getQualifier();
    }

    return res;
  }

  private void writeReplaceFilterFile()
  {
    final Properties props = new Properties();
    for (Iterator it = allBis.iterator(); it.hasNext();) {
      final BundleInfo bi = (BundleInfo) it.next();
      //Note: since the path is an URL we must ensure that '/' is used.
      final String relPath = bi.relPath.replace('\\','/');
      final String[] versPatterns = getVersionPatterns(bi.version);

      if (null!=bi.bsn) {
        props.put("@" +bi.bsn +".jar@", relPath);
        if (extendedReplaceFilter) {
          props.put("@" +bi.bsn +".location@", bi.file.getAbsolutePath());
          props.put("@" +bi.bsn +".name@", bi.bsn);
          props.put("@" +bi.bsn +".version@", bi.version.toString());
        }
        for (int i=0; i<versPatterns.length; i++) {
          final String prefix = "@" +bi.bsn +versPatterns[i];
          props.put(prefix +".jar@", relPath);
          if (extendedReplaceFilter) {
            props.put(prefix +".location@", bi.file.getAbsolutePath());
            props.put(prefix +".name@", bi.bsn);
            props.put(prefix +".version@", bi.version.toString());
          }
        }
      }

      if (null!=bi.name) {
        for (int i=0; i<versPatterns.length; i++) {
          final String prefix = "@" +bi.name +versPatterns[i];
          props.put(prefix +".jar@", relPath);
          if (extendedReplaceFilter) {
            props.put(prefix +".location@", bi.file.getAbsolutePath());
            if (null!=bi.bsn) {
              props.put(prefix +".name@", bi.bsn);
            }
            props.put(prefix +".version@", bi.version.toString());
          }
        }
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

}
