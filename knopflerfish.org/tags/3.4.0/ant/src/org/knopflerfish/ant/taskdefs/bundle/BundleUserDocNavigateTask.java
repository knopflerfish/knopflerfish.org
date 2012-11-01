/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * <p>
 *  This task is used when building bundle user documentation for a
 *  Knopflerfish release, it builds the navigation frame listing
 *  bundles with user documentation.  If you don't intend to create a
 *  new distribution type of Knopflerfish then you're in the wrong
 *  place.
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
 *    Where to put the generated file (directory).
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    tofile
 *   </td>
 *   <td>
 *    The relative path to where the generated file should be
 *    copied. That is the actual location of the generated file
 *    will be <code>outdir</code>/<code>tofile</code>.
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
 *    The file which describes what the page should look like.
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
 *    The title to use on the generated page.
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    docdir
 *   </td>
 *   <td>
 *    The directory with one sub-directory for each user documentation
 *    link to create. Defaults to <code>outdir</code>.
 *
 *    <p>All sub-directories of <tt>docdir</tt> will result in a link
 *       on the generated navigation page. The link will belong to the
 *       default category, have a title set to the name of the
 *       sub-directory and a link path pointing to the file
 *       "index.html" inside the sub-directory. The default link properties
 *       may be overridden by specifying other values in a
 *       properties file named <tt>doc.properties</tt> in the
 *       sub-directory.</p>
 *
 *    <p>The following keys in the properties file are used:</p>
 *
 *    <dl>
 *
 *     <dt><tt>category</tt></dt>
 *     <dd>The name of the category to present the link under.</dd>
 *
 *     <dd>The link text.</dd>
 *     <dt><tt>title</tt></dt>
 *
 *     <dt><tt>linkPath</tt></dt>
 *     <dd>The path that the link points to. The default is a relative
 *         path pointing to the file <tt>index.html</tt> inside the
 *         sub-directory holding the properties file.</dd>
 *
 *     <dt><tt>sortKey</tt></dt>
 *     <dd>String to use when sorting links. Default is the value of
 *         the title key.</dd>
 *
 *     <dt><tt>depth</tt></dt>
 *     <dd>Nesting depth of link presentation. Default is 1. Must
 *         be one or greater.</dd>
 *
 *    </dl>
 *
 *    <p>It is possible to generate more than one link from the same
 *       <tt>doc.properties</tt> file. To do this insert a
 *       <tt>linkCount</tt> key with the number of links to create as
 *       its value. Then for each link add all the keys defined above
 *       with the link number followed by a '.' as key-prefix. Link
 *       number prefixes starts with 0 and must be strictly smaller
 *       than the value of <tt>linkCount</tt>. If there is no category
 *       value with a link number prefix the un-prefixed category value
 *       will be used. I.e., if all links belongs to the same category
 *       it suffices to write the category name once.</p>
 *
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    defaultcategory
 *   </td>
 *   <td>
 *    The name of the category to place links under when not specified
 *    in the doc-subdirectory.
 *   </td>
 *   <td>
 *    No, defaults to "bundle".
 *   </td>
 *  </tr>
 * </table>
 */

