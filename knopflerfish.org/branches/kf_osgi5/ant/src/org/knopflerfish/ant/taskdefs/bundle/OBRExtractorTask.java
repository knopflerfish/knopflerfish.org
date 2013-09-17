/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * <p>
 * Task that analyzes a set of bundle jar files and builds OBR XML
 * documentation from these bundles.
 * </p>
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
 *
 *  <tr>
 *   <td valign=top>baseDir</td>
 *   <td valign=top>
 *    Base directory for scanning for jar files.
 *   </td>
 *   <td valign=top>No.<br> Default value is "."</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>baseURL</td>
 *   <td valign=top>
 *    Base URL for generated bundleupdate locations.
 *   </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>outFile</td>
 *   <td valign=top>
 *    File name of generated repository XML file
 *   </td>
 *   <td valign=top>No.<br> Default value is "repository.xml"</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>repoName</td>
 *   <td valign=top>
 *    Repository name
 *   </td>
 *   <td valign=top>No.<br> Default value is "Knopflerfish bundle repository"</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>externRepoURLs</td>
 *   <td valign=top>
 *    Comma separated list of URLs to external repositories.
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
 *  </tr>
 *
 * <h3>Parameters specified as nested elements</h3>
 * <h4>fileset</h4>
 *
 * (required)<br>
 * <p>
 * All jar files must be specified as a fileset. If there exists
 * jar files
 * <code>[prefix]_all-[suffix].jar</code> and
 * <code>[prefix]-[suffix].jar</code>,
 * <b>only</b>
 * <code>[prefix]-_all-[suffix].jar</code> will be included.
 * </p>
 *
 */
