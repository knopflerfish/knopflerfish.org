/*
 * Copyright (c) 2008-2011, KNOPFLERFISH project
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.osgi.framework.Version;

/**
 * Determines a sub-set of bundles from a given file set. The resulting set of
 * bundles will only contain the highest version of each bundle in the original
 * file set. The resulting set of bundles may then be used in several ways.
 *
 * <p>
 *
 * An <em>OSGi version specification</em> used below is a string on the format
 * <tt><em>Major</em>.<em>Minor</em>.<em>Micro</em>.<em>Qualifier</em></tt>
 * where major, minor and micro are integers, and all parts of the version
 * except major are optional. See
 * {@link org.osgi.framework.Version#Version(java.lang.String)
 * org.osgi.framework.Version} for details. The version formatting used by Maven
 * 2 is also recognized, i.e., a '&#x2011;' between the micro and qualifier
 * fields:
 * <tt><em>Major</em>.<em>Minor</em>.<em>Micro</em>&#x2011;<em>Qualifier</em></tt>.
 *
 * <p>
 *
 * Given a partial bundle name and a file set with bundles, this task may be
 * used to select the bundle with the highest version number and name that
 * matches. E.g.,
 *
 * <pre>
 *   &lt;bundle_locator bundleName="http" property="http.path"&gt;
 *     &lt;fileset dir="${jars.dir}"&gt;
 *       &lt;include name="**&#x002f;*.jar"/&gt;
 *     &lt;/fileset&gt;
 *   &lt;/bundle_locator&gt;
 * </pre>
 *
 * will set the project property <tt>http.path</tt> to the absolute path of the
 * highest version of the bundle named <tt>http</tt> within the given file set.
 * By setting the <tt>bundleName</tt> to <tt>http-1.N.N</tt> the task will
 * select the highest version of the <tt>http</tt>-bundle with the restriction
 * that the Major part of the version number of the selection must be exactly
 * <tt>1</tt>.
 *
 * <p>
 *
 * The bundle locator task can also iterate over a path and replace all
 * non-existing file resources in it that either has a name ending with
 * <tt>-N.N.N.jar</tt> or that is the symbolic name of a bundle with the
 * corresponding bundle with the highest version of the matching bundle from the
 * given file set. Non-existing path entries that does not end in <tt>.jar</tt>
 * or <tt>.zip</tt> that does not match a symbolic bundle name will trigger a
 * build error if <tt>failOnMissingBundles</tt> is set to <tt>true</tt>. The
 * same applies to path entries ending with <tt>-N.N.N.jar</tt> that does not
 * yield a match. The search may be further restricted to specific versions by
 * replacing the <tt>N</tt> in the resource name with a specific version number.
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
 * <tt>bundle.path.Expanded</tt>. The new path will be a copy of the original,
 * <tt>bundle.path</tt>, but with all path elements with a name ending in
 * <tt>-N.N.N.jar</tt> replaced with the corresponding match or removed if no
 * match was found.
 *
 * <p>
 *
 * Another usage of the bundle locator task is to ensure that only the highest
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
 * Here the original pattern set <tt>my.ps</tt> is used to find bundles in the
 * directory <tt>jars.dir</tt>, if more than one version of a bundle matches
 * then only the one with the highest version will be selected. A new pattern
 * set based on the matches is created and saved in the project under the name
 * <tt>my.ps.exact</tt>. This pattern set will contain one include pattern for
 * each matching bundle. The value of the include pattern is the relative path
 * of that bundle (relative to the root directory of the file set that the
 * matching bundle originates from).
 *
 * <p>
 *
 * Finally this task may also be used to create a properties file suitable for
 * using as a replacement filter that will replace bundle names on the form
 * <tt>@name-N.N.N.jar@</tt> or bundle symbolic names on the form
 * <tt>@bundleSymbolicName.jar@</tt> with the relative path within the given
 * file set of the bundle with the given name and the highest version.
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
 * <tr>
 * <td valign=top><b>Attribute</b></td>
 * <td valign=top><b>Description</b></td>
 * <td valign=top><b>Required</b></td>
 * </tr>
 *
 * <tr>
 * <td valign=top>bundleName</td>
 * <td valign=top>
 * The name of the bundle to look for. There are several ways to specify the
 * bundle name.
 * <ul>
 * <li>If the bundle to locate is named like
 * <tt>name&#x2011;<em>OSGi version spec</em>.jar</tt>, then the value of the
 * <tt>bundleName</tt>-attribute may be specified as <tt>name</tt>.
 * <li>The symbolic name of the bundle.
 * </ul>
 *
 * A property with name given by the value of the attribute <tt>property</tt>
 * with the location (absolute path) of the bundle as value is added to the
 * project.
 *
 * </td>
 * <td valign=top>No. No default value.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>bundleNames</td>
 * <td valign=top>
 *
 * A comma separated list of bundle names to look for. There are several ways to
 * specify the bundle name.
 * <ul>
 * <li>If a bundle to locate is named like
 * <tt>name&#x2011;<em>OSGi version spec</em>.jar</tt>, then the value of the
 * <tt>bundleName</tt>-attribute may be specified as <tt>name</tt>.
 * <li>The symbolic name of the bundle.
 * </ul>
 *
 * The absolute path of the matching bundle will be stored in a project property
 * named <tt>bap.<em>bundleName</em></tt>.
 *
 * <p>
 *
 * If the attribute <tt>property</tt> is set its value will be used as prefix
 * for the property names in stead of the default <tt>bap.</tt>
 *
 * </td>
 * <td valign=top>No. No default value.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>property</td>
 * <td valign=top>
 * The name of a project property to be assigned the location of the matching
 * bundle.</td>
 * <td valign=top>Yes when <tt>bundleName</tt> is specified.<br>
 * No default value.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>classPathRef</td>
 * <td valign=top>
 * The reference name (id) of a path-structure to transform.</td>
 * <td valign=top>No. No default value.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>newClassPathId</td>
 * <td valign=top>
 * The transformed path-structure will be added to the current project using
 * this name (id).</td>
 * <td valign=top>Yes when <tt>classPathRef</tt> is specified.<br>
 * No default value.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>patternSetId</td>
 * <td valign=top>
 * Create a pattern set from the set of bundles that are selected by the nested
 * file set(s) and add it to the project using this name (id).</td>
 * <td valign=top>No. No default value.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>bundlePath</td>
 * <td valign=top>
 * Specifies a bundle search path like the one specified in xargs files by the
 * framework property <code>org.knopflerfish.gosg.jars</code>. Path elements are
 * URLs separated by ';'. Setting this property will add a file set with an
 * includes set to <code>&lowast;&lowast;/&lowast;.jar</code> for each path element
 * that is defined as a file-URL.</td>
 * <td valign=top>No. No default value.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>baseDir</td>
 * <td valign=top>
 * Path to directory to use as base-directory to complete relative
 * file URLs in the <code>bundlePath</code>.
 * </td>
 * <td valign=top>No. Defaults to the empty string, i.e., the current
 * working directory.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>failOnMissingBundles</td>
 * <td valign=top>
 *
 * If an entry with a file name like <tt><em>bundleName</em>-N.N.N.jar</tt> is
 * found on the classpath to transform and there is no matching bundle in the
 * file set then a build failure is triggered if this attribute is set to
 * <tt>true</tt>. Same applies if the given <tt>bundleName</tt> or one of the
 * given <tt>bundleNames</tt> does not yield a match.
 *
 * </td>
 * <td valign=top>No.<br>
 * Defaults to <tt>true</tt>.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>extendedReplaceFilter</td>
 * <td valign=top>
 *
 * If set to <tt>true</tt> then the replace filter generated by the
 * <tt>replacefilter</tt> attribute will be extended with the following set of
 * replacements for each matching bundle.
 *
 * <table>
 * <tr>
 * <th>Key</th>
 * <th>Value</th>
 * </tr>
 * <tr>
 * <td valign="top">
 * <tt>@<em>bundleName</em>&#x2011;N.N.N.name@</tt><br>
 * <tt>@<em>Bundle&#x2011;SymbolicName</em>.name@</tt></td>
 * <td valign="top">
 *
 * The bundle symbolic name from the manifest. For bundles with manifest version
 * 1 (i.e., pre OSGi R4 bundles) the bundle name.
 *
 * </td>
 * <tr>
 * <tr>
 * <td valign="top">
 * <tt>@<em>bundleName</em>&#x2011;N.N.N.version@</tt><br>
 * <tt>@<em>Bundle&#x2011;SymbolicName</em>.version@</tt></td>
 * <td valign="top">
 *
 * The bundle version from the manifest.
 *
 * </td>
 * <tr>
 * <tr>
 * <td valign="top">
 * <tt>@<em>bundleName</em>&#x2011;N.N.N.location@</tt><br>
 * <tt>@<em>Bundle&#x2011;SymbolicName</em>.location@</tt></td>
 * <td valign="top">
 *
 * The absolute path of the bundle.
 *
 * </td>
 * <tr>
 * </table>
 *
 * </td>
 * <td valign=top>No.<br>
 * Defaults to <tt>false</tt>.</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>replacefilterfile</td>
 * <td valign=top>
 *
 * Creates a property file suitable for use as the <tt>replacefilterfile</tt>
 * argument in the replace-task. The generated file will contain two entries for
 * each matching bundle.
 *
 * <table>
 * <tr>
 * <th>Key</th>
 * <th>Value</th>
 * </tr>
 * <tr>
 * <td valign="top">
 * <tt>@<em>bundleName</em>&#x2011;N.N.N.jar@</tt><br>
 * <tt>@<em>Bundle&#x2011;SymbolicName</em>.jar@</tt></td>
 * <td valign="top">
 *
 * The relative path to the bundle from the root-directory of the file set that
 * the matching bundle originates from.
 *
 * </td>
 * <tr>
 * </table>
 *
 * </td>
 * <td valign=top>No.<br>
 * No default value.</td>
 * </tr>
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

  private BundleArchives bas;

  private String    bundleName           = null;
  private String    bundleNames          = null;
  private String    property             = null;
  private Reference classPathRef         = null;
  private String    newClassPathId       = null;
  private String    patternSetId         = null;
  private boolean   failOnMissingBundles = true;
  private File      replacefilterfile    = null;
  private boolean   extendedReplaceFilter= false;
  private String    bundlePath           = null;
  private File      baseDir              = new File(".");


  public BundleLocator() {
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

  public void setBundlePath(String bundlePath) throws BuildException {
    this.bundlePath = bundlePath;
    log("bundlePath='" + bundlePath + "'.", Project.MSG_DEBUG);
  }

  public void setBaseDir(File f)
  {
    baseDir = f;
  }

  private URL getBaseURL()
  {
    try {
      return baseDir.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new BuildException("Invalid baseDir, '" + baseDir + "'.", e);
    }
  }

  private void processBundlePath()
  {
    if (null!=bundlePath && 0<bundlePath.length()) {
      final URL baseUrl = getBaseURL();
      // Create a file set for each entry in the bundle path.
      String[] urls = Util.splitwords(this.bundlePath, ";", '"');
      for (int i = 0; i < urls.length; i++) {
        log("Processing URL '" + urls[i] + "' from bundlePath '"
            + this.bundlePath + "'.", Project.MSG_DEBUG);
        try {
          final URL url = new URL(baseUrl, urls[i].trim());
          if ("file".equals(url.getProtocol())) {
            final String path = url.getPath();
            final File dir = new File(path.replace('/', File.separatorChar));
            log("Adding file set with dir '" + dir
                + "' for bundlePath file URL with path '" + path + "'.",
                Project.MSG_VERBOSE);
            FileSet fs = new FileSet();
            fs.setDir(dir);
            fs.setIncludes("**/*.jar");
            fs.setProject(getProject());
            filesets.add(fs);
          }
        } catch (MalformedURLException e) {
          throw new BuildException("Invalid URL, '" + urls[i]
                                   + "' found in bundlePath: '"
                                   + bundlePath + "'.", e);
        }
      }
    }
  }

  // Implements Task
  public void execute() throws BuildException {
    processBundlePath();

    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
    }
    bas = new BundleArchives(this, filesets);

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
   * Check if the given bundle name contains a wild-card version descriptor.
   *
   *
   * @param name The bundle name to check.
   *
   * @return <code>true</code> if a bundle version with wild-card is
   *         part of the bundle name, <code>false</code> otherwise.
   */
  static private boolean isBundleNameWithWildcardVersion(final String name)
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
   * Get the bundle archive for the given bundle name.
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
   * @return <code>null</code> if no matching bundle archive was found.
   */
  private BundleArchives.BundleArchive getBundleArchive(String name)
  {
    log("getBundleArchive("+name +")", Project.MSG_DEBUG);
    if (null==name || 0==name.length()) return null;

    if (name.endsWith(".jar"))   name = name.substring(0, name.length()-4);
    if (name.endsWith(".zip"))   name = name.substring(0, name.length()-4);
    if (name.endsWith("-N.N.N")) name = name.substring(0, name.length()-6);

    SortedSet baSet = (SortedSet) bas.bnToBundleArchives.get(name);
    if (null==baSet) {
      baSet = (SortedSet)
        bas.bsnToBundleArchives.get(BundleArchives.encodeBundleName(name));
    }
    if (null!=baSet) {
      final BundleArchives.BundleArchive ba
        = (BundleArchives.BundleArchive) baSet.last();
      log("getBundleArchive("+name +")->"+ba, Project.MSG_VERBOSE);
      return ba;
    }

    final Pattern pattern = Pattern.compile
      ("^(.+)-(\\d+|N)(?:|\\.(\\d+|N)(?:|\\.(\\d+|N)(?:|\\.([-_0-9a-zA-Z]+))))$");
    final Matcher matcher = pattern.matcher(name);
    if (matcher.matches()) {
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
        return getBundleArchive(name, null, null);
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
        return getBundleArchive(name, min, max);
      }
    } else {
      log("getBundleArchive(" +name +") no valid version found in the name.",
          Project.MSG_VERBOSE);
    }
    return null;
  }

  /**
   * Get the bundle archive with the highest version within the given
   * interval for the given bundle.
   *
   * @param name Name of the bundle to look for. Either the part of
   *             the file name that comes before the file version or
   *             the bundle symbolic name.
   * @param min  The lowest acceptable version number (inclusive).
   * @param max  The highest acceptable version number (exclusive). If
   *             null the highest version of this bundle will be
   *             selected.
   * @return <code>null</code> if no matching bundle archive was found.
   *
   */
  private BundleArchives.BundleArchive getBundleArchive(final String name,
                                                        Version min,
                                                        final Version max)
  {
    SortedSet baSet = (SortedSet) bas.bnToBundleArchives.get(name);
    if (null==baSet) {
      baSet = (SortedSet)
        bas.bsnToBundleArchives.get(BundleArchives.encodeBundleName(name));
    }
    BundleArchives.BundleArchive ba = null;

    if (null!=baSet) {
      if (null==max) { // Select highest available version
        ba = (BundleArchives.BundleArchive) baSet.last();
      } else {
        if (null==min) min = Version.emptyVersion;
        for (Iterator it = baSet.iterator(); it.hasNext(); ) {
          final BundleArchives.BundleArchive candBa
            = (BundleArchives.BundleArchive) it.next();
          if (candBa.version.compareTo(min)<0) continue;
          if (candBa.version.compareTo(max)>=0) break;
          ba = candBa;
        }
      }
    }

    log("getBundleArchive("+name +", " +min +", " +max +")->"+ba,
        Project.MSG_VERBOSE);
    return ba;
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

  private void setProperty(final String bn,
                           final String propName)
  {
    log("Searching for a bundle with name '" +bn +"'.", Project.MSG_DEBUG);
    BundleArchives.BundleArchive ba = getBundleArchive(bn);

    if (ba!=null) {
      getProject().setProperty(propName, ba.file.getAbsolutePath());
      log(propName +" = " +ba.file, Project.MSG_VERBOSE);
    } else {
      final int logLevel = failOnMissingBundles
        ? Project.MSG_ERR : Project.MSG_INFO;
      log("No bundle with name '" +bn +"' found.", logLevel);
      log("Known bundles names: " +bas.getKnownNames(), logLevel);

      if (failOnMissingBundles) {
        throw new BuildException("No bundle with name '" +bn+"' found.");
      }
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
    final Path newPath = new Path(getProject());
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
          final BundleArchives.BundleArchive ba = getBundleArchive(fileName);
          if (ba!=null) {
            final String filePath = ba.file.getAbsolutePath();
            newPath.setPath(filePath);
            added = true;
            log(fileName +" => " +filePath, Project.MSG_VERBOSE);
          } else if (isBundleNameWithWildcardVersion(fileName)) {
            final int logLevel = failOnMissingBundles
              ? Project.MSG_ERR : Project.MSG_INFO;
            log("No match for '" +fileName +"' when expanding the path named '"
                +classPathRef.getRefId() +"'.", logLevel);
            log("Known bundles names: " +bas.getKnownNames(), logLevel);
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
    getProject().addReference(newClassPathId, newPath);
  } // end of transform path structure.


  private void createPatternSet()
  {
    final PatternSet patternSet = new PatternSet();

    log("Creating a patternset for the bundles with id='" +patternSetId +"'.",
        Project.MSG_DEBUG);

    for (Iterator it=bas.allBundleArchives.iterator(); it.hasNext();) {
      BundleArchives.BundleArchive ba
        = (BundleArchives.BundleArchive) it.next();

      patternSet.setIncludes(ba.relPath);
      log("Adding includes '" +ba.relPath +"'.", Project.MSG_DEBUG);
    }
    getProject().addReference(patternSetId, patternSet);
  } // end of create pattern set for bundles


  /**
   * Get the set of version patterns (partial versions) used in the
   * keys in replacement filters for the given version.
   * @param version The version to create patterns for.
   * @return Set of version patterns.
   */
  private static String[] getVersionPatterns(Version version)
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
    for (Iterator it = bas.allBundleArchives.iterator(); it.hasNext();) {
      final BundleArchives.BundleArchive ba
        = (BundleArchives.BundleArchive) it.next();
      //Note: since the path is an URL we must ensure that '/' is used.
      final String relPath = ba.relPath.replace('\\','/');
      final String[] versPatterns = getVersionPatterns(ba.version);

      if (null!=ba.bsn) {
        props.put("@" +ba.bsn +".jar@", relPath);
        if (extendedReplaceFilter) {
          props.put("@" +ba.bsn +".location@", ba.file.getAbsolutePath());
          props.put("@" +ba.bsn +".name@", ba.bsn);
          props.put("@" +ba.bsn +".version@", ba.version.toString());
        }
        for (int i=0; i<versPatterns.length; i++) {
          final String prefix = "@" +ba.bsn +versPatterns[i];
          props.put(prefix +".jar@", relPath);
          if (extendedReplaceFilter) {
            props.put(prefix +".location@", ba.file.getAbsolutePath());
            props.put(prefix +".name@", ba.bsn);
            props.put(prefix +".version@", ba.version.toString());
          }
        }
      }

      if (null!=ba.bundleName) {
        for (int i=0; i<versPatterns.length; i++) {
          final String prefix = "@" +ba.bundleName +versPatterns[i];
          props.put(prefix +".jar@", relPath);
          if (extendedReplaceFilter) {
            props.put(prefix +".location@", ba.file.getAbsolutePath());
            if (null!=ba.bsn) {
              props.put(prefix +".name@", ba.bsn);
            }
            props.put(prefix +".version@", ba.version.toString());
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
