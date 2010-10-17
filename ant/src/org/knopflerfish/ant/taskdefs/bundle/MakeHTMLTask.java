/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map.Entry;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

import org.osgi.framework.Version;

/**
 * <p>
 *  This task is used when building distributions of Knopflerfish.
 *  If you don't intend to create a new distribution type of
 *  Knopflerfish then you're in the wrong place.
 * </p>
 *
 * <p>
 *  Task that creates web sites given a template and a source file.
 *  Currently used to create parts of the docs directory in the KF
 *  dist.  It does this by simply replacing certain text strings with
 *  others. For more information on which text strings this is please
 *  check the source code.
 * </p>
 *
 * <p>
 *  Here is a outline of how to use the task and a description
 *  of different parameters and used system properties.
 * </p>
 *
 * <p>
 *
 * <table border=1>
 *  <tr>
 *   <td valign=top><b>Attribute</b></td>
 *   <td valign=top><b>Description</b></td>
 *   <td valign=top><b>Required</b></td>
 *  </tr>
 *  <tr>
 *   <td>
 *    outdir
 *   </td>
 *   <td>
 *    What dir to put the actual the generated file
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *   <td>
 *    tofile
 *   </td>
 *   <td>
 *    The releative path to where the generated file should be
 *    copied. That is the actual location of the generated file
 *    will be <code>outdir</code>/<code>tofile</code>
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    template
 *   </td>
 *   <td>
 *    The file which describes what the page should look like
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    title
 *   </td>
 *   <td>
 *    The page's title
 *   </td>
 *   <td>
 *    No, default is ""
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    description
 *   </td>
 *   <td>
 *    The page's description
 *   </td>
 *   <td>
 *    No, default is ""
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    disable
 *   </td>
 *   <td>
 *    Allows you to disable certain links. This attribute is very ad hoc.
 *    It will use the properties <code>htdocs.link.disabled.class</code>
 *    and <code>htdocs.link.enabled.class</code>. The task will then use the
 *    values of these properties to generate the file.
 *   </td>
 *   <td>
 *    No
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    javadocRelPath
 *   </td>
 *   <td>
 *    Relative path (from outdir) to javadocs.
 *   </td>
 *   <td>
 *    ../../javadoc
 *   </td>
 *  </tr>
 * </table>
 * <p>
 *  <b>Note:</b> instead of using the attributes <code>fromfile</code> and
 *  <code>tofile</code> one can use filesets. It will simply run through
 *  and perform the task on all given files.
 * </p>
 *
 */

