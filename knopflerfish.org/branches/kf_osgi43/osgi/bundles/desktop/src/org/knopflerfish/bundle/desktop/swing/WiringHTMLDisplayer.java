/*
 * Copyright (c) 2013, KNOPFLERFISH project All rights reserved.
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

import java.util.ArrayList;
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
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import org.knopflerfish.util.framework.VersionRange;



public class WiringHTMLDisplayer extends DefaultSwingBundleDisplayer {

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

  class JHTML extends JHTMLBundle {
    private static final long serialVersionUID = 1L;
    protected static final String OSGI_EE = "osgi.ee";

    JHTML(DefaultSwingBundleDisplayer displayer)
    {
      super(displayer);
    }

    @Override
    public StringBuffer bundleInfo(Bundle b)
    {
      final StringBuffer sb = new StringBuffer();
      final boolean useParagraph = true;


      startFont(sb);

      final BundleWiring bw = b.adapt(BundleWiring.class);

      if (useParagraph) {
        sb.append("<p>");
      }

      if (bw == null) {
        sb.append("<b>Unresolved bundle.</b><br>");
        // TODO add resolve button.
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
    private void appendProvidedCapabilities(final StringBuffer sb,
                                            final String nameSpace,
                                            final BundleWiring wiring)
    {
      WireFormatter wf;

      if (BundleRevision.PACKAGE_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterPackage(nameSpace, wiring);
      } else if ("JHTML.OSGI_EE".equals(nameSpace)) {
        wf = new WireFormatterEE(nameSpace, wiring);
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
    private void appendRequiredCapabilities(final StringBuffer sb,
                                            final String nameSpace,
                                            final BundleWiring wiring)
    {
      WireFormatter wf;

      if (BundleRevision.PACKAGE_NAMESPACE.equals(nameSpace)) {
        wf = new WireFormatterPackage(nameSpace, wiring);
      } else if ("JHTML.OSGI_EE".equals(nameSpace)) {
        wf = new WireFormatterEE(nameSpace, wiring);
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
    private void appendCapabilityInfo(StringBuffer sb,
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


    static String getFilterValue(final String filter,
                                 final String attribute)
    {
      int start = filter.indexOf(attribute);
      if (start == -1) {
        return "";
      }
      start += attribute.length();
      while (start<filter.length() && Character.isWhitespace(filter.charAt(start))) {
        ++start;
      }
      if (filter.charAt(start++) != '=') {
        return "";
      }

      final int end = filter.indexOf(')', start);
      if (end == -1) {
        return "";
      }

      final String value = filter.substring(start, end).trim();

      return value;
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

    /**
     * Get a HTML-formated presentation string for the capability described by
     * the given {@code capability}.
     * @param capability capability to be named.
     * @param requirement that is wired to the capability.
     * @return
     */
    String getCapName(final BundleCapability capability,
                      final BundleRequirement requirement)
    {
      final StringBuffer sb = new StringBuffer(50);
      sb.append(capability.getAttributes());

      final BundleWiring capWiring = capability.getRevision().getWiring();
      if (capWiring!=null && !capWiring.isCurrent()) {
        sb.append("&nbsp;<i>pending removal on refresh</i>");
      }

      return sb.toString();
    }

    String getReqName(BundleRequirement req)
    {
      return req.getDirectives().get("filter");
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
      final BundleRevision br = bw.getRevision();
      final Bundle b = br.getBundle();
      if (link) {
        final StringBuffer sb = new StringBuffer(50);
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
      // All declared capabilities
      for (final BundleCapability cap : wiring.getRevision().getDeclaredCapabilities(nameSpace)) {
        final String capName = getCapName(cap, null);
        List<String> requesters = cap2requesters.get(capName);
        if (requesters==null) {
          requesters = new ArrayList<String>();
          cap2requesters.put(capName, requesters);
        }
      }

      // Add requesters for wired capabilities, note this may add "stale"
      // capabilities when there is a wire to an old generation of the bundle.
      final List<BundleWire> wires = wiring.getProvidedWires(nameSpace);
      if (wires != null) {
        for (final BundleWire w : wires) {
          final BundleCapability cap = w.getCapability();

          if (wiring.equals(w.getProviderWiring())) {
            // Only add requirer when the wiring we are presenting is the provider
            final String capName = getCapName(cap, null);

            final BundleWiring bw = w.getRequirerWiring();
            final String wName = getWiringName(bw);

            List<String> requesters = cap2requesters.get(capName);
            if (requesters == null) {
              requesters = new ArrayList<String>();
              cap2requesters.put(capName, requesters);
            }
            requesters.add(wName);
          }
        }
      }

      // Add the current wiring as requester when the bundle itself uses the
      // capability since wires to itself must not be created in the wiring.
      for (final BundleCapability cap : wiring.getCapabilities(nameSpace)) {
        for (final BundleRequirement req : wiring.getRequirements(nameSpace)) {
          if (req.matches(cap)) {
            final String capName = getCapName(cap, null);
            List<String> requesters = cap2requesters.get(capName);
            if (requesters==null) {
              requesters = new ArrayList<String>();
              cap2requesters.put(capName, requesters);
            }
            requesters.add("(" + getWiringName(wiring,false) +")");
            break;
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
      for (int i = 0; i<reqs.size(); i++) {
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
      for (int i = 0; i < reqs.size(); i++) {
        final BundleRequirement req = reqs.get(i);
        final List<BundleCapability> providerCaps = caps.get(i);

        if (providerCaps.isEmpty()) {
          final String capName = getReqName(req);
          List<String> providers = cap2providers.get(capName);
          if (providers == null) {
            providers = new ArrayList<String>();
            cap2providers.put(capName, providers);
          }
        } else {
          for (final BundleCapability cap : providerCaps) {
            final String capName = getCapName(cap, req);

            List<String> providers = cap2providers.get(capName);
            if (providers == null) {
              providers = new ArrayList<String>();
              cap2providers.put(capName, providers);
            }
            final BundleWiring bw = cap.getRevision().getWiring();
            final String wName = getWiringName(bw);
            providers.add(wName);
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

    @Override
    String getCapName(final BundleCapability capability,
                      final BundleRequirement requirement)
    {
      // Make a modifiable clone of the attributes.
      final Map<String, Object> attrs
        = new HashMap<String, Object>(capability.getAttributes());

      final StringBuffer sb = new StringBuffer(50);
      sb.append(attrs.remove(JHTML.OSGI_EE));

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
      if (capWiring!=null && !capWiring.isCurrent()) {
        sb.append("&nbsp;<i>pending removal on refresh</i>");
      }

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

    private void appendVersionAndResolutionDirective(final StringBuffer sb,
                                                     final BundleRequirement requirement)
    {
      final String filter = requirement.getDirectives()
          .get(Constants.FILTER_DIRECTIVE);

      try {
        final VersionRange vr
          = new VersionRange(filter, Constants.VERSION_ATTRIBUTE);
        sb.append("&nbsp;<font size=\"-3\">(");
        sb.append(vr);
        sb.append(")</font>");
      } catch (final IllegalArgumentException iae) {
        System.err.println(iae.getMessage());
      }

      final String resolution = requirement.getDirectives()
          .get(Constants.RESOLUTION_DIRECTIVE);
      if (resolution != null && resolution.length() > 0) {
        if (resolution.equals(Constants.RESOLUTION_MANDATORY)) {
          // Default, don't print
        } else {
          sb.append("&nbsp;");
          sb.append(resolution);
          if (resolution.equals("dynamic")) {
            // print pattern
            sb.append("&nbsp;'<em>");
            sb.append(getFilterValue(filter, BundleRevision.PACKAGE_NAMESPACE));
            sb.append("</em>'");
          }
        }
      }
    }

    @Override
    String getCapName(final BundleCapability capability,
                      final BundleRequirement requirement)
    {
      final StringBuffer sb = new StringBuffer(50);

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
      if (capWiring!=null && !capWiring.isCurrent()) {
        sb.append("&nbsp;<i>pending removal on refresh</i>");
      }

      if (requirement!=null) {
        appendVersionAndResolutionDirective(sb, requirement);

//        sb.append("&nbsp;&nbsp;<font size=\"-2\">");
//        sb.append(requirement.getDirectives().get("filter"));
//        sb.append("</font>");
      }

      return sb.toString();
    }

    @Override
    String getReqName(final BundleRequirement requirement)
    {
      final StringBuffer sb = new StringBuffer(50);
      final String filter = requirement.getDirectives().get("filter");
      sb.append(getFilterValue(filter, BundleRevision.PACKAGE_NAMESPACE));

      appendVersionAndResolutionDirective(sb, requirement);

//      sb.append("&nbsp;&nbsp;<font size=\"-2\">");
//      sb.append(filter);
//      sb.append("</font>");

      return sb.toString();
    }

  }


}
