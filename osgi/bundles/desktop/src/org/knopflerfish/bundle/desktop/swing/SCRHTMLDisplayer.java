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
   * Helper class that handles links to bundle resources.
   * <p>
   * The URL will look like:
   * <code>http://desktop/scr?sid=&lt;SID>&amp;cid=&lt;CID>&amp;ref=&lt;REFERENCE>&amp;cmd=&lt;CMD></code>.
   * </p>
   */
  public static class ScrUrl {
    public static final String URL_SCR_HOST = "desktop";
    public static final String URL_SCR_PREFIX_PATH = "/scr";
    public static final String URL_SCR_KEY_SID = "bid";
    public static final String URL_SCR_KEY_CID = "cid";
    public static final String URL_SCR_KEY_REF = "ref";
    public static final String URL_SCR_KEY_COMP_NAME = "compName";
    public static final String URL_SCR_KEY_CMD = "cmd";
    public static final String URL_SCR_CMD_ENABLE = "Enable";
    public static final String URL_SCR_CMD_DISABLE = "Disable";
    public static final String URL_SCR_CMD_REFRESH = "Refresh";

    /** Service Id of the ScrService owning the component. */
    private long sid = -1;
    /** Component Id of the component the link point to. */
    private long cid = -1;
    /** The component reference that the URL points to (optional). */
    private String ref;
    /** Search for named component. sid, cid optional when present. */
    private String compName;
    /** True if the URL contains a state change command. */
    private boolean isCmd = false;
    /** True if the URL is a command to enable the component.*/
    private boolean doEnable = false;
    /** True if the URL is a command to disable the component.*/
    private boolean doDisable = false;
    /** True if the URL is a refresh command.*/
    private boolean doRefresh = false;

    public static boolean isScrLink(URL url) {
      return URL_SCR_HOST.equals(url.getHost())
          && url.getPath().startsWith(URL_SCR_PREFIX_PATH);
    }

    public ScrUrl(URL url) {
      if (!isScrLink(url)) {
        throw new RuntimeException("URL '" + url + "' does not start with "
            + "http://" + URL_SCR_HOST + URL_SCR_PREFIX_PATH);
      }

      final Map params = Util.paramsFromURL(url);
      if (!params.containsKey(URL_SCR_KEY_SID)
          && !params.containsKey(URL_SCR_KEY_COMP_NAME)) {
        throw new RuntimeException("Invalid service component URL '" + url
            + "' SCR service id is missing.");
      }
      if (!params.containsKey(URL_SCR_KEY_CID)
          && !params.containsKey(URL_SCR_KEY_COMP_NAME)) {
        throw new RuntimeException("Invalid service component URL '" + url
            + "' component id is missing.");
      }
      if (params.containsKey(URL_SCR_KEY_SID)) {
        this.sid = Long.parseLong((String) params.get(URL_SCR_KEY_SID));
      }
      if (params.containsKey(URL_SCR_KEY_CID)) {
        this.cid = Long.parseLong((String) params.get(URL_SCR_KEY_CID));
      }
      this.ref = (String) params.get(URL_SCR_KEY_REF);
      this.compName = (String) params.get(URL_SCR_KEY_COMP_NAME);
      this.isCmd = params.containsKey(URL_SCR_KEY_CMD);
      if (this.isCmd) {
        final String cmd = (String) params.get(URL_SCR_KEY_CMD);
        this.doEnable = URL_SCR_CMD_ENABLE.equals(cmd);
        this.doDisable = URL_SCR_CMD_DISABLE.equals(cmd);
        this.doRefresh = URL_SCR_CMD_REFRESH.equals(cmd);
      }
    }

    public ScrUrl(final ServiceReference scrSR, final Component component) {
      this.sid = ((Long) scrSR.getProperty(Constants.SERVICE_ID)).longValue();
      this.cid = component.getId();
      // Include component name to handle components with id=-1 or changed id
      this.compName = component.getName();
      this.ref = null;
    }

    public ScrUrl(final long sid,
                  final Component component,
                  final Reference ref) {
      this.sid = sid;
      this.cid = component.getId();
      this.ref = ref.getName();
    }

    public ScrUrl(final long sid, final Component component) {
      this.sid = sid;
      this.cid = component.getId();
      this.ref = null;
    }

    public ScrUrl(final String componentName) {
      this.compName = componentName;
      this.sid = -1L;
      this.cid = -1L;
      this.ref = null;
    }

    public long getSid() {
      return sid;
    }

    public long getCid() {
      return cid;
    }

    public void setCid(long cid) {
      this.cid = cid;
    }

    public String getRef() {
      return ref;
    }

    public boolean isCommand() {
      return isCmd;
    }

    public void setCommand(final boolean isCmd) {
      this.isCmd = isCmd;
    }

    public boolean doEnable() {
      return isCmd && doEnable;
    }

    public boolean doDisable() {
      return isCmd && doDisable;
    }

    public boolean doRefresh() {
      return isCmd && doRefresh;
    }

    public String getComponentName() {
      return compName;
    }

    public void setComponentName(final String compName) {
      this.compName = compName;
    }

    private void appendBaseURL(final StringBuffer sb) {
      sb.append("http://");
      sb.append(URL_SCR_HOST);
      sb.append(URL_SCR_PREFIX_PATH);
    }

    private Map getParams() {
      final Map params = new HashMap();
      if (compName!=null) {
        // If componentName set no other params are needed.
        params.put(URL_SCR_KEY_COMP_NAME, compName);
      }
      if (sid>-1) {
        params.put(URL_SCR_KEY_SID, String.valueOf(sid));
      }
      if (cid>-1) {
        params.put(URL_SCR_KEY_CID, String.valueOf(cid));
      }
      if (null != ref) {
        params.put(URL_SCR_KEY_REF, ref);
      }
      if (isCmd) {
        params.put(URL_SCR_KEY_CMD, doEnable ? URL_SCR_CMD_ENABLE
            : URL_SCR_CMD_DISABLE);
      }
      return params;
    }

    public void scrLink(final StringBuffer sb, final String label) {
      sb.append("<a href=\"");
      appendBaseURL(sb);
      Util.appendParams(sb, getParams());
      sb.append("\">");
      sb.append(label);
      sb.append("</a>");
    }

    public void stateForm(final StringBuffer sb, final Component comp) {

      sb.append("<table border=0 cellspacing=1 cellpadding=1 width='100%'>\n");
      sb.append("<tr><td valign='middle'>");
      JHTMLBundle.startFont(sb, "-1");
      sb.append(getComponentState(comp));
      JHTMLBundle.stopFont(sb);
      sb.append("</td><td valign='middle'>");
      JHTMLBundle.startFont(sb, "-1");
      sb.append("<form action=\"");
      appendBaseURL(sb);
      sb.append("\" method=\"get\">");
      sb.append("<input type=\"submit\" name=\"");
      sb.append(URL_SCR_KEY_CMD);
      sb.append("\" value=\"");
      sb.append(comp.getState()==Component.STATE_DISABLED ? URL_SCR_CMD_ENABLE : URL_SCR_CMD_DISABLE);
      sb.append("\">");
      sb.append("&nbsp;&nbsp;");
      sb.append("<input type=\"submit\" name=\"");
      sb.append(URL_SCR_KEY_CMD);
      sb.append("\" value=\"");
      sb.append(URL_SCR_CMD_REFRESH);
      sb.append("\">");
      final Map params = getParams();
      for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
        final Map.Entry entry = (Map.Entry) it.next();
        sb.append("<input type=\"hidden\" name=\"");
        sb.append(entry.getKey());
        sb.append("\" value=\"");
        sb.append(entry.getValue());
        sb.append("\">");
      }
      JHTMLBundle.stopFont(sb);
      sb.append("</form>");
      sb.append("</td></tr>");
      sb.append("</table>");
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

    // Overridden to show all components when no bundle is selected.
    public void updateView(Bundle[] bl)
    {
      if (!isShowing()) {
        // Don't update non-visible components.
        return;
      }

      // Show all components when no bundle is selected.
      if (bl == null || bl.length == 0) {
        setCurrentBID(-1L);

        final StringBuffer sb = new StringBuffer(600);
        appendComponentListHTML(sb, null, null);
        setHTML(sb.toString());
      } else {
        // Use the default listing with one table per selected bundle.
        super.updateView(bl);
      }
    }

    /**
     * Present the components registered for a single bundle.
     * @param b the bundle to present components for.
     */
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
        appendComponentListHTML(sb, b, null);
      }

      sb.append("<table border=0 cellspacing=1 cellpadding=0>\n");
      sb.append("</table>");
      return sb;
    }

  }

  /**
   * Create HTML table with one row for each matching component.
   * @param sb The buffer to append the resulting table to.
   * @param compName If non-null, only include components with a matching name.
   */
  private void appendComponentListHTML(final StringBuffer sb,
                                       final Bundle bundle,
                                       final String compName) {
    sb.append("<html>\n");
    sb.append("<table border=\"0\" width=\"100%\">\n");

    // Table heading when not provided by caller.
    if (null == bundle) {
      sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
      JHTMLBundle.startFont(sb, "-1");
      if (compName!=null) {
        sb.append("SCR Components with name '");
        sb.append(compName);
        sb.append("'");
      } else {
        sb.append("All SCR Components");
      }
      JHTMLBundle.stopFont(sb);
      sb.append("</td>\n");
      sb.append("</tr>\n");
    }

    int startPos = sb.length();
    boolean compFound = false;
    sb.append("<tr><td width=\"100%\">");
    JHTMLBundle.startFont(sb, "-6");
    startComponentTable(sb);

    try {
      for (Iterator it = scrServices.entrySet().iterator(); it.hasNext();) {
        final Map.Entry entry = (Map.Entry) it.next();
        final ServiceReference scrSR = (ServiceReference) entry.getKey();
        final ScrService scrService = (ScrService) entry.getValue();
        final Component[] components = null!=bundle
            ? scrService.getComponents(bundle)
            : ((null!=compName) ? scrService.getComponents(compName)
                : scrService.getComponents());

        for (int i = 0; components!=null && i < components.length; i++) {
          final Component component = components[i];
          appendComponentRow(sb, scrSR, component);
          compFound = true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    stopComponentTable(sb);
    JHTMLBundle.stopFont(sb);
    sb.append("</td>\n");
    sb.append("</tr>\n");
    if(!compFound) {
      // No componets; remove component table.
      sb.setLength(startPos);
    }

    sb.append("</table>\n");
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
    if (scrServices.size()>1) {
      scrUrl.scrLink(sb,
                     String.valueOf(component.getId()) +"@" +scrUrl.getSid());
    } else {
      scrUrl.scrLink(sb, String.valueOf(component.getId()));
    }
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

  static String getComponentState(final Component component) {
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

  public boolean renderUrl(final URL url, final StringBuffer sb) {
    final ScrUrl scrUrl = new ScrUrl(url);

    // Must save value here since the command state is cleared after execution.
    final boolean addToHistory = !scrUrl.isCommand();

    if (null != scrUrl.getRef()) {
      appendReferenceHTML(sb, scrUrl);
    } else if (null!= scrUrl.getComponentName() && -1==scrUrl.getSid()) {
      appendComponentListHTML(sb, null, scrUrl.getComponentName());
    } else {
      appendComponentHTML(sb, scrUrl);
    }

    // URLs with a command must not be added to the history.
    return addToHistory;
  }


  private Component getComponent(final ScrService scr,
                                 final long cid,
                                 final String compName) {
    Component comp = scr.getComponent(cid);
    if (null==comp && null!=compName) {
      Component[] comps = scr.getComponents(compName);
      if (null!=comps) {
        if (comps.length>1) {
          Activator.log.info("Found " + comps.length +" components with name '"
                             +compName +"', using first.");
        }
        comp = comps[0];
      }
    }
    return comp;
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
        Component comp = getComponent(scr, scrUrl.getCid(),
                                      scrUrl.getComponentName());

        if (null != comp) {
          // Shall we enable / disable the component?
          if (scrUrl.doEnable() && Component.STATE_DISABLED == comp.getState()) {
            Activator.log.info("Enabling component: "+comp);
            comp.enable();
            scrUrl.setCommand(false); // Command has been performed!
          }
          if (scrUrl.doDisable() && Component.STATE_DISABLED != comp.getState()) {
            Activator.log.info("Disabling component: "+comp);
            comp.disable();
            scrUrl.setCommand(false); // Command has been performed!
          }
          if (scrUrl.doRefresh()) {
            scrUrl.setCommand(false); // Command has been performed!
          }

          sb.append("<html>");
          sb.append("<table border=0 width='100%'>");

          sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
          sb.append("Service Component #" + comp.getId());
          if (scrServices.size()>1) {
            sb.append("@");
            sb.append(scrUrl.getSid());
          }
          sb.append("</td>\n");
          sb.append("</tr>\n");
          sb.append("</table>\n");

          sb.append("<table cellpadding=\"0\" cellspacing=\"3\" border=\"0\">\n");
          appendComponentLine(sb, "Name", comp.getName());
          if (null != comp.getFactory()) {
            appendComponentLine(sb, "Factory name", comp.getFactory());
          }

          {
            final StringBuffer sb2 = new StringBuffer(60);
            scrUrl.stateForm(sb2, comp);
            appendComponentLine(sb, "State", sb2.toString());
          }

          appendComponentLine(sb, componentServicesLabel(comp),
              componentServices(comp));
          appendComponentLine(sb, "Service factory",
              comp.isServiceFactory() ? "YES" : "NO");
          appendComponentLine(sb, "Properties", componentProperties(comp));

          appendComponentLine(sb, "References", componentReferences(scrUrl.getSid(), comp));

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

    // cellspacing:5 is compensated by cellpadding-left:-5 to left-justify table
    sb.append("<table cellpadding='0' cellspacing='5' border='0' width='100%'>\n");
    sb.append("<tr><th align='left' style='padding-left:-5'>Name</th><th align='left'>Value</th></tr>\n");
    final Dictionary props = comp.getProperties();
    for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();
      final StringWriter sw = new StringWriter();
      final PrintWriter pr = new PrintWriter(sw);

      Util.printObject(pr, props.get(key));

      sb.append("<tr>");
      sb.append("<td valign='top' style='padding-left:-5'>");
      JHTMLBundle.startFont(sb, "-2");
      sb.append(key);
      JHTMLBundle.stopFont(sb);
      sb.append("</td>");

      sb.append("<td valign='top'>");
      sb.append(sw.toString());
      sb.append("</td>");

      sb.append("</tr>\n");
    }
    sb.append("</table>\n");

    return sb.toString();
  }

  String componentReferences(final long sid, final Component comp) {
    final StringBuffer sb = new StringBuffer(100);

    final Reference[] refs = comp.getReferences();
    if (null == refs) {
      sb.append("-");
    } else {
      // cellspacing:10 is compensated by cellpadding-left:-10 to left-justify table
      sb.append("<table cellpadding='0' cellspacing='10' border='0' width='100%'>");
      sb.append("<tr>");
      sb.append("<th align='left' style='padding-left:-10'>Name</th>");
      sb.append("<th align='left'>State</th>");
      sb.append("<th align='left'>Cardinality</th>");
      sb.append("<th align='left'>Policy</th>");
      sb.append("<th align='left'>Service</th>");
      sb.append("</tr>");

      for (int i = 0; i < refs.length; i++) {
        final Reference ref = refs[i];

        sb.append("<tr>");
        sb.append("<td align='left' valign='middle' style='padding-left:-10'>");
        JHTMLBundle.startFont(sb, "-1");
        new ScrUrl(sid, comp, ref).scrLink(sb, ref.getName());
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("<td align='left' valign='middle'>");
        JHTMLBundle.startFont(sb, "-1");
        sb.append(ref.isSatisfied() ? "SATISFIED" : "PENDING");
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("<td align='center' valign='middle'>");
        JHTMLBundle.startFont(sb, "-1");
        sb.append(ref.isOptional() ? "0" : "1");
        sb.append("..");
        sb.append(ref.isMultiple() ? "N" : "1");
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("<td align='left' valign='middle'>");
        JHTMLBundle.startFont(sb, "-1");
        sb.append(ref.isStatic() ? "static" : "dynamic");
        JHTMLBundle.stopFont(sb);
        sb.append("</td>");

        sb.append("<td align='left' valign='middle'>");
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
            sb.append("<table border=0 width='100%'>");

            sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
            sb.append("Service Component #");
            new ScrUrl(scrUrl.getSid(), comp).scrLink(sb, String.valueOf(comp.getId()));
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
