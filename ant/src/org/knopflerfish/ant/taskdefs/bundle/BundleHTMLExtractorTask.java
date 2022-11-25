/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ResourceCollection;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import org.knopflerfish.ant.taskdefs.bundle.BundleArchives.BundleArchive;

/**
 * Task that analyzes a set of bundle jar files and builds HTML documentation
 * from these bundles. Also creates cross-references to bundle dependencies.
 *
 * <p>
 * All generated HTML will be stored in the directory specified with the
 * attribute <code>outDir</code> preserving the directory structure underneath
 * the files sets that are scanned for jar-files.
 *
 * E.g., a nested file set that selects
 *
 * <pre>
 * log / log - api.jar
 * </pre>
 *
 * will result in an HTML-file
 *
 * <pre>
 *  <it>outDir</it>/log/log-api.html
 * </pre>
 *
 *
 * <p>
 * The bundle analyzes is based on the attributes in the manifest.
 * </p>
 *
 * <h3>Parameters</h3>
 *
 * <table border=>
 * <tr>
 * <td valign=top><b>Attribute</b></td>
 * <td valign=top><b>Description</b></td>
 * <td valign=top><b>Required</b></td>
 * </tr>
 * <tr>
 * <td valign=top>javadocRelPath</td>
 * <td valign=top>Relative path (from outDir) to javadocs.</td>
 * <td valign=top>No.<br>
 * Default value is "."</td>
 * </tr>
 * <tr>
 * <td valign=top>outDir</td>
 * <td valign=top>
 * Directory to place resulting files in.</td>
 * <td valign=top>No.<br>
 * Default value is "."</td>
 * </tr>
 * <tr>
 * <td valign=top>templateHTMLDir</td>
 * <td valign=top>
 * Directory containing HTML template files. This directory must contain the
 * files:
 *
 * <pre>
 *    bundle_index.html
 *    bundle_list.html
 *    bundle_main.html
 *    bundle_info.html
 *    package_list.html
 *    style.css
 * </pre>
 *
 * </td>
 * </td>
 * <td valign=top>No.<br>
 * Default value is "."</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>systemPackageSet</td>
 * <td valign=top>
 * Comma-separated set of packages which are system packages and thus globally
 * available. These are not cross-referenced.</td>
 * <td valign=top>No.<br>
 * Default value is <code>javax.swing, javax.accessibility,
 *     javax.servlet, javax.xml,org.xml, org.w3c, java, com.sun</code></td>
 *
 * <tr>
 * <td valign=top>skipAttribSet</td>
 * <td valign=top>
 * Comma-separated set of manifest attributes which shouldn't be printed.</td>
 * <td valign=top>No.<br>
 * Default value is <code>Manifest-Version, Ant-Version,
 *     Bundle-Config, Created-By, Built-From</code></td>
 * </tr>
 *
 * <tr>
 * <td valign=top>includeSourceFiles</td>
 * <td valign=top>
 * Controls if Java source files shall be copied and linked into the HTML
 * structure.</td>
 * <td valign=top>No.<br>
 * Default value "False"</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>includeSourceFileRepositoryLinks</td>
 * <td valign=top>
 * Controls if links to the repository version of Java source files shall be
 * added to the HTML structure. The link target will be created from local file
 * names by replacing the path prefix that matches the property
 * <code>rootDir</code> by <code>repositoryURL</code>.</td>
 * <td valign=top>No.<br>
 * Default value "False"</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>rootDir</td>
 * <td valign=top>
 * The prefix of the source file absolute path to remove when creating a
 * repository URL for the source file. See
 * <code>includeSourceFileRepositoryLinks</code> for details.</td>
 * <td valign=top>No.<br>
 * Default value ""</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>repositoryURL</td>
 * <td valign=top>
 * The base URL to source file repository. See
 * <code>includeSourceFileRepositoryLinks</code> for details.</td>
 * <td valign=top>No.<br>
 * Default value ""</td>
 * </tr>
 *
 * <tr>
 * <td valign=top>listHeader</td>
 * <td valign=top>
 * Heading to print at the top of the bundle list in the left frame of the page.
 * </td>
 * <td valign=top>No.<br>
 * Default value is ""</td>
 * </tr>
 *
 * </table>
 *
 * <h3>Parameters specified as nested elements</h3> <h4>fileset</h4>
 *
 * (required)<br>
 * <p>
 * Jar files to analyze must be selected by a nested file set. All jar file
 * selected by a nested file set will be analyzed.
 * </p>
 *
 * <h3>Examples</h3>
 *
 * <pre>
 * &lt;bundlehtml templateHTMLDir    = "${ant.dir}/html_template"
 *                outDir             = "${release.dir}/docs"
 *                baseDir            = "${release.dir}/osgi"
 *                javadocRelPath     = "../javadoc"
 *   &gt;
 *
 *     &lt;fileset dir="${release.dir}/osgi/jars"&gt;
 *       &lt;include name = "&ast;&ast;/&ast;.jar"/&gt;
 *     &lt;/fileset&gt;
 * </pre>
 *
 */