public class MakeHTMLTask
  extends Task
{
  private final static String TIMESTAMP
    = new SimpleDateFormat("EE MMMM d yyyy, HH:mm:ss", Locale.ENGLISH)
    .format(new Date());
  private final static String YEAR
    = new SimpleDateFormat("yyyy",  Locale.ENGLISH).format(new Date());

  private String lib_suffix = "";
  private String api_suffix = "_api";
  private String impl_suffix = "";
  private String all_suffix = "_all";
  private String projectName = "";
  private boolean do_manpage = false;
  private String javadocRelPath = "../../javadoc";

  /**
   * The source file
   */
  private String fromFile;

  /**
   * Target directory, where everything will end up
   */
  private File outdir;

  /**
   * The relative path to the target file from output dir
   */
  private File toFile;

  /**
   * File's title
   */
  private String title;

  /**
   * Description
   */
  private String description;

  /**
   * Template file
   */
  private File template;

  /**
   * Bundle list
   */
  private String bundleList;

  private ArrayList filesets = new ArrayList();
  private String disable;

  public void setFromfile(String s) {
    fromFile = s;
  }

  public void setTofile(String s) {
    toFile = new File(s);
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setOutdir(String s) {
    outdir = new File(s);
  }

  public void setTemplate(String template) {
    this.template = new File(template);
  }

  public void setBundleList(String bundleList) {
    this.bundleList = bundleList;
  }

  public void setManstyle(String manstyle) {
    this.do_manpage = true;
  }


  public void setDisable(String disabled) {
    this.disable = disabled;
  }

  public void setJavadocRelPath(String s) {
    this.javadocRelPath = s;
  }

  public void addFileset(FileSet fs) {
    filesets.add(fs);
  }

  public void execute() {
    Project proj = getProject();
    this.projectName = proj.getName();

    if (template == null) throw new BuildException("template must be set");
    if (title == null) title = "";
    if (description == null) description = "";

    if (filesets.isEmpty() && fromFile == null && toFile == null) {
      throw new BuildException("Need to specify tofile and fromfile or give a fileset");
    }

    // Set suffixes
    impl_suffix = proj.getProperty("impl.suffix");
    api_suffix = proj.getProperty("api.suffix");
    lib_suffix = proj.getProperty("lib.suffix");
    all_suffix = proj.getProperty("all.suffix");

    if (filesets.isEmpty()) {
      // log("Project base is: " + getProject().getBaseDir());
      // log("Attempting to transform: " + fromFile);
      if (!FileUtils.isAbsolutePath(fromFile)) {
        fromFile = getProject().getBaseDir() + File.separator + fromFile;
      }
      transform(fromFile, toFile.toString());
    } else {
      if (fromFile != null)
        throw new BuildException("Can not specify fromfile when using filesets");
      if (toFile != null)
        throw new BuildException("Can not specify tofile when using filesets");

      for (Iterator iter = filesets.iterator(); iter.hasNext(); ) {
        FileSet fs = (FileSet) iter.next();
        DirectoryScanner ds = fs.getDirectoryScanner(getProject());
        String[] files = ds.getIncludedFiles();

        for (int i = 0; i < files.length; i++) {
          transform(new File(fs.getDir(getProject()),
                             files[i]).getAbsolutePath(), files[i]);
        }
      }
    }
  }

  private void transform(String fromFile, String toFile) {
    if (fromFile == null) throw new BuildException("fromfile must be set");
    if (toFile == null) throw new BuildException("tofile must be set");

    try {
      Project proj = getProject();
      File tmp = new File(outdir, toFile).getParentFile();

      if (!tmp.exists()) {
        if (tmp.exists() || !tmp.mkdirs()) {
          throw new IOException("Could not create " + tmp);
        }
      }

      tmp = new File(toFile);
      String pathToRoot = ".";

      while ((tmp = tmp.getParentFile()) != null) {
        pathToRoot = pathToRoot + "/..";
      }

      // Compute the relative path from directory holding the toFile
      // to the javadoc direcotry.
      String pathToOutDir = "";
      tmp = new File(outdir, toFile).getParentFile();
      while ((tmp = tmp.getParentFile()) != null && tmp.equals(outdir)) {
        pathToOutDir = pathToOutDir + "../";
      }
      final String pathToJavadocDir = pathToOutDir + javadocRelPath;

      String content = Util.loadFile(template.toString());
      content = Util.replace(content, "$(LINKS)", links());
      content = Util.replace(content, "$(MAIN)", Util.loadFile(fromFile));
      content = Util.replace(content, "$(TITLE)", title);
      content = Util.replace(content, "$(DESC)", description);
      content = Util.replace(content, "$(TSTAMP)", TIMESTAMP);
      content = Util.replace(content, "$(YEAR)", YEAR);
      content = Util.replace(content, "$(USER)",
                             System.getProperty("user.name"));
      content = Util.replace(content, "$(VERSION)",
                             proj.getProperty("version"));
      content = Util.replace(content, "$(BASE_VERSION)",
                             proj.getProperty("base_version"));
      content = Util.replace(content, "$(DISTNAME)",
                             proj.getProperty("distname"));
      content = Util.replace(content, "$(DISTRIB_NAME)",
                             proj.getProperty("distrib.name"));
      content = Util.replace(content, "$(RELEASE_NAME)",
                             proj.getProperty("release.name"));
      content = Util.replace(content, "$(MESSAGE)",
                             proj.getProperty("release"));
      content = Util.replace(content, "$(BUNDLE_LIST)", bundleList);
      content = Util.replace(content, "$(ROOT)", pathToRoot);
      content = Util.replace(content, "$(JAVADOC)",
                             proj.getProperty("JAVADOC"));
      content = Util.replace(content, "$(CLASS_NAVIGATION)",
                             proj.getProperty("css_navigation_enabled"));
      content = Util.replace(content, "$(SVN_REPO_URL)",
                             proj.getProperty("svn.repo.url"));
      content = Util.replace(content, "$(SVN_TAG)",proj.getProperty("svn.tag"));
      content = Util.replace(content, "$(JAVADOCPATH)", pathToJavadocDir);
      content = Util.replace(content, "$(JAVADOCLINK)", pathToJavadocDir + "/index.html?");

      // Used for bundle_doc generation
      if (do_manpage) {
        content = Util.replace(content, "$(BUNDLE_NAME)", this.projectName);
        content = Util.replace(content, "$(BUNDLE_VERSION)",
                               proj.getProperty("bmfa.Bundle-Version"));

        // Create links to generated jardoc for this bundle
        StringBuffer sbuf = new StringBuffer();
        generateJardocPath(sbuf, "do_build_lib", lib_suffix);
        generateJardocPath(sbuf, "do_build_api", api_suffix);
        generateJardocPath(sbuf, "do_build_all", all_suffix);
        generateJardocPath(sbuf, "do_build_impl", impl_suffix);
        content = Util.replace(content, "$(BUNDLE_JARDOCS)", sbuf.toString());

        final String epkgs
          = formatPkgsWithJavadocLinks(getExports(), pathToJavadocDir);
        content = Util.replace(content, "$(BUNDLE_EXPORT_PACKAGE)", epkgs);

        // Replce H1-H3 headers to man page style, if manpage style
        content = Util.replace(content, "<h1", "<h1 class=\"man\"");
        content = Util.replace(content, "<H1", "<h1 class=\"man\"");
        content = Util.replace(content, "<h2", "<h2 class=\"man\"");
        content = Util.replace(content, "<H2", "<h2 class=\"man\"");
        content = Util.replace(content, "<h3", "<h3 class=\"man\"");
        content = Util.replace(content, "<H3", "<h3 class=\"man\"");
      }

      String s = proj.getProperty("navigation_pages");
      String navEnabled = proj.getProperty("css_navigation_enabled");
      String navDisabled = proj.getProperty("css_navigation_disabled");
      // System.out.println("Navigation pages: " + s);
      if (s != null) {
        String[] navPages = Util.splitwords(s);
        for (int i = 0; i < navPages.length; i++) {
          // System.out.println("Checking: " + navPages[i]);
          if (disable != null && disable.equals(navPages[i])) {
            // System.out.println("Disabling: " + "$(CLASS_NAVIGATION_" + navPages[i] + ")");
            content = Util.replace(content, "$(CLASS_NAVIGATION_" + navPages[i] + ")", navDisabled);
          }
          else {
            // System.out.println("Enabling: " + "$(CLASS_NAVIGATION_" + navPages[i] + ")");
            content = Util.replace(content, "$(CLASS_NAVIGATION_" + navPages[i] + ")", navEnabled);
          }
        }
      }

      Util.writeStringToFile(new File(outdir, toFile), content);
      log("Created: " + new File(outdir, toFile));
    } catch (IOException e) {
      e.printStackTrace();
      throw new BuildException(e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException(e);
    }
  }

  private static final String LINK_BASE = "htdocs.link.";
  private static final String LINK_ID = LINK_BASE + "id.";
  private static final String LINK_TYPE = LINK_BASE + "type.";
  private static final String LINK_NAME = LINK_BASE + "name.";
  private static final String LINK_URL = LINK_BASE + "url.";
  private static final String CSS_CLASS_ENABLED = "htdocs.link.enabled.class";
  private static final String CSS_CLASS_DISABLED = "htdocs.link.disabled.class";

  private String links() {
    Project proj = getProject();
    StringBuffer buf = new StringBuffer();

    for (int i = 0; ; i++) {
      String id   = proj.getProperty(LINK_ID + i);

      if (id == null) {
        break;
      }

      String type = proj.getProperty(LINK_TYPE + i);
      if (type == null) {
        throw new BuildException("must set htdocs.link.type." + i);
      }

      if (type.equals("separator")) {
        buf.append("<p></p>");

      } else if (type.equals("link")) {

        String name = proj.getProperty(LINK_NAME + i);
        String url  = proj.getProperty(LINK_URL + i);
        if (name == null) {
          throw new BuildException("Name not set for htdocs.link.url." + i);
        }

        String cssClass = null;

        if (disable != null && disable.equals(id)) {
          cssClass = getProject().getProperty(CSS_CLASS_DISABLED);
        } else {
          cssClass = getProject().getProperty(CSS_CLASS_ENABLED);
        }

        buf.append("<a class=\"" + cssClass + "\" href=\"" + url
                   + "\">" + name + "</a><br/>\n");
      } else {
        throw new BuildException("Do not recognize type " + type);
      }
    }

    return buf.toString();
  }

  // Returns a bool for s Project prop. If null => false
  private boolean getBoolProperty(String prop) {
    if (prop == null)
      return false;

    Boolean b = Boolean.valueOf(getProject().getProperty(prop));

    return b.booleanValue();
  }

  private void generateJardocPath(StringBuffer sbuf,
                                  String prop,
                                  String suffix)
  {
    if (getBoolProperty(prop)) {
      sbuf.append("<a target=\"_top\" href=\"../../jars/index.html?bundle=");
      sbuf.append(this.projectName);
      sbuf.append("/");
      sbuf.append(this.projectName);
      sbuf.append(suffix);
      sbuf.append(".html\">");
      sbuf.append(this.projectName);
      sbuf.append(suffix);
      sbuf.append("</a><br>\n");
    }
  }


  static String replace(String src, String a, String b) {
    return Util.replace(src, a, b == null ? "" : b);
  }


  // Get the value of the export package manifest header for this bundle.
  private String getExports()
  {
    final Project proj = getProject();

    // First try to get Export-Package header from the generated manifests
    final String outdirS = proj.getProperty("outdir");
    if (null!=outdirS && 0<outdirS.length()) {
      final File outdir = new File(outdirS);
      final String[] mfFileNames = { "api.mf", "lib.mf", "all.mf", "impl.mf" };
      for (int i = 0; i<mfFileNames.length; i++) {
        final File mfFile = new File(outdir, mfFileNames[i]);
        if (mfFile.exists()) {
          InputStreamReader ir = null;
          try {
            final FileInputStream   is = new FileInputStream(mfFile);
            ir = new InputStreamReader(is, "UTF-8");
            final Manifest mf = new Manifest(ir);
            final Manifest.Attribute attr
              = mf.getMainSection().getAttribute("Export-Package");
            if (null!=attr) {
              final String value = attr.getValue();
              if (null!=value && 0<value.length()) {
                return value;
              }
            }
          } catch (ManifestException me) {
            throw new BuildException("Manifest file " + mfFile + " is invalid",
                                     me, getLocation());
          } catch (IOException ioe) {
            throw new BuildException("Failed to read " + mfFile,
                                     ioe, getLocation());
          } finally {
            if (ir != null) {
              try {
                ir.close();
              } catch (IOException e) {
                // ignore
              }
            }
          }
        }
      }
    }

    // Fallback to the value from the template manifest.
    String res = proj.getProperty("bmfa.Export-Package");
    if ("[bundle.emptystring]".equals(res)) {
      res = "";
    }
    return res;
  }

  /** Template row for an exported package. */
  private static final String packageListRow
    = "${namelink}&nbsp;${version}<br/>\n";

  /**
   * Create one row in the output for each exported package. The
   * format of the row is determined by {@link #packageListRow}.
   *
   * @param epkgs The value of the "Export-Package" attribute to
   *              present.
   * @param pathToJavadoc Relative path from the output directory to
   *              the root of the javadoc tree to link to.
   */
  private String formatPkgsWithJavadocLinks(final String epkgs,
                                            final String pathToJavadoc)
  {
    final StringBuffer res = new StringBuffer();

    // Maps pkgName -> version
    final Map epkgMap = BundleHTMLExtractorTask.parseNames(epkgs, false, null);
    if (0<epkgMap.size()) {
      for (Iterator it = epkgMap.entrySet().iterator(); it.hasNext(); ) {
        final Map.Entry entry = (Map.Entry) it.next();
        final String pkg = (String) entry.getKey();
        final Version version = (Version) entry.getValue();

        final String docFile = replace(pkg, ".", "/") +"/package-summary.html";
        final String docPath = pathToJavadoc +"/index.html?" +docFile;

        final File f = new File(outdir +File.separator
                                +pathToJavadoc.replace('/', File.separatorChar)
                                +File.separator
                                +docFile.replace('/', File.separatorChar));

        String row = packageListRow;
        if(pathToJavadoc != null && !"".equals(pathToJavadoc)) {
          if (!f.exists()) {
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

        res.append(row);
      }
    }
    return res.toString();
  }

}