public class BundleUserDocNavigateTask
  extends Task
{
  public static final String DOC_PROPS_FILE_NAME = "doc.properties";

  /**
   * Target directory, where everything will end up
   */
  private File outDir;


  public void setOutdir(File f) {
    outDir = f;
    if (null==docDir) docDir = f;
  }


  /**
   * The relative path to the target file from output dir
   */
  private String toFileName;

  public void setTofile(String s) {
    toFileName = s;
  }

  /**
   * Template file
   */
  private File template;

  public void setTemplate(File f) {
    template = f;
  }


  /**
   * The title of the generated page
   */
  private String pageTitle;

  public void setTitle(String s) {
    pageTitle = s;
  }


  /**
   * The directory to analyze sub-directories in.
   */
  private File docDir;


  public void setDocdir(File f) {
    docDir = f;
  }


  /**
   * The default category name
   */
  private String defaultCategory = "bundle";

  public void setDefaultcategory(String s) {
    defaultCategory = s;
  }

  // Mapping from category name to sorted set with link data.
  private Map/*<String,SortedSet<LinkData>>*/ categories = new HashMap();

  // Insert a link data item to a category in the categories map
  private void add(final String category, final LinkData ld)
  {
    SortedSet lds = (SortedSet) categories.get(category);
    if (null==lds) {
      lds = new TreeSet();
      categories.put(category, lds);
    }
    lds.add(ld);
  }

  // Build a html-string with links to the lds in the set.
  private String links(final Set lds)
  {
    final StringBuffer sb = new StringBuffer(1024);
    for (Iterator it = lds.iterator(); it.hasNext(); ) {
      final LinkData ld = (LinkData) it.next();

      sb.append("      <dd class=\"leftmenu");
      sb.append(ld.depth +1);
      sb.append("\">");
      sb.append("<a target=\"bundledoc_main\" href=\"");
      sb.append(ld.linkPath);
      sb.append("\">");
      sb.append(ld.title);
      sb.append("</a></dd>\n");
    }

    return sb.toString();
  }

  // The data needed to create a document link in a category.
  static class LinkData implements Comparable
  {
    String title;
    String linkPath;
    String sortKey;
    int depth = 1;

    public LinkData(final String title, final String linkPath)
    {
      this.title = title;
      this.sortKey = title.toLowerCase();
      this.linkPath = linkPath;
    }

    public int compareTo(Object o)
    {
      LinkData other = (LinkData) o;
      return sortKey.compareTo(other.sortKey);
    }
  }

  private String fillLinkData(final Properties docProps,
                              final String prefix,
                              final LinkData ld)
  {
    ld.title = docProps.getProperty(prefix +"title", ld.title);
    ld.sortKey = docProps.getProperty(prefix +"sortKey", ld.title);
    ld.linkPath = docProps.getProperty(prefix +"linkPath", ld.linkPath);
    final String sd = docProps.getProperty(prefix +"depth");
    if (null!=sd && 0<sd.length()) {
      try {
        ld.depth = Integer.parseInt(sd);
      } catch (NumberFormatException nfe) {
      }
    }
    final String category = docProps.getProperty("category", defaultCategory);
    return docProps.getProperty(prefix +"category", category);
  }

  private void analyzeDocDir()
  {
    final File[] files = docDir.listFiles();
    for (int i=0; i<files.length; i++) {
      final File file = files[i];

      // Only interested in directories.
      if (!file.isDirectory()) continue;

      final String defaultTitle = file.getName();
      final String defaultLinkPath = file.getName() +"/index.html";

      final File docPropsFile = new File(file, DOC_PROPS_FILE_NAME);
      if (docPropsFile.canRead()) {
        try {
          final Properties docProps = new Properties();
          docProps.load(new FileInputStream(docPropsFile));

          final String linkCntS = docProps.getProperty("linkCount");
          int linkCnt = 0;
          if (null!=linkCntS && 0<linkCntS.length()) {
            try {
              linkCnt = Integer.parseInt(linkCntS);
            } catch (NumberFormatException nfe) {
              log("Invalid linkCount value, '"+linkCntS
                  +"' found in '"+docPropsFile +"': "+nfe.getMessage(),
                  Project.MSG_WARN);
            }
          }
          if (0==linkCnt) {
            final LinkData ld = new LinkData(defaultTitle, defaultLinkPath);
            final String category = fillLinkData(docProps, "", ld);
            add(category, ld);
          } else {
            for (int j=0; j<linkCnt; j++) {
              final LinkData ld = new LinkData(defaultTitle, defaultLinkPath);
              final String category = fillLinkData(docProps,
                                                   String.valueOf(j)+".",
                                                   ld);
              add(category, ld);
            }
          }
        } catch (IOException ioe) {
          log("Failed to load user documentation property description from '"
              +docPropsFile +"': "+ioe.getMessage(), Project.MSG_ERR);
        }
      } else {
        // No doc.properties; create a default link
        final LinkData ld = new LinkData(defaultTitle, defaultLinkPath);
        add(defaultCategory, ld);
      }
    }
  }


  public void execute() {
    if (template == null)   throw new BuildException("template must be set");
    if (docDir == null)     throw new BuildException("docdir must be set");
    if (outDir == null)     throw new BuildException("outdir must be set");
    if (toFileName == null) throw new BuildException("tofile must be set");

    if (defaultCategory == null)
      throw new BuildException("defaultCategory must not be null.");

    analyzeDocDir();

    transform(template, toFileName);
  }

  private void transform(final File fromFile, final String toFileName) {

    try {
      // Ensure that the direcotry to write the output file to exists
      final File toFile = new File(outDir, toFileName);
      File tmp = toFile.getParentFile();
      if (!tmp.exists()) {
        if (tmp.exists() || !tmp.mkdirs()) {
          throw new IOException("Could not create " + tmp);
        }
      }

      String content = Util.loadFile(fromFile.getAbsolutePath());
      content = Util.replace(content, "$(TITLE)", pageTitle);

      for (Iterator it = categories.entrySet().iterator(); it.hasNext();) {
        final Map.Entry entry = (Map.Entry) it.next();
        final String category = (String) entry.getKey();
        final Set lds = (Set) entry.getValue();

        final String ldHtml = links(lds);
        if (0<ldHtml.length()) {
          int oldContentLength = content.length();
          content = Util.replace(content, "$("+category+")", links(lds));
          if (oldContentLength == content.length()) {
            final String msg = "Found bundle user documentation with category '"
              +category +"', but there is no such category in the bundle user "
              +"documentation navigate list template, '"
              +fromFile.getAbsolutePath() +"'.";

            log(msg, Project.MSG_ERR);
            throw new BuildException(msg);
          }
        }
      }

      Util.writeStringToFile(toFile, content);
      log("Created: " + toFile.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();
      throw new BuildException(e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException(e);
    }
  }

}