public class BundleHTMLExtractorTask
  extends Task
{

  /** Last part of the javadoc URL for a class / interface. */
  static final String HTML = ".html";
  /** Last part of the javadoc URL for a package. */
  static final String PACKAGE_SUMMARY_HTML = "/package-summary.html";

  private static final String listSeparator = "<br>\n";
  private static final String indexListHeader = "<h2>${bundle.list.header}</h2>";

  private static final String indexListRow =
    "<a target=\"bundle_main\" href=\"${bundledoc}\">${FILE.short}</a><br>\n";

  private static final String indexMainRow =
    "<tr>"
        + "<td><a target=\"bundle_main\" href=\"${bundledoc}\">${FILE.short}</a></td><td>"
        + "<td>${Bundle-Description}</td>" + "</tr>\n";

  private static final String indexMainUnresolved =
    "<table>\n <tr><td colspan=2 class=\"mfheader\">Unresolved packages</td></tr>\n${unresolvedRows}\n</table>\n";

  private static final String indexMainUnresolvedRow =
    " <tr><td>${FILE.short}</td>\n  <td>${pkgs}</td></tr>\n";

  private static final String bundleRow =
    "<tr><td><a href=\"${bundledoc}\">${FILE.short}</a></td><td>${what}</td></tr>\n";

  private static final String packageListRow =
    "<tr><td>${pkg}</td><td>${providers}</td></tr>\n";

  private static final String missingRow =
    "<tr><td>${name}</td><td>${version}</td></tr>\n";

  private static final String pkgHTML = "${namelink}";
  private static final String pkgHTMLversion = "${namelink}&nbsp;${version}";

  private final Map<String, Object> globalVars = new TreeMap<>();

  /**
   * Mapping from package name to bundle archive that uses the package
   * containing one entry for each package that does not have any java doc.
   */
  private final Map<String, BundleArchive> missingDocs = new TreeMap<>();

  public BundleHTMLExtractorTask()
  {

    setListProps("Export-Package," + "Import-Package," + "Import-Service,"
                 + "Export-Service");

    setSystemPackageSet("javax.swing," + "javax.accessibility,"
                        + "javax.servlet," + "javax.xml," + "org.xml,"
                        + "org.w3c," + "java," + "com.sun," + "com.apple.eawt");

    setSkipAttribSet("Manifest-Version," + "Ant-Version," + "Bundle-Config,"
                     + "Created-By"
    // "Built-From",
    );

    setAlwaysProps("Build-Date," + "Bundle-Activator," + "Bundle-Classpath,"
                   + "Bundle-ContactAddress," + "Bundle-Description,"
                   + "Bundle-License," + "Bundle-DocURL,"
                   + "Bundle-ManifestVersion," + "Bundle-Name,"
                   + "Bundle-SymbolicName," + "Bundle-Vendor,"
                   + "DynamicImport-Package," + "Export-Package,"
                   + "Import-Package," + "Provide-Capability,"
                   + "Require-Capability," + "Main-class");
  }

  private boolean bCheckJavaDoc = true;

  public void setCheckJavaDoc(boolean b)
  {
    this.bCheckJavaDoc = b;
  }

  private File templateHTMLDir = new File(".");

  public void setTemplateHTMLDir(File f)
  {
    this.templateHTMLDir = f;

    if (!templateHTMLDir.exists()) {
      throw new BuildException("templateHTMLDir: " + f + " does not exist");
    }
    if (!templateHTMLDir.isDirectory()) {
      throw new BuildException("templateHTMLDir: " + f + " is not a directory");
    }
  }

  private boolean include_source_files = false;

  public void setIncludeSourceFiles(boolean b)
  {
    this.include_source_files = b;
  }

  private boolean includeSourceFileRepositoryLinks = false;

  public void setIncludeSourceFileRepositoryLinks(boolean b)
  {
    this.includeSourceFileRepositoryLinks = b;
  }

  private String rootDir = null;

  public void setRootDir(File f)
  {
    rootDir = f.getAbsolutePath() + File.separator;
  }

  private URL repositoryURL = null;

  public void setRepositoryURL(URL url)
  {
    repositoryURL = url;
  }

  File getBundleInfoTemplate()
  {
    return new File(templateHTMLDir, "bundle_info.html");
  }

  File getBundleCSSTemplate()
  {
    return new File(templateHTMLDir, "style.css");
  }

  File getBundleListTemplate()
  {
    return new File(templateHTMLDir, "bundle_list.html");
  }

  File getPackageListTemplate()
  {
    return new File(templateHTMLDir, "package_list.html");
  }

  File getBundleMainTemplate()
  {
    return new File(templateHTMLDir, "bundle_main.html");
  }

  File getBundleIndexTemplate()
  {
    return new File(templateHTMLDir, "bundle_index.html");
  }

  File getBundleHeaderTemplate()
  {
    return new File(templateHTMLDir, "bundle_header.html");
  }

  private File outDir = new File(".");

  public void setOutDir(String s)
  {
    this.outDir = new File((new File(s)).getAbsolutePath());
  }

  private String javadocRelPath = null;

  public void setJavadocRelPath(String s)
  {
    this.javadocRelPath = s;
  }

  private final List<ResourceCollection> filesets = new ArrayList<>();

  @SuppressWarnings("unused")
  public void addFileset(FileSet set)
  {
    filesets.add(set);
  }

  Set<String> listPropSet = new HashSet<>();

  public void setListProps(String s)
  {
    listPropSet = Util.parseEnumeration("list props", s);
  }

  Set<String> alwaysPropSet = new HashSet<>();

  /**
   * Comma separated string with keys that shall have an empty default value.
   */
  public void setAlwaysProps(String s)
  {
    alwaysPropSet = Util.parseEnumeration("always props", s);
  }

  Set<String> skipAttribSet = new HashSet<>();

  public void setSkipAttribSet(String s)
  {
    skipAttribSet = Util.parseEnumeration("skip attributes set", s);
  }

  Set<String> systemPackageSet = new HashSet<>();

  public void setSystemPackageSet(String s)
  {
    systemPackageSet = Util.parseEnumeration("system packages", s);
  }

  private String listHeader = "";

  public void setListHeader(String s)
  {
    listHeader = s;
  }

  // Implements Task
  @Override
  public void execute()
      throws BuildException
  {
    if (filesets.size() == 0) {
      throw new BuildException("No nested file sets specified");
    }

    log("Loading bundle information:", Project.MSG_VERBOSE);
    BundleArchives bas = new BundleArchives(this, filesets, true);
    bas.doProviders();

    log("Writing bundle jar docs pages.", Project.MSG_VERBOSE);
    try {
      for (final Entry<String, SortedSet<BundleArchive>> entry : bas.bsnToBundleArchives
          .entrySet()) {
        final Set<BundleArchive> bsnSet = entry.getValue();
        // Sorted set with bundle archives, same BSN, different versions
        for (final Object element : bsnSet) {
          final BundleArchive ba = (BundleArchive) element;
          writeBundlePage(ba);
        }
      }

      final String mainPageTemplate =
        FileUtil.loadFile(getBundleMainTemplate().getAbsolutePath());
      writeBundlesPage(new File(outDir, "main.html"), mainPageTemplate,
                       indexMainRow, bas);

      final String menuPageTemplate =
        FileUtil.loadFile(getBundleListTemplate().getAbsolutePath());
      writeBundlesPage(new File(outDir, "list.html"), menuPageTemplate,
                       indexListRow, bas);

      final String pkgListPageTemplate =
        FileUtil.loadFile(getPackageListTemplate().getAbsolutePath());
      writePkgListPage(new File(outDir, "package_list.html"),
                       pkgListPageTemplate, packageListRow, bas);

      copyFile(getBundleIndexTemplate(), new File(outDir, "index.html"));
      copyFile(getBundleHeaderTemplate(), new File(outDir, "header.html"));
      copyFile(getBundleCSSTemplate(), new File(outDir, "style.css"));

      for (final Object element : missingDocs.keySet()) {
        final String name = (String) element;

        log("Missing javadoc for " + name, Project.MSG_WARN);
      }
    } catch (final IOException e) {
      throw new BuildException("Faild to write bundle jar docs: " + e, e);
    }
  }

  /**
   * Mapping from bundle archive to Map with variables and their expansion for
   * the that particular bundle archive.
   */
  private final Map<BundleArchive, Map<String, Object>> ba2varMap =
      new HashMap<>();

  /**
   * Get the variable expansion map the the given bundle archive.
   *
   * @param ba
   *          bundle archive to get variable expansions for.
   * @return Mapping form variable name to expansion.
   * @throws IOException
   *           When outDir can not be made canonical.
   */
  private Map<String, Object> getVarMap(final BundleArchive ba)
      throws IOException
  {
    Map<String, Object> res = ba2varMap.get(ba);
    if (null == res) {
      res = new HashMap<>();
      ba2varMap.put(ba, res);

      // Populate the variable expansion map
      final String relPath = replace(ba.relPath, ".jar", "");
      final String path = outDir.getCanonicalPath() + File.separator + relPath;
      res.put("html.file", path + HTML);
      res.put("html.uri", replace(relPath, "\\", "/") + HTML);
      res.put("src.dir", path + "/src");
      res.put("src.uri", replace(relPath, "\\", "/") + "/src");
      // relative path from the outDir to the bundle doc file for this bundle
      res.put("bundledoc", replace(relPath, "\\", "/") + HTML);

      res.put("FILE", ba.file.getName());
      res.put("FILE.short", replace(ba.file.getName(), ".jar", ""));
      res.put("BYTES", String.valueOf(ba.file.length()));
      res.put("KBYTES", String.valueOf(ba.file.length() / 1024));
      res.put("FILEINFO", ba.file.length() + " bytes"
                          + (0 < ba.srcCount ? ", includes source" : ""));

      int sepIx = ba.relPath.indexOf(File.separator);
      String relPathUp = "";
      while (sepIx > -1) {
        relPathUp += ".." + File.separator;
        sepIx = ba.relPath.indexOf(File.separator, sepIx + 1);
      }
      res.put("relpathup", relPathUp);

      res.put("jarRelPath",
              createRelPath(new File(path).getParentFile(),
                            ba.file.getCanonicalPath()));
      res.put("javadocdir", javadocRelPath != null ? javadocRelPath : "");

      // Manifest entries
      for (final Entry<Object, Object> entry : ba.mainAttributes.entrySet()) {
        final String key = entry.getKey().toString();
        String value = entry.getValue().toString();

        // Special formatting of the value for some keys:
        if ("Export-Package".equals(key)) {
          value =
            getPackagesJavadocString(ba, relPathUp, ba.pkgExportMap,
                                     PACKAGE_SUMMARY_HTML);
        } else if ("Import-Package".equals(key)) {
          value =
            getPackagesJavadocString(ba, relPathUp, ba.pkgImportMap,
                                     PACKAGE_SUMMARY_HTML);
        } else if ("Import-Service".equals(key)) {
          value =
            getPackagesJavadocString(ba, relPathUp, ba.serviceImportMap, HTML);
        } else if ("Export-Service".equals(key)) {
          value =
            getPackagesJavadocString(ba, relPathUp, ba.serviceExportMap, HTML);
        } else if (listPropSet.contains(key)) {
          value = replace(value, ",", listSeparator);
        }
        res.put(key, value);
      }

      // Key that shall have an empty value if not set from some other data.
      for (final Object element : alwaysPropSet) {
        final String key = element.toString();

        if (!res.containsKey(key)) {
          res.put(key, "");
        }
      }

    }
    return res;
  }

  /**
   * Build a HTML formated string with one row for each class / package in the
   * given map.
   *
   * @param ba
   *          The bundle archive that owns the package mapping to present.
   * @param relPathUp
   *          relative path up from the directory where the file with the return
   *          string will be written to the specified out directory.
   * @param map
   *          Mapping from package name to package version.
   * @param linkSuffix
   *          Suffix to add to the java doc link for the package.
   * @return String representation of the packages in the map.
   */
  private String getPackagesJavadocString(final BundleArchive ba,
                                          final String relPathUp,
                                          final Map<String, ?> map,
                                          final String linkSuffix)
  {
    final StringBuilder sb = new StringBuilder();

    for (final Entry<String, ?> entry : map.entrySet()) {
      final String name = entry.getKey();
      final String version = entry.getValue().toString();

      sb.append(getJavadocString(ba, relPathUp, name, version, linkSuffix))
          .append("<br>\n");
    }

    return sb.toString();
  }

  /**
   * Builds a comma separated string with the package names in the set (linking
   * to javadoc).
   *
   * @param relPathUp
   *          relative path up from the directory where the file with the return
   *          string will be written to the specified out directory.
   * @param set
   *          The package names to present links for.
   * @return Comma separated string with all the packages in the set.
   */
  private String getPackagesJavadocString(final String relPathUp,
                                          final Set<String> set)
  {
    final StringBuilder sb = new StringBuilder();
    for (final String pkgName : set) {
      if (0 < sb.length()) {
        sb.append(", ");
      }
      sb.append(getPackageJavadocString(null, relPathUp, pkgName, ""));
    }
    return sb.toString();
  }

  /**
   * Build a HTML formated string with links to the javadoc for one named
   * package.
   *
   * @param ba
   *          The bundle archive that owns the package mapping to present.
   * @param relPathUp
   *          relative path up from the directory where the file with the return
   *          string will be written to the specified out directory.
   * @param pkg
   *          The java package to present.
   * @param version
   *          The version of the java package to present.
   * @return String representation of the packages in the map.
   */
  private String getPackageJavadocString(final BundleArchive ba,
                                         final String relPathUp,
                                         final String pkg,
                                         final String version)
  {
    return getJavadocString(ba, relPathUp, pkg, version, PACKAGE_SUMMARY_HTML);
  }

  /**
   * Build a HTML formated string (one row) for the given package / class.
   *
   * @param ba
   *          The bundle archive that owns the package mapping to present.
   * @param relPathUp
   *          relative path up from the directory where the file with the return
   *          string will be written to the specified out directory.
   * @param pkg
   *          The java package to present.
   * @param version
   *          The version of the java package to present.
   * @param linkSuffix
   *          Suffix to add to the java doc link for the package. Should be
   *          {@link #HTML} for classes and {@link #PACKAGE_SUMMARY_HTML} when
   *          <code>pkg</code> is a java package.
   *
   * @return HTML string representation of the package / class linking to local
   *         Javadoc.
   */
  private String getJavadocString(final BundleArchive ba,
                                  final String relPathUp,
                                  final String pkg,
                                  final String version,
                                  final String linkSuffix)
  {
    String row =
      (null == version || 0 == version.length()) ? pkgHTML : pkgHTMLversion;

    final String docFile = replace(pkg, ".", "/") + linkSuffix;
    final String docPath =
      relPathUp + javadocRelPath + "/index.html?" + docFile;

    final File f =
      new File(outDir + File.separator + javadocRelPath + File.separator
               + docFile);

    if (javadocRelPath != null && !"".equals(javadocRelPath)) {
      if (isSystemPackage(pkg)) {
        row = replace(row, "${namelink}", "${name}");
      } else if ((bCheckJavaDoc && !f.exists())) {
        row = replace(row, "${namelink}", "${name}");
        if (null != ba) {
          missingDocs.put(pkg, ba);
        }
      } else {
        row =
          replace(row, "${namelink}",
                  "<a target=\"_top\" href=\"${javadoc}\">${name}</a>");
      }
    } else {
      row = replace(row, "${namelink}", "${name}");
    }

    row = replace(row, "${name}", pkg);
    row = replace(row, "${version}", version);
    row = replace(row, "${javadoc}", docPath);

    return row;
  }

  private String stdReplace(final BundleArchive ba, final String template)
      throws IOException
  {
    String res = template;

    for (final Entry<String, Object> entry : globalVars.entrySet()) {
      res =
        replace(res, "${" + entry.getKey() + "}", entry.getValue().toString());
    }

    final Map<String, Object> baVars = getVarMap(ba);
    for (final Entry<String, Object> entry : baVars.entrySet()) {
      res =
        replace(res, "${" + entry.getKey() + "}", entry.getValue().toString());
    }

    return res;
  }

  /**
   * Replace <code>${MF.UNHANDLED}</code> with one line for each manifest
   * attribute of <code>ba</code>that is not explicitly substituted in the
   * template string and this is not included in the set of manifest attributes
   * to be skipped.
   *
   * @param template
   *          The template string to do the replacement in.
   * @param ba
   *          The bundle archive to insert manifest attributes from.
   * @return the template string with <code>${MF.UNHANDLED}</code> replaced.
   */
  private String mfaReplace(final String template, final BundleArchive ba)
      throws IOException
  {
    final Set<String> handledSet = new TreeSet<>();
    // The set of manifest attribute names in the bundle.
    final Set<String> mfanSet = new TreeSet<>();
    for (final Object element : ba.mainAttributes.keySet()) {
      final String mfan = element.toString();

      mfanSet.add(mfan);
      if (template.contains("${" + mfan + "}")) {
        handledSet.add(mfan);
      }
    }

    // Build replacement string for ${MF.UNHANDLED} that shall contain one row
    // for
    // each manifest attribute not in the template and not in the skip set.
    final Map<String, Object> varMap = getVarMap(ba);
    final Set<String> otherMfans = new TreeSet<>(mfanSet);
    otherMfans.removeAll(skipAttribSet);
    otherMfans.removeAll(handledSet);
    final StringBuilder mfOtherAttributes = new StringBuilder();
    for (final String key : otherMfans) {
      String value = (String) varMap.get(key);
      // If value is a valid URL, present it as a link
      try {
        final URL url = new URL(value);
        value = "<a target=\"_top\" href=\"" + url + "\">" + value + "</a>";
      } catch (final MalformedURLException ignored) {
      }
      mfOtherAttributes.append("<tr>\n").append(" <td>").append(key)
          .append("</td>\n").append(" <td>").append(value).append("</td>\n")
          .append("</tr>\n");
    }
    return replace(template, "${MF.UNHANDLED}", mfOtherAttributes.toString());
  }

  /**
   * Replace <code>${depending.list}</code> with one row for each bundle that
   * may import a package exported by <code>ba</code>. The row will contain the
   * bundle name and all the packages it may import.
   *
   * @param template
   *          The template string to do the replacement in.
   * @param ba
   *          The bundle archive to insert manifest attributes from.
   * @return the template string with <code>${depending.list}</code> replaced.
   */
  private String dependingReplace(final String template, final BundleArchive ba)
      throws IOException
  {
    final Map<String, Object> varMap = getVarMap(ba);
    final String relPathUp = varMap.get("relpathup").toString();

    // Build list of bundles depending on this bundle.
    final StringBuilder dependingList = new StringBuilder();
    if (ba.pkgProvidedMap.size() == 0) {
      dependingList.append("None found");
    } else {
      for (final Entry<BundleArchive, SortedSet<String>> entry : ba.pkgProvidedMap
          .entrySet()) {
        final BundleArchive dependentBa = entry.getKey();
        final Set<String> pkgs = entry.getValue();

        String row =
          replace(bundleRow, "${what}",
                  getPackagesJavadocString(relPathUp, pkgs));
        row =
          replace(row, "${bundledoc}",
                  relPathUp + getVarMap(dependentBa).get("html.uri"));
        row = stdReplace(dependentBa, row);
        dependingList.append(row);
      }
    }
    return replace(template, "${depending.list}", dependingList.toString());
  }

  /**
   * Replace <code>${depends.list}</code> with one row for each bundle that may
   * import a package exported by <code>ba</code>. The row will contain the
   * bundle name and all the packages it may import.
   *
   * @param template
   *          The template string to do the replacement in.
   * @param ba
   *          The bundle archive to insert manifest attributes from.
   * @return the template string with <code>${depends.list}</code> replaced.
   */
  private String providersReplace(final String template, final BundleArchive ba)
      throws IOException
  {
    final Map<String, Object> varMap = getVarMap(ba);
    final String relPathUp = varMap.get("relpathup").toString();

    // Build list of bundles that this bundle depends on, i.e., bundles that
    // provides pkgs to this one.
    final StringBuilder providersList = new StringBuilder();
    if (ba.pkgProvidersMap.size() == 0 && ba.pkgUnprovidedMap.size() == 0) {
      providersList.append("None found");
    } else {
      for (final Entry<BundleArchive, SortedSet<String>> entry : ba.pkgProvidersMap
          .entrySet()) {
        final BundleArchive providingBa = entry.getKey();
        final Set<String> pkgs = entry.getValue();

        String row =
          replace(bundleRow, "${what}",
                  getPackagesJavadocString(relPathUp, pkgs));
        row =
          replace(row, "${bundledoc}",
                  relPathUp + getVarMap(providingBa).get("html.uri"));
        row = stdReplace(providingBa, row);
        providersList.append(row);
      }

      boolean unresolvedHeadingInculded = false;
      for (final Entry<String, VersionRange> entry : ba.pkgUnprovidedMap
          .entrySet()) {
        final String pkgName = entry.getKey();

        if (!isSystemPackage(pkgName)) {
          if (!unresolvedHeadingInculded) {
            String row = missingRow;
            row = replace(row, "${name}", "<b>Unresolved</b>");
            row = replace(row, "${version}", "");

            providersList.append(row);
            unresolvedHeadingInculded = true;
          }
          final Object versionRange = entry.getValue();

          String row = missingRow;
          row = replace(row, "${name}", pkgName);
          row = replace(row, "${version}", versionRange.toString());

          providersList.append(row);
        }
      }
    }
    return replace(template, "${depends.list}", providersList.toString());
  }

  /**
   * Replace <code>${sources.list}</code> with one row for each source file that
   * can be found.
   *
   * @param template
   *          The template string to do the replacement in.
   * @param ba
   *          The bundle archive to insert manifest attributes from.
   * @return the template string with <code>${sources.list}</code> replaced.
   */
  private String sourcesReplace(final String template, final BundleArchive ba)
      throws IOException
  {
    final Map<String, Object> varMap = getVarMap(ba);
    final List<String> srcList = new ArrayList<>();
    final StringBuilder sb = new StringBuilder();

    if (include_source_files) {
      log("including source files in jardoc", Project.MSG_VERBOSE);
      srcList
          .addAll(ba.extractSources(new File((String) varMap.get("src.dir"))));
    } else {
      log("includeSourceFiles is not set, skipping sources",
          Project.MSG_VERBOSE);
    }

    if (srcList.size() > 0) {
      sb.append("<table>\n");
      for (final Object element : srcList) {
        final String name = (String) element;
        final String uri =
          replace(ba.file.getName(), ".jar", "") + "/src/" + name;

        sb.append(" <tr>\n");
        sb.append("  <td>\n");
        sb.append("    <a href=\"").append(uri).append("\">").append(name).append("<a>\n");
        sb.append("  </td>\n");
        sb.append(" </tr>\n");

      }
      sb.append("</table>");
    } else {
      // No source files extracted
      if (includeSourceFileRepositoryLinks) {
        final Map<String, String> srcRepositoryLinkMap =
          ba.getSrcRepositoryLinks(rootDir, repositoryURL);

        if (0 < srcRepositoryLinkMap.size()) {
          sb.append("<table>");
          for (final Entry<String, String> entry : srcRepositoryLinkMap
              .entrySet()) {
            final String name = entry.getKey();
            final String href = entry.getValue();

            sb.append(" <tr>\n");
            sb.append("  <td>\n");
            sb.append("    <a href=\"").append(href).append("\">").append(name).append("<a>\n");
            sb.append("  </td>\n");
            sb.append(" </tr>\n");

          }
          sb.append("</table>");
        }

      }
    }
    if (0 == sb.length()) {
      sb.append("None found");
    }
    return replace(template, "${sources.list}", sb.toString());
  }

  private void writeBundlePage(final BundleArchive ba)
      throws IOException
  {
    final Map<String, Object> varMap = getVarMap(ba);

    final String template =
      FileUtil.load(getBundleInfoTemplate().getAbsolutePath());
    String res = mfaReplace(template, ba);
    res = dependingReplace(res, ba);
    res = providersReplace(res, ba);
    res = sourcesReplace(res, ba);
    res = stdReplace(ba, res);

    final String outName = (String) varMap.get("html.file");
    FileUtil.writeStringToFile(new File(outName), res);
    log("Wrote " + outName, Project.MSG_VERBOSE);
  }

  /**
   * Write the a page that lists all bundles.
   *
   * @param outFile
   *          The file to write to.
   * @param template
   *          The template to use for this file.
   * @param rowTemplate
   *          Template to use for a bundle row in the listing.
   */
  private void writeBundlesPage(final File outFile,
                                final String template,
                                final String rowTemplate,
                                final BundleArchives bas)
      throws IOException
  {
    final String listHeaderRow =
      (null != listHeader && listHeader.length() > 0)
        ? replace(indexListHeader, "${bundle.list.header}", listHeader)
        : listHeader;
    String html = replace(template, "${bundle.list.header}", listHeaderRow);

    final StringBuilder bundleList = new StringBuilder();
    final StringBuilder unresolvedList = new StringBuilder();

    // Build the list of bundles
    for (final Entry<String, SortedSet<BundleArchive>> entry : bas.bnToBundleArchives
        .entrySet()) {
      final Set<BundleArchive> bsnSet = entry.getValue();
      // Sorted set with bundle archives, same BSN, different versions
      for (final BundleArchive ba : bsnSet) {
        bundleList.append(stdReplace(ba, rowTemplate));

        if (0 < ba.pkgUnprovidedMap.size()) {
          // Build one row for each unprovided, non-system package
          final String urow = stdReplace(ba, indexMainUnresolvedRow);
          final StringBuilder sbUpkg = new StringBuilder();
          for (final Entry<String, VersionRange> uPkgEntry : ba.pkgUnprovidedMap
              .entrySet()) {
            final String pkgName = uPkgEntry.getKey();
            if (isSystemPackage(pkgName)) {
              continue;
            }
            final String version = uPkgEntry.getValue().toString();

            if (0 < sbUpkg.length()) {
              sbUpkg.append(",<br>\n   ");
            }
            sbUpkg.append(getPackageJavadocString(ba, "../", pkgName, version));
            if (ba.pkgImportOptional.contains(pkgName)) {
              sbUpkg.append(" <em>optional</em>");
            }
          }
          if (0 < sbUpkg.length()) {
            unresolvedList.append(replace(urow, "${pkgs}", sbUpkg.toString()));
          }
        }
      }
    }
    html = replace(html, "${bundle.list}", bundleList.toString());
    bundleList.setLength(0);

    if (0 < unresolvedList.length()) {
      final String unresolved =
        replace(indexMainUnresolved, "${unresolvedRows}",
                unresolvedList.toString());
      html = replace(html, "${unresolved.list}", unresolved);
      unresolvedList.setLength(0);
    }
    html = replace(html, "${unresolved.list}", "");

    FileUtil.writeStringToFile(outFile, html);
    log("wrote " + outFile, Project.MSG_VERBOSE);
  }

  /**
   * Write a page listing all the provided Java packages and the bundles that
   * provides each package.
   *
   * @param outFile
   *          The file to write to.
   * @param template
   *          The template to use for this file.
   * @param rowTemplate
   *          Template to use for a bundle row in the listing.
   */
  @SuppressWarnings("SameParameterValue")
  private void writePkgListPage(final File outFile,
                                final String template,
                                final String rowTemplate,
                                final BundleArchives bas)
      throws IOException
  {
    final StringBuilder sb = new StringBuilder();

    for (final Entry<String, SortedMap<Version, SortedSet<BundleArchive>>> entry : bas.allExports
        .entrySet()) {
      final String pkg = entry.getKey();
      final Map<Version, SortedSet<BundleArchive>> vpMap = entry.getValue();

      for (final Entry<Version, SortedSet<BundleArchive>> vpEntry : vpMap
          .entrySet()) {
        final String version = vpEntry.getKey().toString();
        final Set<BundleArchive> providerBas = vpEntry.getValue();

        final String row =
          replace(rowTemplate, "${pkg}",
                  getPackageJavadocString(null, "", pkg, version));
        final StringBuilder sbProviders = new StringBuilder();
        for (final Object element : providerBas) {
          final BundleArchive provider = (BundleArchive) element;
          sbProviders.append(stdReplace(provider, indexListRow));
        }
        sb.append(replace(row, "${providers}", sbProviders.toString()));
      }
    }

    FileUtil.writeStringToFile(outFile,
                               replace(template, "${package.list}",
                                       sb.toString()));
    log("wrote " + outFile, Project.MSG_VERBOSE);
  }

  void copyFile(File templateFile, File outFile)
      throws IOException
  {

    final String src = FileUtil.loadFile(templateFile.getAbsolutePath());

    FileUtil.writeStringToFile(outFile, src);
    log("copied " + outFile, Project.MSG_VERBOSE);
  }

  /**
   * Derive relative path from <tt>fromDir</tt> to the file given by
   * <tt>filePath</tt>.
   */
  @SuppressWarnings("StringConcatenationInLoop")
  static String createRelPath(File fromDir, String filePath)
  {
    String res = "";
    while (fromDir != null && !filePath.startsWith(fromDir.toString())) {
      if (res.length() > 0) {
        res += File.separator;
      }
      res += "..";
      fromDir = fromDir.getParentFile();
    }

    if (fromDir == null) {
      // Relative path not possible
      return filePath;
    }

    if (res.length() > 0) {
      res += File.separator;
    }
    res += filePath.substring(fromDir.toString().length() + 1);
    return res;
  }

  boolean isSystemPackage(String name)
  {
    for (final Object element : systemPackageSet) {
      final String prefix = (String) element;
      if (name.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  /**
   * A variant of {@link Util#replace(String,String,String)} that uses the empty
   * string as replacement for null values.
   *
   * @param src
   *          The template string to do replace all matches in.
   * @param a
   *          The math-string to be replaced.
   * @param b
   *          The replacement string to use.
   * @return The template with all occurrences of <code>a</code> replaced by
   *         <code>b</code>.
   */
  String replace(String src, String a, String b)
  {
    return Util.replace(src, a, b == null ? "" : b);
  }

}
