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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ServiceHTMLDisplayer
  extends DefaultSwingBundleDisplayer
  implements JHTMLBundleLinkHandler
{

  public ServiceHTMLDisplayer(BundleContext bc)
  {
    super(bc, "Services", "Shows bundle services", true);

    bUseListeners = true;
    bUpdateOnBundleChange = true;
    bUpdateOnServiceChange = true;
  }

  // -------------------------------- Service URL ------------------------------
  /**
   * Helper class that handles links to OSGi services.
   * <p>
   * The URL will look like: <code>http://desktop/service?sid=<SID></code>.
   * </p>
   */
  public static class ServiceUrl
  {
    public static final String URL_SERVICE_HOST = "desktop";
    public static final String URL_SERVICE_PREFIX_PATH = "/service";
    public static final String URL_SERVICE_KEY_SID = "sid";

    /** Service Id of the Service the link references. */
    private final long sid;

    public static boolean isServiceLink(URL url)
    {
      return URL_SERVICE_HOST.equals(url.getHost())
             && url.getPath().startsWith(URL_SERVICE_PREFIX_PATH);
    }

    public ServiceUrl(URL url)
    {
      if (!isServiceLink(url)) {
        throw new RuntimeException("URL '" + url + "' does not start with "
                                   + "http://" + URL_SERVICE_HOST
                                   + URL_SERVICE_PREFIX_PATH);
      }

      final Map<String, String> params = Util.paramsFromURL(url);
      if (!params.containsKey(URL_SERVICE_KEY_SID)) {
        throw new RuntimeException("Invalid bundle service URL '" + url
                                   + "' service id is missing.");
      }
      this.sid = Long.parseLong(params.get(URL_SERVICE_KEY_SID));
    }

    public ServiceUrl(final ServiceReference<?> sr)
    {
      this.sid = ((Long) sr.getProperty(Constants.SERVICE_ID)).longValue();
    }

    public ServiceUrl(final Long serviceId)
    {
      this.sid = serviceId.longValue();
    }

    public long getSid()
    {
      return sid;
    }

    private void appendBaseURL(final StringBuilder sb)
    {
      sb.append("http://");
      sb.append(URL_SERVICE_HOST);
      sb.append(URL_SERVICE_PREFIX_PATH);
    }

    private Map<String, String> getParams()
    {
      final Map<String, String> params = new HashMap<String, String>();
      params.put(URL_SERVICE_KEY_SID, String.valueOf(sid));
      return params;
    }

    public void serviceLink(final StringBuilder sb, final String text)
    {
      sb.append("<a href=\"");
      appendBaseURL(sb);
      Util.appendParams(sb, getParams());
      sb.append("\">");
      sb.append(text);
      sb.append("</a>");
    }
  }

  // -------------------------------- Service URL ------------------------------

  @Override
  public JComponent newJComponent()
  {
    return new JHTML(this);
  }

  @Override
  public void valueChanged(long bid)
  {
    final Bundle[] bl = Activator.desktop.getSelectedBundles();

    for (final JComponent jcomp : components) {
      final JHTML comp = (JHTML) jcomp;
      comp.valueChanged(bl);
    }
  }

  public boolean renderUrl(final URL url, final StringBuilder sb)
  {
    final ServiceUrl serviceUrl = new ServiceUrl(url);

    appendServiceHTML(sb, serviceUrl.getSid());

    return true;
  }

  void appendServiceHTML(final StringBuilder sb, final long sid)
  {
    try {
      final String filter = "(" + Constants.SERVICE_ID + "=" + sid + ")";
      final ServiceReference<?>[] srl =
        Activator.getTargetBC_getServiceReferences(null, filter);
      if (srl != null && srl.length == 1) {
        sb.append("<html>");
        sb.append("<table border=0>");

        sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
        JHTMLBundle.startFont(sb, "-1");
        sb.append("Service #").append(sid);
        sb.append(", ");
        Util.bundleLink(sb, srl[0].getBundle());
        JHTMLBundle.stopFont(sb);
        sb.append("</td>\n");
        sb.append("</tr>\n");
        sb.append("</table>");

        JHTMLBundle.startFont(sb);
        sb.append("<b>Properties</b>");
        JHTMLBundle.stopFont(sb);
        sb.append("<table cellpadding=\"0\" cellspacing=\"1\" border=\"0\">");
        final String[] keys = srl[0].getPropertyKeys();
        for (int i = 0; keys != null && i < keys.length; i++) {

          final StringWriter sw = new StringWriter();
          final PrintWriter pr = new PrintWriter(sw);

          Util.printObject(pr, srl[0].getProperty(keys[i]));

          sb.append("<tr>");
          sb.append("<td valign=\"top\">");
          JHTMLBundle.startFont(sb);
          sb.append(keys[i]);
          JHTMLBundle.stopFont(sb);
          sb.append("</td>");

          sb.append("<td valign=\"top\">");
          sb.append(sw.toString());
          sb.append("</td>");

          sb.append("</tr>");
        }
        sb.append("</table>");

        try {
          formatServiceObject(sb, srl[0]);
        } catch (final Exception e) {
          sb.append("Failed to format service object: ").append(e);
          Activator.log
              .warn("Failed to format service object: " + e, srl[0], e);
        }

        sb.append("</html>");

      } else {
        sb.append("No service with sid=").append(sid);
      }
    } catch (final Exception e2) {
      e2.printStackTrace();
    }
  }

  void formatServiceObject(final StringBuilder sb, final ServiceReference<?> sr)
  {
    final String[] names = (String[]) sr.getProperty(Constants.OBJECTCLASS);

    JHTMLBundle.startFont(sb);
    sb.append("<b>Implemented interfaces</b>");
    sb.append("<br>");
    for (int i = 0; i < names.length; i++) {
      sb.append(names[i]);
      if (i < names.length - 1) {
        sb.append(", ");
      }
    }
    JHTMLBundle.stopFont(sb);
    sb.append("<br>");

    JHTMLBundle.startFont(sb);
    sb.append("<b>Methods</b>");

    sb.append("<table>");
    for (final String name2 : names) {
      try {
        final Class<?> clazz = sr.getBundle().loadClass(name2);
        if (null == clazz) {
          sb.append("<tr><td colspan=\"3\" valign=\"top\" bgcolor=\"#eeeeee\">");
          JHTMLBundle.startFont(sb);
          sb.append("Class not found: ").append(name2);
          JHTMLBundle.stopFont(sb);
          sb.append("</td></tr>");
        } else {
          formatClass(sb, clazz);
        }
      } catch (final ClassNotFoundException e) {
        sb.append("<tr><td colspan=\"3\" valign=\"top\" bgcolor=\"#eeeeee\">");
        JHTMLBundle.startFont(sb);
        sb.append("Class not found: ").append(name2);
        JHTMLBundle.stopFont(sb);
        sb.append("</td></tr>");
      }
    }
    sb.append("</table>");
  }

  void formatClass(final StringBuilder sb, final Class<?> clazz)
  {
    final Method[] methods = clazz.getDeclaredMethods();

    sb.append("<tr>");
    sb.append("<td colspan=\"4\" valign=\"top\" bgcolor=\"#eeeeee\">");
    JHTMLBundle.startFont(sb);
    sb.append(clazz.getName());
    JHTMLBundle.stopFont(sb);
    sb.append("</td></tr>");

    for (int i = 0; i < methods.length; i++) {
      if (!Modifier.isPublic(methods[i].getModifiers())) {
        continue;
      }
      final Class<?>[] params = methods[i].getParameterTypes();
      sb.append("<tr>");

      sb.append("<td valign=\"top\" colspan=\"3\">");
      JHTMLBundle.startFont(sb);
      sb.append(className(methods[i].getReturnType().getName()));

      sb.append("&nbsp;");
      sb.append(methods[i].getName());

      sb.append("(");
      for (int j = 0; j < params.length; j++) {
        sb.append(className(params[j].getName()));
        if (j < params.length - 1) {
          sb.append(",&nbsp;");
        }
      }
      sb.append(");&nbsp;");
      JHTMLBundle.stopFont(sb);
      sb.append("</td>");

      sb.append("</tr>");
    }
  }

  String className(String name)
  {
    if (name.startsWith("[L") && name.endsWith(";")) {
      name = name.substring(2, name.length() - 1) + "[]";
    }

    if (name.startsWith("java.lang.")) {
      name = name.substring(10);
    }

    return name;
  }

  class JHTML
    extends JHTMLBundle
  {
    private static final long serialVersionUID = 1L;

    JHTML(DefaultSwingBundleDisplayer displayer)
    {
      super(displayer);
    }

    @Override
    public StringBuilder bundleInfo(Bundle b)
    {
      final StringBuilder sb = new StringBuilder();

      try {
        final ServiceReference<?>[] srl =
          Activator.getTargetBC_getServiceReferences();
        int nExport = 0;
        int nImport = 0;
        for (int i = 0; srl != null && i < srl.length; i++) {
          final Bundle srlb = srl[i].getBundle();
          if (null == srlb) { // Skip unregistered service.
            continue;
          }
          if (srlb.getBundleId() == b.getBundleId()) {
            nExport++;
          }
          final Bundle[] bl = srl[i].getUsingBundles();
          for (int j = 0; bl != null && j < bl.length; j++) {
            if (bl[j].getBundleId() == b.getBundleId()) {
              nImport++;
            }
          }
        }

        startFont(sb);

        if (nExport > 0) {
          sb.append("<b>Exported services</b>");

          for (int i = 0; srl != null && i < srl.length; i++) {
            final Bundle srlb = srl[i].getBundle();
            if (null != srlb && srlb.getBundleId() == b.getBundleId()) {
              final String[] cl =
                (String[]) srl[i].getProperty(Constants.OBJECTCLASS);
              final Bundle[] bl = srl[i].getUsingBundles();

              for (final String element : cl) {
                sb.append("<br>");
                sb.append("#");
                new ServiceUrl(srl[i])
                    .serviceLink(sb, srl[i].getProperty(Constants.SERVICE_ID)
                        .toString());
                sb.append(" ");
                sb.append(element);
              }

              if (bl != null && bl.length > 0) {
                // sb.append("<b>Used by</b><br>");
                for (int j = 0; bl != null && j < bl.length; j++) {
                  sb.append("<br>");
                  sb.append("&nbsp;&nbsp;");
                  Util.bundleLink(sb, bl[j]);
                }
              }
            }
          }
        }

        if (nImport > 0) {
          sb.append("<br><b>Imported services</b>");
          for (int i = 0; srl != null && i < srl.length; i++) {
            final Bundle[] bl = srl[i].getUsingBundles();
            for (int j = 0; bl != null && j < bl.length; j++) {
              if (bl[j].getBundleId() == b.getBundleId()) {
                final String[] cl =
                  (String[]) srl[i].getProperty(Constants.OBJECTCLASS);
                for (final String element : cl) {
                  sb.append("<br>");
                  sb.append("#");
                  new ServiceUrl(srl[i])
                      .serviceLink(sb, srl[i].getProperty(Constants.SERVICE_ID)
                          .toString());
                  sb.append(" ");
                  sb.append(element);
                }
                sb.append("<br>");
                sb.append("&nbsp;&nbsp;");
                Util.bundleLink(sb, bl[j]);
              }
            }
          }
        }
        sb.append("</font>");
      } catch (final Exception e) {
        e.printStackTrace();
      }
      sb.append("<table border=0 cellspacing=1 cellpadding=0>\n");

      sb.append("</table>");
      return sb;
    }

  }

  public boolean canRenderUrl(final URL url)
  {
    return ServiceUrl.isServiceLink(url);
  }

}
