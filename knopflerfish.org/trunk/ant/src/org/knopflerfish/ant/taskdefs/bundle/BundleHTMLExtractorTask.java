/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

import org.osgi.framework.Version;

/**
 * Task that analyzes a set of bundle jar files and builds HTML documentation
 * from these bundles. Also creates cross-references to bundle dependencies.
 *
 * <p>
 * All generated HTML will be stored in the same directory structure as
 * the scanned jars, e.g a jar file
 * <pre>
 *  <it>baseDir</it>/log/log-api.jar
 * </pre>
 * will have a corresponding
 * <pre>
 *  <it>outDir</it>/log/log-api.html
 * </pre>
 * in the directory specified with the attribute <code>outDir</code>.
 * The part of the original bundle jar path to remove when creating
 * the output directory structure in <code>outDir</code> is specified
 * by the <code>baseDir</code> attribute.
 *
 * <p>
 * Bundle jar files files are analyzed using the static manifest attributes.
 * </p>
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
 *   <td valign=top>javadocRelPath</td>
 *   <td valign=top>Relative path (from outDir) to javadocs.
 *   </td>
 *   <td valign=top>No.<br> Default value is "."</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>outDir</td>
 *   <td valign=top>
 *    Directory to place resulting files in.
 *   </td>
 *   <td valign=top>No.<br> Default value is "."</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>baseDir</td>
 *   <td valign=top>
 *    Remove this part of the path from the specified jar-files and
 *    use the remainder as file name in the outDir.
 *   </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>templateHTMLDir</td>
 *   <td valign=top>
 *   Directory containing HTML template files. This directory must
 *   contain the files:
 *   <pre>
 *    bundle_index.html
 *    bundle_list.html
 *    bundle_main.html
 *    style.css
 *   </pre>
 *   </td>
 *  </td>
 *   <td valign=top>No.<br> Default value is "."</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>systemPackageSet</td>
 *   <td valign=top>
 *    Comma-separated set of packages which are system packages and
 *    thus globally available.
 *    These are not cross-referenced.
 *   </td>
 *   <td valign=top>No.<br>
 *     Default value is <code>javax.swing, javax.accessibility,
 *     javax.servlet, javax.xml,org.xml, org.w3c, java, com.sun</code>
 *   </td>
 *
 *  <tr>
 *   <td valign=top>skipAttribSet</td>
 *   <td valign=top>
 *    Comma-separated set of manifest attributes which shouldn't be printed.
 *   </td>
 *   <td valign=top>No.<br>
 *     Default value is <code>Manifest-Version, Ant-Version,
 *     Bundle-Config, Created-By, Built-From</code>
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>includeSourceFiles</td>
 *   <td valign=top>
 *    Controls if Java source files shall be copied and linked into
 *    the HTML structure.
 *   </td>
 *   <td valign=top>No.<br>
 *   Default value "False"
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>listHeader</td>
 *   <td valign=top>
 *    Heading to print at the top of the bundle list in the left frame
 *    of the page.
 *   </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *
 * </table>
 *
 * <h3>Parameters specified as nested elements</h3>
 * <h4>fileset</h4>
 *
 * (required)<br>
 * <p>
 * All jar files must be specified as a fileset. No jar files
 * are ignored.
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
public class BundleHTMLExtractorTask extends Task {

  private Vector    filesets = new Vector();
  private FileUtils fileUtils;

  private File   templateHTMLDir     = new File(".");
  private String listSeparator       = "<br>\n";
  private File   outDir              = new File(".");
  private File   baseDir             = null;
  private String javadocRelPath      = null;
  private String listHeader          = "";

  private String indexListHeader = "<h2>${bundle.list.header}</h2>";

  private String indexListRow =
    "<a target=\"bundle_main\" href=\"${bundledoc}\">${FILE.short}</a><br>";

  private String indexMainRow =
    "<tr>" +
    "<td><a target=\"bundle_main\" href=\"${bundledoc}\">${FILE.short}</a></td><td>" +
    "<td>${Bundle-Description}</td>" +
    "</tr>\n";

  private String bundleRow    =
    "<tr><td><a href=\"${bundledoc}\">${FILE.short}</a></td><td>${what}</td></tr>\n";

  private String packageListRow   =
    "<tr><td>${namelink}&nbsp;${version}</td><td>${providers}</td></tr>\n";

  private String missingRow   =
    "<tr><td>${name}</td><td>${version}</td></tr>\n";

  private String rowHTML      =
    "<a href=\"${bundle.uri}\">${FILE.short}</a><br>\n";

  private String pkgHTML      =
    "${namelink}&nbsp;${version}<br>";

  private boolean bCheckJavaDoc  = true;
  private boolean include_source_files  = false;

  Map     jarMap     = new TreeMap(new FileNameComparator());
  Map     globalVars = new TreeMap();


  Map     missingDocs = new TreeMap();

  public BundleHTMLExtractorTask() {

    fileUtils = FileUtils.newFileUtils();

    setListProps("Export-Package," +
                 "Import-Package," +
                 "Import-Service," +
                 "Export-Service");

    setSystemPackageSet("javax.swing," +
                        "javax.accessibility," +
                        "javax.servlet," +
                        "javax.xml," +
                        "org.xml," +
                        "org.w3c," +
                        "java," +
                        "com.sun");

    setSkipAttribSet("Manifest-Version," +
                     "Ant-Version," +
                     "Bundle-Config," +
                     "Created-By"
                     //              "Built-From",
                     );

    setAlwaysProps("Bundle-Activator," +
                   "Bundle-Vendor," +
                   "Bundle-Name," +
                   "Bundle-Description," +
                   "Export-Package," +
                   "Import-Package," +
                   "Import-Service," +
                   "Export-Service," +
                   "Main-class," +
                   "Build-Date," +
                   "Bundle-DocURL," +
                   "Bundle-Classpath," +
                   "Bundle-ContactAddress," +
                   "Bundle-Activator");
  }

  public void setCheckJavaDoc(String s) {
    this.bCheckJavaDoc = "true".equals(s);
  }

  public void setTemplateHTMLDir(String s) {
    this.templateHTMLDir = new File(s);

    if(!templateHTMLDir.exists()) {
      throw new BuildException("templateHTMLDir: " + s + " does not exist");
    }
    if(!templateHTMLDir.isDirectory()) {
      throw new BuildException("templateHTMLDir: " + s + " is not a directory");
    }
  }

  public void setIncludeSourceFiles(String s) {
    this.include_source_files = "true".equals(s);
  }

  File getBundleInfoTemplate() {
    return new File(templateHTMLDir, "bundle_info.html");
  }

  File getBundleCSSTemplate() {
    return new File(templateHTMLDir, "style.css");
  }

  File getBundleListTemplate() {
    return new File(templateHTMLDir, "bundle_list.html");
  }

  File getPackageListTemplate() {
    return new File(templateHTMLDir, "package_list.html");
  }

  File getBundleMainTemplate() {
    return new File(templateHTMLDir, "bundle_main.html");
  }

  File getBundleIndexTemplate() {
    return new File(templateHTMLDir, "bundle_index.html");
  }

  File getBundleHeaderTemplate() {
    return new File(templateHTMLDir, "bundle_header.html");
  }

  public void setOutDir(String s) {
    this.outDir = new File((new File(s)).getAbsolutePath());
  }

  public void setBaseDir(String s) {
    this.baseDir = new File((new File(s)).getAbsolutePath());
  }

  public void setJavadocRelPath(String s) {
    this.javadocRelPath = s;
  }

  public void addFileset(FileSet set) {
    filesets.addElement(set);
  }

  Set listPropSet      = new HashSet();
  Set skipAttribSet    = new HashSet();
  Set alwaysPropSet    = new HashSet();
  Set systemPackageSet = new HashSet();

  public void setListProps(String s) {
    listPropSet = Util.makeSetFromStringList(s);
  }

  public void setAlwaysProps(String s) {
    alwaysPropSet = Util.makeSetFromStringList(s);
  }

  public void setSkipAttribSet(String s) {
    skipAttribSet = Util.makeSetFromStringList(s);
  }

  public void setSystemPackageSet(String s) {
    systemPackageSet = Util.makeSetFromStringList(s);
  }

  public void setListHeader(String s) {
    listHeader = s;
  }

  // Implements Task
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
    }

    try {
      for (int i = 0; i < filesets.size(); i++) {
        FileSet          fs      = (FileSet) filesets.elementAt(i);
        DirectoryScanner ds      = fs.getDirectoryScanner(project);
        File             projDir = fs.getDir(project);

        String[] srcFiles = ds.getIncludedFiles();
        String[] srcDirs  = ds.getIncludedDirectories();

        for (int j = 0; j < srcFiles.length ; j++) {
          File file = new File(projDir, srcFiles[j]);
          if(file.getName().endsWith(".jar")) {
            jarMap.put(file, new BundleInfo(file));
          }
        }
      }

      log("analyzing " + jarMap.size() + " bundles");
      if (include_source_files) {
        log("including source files in jardoc");
      }
      else {
        log("includeSourceFiles is not set, skipping sources");
      }

      for(Iterator it = jarMap.keySet().iterator(); it.hasNext();) {
        File       file = (File)it.next();
        BundleInfo info = (BundleInfo)jarMap.get(file);

        info.load();
      }

      System.out.println("writing bundle info html pages");
      for(Iterator it = jarMap.keySet().iterator(); it.hasNext();) {
        File       file = (File)it.next();
        BundleInfo info = (BundleInfo)jarMap.get(file);

        info.writeInfo();
      }

      makeListPage(getBundleMainTemplate(),
                   new File(outDir, "main.html"),
                   indexMainRow);

      makeListPage(getBundleListTemplate(),
                   new File(outDir, "list.html"),
                   indexListRow);

      makePackageListPage(getPackageListTemplate(),
                          new File(outDir, "package_list.html"),
                          packageListRow);

      copyFile(getBundleIndexTemplate(),
               new File(outDir, "index.html"));

      copyFile(getBundleHeaderTemplate(),
               new File(outDir, "header.html"));

      copyFile(getBundleCSSTemplate(),
               new File(outDir, "style.css"));


      for(Iterator it = missingDocs.keySet().iterator(); it.hasNext();) {
        String name = (String)it.next();

        System.out.println("Missing javadoc for " + name);
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to extract bundle info: " + e, e);
    }
  }

  void makeListPage(File templateFile,
                    File outFile,
                    String rowTemplate)
    throws IOException {

    int unresolvedCount = 0;

    String html = Util.loadFile(templateFile.getAbsolutePath());

    String listHeaderRow
      = (null!=listHeader && listHeader.length()>0)
      ? replace(indexListHeader, "${bundle.list.header}", listHeader)
      : listHeader;
    html = replace(html, "${bundle.list.header}", listHeaderRow);

    StringBuffer sb = new StringBuffer();
    for(Iterator it = jarMap.keySet().iterator(); it.hasNext();) {
      File       file = (File)it.next();
      BundleInfo info = (BundleInfo)jarMap.get(file);

      unresolvedCount += info.unresolvedMap.size();

      String row = rowTemplate;
      row = info.stdReplace(row, false);
      row = replace(row, "${bundledoc}", info.relPath +".html");

      sb.append(row);
    }



    html = replace(html, "${bundle.list}", sb.toString());

    sb = new StringBuffer();

    if(unresolvedCount > 0) {

      sb.append("<table>\n");
      sb.append("<tr>\n" +
                " <td colspan=2 class=\"mfheader\">Unresolved packages</td>\n" +
                "</tr>\n");

      for(Iterator it = jarMap.keySet().iterator(); it.hasNext();) {
        File       file = (File)it.next();
        BundleInfo info = (BundleInfo)jarMap.get(file);

        if(info.unresolvedMap.size() > 0) {
          sb.append("<tr>\n" +
                    " <td>" + info.file.getName() + "</td>\n");

          sb.append("<td>");
          for(Iterator it2 = info.unresolvedMap.keySet().iterator();
              it2.hasNext();)
            {
              String pkgName = (String)it2.next();
              Object version = info.unresolvedMap.get(pkgName);

              sb.append(pkgName + " " + version + "<br>\n");
            }
          sb.append("</td>");
          sb.append("<tr>");
        }
      }
      sb.append("</table>\n");
    }

    html = replace(html, "${unresolved.list}", sb.toString());





    Util.writeStringToFile(outFile, html);
    System.out.println("wrote " + outFile);

  }

  void makePackageListPage(File templateFile,
                           File outFile,
                           String rowTemplate)
    throws IOException
  {
    String html = Util.loadFile(templateFile.getAbsolutePath());

    SortedSet pkgs = new TreeSet();
    for (Iterator it=jarMap.values().iterator(); it.hasNext();) {
      BundleInfo info = (BundleInfo) it.next();
      pkgs.addAll(info.pkgExportMap.keySet());
    }

    StringBuffer sb = new StringBuffer();
    for(Iterator it = pkgs.iterator(); it.hasNext();) {
      String pkg = (String) it.next();

      TreeMap pkgVersions = new TreeMap();
      for (Iterator it2=jarMap.values().iterator(); it2.hasNext();) {
        BundleInfo info = (BundleInfo) it2.next();
        Version ver = (Version) info.pkgExportMap.get(pkg);
        if (null!=ver) {
          TreeSet providers = (TreeSet) pkgVersions.get(ver);
          if (null==providers) {
            providers = new TreeSet(new BundleInfoComparator());
          }
          providers.add(info);
          pkgVersions.put(ver,providers);
        }
      }
      for (Iterator it3=pkgVersions.keySet().iterator(); it3.hasNext();) {
        String  row = rowTemplate;
        Version version = (Version) it3.next();

        String docFile = replace(pkg, ".", "/") +"/package-summary.html";
        String docPath = javadocRelPath +"/index.html?" +docFile;

        File f = new File(outDir +File.separator
                          +javadocRelPath +File.separator +docFile);

        if(javadocRelPath != null && !"".equals(javadocRelPath)) {
          if( isSystemPackage(pkg) ) {
            row = replace(row, "${namelink}", "${name}");
          } else if ( (bCheckJavaDoc && !f.exists()) ) {
            row = replace(row, "${namelink}", "${name}");
          } else {
            row = replace(row,
                          "${namelink}",
                          "<a target=\"_top\" href=\"${javadoc}\">${name}</a>");
          }
        } else {
          row = replace(row, "${namelink}", "${name}");
        }
        row = replace(row, "${name}", pkg);
        row = replace(row, "${version}", version.toString());
        row = replace(row, "${javadoc}", docPath);

        TreeSet providers = (TreeSet) pkgVersions.get(version);
        StringBuffer sbProviders = new StringBuffer();
        for (Iterator it4=providers.iterator(); it4.hasNext();) {
          BundleInfo info = (BundleInfo) it4.next();
          String providerRow = indexListRow;
          providerRow = info.stdReplace(providerRow, false);
          providerRow = replace(providerRow,
                                "${bundledoc}",
                                info.relPath +".html");
          sbProviders.append(providerRow);
        }
        row = replace(row, "${providers}", sbProviders.toString());
        sb.append(row);
      }
    }

    html = replace(html, "${package.list}", sb.toString());

    Util.writeStringToFile(outFile, html);
    System.out.println("wrote " + outFile);
  }

  void copyFile(File templateFile, File outFile)
    throws IOException {

    String src = Util.loadFile(templateFile.getAbsolutePath());

    Util.writeStringToFile(outFile, src);
    System.out.println("copied " + outFile);
  }

  interface MapSelector {
    public Map getMap(BundleInfo info);
  }


  class BundleInfo {
    File       file;
    Attributes attribs;
    Map        vars      = new TreeMap();

    // String (package name) -> Version
    Map        pkgExportMap = new TreeMap();
    // String (package name) -> VersionRange
    Map        pkgImportMap = new TreeMap();
    // String (package name) -> Version
    Map        serviceExportMap = new TreeMap();
    // String (package name) -> VersionRange
    Map        serviceImportMap = new TreeMap();

    String     relPath   = "";
    String     relPathUp = "";
    String     path      = "";
    String     jarRelPath = "";

    Map unresolvedMap = new TreeMap();

    public BundleInfo(File file) throws IOException  {
      this.file = file;

      String filePath = file.getCanonicalPath();
      String basePath = baseDir.getCanonicalPath();

      if (filePath.startsWith(basePath)) {
        relPath = filePath.substring(basePath.length()+1);
        if (relPath.endsWith(".jar")) {
          relPath = relPath.substring(0,relPath.length()-4);
        }
        path = outDir.getCanonicalPath() + File.separator + relPath;
        int sepIx = relPath.indexOf(File.separator);
        while (sepIx>-1) {
          relPathUp += ".." +File.separator;;
          sepIx = relPath.indexOf(File.separator,sepIx+1);
        }
        jarRelPath = createRelPath(new File(path).getParentFile(), filePath);
      } else {
        String fileDir = file.getParentFile().getCanonicalPath();
        if (basePath.startsWith(fileDir)) {
          // Outside baseDir, place resulting file directly in outDir
          relPath    = file.getName();
          if (relPath.endsWith(".jar")) {
            relPath = relPath.substring(0,relPath.length()-4);
          }
          path       = outDir.getCanonicalPath() + File.separator + relPath;
          relPathUp  = "";
          jarRelPath = createRelPath(new File(path).getParentFile(), filePath);
        } else {
          throw new BuildException("The file '"+filePath
                                   +"' does not reside in baseDir ("
                                   +basePath +")!");
        }
      }
    }

    /**
     * Derive relative path from <tt>fromDir</tt> to the file given by
     * <tt>filePath</tt>.
     */
    private String createRelPath(File fromDir, String filePath)
    {
      String res = "";
      while (fromDir!=null && !filePath.startsWith(fromDir.toString())) {
        if (res.length()>0) {
          res += File.separator;
        }
        res += "..";
        fromDir = fromDir.getParentFile();
      }
      if (res.length()>0) {
        res += File.separator;
      }
      res += filePath.substring(fromDir.toString().length()+1);
      return res;
    }

    public void load() throws Exception {
      JarFile    jarFile      = new JarFile(file);
      Manifest   mf           = jarFile.getManifest();
      attribs                 = mf.getMainAttributes();

      vars.put("html.file", path +".html");
      vars.put("html.uri",  replace(relPath, "\\", "/"));

      pkgExportMap     = parseNames(attribs.getValue("Export-Package"), false);
      pkgImportMap     = parseNames(attribs.getValue("Import-Package"), true);
      serviceExportMap = parseNames(attribs.getValue("Export-Service"), false);
      serviceImportMap = parseNames(attribs.getValue("Import-Service"), true);

      extractSource(jarFile, new File( path +"/src"));
    }


    // String -> String
    Map sourceMap = new TreeMap();
    boolean bSourceInside = false;

    void extractSource(JarFile jarFile, File destDir) throws IOException {

      if (!include_source_files) {
        return;
      }

      String prefix = "OSGI-OPT/src";

      int count = 0;
      for(Enumeration e = jarFile.entries(); e.hasMoreElements(); ) {
        ZipEntry entry = (ZipEntry)e.nextElement();

        if(entry.getName().startsWith(prefix)) {
          count++;
        }
      }


      if(count > 0) {
        bSourceInside = true;

        //System.out.println("found " + count + " source files in " + jarFile.getName());

        //System.out.println("creating "+ destDir.getAbsolutePath());
        destDir.mkdirs();

        for(Enumeration e = jarFile.entries(); e.hasMoreElements(); ) {
          ZipEntry entry = (ZipEntry)e.nextElement();

          if(entry.getName().startsWith(prefix)) {
            if(entry.isDirectory()) {
              makeDir(jarFile, entry, destDir, prefix);
            } else {
              copyEntry(jarFile, entry, destDir, prefix);

              String s = replace(entry.getName(), prefix + "/", "");
              sourceMap.put(s, s);
            }
            count++;
          }
        }

      } else {
        // Check if we can copy source from original pos
        String sourceDir = (String)attribs.getValue("Built-From");
        if(sourceDir != null && !"".equals(sourceDir)) {
          File src = new File(sourceDir, "src");
          copyTree(src, destDir, src.toString() + File.separator, ".java");
        }
      }
    }

    void copyTree(File src, File dest,
                  String prefix,
                  String suffix) throws IOException {
      if(src.isDirectory()) {
        if(!dest.exists()) {
          dest.mkdirs();
        }

        String[] files = src.list();
        for(int i = 0; i < files.length; i++) {
          copyTree(new File(src, files[i]),
                   new File(dest, files[i]),
                   prefix,
                   suffix);
        }
      } else if(src.isFile()) {
        if(src.getName().endsWith(suffix)) {
          String path = src.getAbsolutePath();

          String s = replace(replace(path, prefix, ""), "\\", "/");

          copyStream(new FileInputStream(src),  new FileOutputStream(dest));
          sourceMap.put(s, s);
        }
      }
    }


    void makeDir(ZipFile file,
                 ZipEntry entry,
                 File destDir,
                 String prefix) throws IOException {
      File d = new File(destDir, replace(entry.getName(), prefix, ""));

      d.mkdirs();
      //System.out.println("created dir  " + d.getAbsolutePath());
    }


    void copyEntry(ZipFile file,
                   ZipEntry entry,
                   File destDir,
                   String prefix) throws IOException {
      File d = new File(destDir, replace(entry.getName(), prefix, ""));

      File dir = d.getParentFile();

      if(!dir.exists()) {
        dir.mkdirs();
      }

      //System.out.println("extracting to " + d.getAbsolutePath());

      copyStream(new BufferedInputStream(file.getInputStream(entry)),
                 new BufferedOutputStream(new FileOutputStream(d)));
    }


    void copyStream(InputStream is, OutputStream os) throws IOException {
      byte[] buf = new byte[1024];


      BufferedInputStream in   = null;
      BufferedOutputStream out = null;

      try {
        in  = new BufferedInputStream(is);
        out = new BufferedOutputStream(os);
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0) {
          out.write(buf, 0, n);
          total += n;
        }
      } finally {
        try { in.close(); } catch (Exception ignored) { }
        try { out.close(); } catch (Exception ignored) { }
      }
    }



    Map parseNames(String s, boolean range) {

      Map map = new TreeMap();

      //System.out.println(file + ": " + s);
      if(s != null) {
        s = s.trim();
        String[] lines = Util.splitwords(s, ",", '\"');
        for(int i = 0; i < lines.length; i++) {
          String[] words = Util.splitwords(lines[i].trim(), ";", '\"');
          if(words.length < 1) {
            throw new RuntimeException("bad package spec '" + s + "'");
          }
          String spec = "0";
          String name = words[0].trim();

          for(int j = 1; j < words.length; j++) {
            String[] info = Util.splitwords(words[j], "=", '\"');

            if(info.length == 2) {
              if("specification-version".equals(info[0].trim())) {
                spec = info[1].trim();
              } else if("version".equals(info[0].trim())) {
                spec = info[1].trim();
              }
            }
          }
          if (range) {
            map.put(name, new VersionRange(spec));
          } else {
            map.put(name, new Version(spec));
          }
        }
      }

      return map;
    }

    public void writeInfo() throws IOException {

      String template = stdReplace(Util.load(getBundleInfoTemplate().getAbsolutePath()));
      String outName  = (String)vars.get("html.file");

      //System.out.println("jar info from " + file + " to " +outName);

      Util.writeStringToFile(new File(outName), template);
    }

    public String stdReplace(String template) throws IOException {
      return stdReplace(template, true);
    }


    public String stdReplace(String template, boolean bDepend) throws IOException {

      for(Iterator it = globalVars.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();
        Object val = globalVars.get(key);

        template = replace(template, "${" + key + "}", "" + val);

        //System.out.println(key + "->" + val);
      }

      for(Iterator it = vars.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();
        Object val = vars.get(key);

        template = replace(template, "${" + key + "}", "" + val);

        //System.out.println(key + "->" + val);
      }

      Set handledSet = new TreeSet();

      for(Iterator it = attribs.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();

        if(-1 != template.indexOf("${" + key.toString() + "}")) {
          handledSet.add(key.toString());
        }
      }

      for(Iterator it = attribs.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();
        Object val = attribs.get(key);


        String str = (String)attribs.get(key);

        if("Export-Package".equals(key.toString()) ||
           "Import-Package".equals(key.toString()) ||
           "Import-Service".equals(key.toString()) ||
           "Export-Service".equals(key.toString())) {
        } else {
          if(listPropSet.contains(key.toString())) {
            str = replace(str, ",", listSeparator);
          }

          template = replace(template, "${" + key + "}", str);
        }
      }


      template = replace(template,
                         "${Export-Package}",
                         getPackagesString(pkgExportMap,
                                           "/package-summary.html"));

      template = replace(template,
                         "${Import-Package}",
                         getPackagesString(pkgImportMap,
                                           "/package-summary.html"));


      template = replace(template,
                         "${Export-Service}",
                         getPackagesString(serviceExportMap,
                                           ".html"));

      template = replace(template,
                         "${Import-Service}",
                         getPackagesString(serviceImportMap,
                                           ".html"));


      for(Iterator it = alwaysPropSet.iterator(); it.hasNext(); ) {
        String key = (String)it.next();
        String val = "";

        template = replace(template, "${" + key + "}", val);
      }

      StringBuffer sb = new StringBuffer();

      for(Iterator it = attribs.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();

        if(!handledSet.contains(key.toString())) {
          if(!skipAttribSet.contains(key.toString())) {
            sb.append("<tr>\n" +
                      " <td>" + key + "</td>\n" +
                      " <td>" + attribs.getValue(key.toString()) + "</td>\n" +
                      "</tr>\n");
          }
        }
      }

      template = replace(template,  "${MF.UNHANDLED}",  sb.toString());

      template = replace(template,  "${FILE}",  file.getName());
      template = replace(template,  "${FILE.short}",  replace(file.getName(), ".jar", ""));
      template = replace(template,  "${BYTES}", "" + file.length());
      template = replace(template,  "${KBYTES}", "" + (file.length() / 1024));

      template = replace(template,  "${relpathup}",  relPathUp);
      template = replace(template,  "${jarRelPath}", jarRelPath);
      template = replace(template,  "${javadocdir}",
                         javadocRelPath != null ? javadocRelPath : "");


      if(bDepend) {
        unresolvedMap = new TreeMap();

        template = replace(template,
                           "${depending.list}",
                           getDepend(
                                     new MapSelector() {
                                       public Map getMap(BundleInfo info) {
                                         return info.pkgExportMap;
                                       }
                                     },
                                     new MapSelector() {
                                       public Map getMap(BundleInfo info) {
                                         return info.pkgImportMap;
                                       }
                                     },
                                     null,
                                     false));

        template = replace(template,
                           "${depends.list}",
                           getDepend(
                                     new MapSelector() {
                                       public Map getMap(BundleInfo info) {
                                         return info.pkgImportMap;
                                       }
                                     },
                                     new MapSelector() {
                                       public Map getMap(BundleInfo info) {
                                         return info.pkgExportMap;
                                       }
                                     },
                                     unresolvedMap,
                                     true));

      }

      sb = new StringBuffer();
      if(sourceMap.size() > 0) {

        String srcBase = replace(file.getName(), ".jar", "") + "/src";

        sb.append("<table>");
        for(Iterator it = sourceMap.keySet().iterator(); it.hasNext();) {
          String name = (String)it.next();


          sb.append(" <tr>\n");
          sb.append("  <td>\n");
          sb.append("  <a href=\"" + srcBase + "/" + name + "\">" + name + "<a>\n");
          sb.append(" </tr>\n");
          sb.append(" </tr>\n");

        }
        sb.append("</table>");

      } else{
        sb.append("None found");
      }

      template = replace(template, "${sources.list}", sb.toString());
      if(bSourceInside && sourceMap.size() > 0) {
        template = replace(template,
                           "${FILEINFO}",
                           file.length() + " bytes, includes <a href=\"#source\">source</a>");
      } else {
        template = replace(template,
                           "${FILEINFO}",
                           file.length() + " bytes");
      }


      return template;
    }


    String getDepend(MapSelector importMap,
                     MapSelector exportMap,
                     Map         unresolvedMapDest,
                     boolean bShowUnresolved) throws IOException {

      Map map        = new TreeMap();

      if(unresolvedMapDest == null) {
        unresolvedMapDest = new TreeMap();
      }

      for(Iterator it = importMap.getMap(this).keySet().iterator();
          it.hasNext(); ) {
        String name    = (String)it.next();
        Object version = importMap.getMap(this).get(name);

        boolean bFound = false;
        for(Iterator it2 = jarMap.keySet().iterator(); it2.hasNext();) {
          File jarFile = (File)it2.next();
          BundleInfo info = (BundleInfo)jarMap.get(jarFile);

          for(Iterator it3 = exportMap.getMap(info).keySet().iterator(); it3.hasNext(); ) {
            String name2    = (String) it3.next();
            Object version2 = exportMap.getMap(info).get(name);

            if(name.equals(name2)) {
              VersionRange versions = (version instanceof VersionRange)
                ? (VersionRange) version : (VersionRange) version2;
              Version ver = (version instanceof Version)
                ? (Version) version : (Version) version2;

              if(versions.contains(ver)) {
                bFound = true;
                map.put(jarFile, name);
              } else {
                System.out.println(file +": need " +name +" version "
                                   +version +", found version " +version2);
              }
            }
          }
        }

        if(!bFound && !isSystemPackage(name)) {
          unresolvedMapDest.put(name, version);
        }

      }
      StringBuffer sb = new StringBuffer();

      if(map.size() == 0 && unresolvedMapDest.size() == 0) {
        sb.append("None found");
      } else {
        for(Iterator it = map.keySet().iterator(); it.hasNext();) {
          Object     key     = it.next();
          File       jarFile = (File)key;
          BundleInfo info    = (BundleInfo)jarMap.get(jarFile);
          String     what    = (String)map.get(jarFile);

          String row = info.stdReplace(bundleRow, false);

          row = replace(row, "${what}", what);
          row = replace(row, "${bundledoc}", relPathUp +info.relPath +".html");
          sb.append(row);
        }


        if(bShowUnresolved) {
          if(unresolvedMapDest.size() > 0) {
            String row = missingRow;

            row = replace(row, "${name}",    "<b>Unresolved</b>");
            row = replace(row, "${version}", "");

            sb.append(row);
          }
          for(Iterator it = unresolvedMapDest.keySet().iterator(); it.hasNext();) {
            String name    = (String) it.next();
            Object version = unresolvedMapDest.get(name);

            String row = missingRow;

            row = replace(row, "${name}",    name);
            row = replace(row, "${version}", version.toString());

            sb.append(row);

          }
        }

      }
      return sb.toString();
    }


    String getPackageString(String pkg, Object version, String linkSuffix)
    {
      String row = pkgHTML;

      String docFile = replace(pkg, ".", "/") + linkSuffix;
      String docPath = relPathUp + javadocRelPath + "/index.html?" + docFile;

      File f = new File(outDir + File.separator
                        + javadocRelPath + File.separator + docFile);

      if(javadocRelPath != null && !"".equals(javadocRelPath)) {
        if( isSystemPackage(pkg) ) {
          row = replace(row, "${namelink}", "${name}");
        } else if ( (bCheckJavaDoc && !f.exists()) ) {
          row = replace(row, "${namelink}", "${name}");
          missingDocs.put(pkg, this);
        } else {
          row = replace(row,
                        "${namelink}",
                        "<a target=\"_top\" href=\"${javadoc}\">${name}</a>");
        }
      } else {
        row = replace(row, "${namelink}", "${name}");
      }

      row = replace(row, "${name}", pkg);
      row = replace(row, "${version}", version.toString());
      row = replace(row, "${javadoc}", docPath);

      return row;
    }

    String getPackagesString(Map map, String linkSuffix) {
      StringBuffer sb = new StringBuffer();

      for(Iterator it = map.keySet().iterator(); it.hasNext(); ) {
        String name    = (String)it.next();
        Object version = map.get(name);

        sb.append(getPackageString(name, version, linkSuffix));
      }

      return sb.toString();
    }
  }


  boolean isSystemPackage(String name) {
    for(Iterator it = systemPackageSet.iterator(); it.hasNext();) {
      String prefix = (String)it.next();
      if(name.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }


  String replace(String src, String a, String b) {
    return Util.replace(src, a, b == null ? "" : b);
  }

  /**
   * Comparator that sorts file objects based on their file name.
   */
  class FileNameComparator
    implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      File f1 = (File) o1;
      File f2 = (File) o2;
      return f1.getName().compareTo(f2.getName());
    }

    public boolean equals(Object obj)
    {
      return this==obj;
    }
  }

  /**
   * Comparator that sorts BundleInfo objects based on their file name.
   */
  class BundleInfoComparator
    implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      BundleInfo info1 = (BundleInfo) o1;
      BundleInfo info2 = (BundleInfo) o2;
      return info1.file.getName().compareTo(info2.file.getName());
    }

    public boolean equals(Object obj)
    {
      return this==obj;
    }
  }

}
