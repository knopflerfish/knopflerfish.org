/*
 * Copyright (c) 2013-2022, KNOPFLERFISH project All rights reserved.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

import org.knopflerfish.util.framework.VersionRange;



public class WiringHTMLDisplayer
  extends DefaultSwingBundleDisplayer
  implements FrameworkListener, JHTMLBundleLinkHandler
{

  /**
   * Create a HTML-link for the given string if it starts with something that
   * looks like a URL protocol.
   *
   * @param value
   *          the text to make a link of
   * @return the text or the text wrapped in a HTML link.
   */
  static String makeLink(String value)
  {
    if (value.startsWith("http:") || value.startsWith("https:")
        || value.startsWith("ftp:") || value.startsWith("file:")) {
      value = "<a href=\"" + value + "\">" + value + "</a>";
    }
    return value;
  }
  public WiringHTMLDisplayer(BundleContext bc) {
    super(bc, "Wiring", "Shows wiring between bundle revisions.", true);

    bUseListeners          = true;
    bUpdateOnBundleChange  = true;
  }

  @Override
  public JComponent newJComponent() {
    return new JHTML(this);
  }

  @Override
  public void valueChanged(long bid) {
    final Bundle[] bl = Activator.desktop.getSelectedBundles();

    for (final JComponent jcomp : components) {
      try {
        final JHTML comp = (JHTML) jcomp;
        comp.valueChanged(bl);
      } catch (final Throwable t) {
        t.printStackTrace(System.err);
      }
    }
  }

  @Override
  public void frameworkEvent(FrameworkEvent event)
  {
    // Refresh is done, update displayer.
    valueChanged(event.getBundle().getBundleId());
  }

  @Override
  public boolean canRenderUrl(URL url)
  {
    return WiringUrl.isWiringLink(url);
  }

  @Override
  public boolean renderUrl(URL url, StringBuilder sb)
  {
    final WiringUrl wiringUrl = new WiringUrl(url);
    // URLs with a command must not be added to the history.
    boolean addToHistory = !wiringUrl.isCommand();

    if (wiringUrl.doResolve()) {
      final long bid = wiringUrl.getBid();
      final Collection<Bundle> bundles = wiringUrl.getBundles();
      final Bundle systemBundle = Activator.getTargetBC_getBundle(0);
      final FrameworkWiring frameworkWiring =
        systemBundle.adapt(FrameworkWiring.class);
      frameworkWiring.resolveBundles(bundles);
      valueChanged(bid);
    } else if (wiringUrl.doAskRefresh()) {
      addToHistory = true; // Presentation command that does not change state.
      appendRefreshMessage(sb, wiringUrl);
    } else if (wiringUrl.doRefresh()) {
      sb.append("  Refreshing...  ");
      final Collection<Bundle> bundles = wiringUrl.getBundles();

      final Bundle systemBundle = Activator.getTargetBC_getBundle(0);
      final FrameworkWiring frameworkWiring =
        systemBundle.adapt(FrameworkWiring.class);

      frameworkWiring.refreshBundles(bundles, this);
    }

    return addToHistory;
  }


  public void appendRefreshMessage(final StringBuilder sb,
                                   final WiringUrl wiringUrl) {
    final Bundle systemBundle = Activator.getTargetBC_getBundle(0);
    final FrameworkWiring frameworkWiring =
      systemBundle.adapt(FrameworkWiring.class);

    final Collection<Bundle> bundles = wiringUrl.getBundles();
    final Collection<Bundle> closure =
      frameworkWiring.getDependencyClosure(bundles);

    sb.append("<table border=\"0\" width=\"100%\">\n");
    sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
    sb.append("Refresh Opertation");
    sb.append("</td>\n");
    sb.append("</tr>\n");

    sb.append("<tr><td bgcolor=\"#ffffff\">");
    JHTMLBundle.startFont(sb, "-1");
    sb.append("<b>Refresh operation for</b><br><font color=\"#444444\">");
    for (final Bundle b : bundles) {
      sb.append("&nbsp;&nbsp;#");
      sb.append(b.getBundleId());
      Util.bundleLink(sb, b);
      sb.append("&nbsp;&nbsp;(");
      final String state = Util.stateName(b.getState());
      sb.append(state);
      sb.append(")");
      sb.append("<br>");
    }
    sb.append("</font></td></tr>\n");
    JHTMLBundle.stopFont(sb);

    sb.append("<tr><td bgcolor=\"#ffffff\">");
    JHTMLBundle.startFont(sb, "-1");
    sb.append("<em>will affect:</em><br><font color=\"#666666\">");
    for (final Bundle b : closure) {
      sb.append("&nbsp;&nbsp;#");
      sb.append(b.getBundleId());
      sb.append("&nbsp;&nbsp;");
      Util.bundleLink(sb, b);
      sb.append("&nbsp;&nbsp;(");
      final String state = Util.stateName(b.getState());
      sb.append(state);
      sb.append(")");
      sb.append("<br>");
    }
    sb.append("</font></td></tr>\n");
    JHTMLBundle.stopFont(sb);

    sb.append("<tr><td bgcolor=\"#ffffff\">");
    wiringUrl.refreshForm(sb, bundles);
    sb.append("</td>\n");
    sb.append("</tr>\n");
    sb.append("</table>\n");
  }


  //-------------------------------- Wiring URL ---------------------------------
  /**
   * Helper class that handles links for wiring operations.
   * <p>
   * The URL will look like:
   * <code>http://desktop/wiring?bid=&lt;BID>&amp;cmd=&lt;CMD></code>.
   * </p>
   */
  public static class WiringUrl {
    public static final String URL_WIRING_HOST = "desktop";
    public static final String URL_WIRING_PREFIX_PATH = "/wiring";
    public static final String URL_WIRING_KEY_BID = "bid";
    public static final String URL_WIRING_KEY_CMD = "cmd";
    public static final String URL_WIRING_CMD_ASK_REFRESH = "AskRefresh";
    public static final String URL_WIRING_CMD_REFRESH = "Refresh";
    public static final String URL_WIRING_CMD_RESOLVE = "Resolve";

    /** Bundle id of the bundle the URL is about. */
    private long bid = -1;
    /** True if the URL contains a command. */
    private boolean isCmd = false;
    /** True if the URL is a command to resolve the bundle.*/
    private boolean doResolve = false;
    /** True if the URL is a command to refresh the bundle.*/
    private boolean doRefresh = false;
    /** True if the URL is a command to ask to perform refresh of the bundle.*/
    private boolean doAskRefresh = false;

    /**
     * Mapping from bundle id to bundle object to be able to handle links for
     * uninstalled bundles.
     */
    private static final Map<Long, Bundle> bidToBundle =
      new HashMap<Long, Bundle>();

    public static boolean isWiringLink(URL url) {
      return URL_WIRING_HOST.equals(url.getHost())
          && url.getPath().startsWith(URL_WIRING_PREFIX_PATH);
    }

    public WiringUrl(URL url)
    {
      if (!isWiringLink(url)) {
        throw new RuntimeException("URL '" + url + "' does not start with "
                                   + "http://" + URL_WIRING_HOST
                                   + URL_WIRING_PREFIX_PATH);
      }

      final Map<String, String> params = Util.paramsFromURL(url);
      if (params.containsKey(URL_WIRING_KEY_BID)) {
        bid = Long.parseLong(params.get(URL_WIRING_KEY_BID));
      } else {
        throw new RuntimeException("Invalid wiring command URL '" + url
                                   + "' bundle id is missing.");
      }
      isCmd = params.containsKey(URL_WIRING_KEY_CMD);
      if (isCmd) {
        final String cmd = params.get(URL_WIRING_KEY_CMD);
        doAskRefresh = URL_WIRING_CMD_ASK_REFRESH.equals(cmd);
        doRefresh = URL_WIRING_CMD_REFRESH.equals(cmd);
        doResolve = URL_WIRING_CMD_RESOLVE.equals(cmd);
      }
    }

    public WiringUrl(final Bundle bundle)
    {
      bid = bundle.getBundleId();
      bidToBundle.put(bid, bundle);
    }

    public long getBid() {
      return bid;
    }

    /**
     * Returns the bundle selected by {@code bid} wrapped in a collection of
     * bundles.
     *
     * <p>
     * <em>Note</em>: Calling this method will clear the {@code pidToBundle} map
     * if the bundle is present in it.
     *
     * @return a collection of bundles selected by this URL.
     */
    public Collection<Bundle> getBundles()
    {
      Bundle bundle = bidToBundle.get(bid);
      if (bundle != null) {
        bidToBundle.clear();
      } else {
        bundle = Activator.getTargetBC_getBundle(bid);
      }
      final Collection<Bundle> bundles = Collections.singleton(bundle);
      return bundles;
    }

    public boolean isCommand() {
      return isCmd;
    }

    public boolean doAskRefresh() {
      return isCmd && doAskRefresh;
    }

    public boolean doRefresh() {
      return isCmd && doRefresh;
    }

    public boolean doResolve() {
      return isCmd && doResolve;
    }

    private void appendBaseURL(final StringBuilder sb) {
      sb.append("http://");
      sb.append(URL_WIRING_HOST);
      sb.append(URL_WIRING_PREFIX_PATH);
    }

    private Map<String, String> getParams() {
      final Map<String, String> params = new HashMap<String, String>();
      if (bid>-1) {
        params.put(URL_WIRING_KEY_BID, String.valueOf(bid));
      }
      if (doAskRefresh) {
        params.put(URL_WIRING_KEY_CMD, URL_WIRING_CMD_ASK_REFRESH);
      } else if (doRefresh) {
        params.put(URL_WIRING_KEY_CMD, URL_WIRING_CMD_REFRESH);
      } else if (doResolve) {
        params.put(URL_WIRING_KEY_CMD, URL_WIRING_CMD_RESOLVE);
      }
      return params;
    }

    public void refreshLink(final StringBuilder sb, final String label) {
      doAskRefresh = true;
      doRefresh = false;
      doResolve = false;

      sb.append("<a href=\"");
      appendBaseURL(sb);
      Util.appendParams(sb, getParams());
      sb.append("\">");
      sb.append(label);
      sb.append("</a>");
    }

    /**
     * Render a form with a single button, labeled "Refresh" that triggers a
     * refresh operation.
     *
     * @param sb
     *          The string buffer to write HTML to.
     * @param bundles
     *          A singleton set of bundles to initialize the refresh operation
     *          from.
     */
    public void refreshForm(final StringBuilder sb, Collection<Bundle> bundles)
    {
      doAskRefresh = false;
      doRefresh = true;
      doResolve = false;

      // Handling of uninstalled bundles requires us to keep the bundle object.
      for (final Bundle b : bundles) {
        bid = b.getBundleId();
        bidToBundle.put(bid, b);
      }

      sb.append("<form action=\"");
      appendBaseURL(sb);
      sb.append("\" method=\"get\">");
      sb.append("<input type=\"submit\" name=\"");
      sb.append(URL_WIRING_KEY_CMD);
      sb.append("\" value=\"");
      sb.append(URL_WIRING_CMD_REFRESH);
      sb.append("\">");
      for (final Entry<String,String> entry : getParams().entrySet()) {
        sb.append("<input type=\"hidden\" name=\"");
        sb.append(entry.getKey());
        sb.append("\" value=\"");
        sb.append(entry.getValue());
        sb.append("\">");
      }
      sb.append("</form>");
    }

    public void resolveForm(final StringBuilder sb) {
      doAskRefresh = false;
      doRefresh = false;
      doResolve = true;

      sb.append("<form action=\"");
      appendBaseURL(sb);
      sb.append("\" method=\"get\">");
      sb.append("<input type=\"submit\" name=\"");
      sb.append(URL_WIRING_KEY_CMD);
      sb.append("\" value=\"");
      sb.append(URL_WIRING_CMD_RESOLVE);
      sb.append("\">");
      for (final Entry<String,String> entry : getParams().entrySet()) {
        sb.append("<input type=\"hidden\" name=\"");
        sb.append(entry.getKey());
        sb.append("\" value=\"");
        sb.append(entry.getValue());
        sb.append("\">");
      }
      sb.append("</form>");
    }

  }


  class JHTML extends JHTMLBundle {
    private static final long serialVersionUID = 1L;

    JHTML(DefaultSwingBundleDisplayer displayer)
    {
      super(displayer);
    }

    @Override
    public StringBuilder bundleInfo(Bundle b)
    {
      final StringBuilder sb = new StringBuilder();
      final boolean useParagraph = true;


      startFont(sb);

      final BundleWiring bw = b.adapt(BundleWiring.class);

      if (useParagraph) {
        sb.append("<p>");
      }

      if (bw == null) {
        final BundleRevision rev = b.adapt(BundleRevision.class);
        if ((rev.getTypes() & BundleRevision.TYPE_FRAGMENT) == BundleRevision.TYPE_FRAGMENT) {
          sb.append("<b>Un-attached fragment.</b><br>");
        } else {
          sb.append("<b>Unresolved bundle.</b><br>");
          final WiringUrl wiringUrl = new WiringUrl(b);
          wiringUrl.resolveForm(sb);
        }
      } else if (!bw.isInUse()) {
        sb.append("<b>Stale bundle.</b><br>");
      } else {
        // Find all name-spaces that b provides capabilities in.
        final Set<String> providedNameSpaces = getNameSapcesOfProvidedCaps(bw);

        sb.append("<b>Provided Capabilities and Wired Requesters</b><br>");

        for (final String nameSpace : providedNameSpaces) {
          sb.append("<em>");
          sb.append(nameSpace);
          sb.append("</em>:<br>");

          appendProvidedCapabilities(sb, nameSpace, bw);
        }
        if (useParagraph) {
          sb.append("</p>");
        } else {
          sb.append("<br>");
        }


        // Find all name-spaces that b requires capabilities in.
        final Set<String> requiredNameSpaces = getNameSapcesOfRequiredCaps(bw);
        sb.append("<b>Required Capabilities and Wired Providers</b><br>");

        for (final String nameSpace : requiredNameSpaces) {
          sb.append("<em>");
          sb.append(nameSpace);
          sb.append("</em>:<br>");

          appendRequiredCapabilities(sb, nameSpace, bw);
        }
        if (useParagraph) {
          sb.append("</p>");
        }
      }

      return sb;
    }


    private Set<String> getNameSapcesOfProvidedCaps(BundleWiring bw)
    {
      final Set<String> res = new TreeSet<String>();

      for (final BundleCapability cap : bw.getRevision().getDeclaredCapabilities(null)) {
        res.add(cap.getNamespace());
      }

      final List<BundleCapability> caps = bw.getCapabilities(null);
      if (caps != null) {
      for (final BundleCapability cap : caps) {
        res.add(cap.getNamespace());
      }
      }

      return res;
    }

    private Set<String> getNameSapcesOfRequiredCaps(BundleWiring bw)
    {
      final Set<String> res = new TreeSet<String>();

      for (final BundleRequirement req : bw.getRevision().getDeclaredRequirements(null)) {
        res.add(req.getNamespace());
      }

      final List<BundleRequirement> reqs = bw.getRequirements(null);
      if (reqs != null) {
        for (final BundleRequirement req : reqs) {
          res.add(req.getNamespace());
        }
      }

      return res;
    }

    /**
     * Append information about provided capabilities.
     *
     * @param sb Output buffer.
     * @param nameSpace The name space that we present capabilities for.
     * @param wiring The wiring that we present capabilities for.
     */
    private void appendProvidedCapabilities(final StringBuilder sb,
                                            final String nameSpace,
                                            final BundleWiring wiring)
    {
      WireFormatter wf;

      if (BundleRevision.PACKAGE_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterPackage(nameSpace, wiring);
      } else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterEE(nameSpace, wiring);
      } else if (BundleRevision.HOST_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterHost(nameSpace, wiring);
      } else if (BundleRevision.BUNDLE_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterBundle(nameSpace, wiring);
      } else if (IdentityNamespace.IDENTITY_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterID(nameSpace, wiring);
      } else {
        wf = new WireFormatter(nameSpace, wiring);
      }

      // Mapping from capability title to (string) to requesters (strings)
      final Map<String,List<String>> cap2requesters
        = new TreeMap<String, List<String>>();
      wf.providedCapabilitiesView(cap2requesters);
      appendCapabilityInfo(sb, cap2requesters);
    }

    /**
     * Append information about required capabilities.
     *
     * @param sb Output buffer.
     * @param nameSpace The name-space that we present requirements for.
     * @param wiring The wiring that we present requirements for.
     */
    private void appendRequiredCapabilities(final StringBuilder sb,
                                            final String nameSpace,
                                            final BundleWiring wiring)
    {
      WireFormatter wf;

      if (BundleRevision.PACKAGE_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterPackage(nameSpace, wiring);
      } else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterEE(nameSpace, wiring);
      } else if (BundleRevision.HOST_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterHost(nameSpace, wiring);
      } else if (BundleRevision.BUNDLE_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterBundle(nameSpace, wiring);
      } else if (IdentityNamespace.IDENTITY_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterID(nameSpace, wiring);
      } else {
        wf = new WireFormatter(nameSpace, wiring);
      }

      // Mapping from capability title to (string) to provider (strings)
      final Map<String,List<String>> cap2providers
        = new TreeMap<String, List<String>>();
      wf.requiredCapabilitiesView(cap2providers);
      appendCapabilityInfo(sb, cap2providers);
    }


    /**
     * Appends data for provided or required capabilities to the output buffer.
     * @param sb The output buffer.
     * @param capInfos Map with the pre-formated capability information to output.
     */
    private void appendCapabilityInfo(StringBuilder sb,
                                      final Map<String, List<String>> capInfos)
    {
      for (final Entry<String,List<String>> capInfo : capInfos.entrySet()) {
        sb.append("&nbsp;&nbsp;<font color=\"#444444\">");
        sb.append(capInfo.getKey());
        sb.append("</font><br>");
        for (final String user : capInfo.getValue()) {
          sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
          sb.append(user);
          sb.append("<br>");
        }
      }
    }

  }

  static private class WireFormatter {
    final String nameSpace;
    final BundleWiring wiring;


    /**
     * Get the match value for an attribute from a simple filter.
     *
     * @param filter
     *          the filter to analyze
     * @param attribute
     *          the key of the attribute to fetch the desired value of.
     * @return the desired value of the given attribute, "" if the attribute is
     *         not present in the filter or {@code null} if the filter is too
     *         complex.
     */
    static String getFilterValue(final String filter, final String attribute)
    {
      int start = filter.indexOf(attribute);
      if (start == -1) {
        return "";
      }
      start += attribute.length();
      while (start < filter.length()
             && Character.isWhitespace(filter.charAt(start))) {
        ++start;
      }
      if (filter.charAt(start++) != '=') {
        return null;
      }

      final int end = filter.indexOf(')', start);
      if (end == -1) {
        return null;
      }

      final String value = filter.substring(start, end).trim();

      start = filter.indexOf(attribute, end);

      return start == -1 ? value : (String) null;
    }


    /**
     * Creates a wire formatter for a name-space.
     * @param nameSpace The name-space to present info for.
     * @param wiring The wiring to present info for.
     */
    WireFormatter(final String nameSpace,
                  final BundleWiring wiring)
    {
      this.wiring = wiring;
      this.nameSpace = nameSpace;
    }

    static void appendPendingRemovalOnRefresh(final StringBuilder sb,
                                              final BundleWiring wiring)
    {
      if (wiring!=null && !wiring.isCurrent()) {
        sb.append("&nbsp;<i>pending removal on ");
        new WiringUrl(wiring.getBundle()).refreshLink(sb, "refresh");
        sb.append("</i>");
      }
    }

    /**
     * Get a HTML-formated presentation string for the capability described by
     * the given {@code capability}.
     * @param capability capability to be named.
     * @return
     */
    String getCapName(final BundleCapability capability)
    {
      final StringBuilder sb = new StringBuilder(50);
      sb.append(capability.getAttributes());

      final BundleWiring capWiring = capability.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, capWiring);

      return sb.toString();
    }

    /**
     * Present a generic requirement by showing its filter.
     *
     * @param requirement The requirement to present.
     * @return string presentation of the requirement.
     */
    String getReqName(BundleRequirement requirement)
    {
      final StringBuilder sb = new StringBuilder(50);

      final String filter = requirement.getDirectives().get("filter");
      if (filter != null) {
        sb.append(filter);
      } else {
        sb.append("&nbsp;&emdash;&nbsp;");
      }

      final BundleWiring reqWiring = requirement.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, reqWiring);

      return sb.toString();
    }

    /**
     * Get a HTML-formated presentation string for the owner of a wiring.
     * E.g., for the provide or requester of a capability.
     *
     * @param bw the bundle wiring to present.
     *
     * @return bundle name as a bundle selection link.
     */
    String getWiringName(BundleWiring bw) {
      return getWiringName(bw, true);
    }

    /**
     * Get a HTML-formated presentation string for the owner of a wiring.
     * E.g., for the provide or requester of a capability.
     *
     * @param bw the bundle wiring to present.
     * @param link if true return a bundle selection link.
     * @return bundle name as a bundle selection link.
     */
    String getWiringName(BundleWiring bw, boolean link)
    {
      if (bw == null) {
        return "&mdash;?&mdash;";
      }
      final BundleRevision br = bw.getRevision();
      final Bundle b = br.getBundle();
      if (link) {
        final StringBuilder sb = new StringBuilder(50);
        Util.bundleLink(sb, b);
        return sb.toString();
      }
      return Util.getBundleName(b);
    }

    /**
     * Group wires related to the same capability and build a map with the title
     * for the capability as the key and a list with one entry for each wired
     * requester of the capability as the value.
     *
     * @param cap2requesters map to return the result in.
     */
    final void providedCapabilitiesView(Map<String, List<String>> cap2requesters)
    {
      // The capabilities provided by the wiring we are handling.
      final List<BundleCapability> caps = new ArrayList<BundleCapability>();

      // All declared capabilities
      for (final BundleCapability cap : wiring.getRevision().getDeclaredCapabilities(nameSpace)) {
        caps.add(cap);
      }

      // All active capabilities (additional caps may come from attached fragments)
      for (final BundleCapability cap : wiring.getCapabilities(nameSpace)) {
        final int i = caps.indexOf(cap);
        if (-1 == i) {
          caps.add(cap);
        }
      }

      // reqs[i] holds a list with the wired requirer, if any, for caps[i].
      final List<List<BundleRequirement>> reqs =
        new ArrayList<List<BundleRequirement>>(caps.size());
      for (int i = 0; i < caps.size(); i++) {
        reqs.add(new ArrayList<BundleRequirement>());
      }

      // Add requirer for wired capabilities.
      for (final BundleWire w : wiring.getProvidedWires(nameSpace)) {
        final BundleCapability cap = w.getCapability();
        final int i = caps.indexOf(cap);
        if (-1 == i) {
          System.err
              .println("Found wire from bundle capability that is not part"
                       + " of the capabilities in the bundle wiring: " + cap);
          continue;
        }
        reqs.get(i).add(w.getRequirement());
      }

      // Add the current wiring as requester when the bundle itself uses the
      // capability since wires to itself is not included in
      // wiring.getProvidedWires().
      for (final BundleWire w : wiring.getRequiredWires(nameSpace)) {
        final BundleRequirement req = w.getRequirement();
        final BundleCapability  cap = w.getCapability();
        if (req.getRevision().equals(wiring.getRevision())
            && cap.getRevision().equals(wiring.getRevision())) {
          final int i = caps.indexOf(cap);
          if (-1 == i) {
            System.err
                .println("Found wire to self from bundle capability that "
                         + "is not part of the capabilities in the "
                         + "bundle wiring: " + cap);
            continue;
          }
          reqs.get(i).add(w.getRequirement());
        }
      }

      // Populate cap2requesters
      final StringBuilder sb = new StringBuilder(50);
      for (int i = 0; i < caps.size(); i++) {
        final BundleCapability cap = caps.get(i);
        final List<BundleRequirement> requesterReqs = reqs.get(i);

        final String capName = getCapName(cap);
        List<String> requesters = cap2requesters.get(capName);
        if (requesters == null) {
          requesters = new ArrayList<String>();
          cap2requesters.put(capName, requesters);
        }
        if (requesterReqs.isEmpty()) {
          requesters.add("&mdash");
        } else {
          for (final BundleRequirement req : requesterReqs) {
            sb.setLength(0);
            final BundleWiring bw = req.getRevision().getWiring();
            if (bw==null) {
              sb.append(Util.getBundleName(req.getRevision().getBundle()));
            } else {
              sb.append(getWiringName(bw, !wiring.equals(bw)));
            }
            sb.append("&nbsp;&nbsp;&mdash;&nbsp;&nbsp;<font size=\"-2\" color=\"#666666\">");
            sb.append(getReqName(req));
            sb.append("</font>");
            requesters.add(sb.toString());
          }
        }
      }
    }


    final void requiredCapabilitiesView(Map<String, List<String>> cap2providers)
    {
      // The requirements of the wiring we are handling.
      final List<BundleRequirement> reqs = new ArrayList<BundleRequirement>();

      // All declared requirements
      for (final BundleRequirement req : wiring.getRevision()
          .getDeclaredRequirements(nameSpace)) {
        reqs.add(req);
      }

      // All active requirements
      for (final BundleRequirement req : wiring.getRequirements(nameSpace)) {
        final int i = reqs.indexOf(req);
        if (-1 == i) {
          reqs.add(req);
        }
      }

      // caps[i] holds a list with the wired capabilities, if any, for reqs[i].
      final List<List<BundleCapability>> caps =
        new ArrayList<List<BundleCapability>>(reqs.size());
      for (int i = 0; i < reqs.size(); i++) {
        caps.add(new ArrayList<BundleCapability>());
      }

      // Add provider for wired requirements.
      for (final BundleWire w : wiring.getRequiredWires(nameSpace)) {
        final BundleRequirement req = w.getRequirement();
        final int i = reqs.indexOf(req);
        if (-1 == i) {
          System.err.println("Found wire to bundle requirement that is not part"
                             +" of the requirements in the bundle wiring: "
                             + req);
          continue;
        }
        caps.get(i).add(w.getCapability());
      }

      // Populate cap2providers
      final StringBuilder sb = new StringBuilder(50);
      for (int i = 0; i < reqs.size(); i++) {
        final BundleRequirement req = reqs.get(i);
        final List<BundleCapability> providerCaps = caps.get(i);

        final String capName = getReqName(req);
        List<String> providers = cap2providers.get(capName);
        if (providers == null) {
          providers = new ArrayList<String>();
          cap2providers.put(capName, providers);
        }
        if (providerCaps.isEmpty()) {
          providers.add("&mdash");
        } else {
          for (final BundleCapability cap : providerCaps) {
            sb.setLength(0);
            final BundleWiring bw = cap.getRevision().getWiring();
            sb.append(getWiringName(bw));
            sb.append("&nbsp;&nbsp;&mdash;&nbsp;&nbsp;<font size=\"-2\" color=\"#666666\">");
            sb.append(getCapName(cap));
            sb.append("</font>");
            providers.add(sb.toString());
          }
        }
      }
    }
  }


  static private class WireFormatterEE extends WireFormatter
  {

    /**
     * @param nameSpace
     *          The name-space to present info for.
     * @param wiring
     *          The wiring to present info for.
     */
    WireFormatterEE(final String nameSpace,
                    final BundleWiring wiring)
    {
      super(nameSpace, wiring);
    }

    private void appendVersion(final StringBuilder sb,
                               final BundleRequirement requirement)
    {
      final String filter =
        requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);

      try {
        final VersionRange vr =
          new VersionRange(filter, Constants.VERSION_ATTRIBUTE, true);
        sb.append("&nbsp;");
        sb.append(vr.toHtmlString());
      } catch (final IllegalArgumentException iae) {
        System.err.println(iae.getMessage());
      }
    }

    @Override
    String getCapName(final BundleCapability capability)
    {
      // Make a modifiable clone of the attributes.
      final Map<String, Object> attrs
        = new HashMap<String, Object>(capability.getAttributes());

      final StringBuilder sb = new StringBuilder(50);
      sb.append(attrs.remove(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE));

      @SuppressWarnings("unchecked")
      final List<Version> versions = (List<Version>) attrs
          .remove(Constants.VERSION_ATTRIBUTE);
      if (versions!=null) {
        sb.append("&nbsp;");
        sb.append(versions);
      }

      if (!attrs.isEmpty()) {
        sb.append("&nbsp;");
        sb.append(attrs);
      }

      final BundleWiring capWiring = capability.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, capWiring);

      return sb.toString();
    }

    @Override
    String getReqName(final BundleRequirement requirement)
    {
      final StringBuilder sb = new StringBuilder(50);
      final String filter = requirement.getDirectives().get("filter");
      final String eeName = getFilterValue(filter, ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);

      if (eeName != null) {
        sb.append(eeName);
        appendVersion(sb, requirement);
      } else {
        // Filter too complex to extract info from...
        sb.append(filter);
      }

      final BundleWiring reqWiring = requirement.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, reqWiring);

      return sb.toString();
    }
  }

  static private class WireFormatterHost extends WireFormatter
  {

    /**
     * @param nameSpace
     *          The name-space to present info for.
     * @param wiring
     *          The wiring to present info for.
     */
    WireFormatterHost(final String nameSpace,
                    final BundleWiring wiring)
    {
      super(nameSpace, wiring);
    }

    private void appendVersion(final StringBuilder sb,
                               final BundleRequirement requirement)
    {
      final String filter =
        requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);

      try {
        final VersionRange vr =
          new VersionRange(filter, Constants.BUNDLE_VERSION_ATTRIBUTE, true);
        sb.append("&nbsp;");
        sb.append(vr.toHtmlString());
      } catch (final IllegalArgumentException iae) {
        System.err.println(iae.getMessage());
      }
    }

    @Override
    String getCapName(final BundleCapability capability)
    {
      // Make a modifiable clone of the attributes.
      final Map<String, Object> attrs
        = new HashMap<String, Object>(capability.getAttributes());

      final StringBuilder sb = new StringBuilder(50);
      sb.append(attrs.remove(BundleRevision.HOST_NAMESPACE));

      final Version version =
        (Version) attrs.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
      if (version != null) {
        sb.append("&nbsp;");
        sb.append(version);
      }

      if (!attrs.isEmpty()) {
        sb.append("&nbsp;");
        sb.append(attrs);
      }

      final BundleWiring capWiring = capability.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, capWiring);

      return sb.toString();
    }

    @Override
    String getReqName(final BundleRequirement requirement)
    {
      final StringBuilder sb = new StringBuilder(50);
      final String filter = requirement.getDirectives().get("filter");
      final String hostName = getFilterValue(filter, BundleRevision.HOST_NAMESPACE);
      if (hostName != null) {
        sb.append(hostName);
        appendVersion(sb, requirement);
      } else {
        // Filter too complex to extract info from...
        sb.append(filter);
      }

      final BundleWiring reqWiring = requirement.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, reqWiring);

      return sb.toString();
    }

  }


  static private class WireFormatterID extends WireFormatter
  {

    /**
     * @param nameSpace
     *          The name-space to present info for.
     * @param wiring
     *          The wiring to present info for.
     */
    WireFormatterID(final String nameSpace, final BundleWiring wiring)
    {
      super(nameSpace, wiring);
    }

    private void appendVersionAndType(final StringBuilder sb,
                                      final BundleRequirement requirement)
    {
      final String filter =
        requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);

      try {
        final VersionRange vr =
          new VersionRange(filter, Constants.VERSION_ATTRIBUTE, false);
        sb.append("&nbsp;");
        sb.append(vr.toHtmlString());
      } catch (final IllegalArgumentException iae) {
      }

      final String type = getFilterValue(filter, "type");
      if (type != null) {
        sb.append("&nbsp;");
        sb.append(type);
      }
    }

    @Override
    String getCapName(final BundleCapability capability)
    {
      // Make a modifiable clone of the attributes.
      final Map<String, Object> attrs
        = new TreeMap<String, Object>(capability.getAttributes());

      final StringBuilder sb = new StringBuilder(50);
      sb.append(attrs.remove(IdentityNamespace.IDENTITY_NAMESPACE));

      final Version version =
          (Version) attrs.remove(Constants.VERSION_ATTRIBUTE);
        if (version != null) {
          sb.append("&nbsp;");
          sb.append(version);
        }

      final String type = (String) attrs.remove("type");
      if (type != null) {
        sb.append("&nbsp;");
        sb.append(type);
      }

      if (!attrs.isEmpty()) {
        sb.append("&nbsp;{");
        boolean first = true;
        for (final Entry<String,Object> entry :attrs.entrySet()) {
          if (first) {
            first = false;
          } else {
            sb.append(",&nbsp;");
          }
          sb.append(entry.getKey());
          sb.append('=');
          String value = entry.getValue().toString();
          value = Strings.replace(value, "<", "&lt;");
          value = Strings.replace(value, ">", "&gt;");
          value = WiringHTMLDisplayer.makeLink(value);
          sb.append(value);
        }
        sb.append('}');
      }

      final BundleWiring capWiring = capability.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, capWiring);

      return sb.toString();
    }

    @Override
    String getReqName(final BundleRequirement requirement)
    {
      final StringBuilder sb = new StringBuilder(50);
      final String filter = requirement.getDirectives().get("filter");
      final String idName = getFilterValue(filter, IdentityNamespace.IDENTITY_NAMESPACE);
      if (idName != null) {
        sb.append(idName);
        appendVersionAndType(sb, requirement);
      } else {
        // Filter too complex to extract info from...
        sb.append(filter);
      }

      final BundleWiring reqWiring = requirement.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, reqWiring);

      return sb.toString();
    }

  }


  static private class WireFormatterBundle
    extends WireFormatter
  {

    /**
     * @param nameSpace
     *          The name-space to present info for.
     * @param wiring
     *          The wiring to present info for.
     */
    WireFormatterBundle(final String nameSpace, final BundleWiring wiring)
    {
      super(nameSpace, wiring);
    }

    private void appendVersion(final StringBuilder sb,
                               final BundleRequirement requirement)
    {
      final String filter =
        requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);

      try {
        final VersionRange vr =
          new VersionRange(filter, Constants.BUNDLE_VERSION_ATTRIBUTE, true);
        sb.append("&nbsp;");
        sb.append(vr.toHtmlString());
      } catch (final IllegalArgumentException iae) {
        System.err.println(iae.getMessage());
      }
    }

    @Override
    String getCapName(final BundleCapability capability)
    {
      // Make a modifiable clone of the attributes.
      final Map<String, Object> attrs =
        new HashMap<String, Object>(capability.getAttributes());

      final StringBuilder sb = new StringBuilder(50);
      sb.append(attrs.remove(BundleRevision.BUNDLE_NAMESPACE));

      final Version version =
        (Version) attrs.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
      if (version != null) {
        sb.append("&nbsp;");
        sb.append(version);
      }

      if (!attrs.isEmpty()) {
        sb.append("&nbsp;");
        sb.append(attrs);
      }

      final BundleWiring capWiring = capability.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, capWiring);

      return sb.toString();
    }

    @Override
    String getReqName(final BundleRequirement requirement)
    {
      final StringBuilder sb = new StringBuilder(50);
      final String filter = requirement.getDirectives().get("filter");
      final String bundleName =
        getFilterValue(filter, BundleRevision.BUNDLE_NAMESPACE);
      if (bundleName != null) {
        sb.append(bundleName);
        appendVersion(sb, requirement);
      } else {
        // Filter too complex to extract info from...
        sb.append(filter);
      }

      final BundleWiring reqWiring = requirement.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, reqWiring);

      return sb.toString();
    }

  }

  static private class WireFormatterPackage extends WireFormatter
  {

    /**
     * @param nameSpace
     *          The name-space to present info for.
     * @param wiring
     *          The wiring to present info for.
     */
    WireFormatterPackage(final String nameSpace,
                         final BundleWiring wiring)
    {
      super(nameSpace, wiring);
    }

    private void appendVersionAndResolutionDirective(final StringBuilder sb,
                                                     final BundleRequirement requirement)
    {
      final String filter =
        requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);

      try {
        final VersionRange vr =
          new VersionRange(filter, Constants.VERSION_ATTRIBUTE, true);
        sb.append("&nbsp;");
        sb.append(vr.toHtmlString());
      } catch (final IllegalArgumentException iae) {
        System.err.println(iae.getMessage());
      }

      final String resolution =
        requirement.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
      if (resolution != null && resolution.length() > 0) {
        if (resolution.equals(Constants.RESOLUTION_MANDATORY)) {
          // Default, don't print
        } else {
          sb.append("&nbsp;");
          sb.append(resolution);
        }
      }
    }

    @Override
    String getCapName(final BundleCapability capability)
    {
      final StringBuilder sb = new StringBuilder(50);

      // Make a modifiable clone of the capability attributes.
      final Map<String, Object> attrs
        = new HashMap<String, Object>(capability.getAttributes());

      sb.append(attrs.remove(BundleRevision.PACKAGE_NAMESPACE));

      final Version version = (Version) attrs
          .remove(Constants.VERSION_ATTRIBUTE);
      if (version!=null) {
        sb.append("&nbsp;");
        sb.append(version);
      }

      attrs.remove(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
      attrs.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
      if (!attrs.isEmpty()) {
        sb.append("&nbsp;");
        sb.append(attrs);
      }

      final BundleWiring capWiring = capability.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, capWiring);

      return sb.toString();
    }

    @Override
    String getReqName(final BundleRequirement requirement)
    {
      final StringBuilder sb = new StringBuilder(50);
      final String filter = requirement.getDirectives().get("filter");
      final String pkgName =
        getFilterValue(filter, BundleRevision.PACKAGE_NAMESPACE);
      if (pkgName != null) {
        sb.append(pkgName);
        appendVersionAndResolutionDirective(sb, requirement);
      } else {
        sb.append(filter);
      }

      final BundleWiring reqWiring = requirement.getRevision().getWiring();
      appendPendingRemovalOnRefresh(sb, reqWiring);

      return sb.toString();
    }

  }


}
