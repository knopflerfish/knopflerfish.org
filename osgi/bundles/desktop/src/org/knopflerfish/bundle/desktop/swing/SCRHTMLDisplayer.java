/*
 * Copyright (c) 2012, KNOPFLERFISH project
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;

public class SCRHTMLDisplayer extends DefaultSwingBundleDisplayer implements
    ServiceTrackerCustomizer, JHTMLBundleLinkHandler

{

  public SCRHTMLDisplayer(BundleContext bc) {
    super(bc, "SCR", "Shows service components for selected bundle(s).", true);

    bUseListeners = false;
    bUpdateOnBundleChange = false;
    bUpdateOnServiceChange = false;

    // Remote framework is not supported.
    scrTracker = Activator.getBC().equals(Activator.getTargetBC())
      ? new ServiceTracker(Activator.getBC(), ScrService.class.getName(), this)
      : null;
  }

  /**
   * Mapping <tt>ServiceReference -&gt; ScrService</tt> holding all available
   * ScrServices.
   */
  final Map scrServices = new HashMap();

  ScrService getScrServiceById(final long sid) {
    for (Iterator it = scrServices.entrySet().iterator(); it.hasNext();) {
      final Map.Entry entry = (Map.Entry) it.next();
      final ServiceReference sr = (ServiceReference) entry.getKey();
      final Long srSid = (Long) sr.getProperty(Constants.SERVICE_ID);
      if (srSid.longValue() == sid) {
        return (ScrService) entry.getValue();
      }
    }
    return null;
  }

  private ServiceTracker scrTracker;

  public Object addingService(ServiceReference sr) {
    Activator.log.info("ScrService added.", sr);
    final Object scrService = Activator.getBC().getService(sr);
    if (scrService != null) {
      scrServices.put(sr, scrService);
    }
    return scrService;
  }

  public void modifiedService(ServiceReference sr, Object service) {
  }

  public void removedService(ServiceReference sr, Object service) {
    if (null != scrServices.remove(sr)) {
      if (Activator.log != null) {
        Activator.log.info("ScrService removed.", sr);
      }
    }
  }

  public void open() {
    super.open();

    if (scrTracker != null) {
      scrTracker.open();
    }
  }

  public void close() {

    if (scrTracker != null) {
      scrTracker.close();
    }

    super.close();
  }

  //-------------------------------- SCR URL ---------------------------------
  /**
   * Helper class that handles links to SCR components and their references.
   */
  public static class ScrUrl {
    public static final String URL_COMPONENT_PREFIX = "http://desktop/scr/";

    /** Service Id of the ScrService owning the component. */
    private long sid;
    /** Component Id of the component the link point to. */
    private long cid;
    /** The component reference that the URL points to (optional). */
    private String ref;

    public ScrUrl(URL url) {
      if(!isScrLink(url)) {
        throw new RuntimeException("URL '" + url + "' does not start with " +
                                   URL_COMPONENT_PREFIX);
      }
      final String urlS = url.toString();
      int start = URL_COMPONENT_PREFIX.length();
      int end = urlS.indexOf('/', start+1);
      if (end<start+1) {
        throw new RuntimeException("Invalid service component URL '" + url
                                   + "' component id is missing " + urlS);
      }
      sid = Long.parseLong(urlS.substring(start,end));

      start = end+1;
      end = urlS.indexOf('/', start+1);
      end = -1==end ? urlS.length() : end;
      cid = Long.parseLong(urlS.substring(start, end));

      start = end+1;
      ref = start<urlS.length() ? urlS.substring(start) : null;
    }

    public ScrUrl(final ServiceReference scrSR, final Component component) {
      sid = ((Long) scrSR.getProperty(Constants.SERVICE_ID)).longValue();
      cid = component.getId();
      ref = null;
    }

    public ScrUrl(final long sid,
                  final Component component,
                  final Reference ref) {
      this.sid = sid;
      this.cid = component.getId();
      this.ref = ref.getName();
    }

    public static boolean isScrLink(URL url) {
      return url.toString().startsWith(URL_COMPONENT_PREFIX);
    }

    public long getSid() {
      return sid;
    }

    public long getCid() {
      return cid;
    }

    public String getRef() {
      return ref;
    }


    public void scrLink(final StringBuffer sb, final String text) {
      sb.append("<a href=\"");
      sb.append(URL_COMPONENT_PREFIX);
      sb.append(sid);
      sb.append("/");
      sb.append(cid);
      if (null != ref) {
        sb.append("/");
        sb.append(ref);
      }
      sb.append("\">");
      sb.append(text);
      sb.append("</a>");
    }
  }


  public JComponent newJComponent() {
    return new JHTML(this);
  }

  public void valueChanged(long bid) {
    Bundle[] bl = Activator.desktop.getSelectedBundles();

    for (Iterator it = components.iterator(); it.hasNext();) {
      JHTML comp = (JHTML) it.next();
      comp.valueChanged(bl);
    }
  }

  class JHTML extends JHTMLBundle {
    private static final long serialVersionUID = 1L;

    JHTML(DefaultSwingBundleDisplayer displayer) {
      super(displayer);
    }

    public StringBuffer bundleInfo(Bundle b) {
      final StringBuffer sb = new StringBuffer();

      if (null == scrTracker) {
        startFont(sb, "+2");
        sb.append("SCR displayer not implemented for remote framework.<br>");
        stopFont(sb);
      }

      if (scrServices.isEmpty()) {
        startFont(sb, "+2");
        sb.append("No Service Component Runtime available.<br>");
        stopFont(sb);
      } else {

        try {
          for (Iterator it = scrServices.entrySet().iterator(); it.hasNext();) {
            final Map.Entry entry = (Map.Entry) it.next();
            final ServiceReference scrSR = (ServiceReference) entry.getKey();
            final ScrService scrService = (ScrService) entry.getValue();
            final Component[] components = scrService.getComponents(b);
            if (null == components) {
              sb.append("No SCR components registered.<br/>");
            } else {
              startFont(sb);
              startComponentTable(sb);
              for (int i = 0; i < components.length; i++) {
                final Component component = components[i];
                appendComponentRow(sb, scrSR, component);
              }
              stopComponentTable(sb);
              stopFont(sb);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      sb.append("<table border=0 cellspacing=1 cellpadding=0>\n");
      sb.append("</table>");
      return sb;
    }

  }

  void startComponentTable(final StringBuffer sb) {
    sb.append("<table border=0 cellspacing=1 cellpadding=10>\n");
    sb.append("<tr><th>ID</th><th align='left'>State</th><th align='left'>Name</th><th align='left'>Services</th></tr>\n");
  }

  void stopComponentTable(final StringBuffer sb) {
    sb.append("</table>");
  }

  void appendComponentRow(final StringBuffer sb,
                          final ServiceReference scrSR,
                          final Component component) {
    final ScrUrl scrUrl = new ScrUrl(scrSR, component);

    sb.append("<tr>\n");

    sb.append("<td>");
    scrUrl.scrLink(sb, String.valueOf(component.getId()));
    sb.append("</td>");

    sb.append("<td align='left'>");
    sb.append(getComponentState(component));
    sb.append("</td>");

    sb.append("<td align='left'>");
    scrUrl.scrLink(sb, component.getName());
    sb.append("</td>");

    sb.append("<td align='left'>");
    sb.append(componentServices(component));
    sb.append("</td>");

    sb.append("</tr>\n");
  }

  String getComponentState(final Component component) {
    switch (component.getState()) {
    case Component.STATE_ACTIVATING:
      return "ACTIVATING";
    case Component.STATE_ACTIVE:
      return "ACTIVE";
    case Component.STATE_DEACTIVATING:
      return "DEACTIVATING";
    case Component.STATE_DISABLED:
      return "DISABLED";
    case Component.STATE_DISABLING:
      return "DISABLING";
    case Component.STATE_DISPOSED:
      return "DISPOSED";
    case Component.STATE_DISPOSING:
      return "DISPOSING";
    case Component.STATE_ENABLING:
      return "ENABLING";
    case Component.STATE_FACTORY:
      return "FACTORY";
    case Component.STATE_REGISTERED:
      return "REGISTERED";
    case Component.STATE_UNSATISFIED:
      return "UNSATISFIED";
    default:
      return "?" + String.valueOf(component.getState()) + "?";
    }
  }

  public boolean canRenderUrl(final URL url) {
    return ScrUrl.isScrLink(url);
  }

  public void renderUrl(final URL url, final StringBuffer sb) {
    final ScrUrl scrUrl = new ScrUrl(url);

    if (null == scrUrl.getRef()) {
      appendComponentHTML(sb, scrUrl);
    } else {
      appendReferenceHTML(sb, scrUrl);
    }
  }

  /**
   * Append HTML that presents a single SCR component.
   *
   * @param sb
   *          string buffer to append to.
   * @param scrUrl
   *          URL holding service id of the ScrService owning the component
   *          to present and the id of it.
   */
  void appendComponentHTML(final StringBuffer sb, final ScrUrl scrUrl) {
    try {
      final ScrService scr = getScrServiceById(scrUrl.getSid());
      if (null != scr) {
        final Component comp = scr.getComponent(scrUrl.getCid());

        if (null != comp) {
          sb.append("<html>");
          sb.append("<table border=0>");

          sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
          sb.append("Service Component #" + scrUrl.getCid());
          sb.append("</td>\n");
          sb.append("</tr>\n");
          sb.append("</table>\n");

          sb.append("<table cellpadding=\"0\" cellspacing=\"3\" border=\"0\">\n");
          appendComponentLine(sb, "Name", comp.getName());
          if (null != comp.getFactory()) {
            appendComponentLine(sb, "Factory name", comp.getFactory());
          }
          appendComponentLine(sb, "State", getComponentState(comp));
          appendComponentLine(sb, componentServicesLabel(comp),
              componentServices(comp));
          appendComponentLine(sb, "Service factory",
              comp.isServiceFactory() ? "YES" : "NO");
          appendComponentLine(sb, "Properties", componentProperties(comp));

          appendComponentLine(sb, "&nbsp;", "&nbsp;");

          appendComponentLine(sb, "References", componentReferences(scrUrl.getSid(), comp));

          appendComponentLine(sb, "&nbsp;", "&nbsp;");

          {
            final StringBuffer bundleSb = new StringBuffer(20);
            Util.bundleLink(bundleSb, comp.getBundle());
            appendComponentLine(sb, "Bundle", bundleSb.toString());
          }
          appendComponentLine(sb, "Configuration policy",
              comp.getConfigurationPolicy());
          appendComponentLine(sb, "Default enabled",
              comp.isDefaultEnabled() ? "YES" : "NO");
          appendComponentLine(sb, "Immediate", comp.isImmediate() ? "YES"
              : "NO");
          appendComponentLine(sb, "Implementation class",
              "<tt>" + comp.getClassName() + "</tt>");
          appendComponentLine(sb, "Activate method name", comp.getActivate()
              + (comp.isActivateDeclared() ? "" : " (default)"));
          appendComponentLine(sb, "Dectivate method name", comp.getDeactivate()
              + (comp.isDeactivateDeclared() ? "" : " (default)"));
          appendComponentLine(sb, "Modified method name",
              null != comp.getModified() ? comp.getModified() : "-");

          sb.append("</table>\n");
        } else {
          sb.append("No SCR component with id=" + scrUrl.getCid());
        }
      } else {
        sb.append("No SCR service with sid=" + scrUrl.getSid());
      }
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }

  void appendComponentLine(final StringBuffer sb, final String label,
      final String value) {
    JHTMLBundle.appendRow(sb, "-1", "align='left'", label, value);
  }

  String componentServicesLabel(final Component comp) {
    final String[] services = comp.getServices();
    return (null == services || services.length == 1) ? "Service" : "Services";
  }

  String componentServices(final Component comp) {
    final StringBuffer sb = new StringBuffer(100);

    final String[] services = comp.getServices();
    if (null == services) {
      sb.append("-");
    } else {
      for (int i = 0; i < services.length; i++) {
        if (0 < i) {
          sb.append(",<br>");
        }
        sb.append(services[i]);
      }
    }
    return sb.toString();
  }

  String componentProperties(final Component comp) throws IOException {
    final StringBuffer sb = new StringBuffer(100);

    sb.append("<table cellpadding=\"0\" cellspacing=\"3\" border=\"0\">");
    final Dictionary props = comp.getProperties();
    for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();
      final StringWriter sw = new StringWriter();
      final PrintWriter pr = new PrintWriter(sw);

      Util.printObject(pr, props.get(key));

      sb.append("<tr>");
      sb.append("<td valign=\"top\">");
      JHTMLBundle.startFont(sb, "-1");
      sb.append(key);
      JHTMLBundle.stopFont(sb);
      sb.append("</td>");

      sb.append("<td valign=\"top\">");
      sb.append(sw.toString());
      sb.append("</td>");

      sb.append("</tr>");
    }
    sb.append("</table>");

    return sb.toString();
  }

  String componentReferences(final long sid, final Component comp) {
    final StringBuffer sb = new StringBuffer(100);

    final Reference[] refs = comp.getReferences();
    if (null == refs) {
      sb.append("-");
    } else {
      sb.append("<table cellpadding=\"0\" cellspacing=\"3\" border=\"0\">");
      sb.append("<tr><th align='left'>Name</th>");
      sb.append("<th align='left'>State</th>");
      sb.append("<th align='left'>Cardinality</th>");
      sb.append("<th align='left'>Policy</th>");
      sb.append("<th align='left'>Service</th>");
      sb.append("</tr>");

      for (int i = 0; i < refs.length; i++) {
        final Reference ref = refs[i];

        sb.append("<tr>");
        sb.append("<td align='left' valign=\"top\">");
        JHTMLBundle.startFont(sb, "-1");
        new ScrUrl(sid, comp, ref).scrLink(sb, ref.getName());
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("<td align='left' valign=\"top\">");
        JHTMLBundle.startFont(sb, "-1");
        sb.append(ref.isSatisfied() ? "SATISFIED" : "PENDING");
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("<td align='center' valign=\"top\">");
        JHTMLBundle.startFont(sb, "-1");
        sb.append(ref.isOptional() ? "0" : "1");
        sb.append("..");
        sb.append(ref.isMultiple() ? "N" : "1");
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("<td align='left' valign=\"top\">");
        JHTMLBundle.startFont(sb, "-1");
        sb.append(ref.isStatic() ? "static" : "dynamic");
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("<td align='left' valign=\"top\">");
        JHTMLBundle.startFont(sb, "-2");
        sb.append("<tt>");
        sb.append(ref.getServiceName());
        sb.append("</tt>");
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("</tr>");
      }
      sb.append("</table>");
    }
    return sb.toString();
  }

  /**
   * Append HTML that presents a reference to a service from a SCR component.
   *
   * @param sb
   *          string buffer to append to.
   * @param scrUrl
   *          Url that holds the data needed to find the reference to present.
   */
  void appendReferenceHTML(final StringBuffer sb, final ScrUrl scrUrl) {
    try {
      final ScrService scr = getScrServiceById(scrUrl.getSid());
      if (null != scr) {
        final Component comp = scr.getComponent(scrUrl.getCid());
        if (null != comp) {
          final Reference[] refs = comp.getReferences();
          Reference ref = null;
          for (int i = 0; i < refs.length; i++) {
            if (refs[i].getName().equals(scrUrl.getRef())) {
              ref = refs[i];
            }
          }
          if (null != ref) {
            sb.append("<html>");
            sb.append("<table border=0>");

            sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
            sb.append("Service Component #");
            sb.append(scrUrl.getCid());
            sb.append(", <i>");
            sb.append(comp.getName());
            sb.append("</i>, <b>Reference</b> <i>");
            sb.append(scrUrl.getRef());
            sb.append("</i></td>\n");
            sb.append("</tr>\n");
            sb.append("</table>\n");

            sb.append("<table cellpadding=\"0\" cellspacing=\"3\" border=\"0\">\n");
            appendComponentLine(sb, "Name", ref.getName());
            appendComponentLine(sb, "State", ref.isSatisfied() ? "SATISFIED"
                : "PENDING");
            appendComponentLine(sb, "Service", ref.getServiceName());
            appendComponentLine(sb, "Cardinality", getCardinality(ref));
            appendComponentLine(sb, "Policy", ref.isStatic() ? "static"
                : "dynamic");

            appendComponentLine(sb, "&nbsp;", "&nbsp;");

            final ServiceReference[] services = ref.getServiceReferences();
            final String servicesLabel = ref.isMultiple() ? "Bound services"
                : "Bound service";
            if (null == services) {
              appendComponentLine(sb, servicesLabel, "NONE");
            } else {
              final StringBuffer serviceSb = new StringBuffer(100);
              for (int i = 0; i < services.length; i++) {
                if (i > 0) {
                  serviceSb.append(", ");
                }
                final ServiceReference service = services[i];
                final Long serviceId = (Long) service
                    .getProperty(Constants.SERVICE_ID);
                serviceSb.append("#");
                new ServiceHTMLDisplayer.ServiceUrl(serviceId).serviceLink(serviceSb, serviceId.toString());
              }
              appendComponentLine(sb, servicesLabel, serviceSb.toString());
            }

            appendComponentLine(sb, "&nbsp;", "&nbsp;");

            appendComponentLine(sb, "Bind method",
                getNullAsDash(ref.getBindMethodName()));
            appendComponentLine(sb, "Unbind method",
                getNullAsDash(ref.getUnbindMethodName()));
            appendComponentLine(sb, "Update method",
                getNullAsDash(ref.getUpdatedMethodName()));

            sb.append("</table>\n");
          } else {
            sb.append("No component reference with name=" + scrUrl.getRef());
          }
        } else {
          sb.append("No component with id=" + scrUrl.getCid());
        }
      } else {
        sb.append("No SCR service with sid=" + scrUrl.getSid());
      }
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }

  String getCardinality(final Reference ref) {
    return (ref.isOptional() ? "0" : "1") + ".."
        + (ref.isMultiple() ? "N" : "1");
  }

  String getNullAsDash(final String s) {
    return s == null ? "-" : s;
  }

}
