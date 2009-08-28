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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
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

/**
 * <p>
 * Task that analyzes a set of bundle jar files and builds HTML documentation
 * from these bundles. Also creates cross-references to bundle dependecies.
 * </p>
 *
 * <p>
 * All generated HTML will be stored in the same directory stucture as
 * the scanned jars, e.g a jar file
 * <pre>
 *  jars/log/log-api.jar
 * </pre>
 * will have a corresponding
 * <pre>
 *  jars/log/log-api.html
 * </pre>
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
 *   <td valign=top>Relative path (from baseDir) to javadocs.
 *   </td>
 *   <td valign=top>No.<br> Default value is "."</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>baseDir</td>
 *   <td valign=top>
 *    Base directory for scanning for jar files.
 *   </td>
 *   <td valign=top>No.<br> Default value is "."</td>
 *  </tr>
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
 *    Comma-spearated set of packages which are system packages and
 *    thus globally available.
 *    These are not cross-referenced.
 *   </td>
 *   <td valign=top>No.<br>
 * Default value is "javax.swing,javax.accessibility,javax.servlet,javax.xml,org.xml,org.w3c,java,com.sun"
 </td>
 *
 *  <tr>
 *   <td valign=top>skipAttribSet</td>
 *   <td valign=top>
 *    Comma-spearated set of manifest attributes which shouldn't be printed.
 *   </td>
 *   <td valign=top>No.<br>
 * Default value is "Manifest-Version,Ant-Version,Bundle-Config,Created-By,Built-From"
 </td>
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
 *                baseDir            = "${release.dir}/jars"
 *                javadocRelPath     = "../javadoc"
 *   &gt;
 *
 *     &lt;fileset dir="${release.dir}/jars"&gt;
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
  private File   baseDir             = new File(".");
  private String javadocRelPath      = null;


  private String indexListRow =
    "<a target=\"bundle_main\" href=\"${bundledoc}\">${FILE.short}</a><br>";

  private String indexMainRow =
    "<tr>" +
    "<td><a target=\"bundle_main\" href=\"${bundledoc}\">${FILE.short}</a></td><td>" +
    "<td>${Bundle-Description}</td>" +
    "</tr>\n";

  private String bundleRow    =
    "<tr><td><a href=\"${bundledoc}\">${FILE.short}</a></td><td>${what}</td></tr>\n";

  private String missingRow   =
    "<tr><td>${name}</td><td>${version}</td></tr>\n";

  private String rowHTML      =
    "<a href=\"${bundle.uri}\">${FILE.short}</a><br>\n";

  private String pkgHTML      =
    "${namelink} ${version}<br>";

  private boolean bCheckJavaDoc  = true;

  Map     jarMap     = new TreeMap();
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


  File getBundleInfoTemplate() {
    return new File(templateHTMLDir, "bundle_info.html");
  }

  File getBundleCSSTemplate() {
    return new File(templateHTMLDir, "style.css");
  }

  File getBundleListTemplate() {
    return new File(templateHTMLDir, "bundle_list.html");
  }

  File getBundleMainTemplate() {
    return new File(templateHTMLDir, "bundle_main.html");
  }

  File getBundleIndexTemplate() {
    return new File(templateHTMLDir, "bundle_index.html");
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

      System.out.println("analyzing " + jarMap.size() + " bundles");

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
                   new File(baseDir, "main.html"),
                   indexMainRow);

      makeListPage(getBundleListTemplate(),
                   new File(baseDir, "list.html"),
                   indexListRow);

      copyFile(getBundleIndexTemplate(),
               new File(baseDir, "index.html"));

      copyFile(getBundleCSSTemplate(),
               new File(baseDir, "style.css"));


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

    StringBuffer sb = new StringBuffer();
    for(Iterator it = jarMap.keySet().iterator(); it.hasNext();) {
      File       file = (File)it.next();
      BundleInfo info = (BundleInfo)jarMap.get(file);

      unresolvedCount += info.unresolvedMap.size();

      String row = rowTemplate;

      row = info.stdReplace(row, false);

      row = replace(row,
                    "${bundledoc}",
                    replace(info.path, ".jar", ".html"));

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
              String   pkgName = (String)it2.next();
              ArrayInt version = (ArrayInt)info.unresolvedMap.get(pkgName);

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

    // String (package name) -> ArrayInt (version)
    Map        pkgImportMap = new TreeMap();
    Map        pkgExportMap = new TreeMap();
    Map        serviceExportMap = new TreeMap();
    Map        serviceImportMap = new TreeMap();

    String     relPath = "";
    String     path    = "";

    Map unresolvedMap = new TreeMap();

    public BundleInfo(File file) throws IOException  {
      this.file = file;
      relPath = "";
      File dir = file.getParentFile();

      while(dir != null && !dir.equals(baseDir)) {
        //      System.out.println("** ! " + dir + " || " + baseDir);
        relPath += "../";
        dir = dir.getParentFile();

      }

      if(dir == null) {
        throw new BuildException(baseDir.getAbsolutePath() + " is not parent of " + file.getAbsolutePath());
      }

      if(relPath.equals("")) {
        //      relPath = ".";
      }


      path = replace(file.getCanonicalPath().substring(1 + baseDir.getCanonicalPath().length()), "\\", "/");

      //      System.out.println(file + ", " + relPath + ", " + baseDir.getAbsolutePath() + ", path=" + path);
    }

    public void load() throws Exception {
      JarFile    jarFile      = new JarFile(file);
      Manifest   mf           = jarFile.getManifest();
      attribs                 = mf.getMainAttributes();

      vars.put("html.file", replace(file.toString(), ".jar", ".html"));

      String absBase = baseDir.getCanonicalPath();

      String  htmlFilename = (String)vars.get("html.file");

      File htmlFile = new File(htmlFilename);

      String absFile = htmlFile.getCanonicalPath();

      if(!absFile.startsWith(absBase)) {
        System.out.println("*** base dir is not parent of html file");
        System.out.println("base dir:  " + absBase);
        System.out.println("html file: " + absFile);
      } else {
        String relPath = absFile.substring(absBase.length() + 1);

        //        System.out.println("absFile=" + absFile);
        //        System.out.println("relPath=" + relPath);

        vars.put("html.uri", replace(relPath, "\\", "/"));
      }

      pkgExportMap     = parseNames(attribs.getValue("Export-Package"));
      pkgImportMap     = parseNames(attribs.getValue("Import-Package"));
      serviceExportMap = parseNames(attribs.getValue("Export-Service"));
      serviceImportMap = parseNames(attribs.getValue("Import-Service"));

      if(true) {
        extractSource(jarFile, new File(replace(file.getAbsolutePath(), ".jar", "") + "/src"));
      }
    }


    // String -> String
    Map sourceMap = new TreeMap();
    boolean bSourceInside = false;

    void extractSource(JarFile jarFile, File destDir) throws IOException {

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

        //      System.out.println("found " + count + " source files in " + jarFile.getName());

        //      System.out.println("creating "+ destDir.getAbsolutePath());
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
      //      System.out.println("created dir  " + d.getAbsolutePath());
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

      //      System.out.println("extracting to " + d.getAbsolutePath());

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



    Map parseNames(String s) {

      Map map = new TreeMap();

      //      System.out.println(file + ": " + s);
      if(s != null) {
        s = s.trim();
        String[] lines = Util.splitwords(s, ",", '\"');
        for(int i = 0; i < lines.length; i++) {
          String[] words = Util.splitwords(lines[i].trim(), ";", '\"');
          if(words.length < 1) {
            throw new RuntimeException("bad package spec '" + s + "'");
          }
          String spec = ArrayInt.UNDEF;
          String name = words[0].trim();

          for(int j = 1; j < words.length; j++) {
            String[] info = Util.splitwords(words[j], "=", '\"');

            if(info.length == 2) {
              if("specification-version".equals(info[0].trim())) {
                spec = info[1].trim();
              }
            }
          }

          //      System.out.println(" " + i + ": " + name + ", version=" + spec);
          ArrayInt version = new ArrayInt(spec);

          map.put(name, version);
        }
      }

      return map;
    }

    public void writeInfo() throws IOException {

      //    System.out.println("jar info from " + file);

      String template = stdReplace(Util.load(getBundleInfoTemplate().getAbsolutePath()));
      String outName  = (String)vars.get("html.file");

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

        //      System.out.println(key + "->" + val);
      }

      for(Iterator it = vars.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();
        Object val = vars.get(key);

        template = replace(template, "${" + key + "}", "" + val);

        //      System.out.println(key + "->" + val);
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
                         getPackageString(pkgExportMap,
                                          "/package-summary.html"));

      template = replace(template,
                         "${Import-Package}",
                         getPackageString(pkgImportMap,
                                          "/package-summary.html"));


      template = replace(template,
                         "${Export-Service}",
                         getPackageString(serviceExportMap,
                                          ".html"));

      template = replace(template,
                         "${Import-Service}",
                         getPackageString(serviceImportMap,
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

      template = replace(template,  "${relpath}",     relPath);
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

      for(Iterator it = importMap.getMap(this).keySet().iterator(); it.hasNext(); ) {
        String   name    = (String)it.next();
        ArrayInt version = (ArrayInt)importMap.getMap(this).get(name);

        boolean bFound = false;
        for(Iterator it2 = jarMap.keySet().iterator(); it2.hasNext();) {
          File jarFile = (File)it2.next();
          BundleInfo info = (BundleInfo)jarMap.get(jarFile);

          for(Iterator it3 = exportMap.getMap(info).keySet().iterator(); it3.hasNext(); ) {
            String   name2    = (String)it3.next();
            ArrayInt version2 = (ArrayInt)exportMap.getMap(info).get(name);

            if(name.equals(name2)) {
              if(version2.compareTo(version) >= 0) {
                bFound = true;
                map.put(jarFile, name);
              } else {
                System.out.println(file + ": need " + name + " version " + version + ", found version " + version2);
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
          row = replace(row,
                        "${bundledoc}",
                        replace(relPath + info.path, ".jar", ".html"));
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
            String   name    = (String)it.next();
            ArrayInt version = (ArrayInt)unresolvedMapDest.get(name);

            String row = missingRow;

            row = replace(row, "${name}",    name);
            row = replace(row, "${version}", version.toString());

            sb.append(row);

          }
        }

      }
      return sb.toString();
    }



    String getPackageString(Map map, String linkSuffix) {
      StringBuffer sb = new StringBuffer();

      for(Iterator it = map.keySet().iterator(); it.hasNext(); ) {
        String   name    = (String)it.next();
        ArrayInt version = (ArrayInt)map.get(name);

        String html = pkgHTML;

        String docFile = replace(name, ".", "/") + linkSuffix;
        String docPath = relPath + javadocRelPath + "/" + docFile;

        File f = new File(file.getParentFile(), docPath);


        if(javadocRelPath != null && !"".equals(javadocRelPath)) {
          if( isSystemPackage(name) ) {
            html = replace(html, "${namelink}", "${name}");
          } else if ( (bCheckJavaDoc && !f.exists()) ) {
            html = replace(html, "${namelink}", "${name}");
            missingDocs.put(name, this);
          } else {
            html = replace(html,
                         "${namelink}", "<a href=\"${javadoc}\">${name}</a>");
          }
        } else {
          html = replace(html, "${namelink}", "${name}");
        }

        String row     = replace(html, "${name}", name);

        row = replace(row, "${version}", version.toString());
        row = replace(row, "${javadoc}", docPath);

        sb.append(row);
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


}