public class OBRExtractorTask
  extends Task
{

  private final List<ResourceCollection> filesets =
    new ArrayList<ResourceCollection>();

  private File   baseDir             = new File(".");
  private String baseURL             = "";
  private String outFile             = "repository.xml";
  private String repoName            = "Knopflerfish bundle repository";
  private String repoXSLURL          = "";
  private String externRepoURLs      = null;

  public OBRExtractorTask() {
  }

  public void setBaseDir(String s) {
    this.baseDir = new File((new File(s)).getAbsolutePath());
  }

  public void setBaseURL(String s) {
    this.baseURL = s;
  }

  public void setRepoXSLURL(String s) {
    this.repoXSLURL = s;
  }

  public void setOutFile(String s) {
    this.outFile = s;
  }

  public void setRepoName(String s) {
    this.repoName = s;
  }

  public void setExternRepoURLs(String s) {
    this.externRepoURLs = s;
  }

  public void addFileset(FileSet set) {
    filesets.add(set);
  }

  Map<File,BundleInfo> jarMap = new HashMap<File,BundleInfo>();

  // Implements Task
  @Override
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
    }

    jarMap = new HashMap<File,BundleInfo>();

    System.out.println("loading bundle info...");

    try {
      for (final ResourceCollection rc : filesets) {
        final FileSet          fs      = (FileSet) rc;
        final DirectoryScanner ds      = fs.getDirectoryScanner(getProject());
        final File             projDir = fs.getDir(getProject());

        final String[] srcFiles = ds.getIncludedFiles();

        for (final String srcFile : srcFiles) {
          final File file = new File(projDir, srcFile);
          if(file.getName().endsWith(".jar")) {
            jarMap.put(file, new BundleInfo(file));
          }
        }
      }

      final Set<File> removeSet = new HashSet<File>();

      for (final Object element : jarMap.keySet()) {
        final File       file = (File)element;
        final String name = file.getAbsolutePath();
        if(-1 != name.indexOf("_all-")) {
          final File f2 = new File(Util.replace(name, "_all-", "-"));
          removeSet.add(f2);
          System.out.println("skip " + f2);
        }
      }

      if(removeSet.size() > 0) {
        System.out.println("skipping " + removeSet.size() + " bundles");
      }

      for (final File f  : removeSet) {
        jarMap.remove(f);
      }

      System.out.println("analyzing " + jarMap.size() + " bundles");

      for (final Object element : jarMap.keySet()) {
        final File       file = (File)element;
        final BundleInfo info = jarMap.get(file);

        info.load();
      }

      System.out.println("writing bundle OBR to " + outFile);

      OutputStream out = null;
      try {
        out = new FileOutputStream(outFile);
        final PrintStream ps = new PrintStream(out);

        ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        if(repoXSLURL != null && !"".equals(repoXSLURL)) {
          ps.println("<?xml-stylesheet type=\"text/xsl\" href=\"" +
                     repoXSLURL + "\"?>");
        }
        ps.println("");
        ps.println("<!-- Generated by " + getClass().getName() +
                   " -->");
        ps.println("");
        ps.println("<bundles>");
        ps.println(" <dtd-version>1.0</dtd-version>");
        ps.println(" <repository>");
        ps.println("  <name>" +repoName +"</name>");
        ps.println("  <date>" + (new Date()) + "</date>");
        if (externRepoURLs!=null && 0<externRepoURLs.length()) {
          final StringTokenizer st = new StringTokenizer(externRepoURLs,",");
          ps.println("  <extern-repositories>");
          while (st.hasMoreTokens()) {
            final String repoURL = st.nextToken().trim();
            ps.println("   <url>" +repoURL +"</url>");
          }
          ps.println("  </extern-repositories>");
        }
        ps.println(" </repository>");

        for (final Object element : jarMap.keySet()) {
          final File       file = (File)element;
          final BundleInfo info = jarMap.get(file);

          info.writeBundleXML(ps);
        }
        ps.println("</bundles>");
        ps.close();
      } catch (final Exception e) {
        e.printStackTrace();
      } finally {
        try { out.close(); } catch (final Exception ignored) { }
      }

    } catch (final Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to extract bundle info: " + e, e);
    }
  }

  class BundleInfo {
    File       file;
    Attributes attribs;
    String path;
    Map<String, VersionRange> pkgExportMap;
    Map<String, VersionRange> pkgImportMap;
    Set<String> optionalPkgs = new HashSet<String>();

    public BundleInfo(File file) throws IOException  {
      this.file = file;
      path = Util.replace(file.getCanonicalPath().substring(1 + baseDir.getCanonicalPath().length()), "\\", "/");
    }

    public void load() throws Exception {
      final JarFile    jarFile      = new JarFile(file);
      final Manifest   mf           = jarFile.getManifest();
      attribs                 = mf.getMainAttributes();

      pkgExportMap     = parseNames(attribs.getValue("Export-Package"));
      pkgImportMap     = parseNames(attribs.getValue("Import-Package"));

    }


    Map<String,VersionRange> parseNames(String s) {

      final Map<String, VersionRange> map = new TreeMap<String, VersionRange>();

      //      System.out.println(file + ": " + s);
      if(s != null) {
        s = s.trim();
        final String[] lines = Util.splitwords(s, ",", '\"');
        for (final String line : lines) {
          final String[] words = Util.splitwords(line.trim(), ";", '\"');
          if(words.length < 1) {
            throw new RuntimeException("bad package spec '" + s + "'");
          }
          String spec = "0";
          final String name = words[0].trim();

          for(int j = 1; j < words.length; j++) {
            final String[] info = Util.splitwords(words[j], "=", '\"');

            if(info.length == 2) {
              String lhs = info[0].trim();
              final boolean isDirective = lhs.charAt(lhs.length()-1) == ':';
              if("specification-version".equals(lhs)
                 || "version".equals(lhs)) {
                spec = info[1].trim();
              } else if (isDirective) {
                lhs = lhs.substring(0, lhs.length()-1).trim();
                if ("resolution".equals(lhs)
                    && "optional".equals(info[1].trim())) {
                  optionalPkgs.add(name);
                }
              }
            }
          }

          map.put(name, new VersionRange(spec));
        }
      }

      return map;
    }

    public void writeBundleXML(PrintStream out) throws IOException {

      out.println("");
      out.println(" <!-- " + path + " -->");
      out.println(" <bundle>");

      printAttrib(out, "bundle-name",        "Bundle-Name");
      printAttrib(out, "bundle-version",     "Bundle-Version");
      printAttrib(out, "bundle-docurl",      "Bundle-DocURL");
      printAttrib(out, "bundle-category",    "Bundle-Category");
      printAttrib(out, "bundle-vendor",      "Bundle-Vendor");
      printAttrib(out, "bundle-description",    "Bundle-Description");
      printAttrib(out, "bundle-subversionurl",  "Bundle-SubversionURL");
      printAttrib(out, "bundle-apivendor",      "Bundle-APIVendor");
      printAttrib(out, "bundle-uuid",           "Bundle-UUID");
      printAttrib(out, "application-icon",      "Application-Icon");


      out.print("  <bundle-updatelocation>");
      out.print(baseURL + path);
      out.println("</bundle-updatelocation>");

      dumpPackages(out, "export-package", pkgExportMap);
      dumpPackages(out, "import-package", pkgImportMap);

      out.print("  <bundle-filesize>");
      out.print(Long.toString(file.length()));
      out.println("</bundle-filesize>");


      out.println(" </bundle>");

    }

    void dumpPackages(PrintStream out, String tag, Map<String, VersionRange> map) {
      for (final Entry<String,VersionRange> entry : map.entrySet()) {
        final String pkg  = entry.getKey();
        final VersionRange spec = entry.getValue();
        boolean bSkip = false;
        String  skipReason = null;
        if("import-package".equals(tag)) {
          final VersionRange exportSpec = pkgExportMap.get(pkg);
          if(exportSpec != null && spec.contains(exportSpec.lowerBound)) {
            // Skip import of exported packages
            bSkip = true;
            skipReason = "own import since exported with same version ";
          }
          if(optionalPkgs.contains(pkg)) {
            // Skip import of package with optional resolution
            bSkip = true;
            skipReason = "pkg with optional resolution ";
          }
        }
        if(bSkip) {
          out.println("  <!-- skip " +skipReason + pkg + "; " + spec + " -->");
        } else {
          out.print("  <" + tag + " package=\"" + pkg + "\"");
          if("".equals(spec)) {
            out.println("/>");
          } else {
            out.println("\n                  specification-version=\"" + spec + "\"/>");
          }
        }
      }
    }

    void printAttrib(PrintStream out, String tag, String key) {
      String val = attribs.getValue(key);
      if(val == null) {
        val = "";
      }
      out.println("  <" + tag + ">" +  val + "</" + tag + ">");
    }
  }
}
