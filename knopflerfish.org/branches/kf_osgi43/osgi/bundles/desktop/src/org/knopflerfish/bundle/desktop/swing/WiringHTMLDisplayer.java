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

      if (bw != null && bw.isInUse()) {
        // Find all name-spaces that b provides capabilities in.
        final Set<String> providedNameSpaces = getNameSapcesOfProvidedCaps(bw);

        sb.append("<b>Provided Capabilities and Requiring Bundles</b><br>");

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
        sb.append("<b>Required Capabilities and Providing Bundles</b><br>");

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

      final List<BundleWire> pbws = bw.getProvidedWires(null);
      if (pbws != null) {
        for (final BundleWire wire : pbws) {
          res.add(wire.getCapability().getNamespace());
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

      final List<BundleWire> pbws = bw.getProvidedWires(null);
      if (pbws != null) {
        for (final BundleWire wire : pbws) {
          res.add(wire.getCapability().getNamespace());
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
      } else if ("osgi.ee".equals(nameSpace)) {
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
      } else if ("osgi.ee".equals(nameSpace)) {
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
     * @param pkgInfos Map with the pre-formated capability information to output.
     */
    private void appendCapabilityInfo(StringBuffer sb,
                                      final Map<String, List<String>> pkgInfos)
    {
      for (final Entry<String,List<String>> pkgInfo : pkgInfos.entrySet()) {
        sb.append("&nbsp;&nbsp;");
        sb.append(pkgInfo.getKey());
        sb.append("<br>");
        for (final String user : pkgInfo.getValue()) {
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
     * Appends a formated string for the given package to the output buffer.
     *
     * @param sb
     *          Output buffer.
     * @param pkg
     *          Name of the package.
     * @param version
     *          Version of the package.
     * @param attrs
     *          Other attributes for the package.
     */
    static void appendFormatedPackage(final StringBuffer sb,
                                      final String pkg,
                                      final String version,
                                      final Map<String, Object> attrs,
                                      final String extra)
    {
      sb.append("<font color=\"#444444\">");
      sb.append(pkg);
      sb.append("&nbsp;");
      sb.append(version);
      if (attrs != null && !attrs.isEmpty()) {
        sb.append("&nbsp;");
        sb.append(attrs);
      }
      if (extra!=null) {
        sb.append(extra);
      }
      sb.append("</font>");
    }

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
     * the given {@code attrs} map.
     * @param bundleWiring  the bundle wiring of the provider of this capability.
     * @param attrs capability attributes.
     * @return
     */
    String getCapName(BundleWiring bundleWiring, Map<String, Object> attrs)
    {
      final StringBuffer sb = new StringBuffer(50);
      sb.append(attrs);
      if (!bundleWiring.isCurrent()) {
        sb.append("&nbsp;<i>pending removal on refresh</i>");
      }

      return sb.toString();
    }

    String getReqName(BundleWiring wiring2, String filter)
    {
      return filter;
    }

    /**
     * Get a HTML-formated presentation string for the owner of a wiring.
     * E.g., for the provide or requester of a capability.
     *
     * @return bundle name as a bundle selection link.
     */
    String getWiringName(BundleWiring bw) {
      final BundleRevision br = bw.getRevision();
      final Bundle b = br.getBundle();
      final StringBuffer sb = new StringBuffer(50);
      Util.bundleLink(sb, b);
      return sb.toString();
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
        final Map<String,Object> attrs
        = new TreeMap<String, Object>(cap.getAttributes());

        final String capName = getCapName(wiring, attrs);
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
          final Map<String, Object> attrs
            = new TreeMap<String, Object>(cap.getAttributes());

          if (wiring.equals(w.getProviderWiring())) {
            // Only add requirer when the wiring we are presenting is the provider
            final String capName = getCapName(wiring, attrs);

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
            final Map<String,Object> attrs
            = new TreeMap<String, Object>(cap.getAttributes());

            final String capName = getCapName(wiring, attrs);
            List<String> requesters = cap2requesters.get(capName);
            if (requesters==null) {
              requesters = new ArrayList<String>();
              cap2requesters.put(capName, requesters);
            }
            requesters.add(getWiringName(wiring) +" direct (no wire)");
            break;
          }
        }
      }

    }


    final void requiredCapabilitiesView(Map<String, List<String>> cap2providers)
    {
      // All declared requirements
      for (final BundleRequirement req : wiring.getRevision()
          .getDeclaredRequirements(nameSpace)) {
        final String filter = req.getDirectives().get("filter");
        final String capName = getReqName(wiring, filter);

        List<String> providers = cap2providers.get(capName);
        if (providers == null) {
          providers = new ArrayList<String>();
          cap2providers.put(capName, providers);
        }
      }

      // All active requirements
      for (final BundleRequirement req : wiring.getRequirements(nameSpace)) {
        final String filter = req.getDirectives().get("filter");
        final String capName = getReqName(wiring, filter);

        List<String> providers = cap2providers.get(capName);
        if (providers == null) {
          providers = new ArrayList<String>();
          cap2providers.put(capName, providers);
        }
      }

      // Add provider for wired requirements, note this may add "stale"
      // requirements when there is a wire to an old generation of a provider.
      for (final BundleWire w : wiring.getRequiredWires(nameSpace)) {
        final BundleRequirement req = w.getRequirement();
        final String filter = req.getDirectives().get("filter");
        final String capName = getReqName(wiring, filter);

        final BundleWiring bw = w.getProviderWiring();
        final String wName = getWiringName(bw);

        List<String> providers = cap2providers.get(capName);
        if (providers==null) {
          providers = new ArrayList<String>();
          cap2providers.put(capName, providers);
        }
        providers.add(wName);
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
    String getCapName(BundleWiring bundleWiring, Map<String, Object> attrs)
    {
      final String eeName = (String) attrs.remove("osgi.ee");
      @SuppressWarnings("unchecked")
      final List<Version> versions = (List<Version>) attrs
          .remove(Constants.VERSION_ATTRIBUTE);

      final StringBuffer sb = new StringBuffer(50);
      final String extra = bundleWiring.isCurrent() ? (String) null
          : "&nbsp;<i>pending removal on refresh</i>";
      appendFormatedPackage(sb, eeName, versions.toString(), attrs, extra);

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

    @Override
    String getCapName(BundleWiring bundleWiring, Map<String, Object> attrs)
    {
      final String pkg = (String) attrs
          .remove(BundleRevision.PACKAGE_NAMESPACE);
      final Version version = (Version) attrs
          .remove(Constants.VERSION_ATTRIBUTE);
      attrs.remove(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
      attrs.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);

      final StringBuffer sb = new StringBuffer(50);
      final String extra = bundleWiring.isCurrent() ? (String) null
          : "&nbsp;<i>pending removal on refresh</i>";
      appendFormatedPackage(sb, pkg, version==null? "" :version.toString(), attrs, extra);

      return sb.toString();
    }

    @Override
    String getReqName(final BundleWiring wiring2, final String filter)
    {
      final String pkg = getFilterValue(filter, BundleRevision.PACKAGE_NAMESPACE);

      final StringBuffer sb = new StringBuffer(50);
      appendFormatedPackage(sb, pkg, "", null, null);

      return sb.toString();
    }

  }


}
