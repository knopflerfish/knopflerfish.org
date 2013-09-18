/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.service.component.ComponentConstants;

import org.knopflerfish.framework.Util.HeaderEntry;

public class ManifestHTMLDisplayer
  extends DefaultSwingBundleDisplayer
  implements JHTMLBundleLinkHandler
{

  public ManifestHTMLDisplayer(BundleContext bc)
  {
    super(bc, "Manifest", "Shows bundle manifest", true);
  }

  // --------------------------- Resource URL ----------------------------
  /**
   * Helper class that handles links to bundle resources.
   * <p>
   * The URL will look like:
   * <code>http://desktop/resource/<PATH>?bid=<BID>&amp;scr=<isSCR></code>.
   * </p>
   */
  public static class ResourceUrl
  {
    public static final String URL_RESOURCE_HOST = "desktop";
    public static final String URL_RESOURCE_PREFIX_PATH = "/resource";
    public static final String URL_RESOURCE_KEY_BID = "bid";
    public static final String URL_RESOURCE_KEY_SCR = "scr";

    /** Bundle Id of the bundle that owns the resource. */
    private final long bid;

    /**
     * Path within the bundle to the resource. File name part may be a pattern
     * according to the rules of the second argument of
     * {@link Bundle#findEntries(String, String, boolean)}.
     */
    private final String path;

    /**
     * Indicates if the resource is a SCR component-xml file or not.
     */
    private final boolean isScr;

    /**
     * Check if the given <code>url</code> is a bundle resource URL or not.
     *
     * @param url
     *          The <code>url</code> to check.
     * @return <code>true</code> if the given <code>url</code> is a bundle
     *         resource URL, <code>false</code> otherwise.
     */
    public static boolean isResourceLink(URL url)
    {
      return URL_RESOURCE_HOST.equals(url.getHost())
             && url.getPath().startsWith(URL_RESOURCE_PREFIX_PATH);
    }

    /**
     * Create a resource URL from a {@link URL}.
     *
     * @param url
     *          The URL to parse as a resource URL.
     */
    public ResourceUrl(URL url)
    {
      if (!isResourceLink(url)) {
        throw new RuntimeException("URL '" + url + "' does not start with "
                                   + "http://" + URL_RESOURCE_HOST
                                   + URL_RESOURCE_PREFIX_PATH);
      }
      this.path = url.getPath().substring(URL_RESOURCE_PREFIX_PATH.length());

      final Map<String, String> params = Util.paramsFromURL(url);
      if (!params.containsKey(URL_RESOURCE_KEY_BID)) {
        throw new RuntimeException("Invalid bundle resource URL '" + url
                                   + "' bundle id is missing " + url.toString());
      }
      this.bid = Long.parseLong(params.get(URL_RESOURCE_KEY_BID));
      this.isScr = "true".equals(params.get(URL_RESOURCE_KEY_SCR));
    }

    public ResourceUrl(final Bundle bundle, final String path,
                       final boolean isSCR)
    {
      this.bid = bundle.getBundleId();
      this.path = path;
      this.isScr = isSCR;
    }

    public long getBid()
    {
      return bid;
    }

    /**
     * @return the full path including file name pattern.
     */
    public String getFullPath()
    {
      return path;
    }

    /**
     * @return the path without the file name pattern part.
     */
    public String getPath()
    {
      final int i = path.lastIndexOf('/');
      return i == -1 ? "" : path.substring(0, i);
    }

    /**
     * @return the file name pattern part of the full path.
     */
    public String getFilenamePattern()
    {
      final int i = path.lastIndexOf('/');
      return i == -1 ? path : path.substring(i + 1);
    }

    public boolean isSCR()
    {
      return isScr;
    }

    private void appendBaseURL(final StringBuffer sb)
    {
      sb.append("http://");
      sb.append(URL_RESOURCE_HOST);
      sb.append(URL_RESOURCE_PREFIX_PATH);
      sb.append('/');
      sb.append(path);
    }

    private Map<String, String> getParams()
    {
      final HashMap<String, String> params = new HashMap<String, String>();
      params.put(URL_RESOURCE_KEY_BID, String.valueOf(bid));
      params.put(URL_RESOURCE_KEY_SCR, String.valueOf(isScr));
      return params;
    }

    public void resourceLink(final StringBuffer sb)
    {
      resourceLink(sb, path);
    }

    public void resourceLink(final StringBuffer sb, final String label)
    {
      sb.append("<a href=\"");
      appendBaseURL(sb);
      Util.appendParams(sb, getParams());
      sb.append("\">");
      sb.append(label);
      sb.append("</a>");
    }
  }
  // -------------------------- End Resource URL ----------------------------

  @Override
  public JComponent newJComponent()
  {
    return new JHTML(this);
  }

  @Override
  public void valueChanged(long bid)
  {
    final Bundle[] bl = Activator.desktop.getSelectedBundles();

    for (final JComponent jComponent : components) {
      final JHTML comp = (JHTML) jComponent;
      comp.valueChanged(bl);
    }
  }

  class JHTML
    extends JHTMLBundle
  {
    private static final String BUNDLE_LICENSE = "Bundle-License";
    private static final long serialVersionUID = 1L;

    JHTML(DefaultSwingBundleDisplayer displayer)
    {
      super(displayer);
    }

    @Override
    public StringBuffer bundleInfo(Bundle b)
    {
      final StringBuffer sb = new StringBuffer();

      final Dictionary<String, String> headers = b.getHeaders();

      sb.append("<table border=0 cellspacing=1 cellpadding=0>\n");
      appendRow(sb, BG_COLOR_BUNDLE_DATA, null, null, "Location",
                "" + b.getLocation());

      final List<BundleRevision> bundleRevisions =
        b.adapt(BundleRevisions.class).getRevisions();
      String state = Util.stateName(b.getState());
      if (bundleRevisions.size() > 1) {
        state += ", pending refresh.";
      }
      appendRow(sb, BG_COLOR_BUNDLE_DATA, null, null, "State", state);

      if (b.getSymbolicName() != null) {
        appendRow(sb, BG_COLOR_BUNDLE_DATA, null, null, "Symbolic name",
                  b.getSymbolicName());
      }
      appendRow(sb,
                BG_COLOR_BUNDLE_DATA,
                null,
                null,
                "Last modified",
                ""
                    + new SimpleDateFormat().format(new Date(b
                        .getLastModified())));

      final BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
      if (bsl != null) {
        String level = "";
        try {
          level = Integer.toString(bsl.getStartLevel());
          if (bsl.isPersistentlyStarted()) {
            level += ", persistently started";
          }
        } catch (final IllegalArgumentException e) {
          level = "not managed";
        }
        appendRow(sb, BG_COLOR_BUNDLE_DATA, null, null, "Start level", level);
      }

      // Spacer for better layout (and separation of non-manifest data):
      appendRow(sb,
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;",
                "");

      final ArrayList<String> headerKeys =
        new ArrayList<String>(headers.size());
      for (final Enumeration<String> e = headers.keys(); e.hasMoreElements();) {
        headerKeys.add(e.nextElement());
      }
      Collections.sort(headerKeys);
      for (final String key : headerKeys) {
        String value = headers.get(key);
        if (value != null && !"".equals(value)) {
          value = Strings.replace(value, "<", "&lt;");
          value = Strings.replace(value, ">", "&gt;");
          if (Constants.IMPORT_PACKAGE.equals(key) || Constants.EXPORT_SERVICE.equals(key)
              || Constants.BUNDLE_CLASSPATH.equals(key) || "Classpath".equals(key)
              || Constants.IMPORT_SERVICE.equals(key) || Constants.EXPORT_PACKAGE.equals(key)) {
            value = Strings.replaceWordSep(value, ",", "<br>", '"');
          } else if (ComponentConstants.SERVICE_COMPONENT.equals(key)) {
            final StringBuffer sb2 = new StringBuffer(60);
            // Re-uses the manifest entry parser from the KF-framework
            for (final HeaderEntry he : org.knopflerfish.framework.Util
                .parseManifestHeader(BUNDLE_LICENSE, value, true, true, false)) {
              if (sb2.length() > 0) {
                sb2.append(", ");
              }
              final String pattern = he.getKey();
              new ResourceUrl(b, pattern, true).resourceLink(sb2);
            }
            value = sb2.toString();
          } else if (BUNDLE_LICENSE.equals(key)) {
            // Re-uses the manifest entry parser from the KF-framework
            final StringBuffer sb2 = new StringBuffer(200);
            for (final HeaderEntry he : org.knopflerfish.framework.Util
                .parseManifestHeader(BUNDLE_LICENSE, value, true, true, false)) {
              if (sb2.length() > 0) {
                sb2.append(", ");
              }
              final String licenseName = he.getKey();
              sb2.append(makeLink(licenseName));
              for (final Entry<String, Object> attributeEntry : he
                  .getAttributes().entrySet()) {
                sb2.append("; ");
                sb2.append(attributeEntry.getKey());
                sb2.append("=");
                sb2.append(makeLink(attributeEntry.getValue().toString()));
              }
            }
            value = sb2.toString();
          } else {
            value = makeLink(value);
          }
          appendRow(sb, key, value);
        }
      }
      sb.append("</table>");
      return sb;
    }

    /**
     * Create a HTML-link for the given string if it starts with something that
     * looks like a URL protocol.
     *
     * @param value
     *          the text to make a link of
     * @return the text or the text wrapped in a HTML link.
     */
    private String makeLink(String value)
    {
      if (value.startsWith("http:") || value.startsWith("https:")
          || value.startsWith("ftp:") || value.startsWith("file:")) {
        value = "<a href=\"" + value + "\">" + value + "</a>";
      }
      return value;
    }
  }

  @Override
  public boolean canRenderUrl(final URL url)
  {
    return ResourceUrl.isResourceLink(url);
  }

  @Override
  public boolean renderUrl(final URL url, final StringBuffer sb)
  {
    final ResourceUrl resUrl = new ResourceUrl(url);

    appendResourceHTML(sb, resUrl);
    return true; // Always OK to add ResourceUrls to history.
  }

  void appendResourceHTML(final StringBuffer sb, final ResourceUrl resUrl)
  {
    final Bundle bundle = Activator.getTargetBC_getBundle(resUrl.getBid());
    sb.append("<html>");
    sb.append("<table border=0 width=\"100%\">");

    final Enumeration<URL> resEnum =
      bundle.findEntries(resUrl.getPath(), resUrl.getFilenamePattern(), true);
    while (resEnum.hasMoreElements()) {
      final URL url = resEnum.nextElement();

      sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
      JHTMLBundle.startFont(sb, "-1");
      sb.append("#" + bundle.getBundleId() + " " + url.getPath());
      JHTMLBundle.stopFont(sb);
      sb.append("</td>\n");
      sb.append("</tr>\n");

      sb.append("<tr>");
      sb.append("<td>");
      sb.append("<pre>");
      JHTMLBundle.startFont(sb, "-1");
      try {
        final byte[] bytes = Util.readStream(url.openStream());
        String value = new String(bytes);
        value = Strings.replace(value, "<", "&lt;");
        value = Strings.replace(value, ">", "&gt;");

        if (resUrl.isSCR()) {
          // Break down component start tag into 4 pieces:
          // $1 <scr:component .* name="
          // $2 XMLNS prefix part in $1 if present
          // $3 the actual component name
          // $4 " .*>
          final Pattern p =
            Pattern
                .compile("(&lt;(\\w*?:)?component\\s.*?name\\s*=\\s*\")([^\"]*)(\".*?&gt;)",
                         Pattern.DOTALL);
          final Matcher m = p.matcher(value);
          final StringBuffer sb2 = new StringBuffer();
          while (m.find()) {
            final StringBuffer sb3 = new StringBuffer();
            sb3.setLength(0);
            sb3.append("$1");
            new SCRHTMLDisplayer.ScrUrl(m.group(3)).scrLink(sb3, m.group(3));
            sb3.append("$4");
            m.appendReplacement(sb2, sb3.toString());
          }
          m.appendTail(sb2);
          value = sb2.toString();
        }
        sb.append(value);
      } catch (final Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        sb.append(sw.toString());
      }
      JHTMLBundle.stopFont(sb);
      sb.append("</pre>");
      sb.append("</td>");
      sb.append("</tr>");
    }
    sb.append("</table>");
    sb.append("</html>");
  }

}
