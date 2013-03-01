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
import java.util.TreeMap;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
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

    for(final JComponent jcomp : components) {
      final JHTML comp = (JHTML) jcomp;
      comp.valueChanged(bl);
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

      // Group wires on name-space
      final Map<String,List<BundleWire>> nswMap
        = new HashMap<String,List<BundleWire>>();

      final List<BundleWire> pws = bw.getProvidedWires(null);
      if (pws != null) { // is null when bundle wiring not in use!
        sb.append("<b>Provided Capabilities and Wired Requireing Bundles</b><br>");

        groupWiresOnNameSpace(nswMap, pws);

        for (final Entry<String, List<BundleWire>> entry : nswMap.entrySet()) {
          final String nameSpace = entry.getKey();
          sb.append("<em>");
          sb.append(nameSpace);
          sb.append("</em>:<br>");

          if (BundleRevision.PACKAGE_NAMESPACE.equals(nameSpace)) {
            appendProvidedCapabilitesPackage(sb, entry.getValue());
          } else if ("osgi.ee".equals(nameSpace)) {
            appendProvidedCapabilitiesEE(sb, entry.getValue());
          } else {
            appendProvidedCapabilityGeneric(sb, entry.getValue());
          }
        }
        if (useParagraph) {
          sb.append("</p>");
        }
      }

      final List<BundleWire> rws = bw.getRequiredWires(null);
      if (rws != null) { // is null when bundle wiring not in use!
        sb.append("<b>Required Capabilities and Wired Providing Bundles</b><br>");

        nswMap.clear();
        groupWiresOnNameSpace(nswMap, rws);

        for (final Entry<String, List<BundleWire>> entry : nswMap.entrySet()) {
          final String nameSpace = entry.getKey();
          sb.append("<em>");
          sb.append(nameSpace);
          sb.append("</em>:<br>");

          if (BundleRevision.PACKAGE_NAMESPACE.equals(nameSpace)) {
            appendImportPackages(sb, entry.getValue());
          } else if ("osgi.ee".equals(nameSpace)) {
            appendRequiredCapabilitiesEE(sb, entry.getValue());
          } else {
            appendRequiredCapabilityGeneric(sb, entry.getValue());
          }
        }

        if (useParagraph) {
          sb.append("</p>");
        }
      }

      return sb;
    }

    private void groupWiresOnNameSpace(final Map<String, List<BundleWire>> nswMap,
                                       final List<BundleWire> pws)
    {
      for (final BundleWire w : pws) {
        List<BundleWire> ws = nswMap.get(w.getCapability().getNamespace());
        if (ws==null) {
          ws = new ArrayList<BundleWire>();
          nswMap.put(w.getCapability().getNamespace(), ws);
        }
        ws.add(w);
      }
    }

    /**
     * Append information about the provider side of the given wires.
     *
     * @param sb Output buffer.
     * @param wires The set of wires to present.
     */
    private void appendProvidedCapabilityGeneric(final StringBuffer sb,
                                                 final List<BundleWire> wires)
    {
      // Mapping from capability title to (string) to requesters (strings)
      final Map<String,List<String>> cap2requesters
        = new TreeMap<String, List<String>>();

      final WireFormatter wf = new WireFormatter(wires);
      wf.providedCapabilitiesView(cap2requesters);
      appendCapabilityInfo(sb, cap2requesters);
    }

    private void appendProvidedCapabilitiesEE(StringBuffer sb,
                                              List<BundleWire> wires)
    {
      // Mapping from capability title to (string) to requesters (strings)
      final Map<String,List<String>> cap2requesters
        = new TreeMap<String, List<String>>();

      final WireFormatter wf = new WireFormatterEE(wires);
      wf.providedCapabilitiesView(cap2requesters);
      appendCapabilityInfo(sb, cap2requesters);
    }

    /**
     * Append information about the provider side of the given wires
     * representing exported packages.
     *
     * @param sb Output buffer.
     * @param wires The set of package wires to present.
     */
    private void appendProvidedCapabilitesPackage(StringBuffer sb, List<BundleWire> wires)
    {
      // Mapping from capability title to (string) to requesters (strings)
      final Map<String,List<String>> cap2requesters
        = new TreeMap<String, List<String>>();

      final WireFormatter wf = new WireFormatterPackage(wires);
      wf.providedCapabilitiesView(cap2requesters);
      appendCapabilityInfo(sb, cap2requesters);
    }

    /**
     * Append information about the requirement side of the given wires.
     *
     * @param sb Output buffer.
     * @param wires The set of wires to present.
     */
    private void appendRequiredCapabilityGeneric(final StringBuffer sb,
                                                 final List<BundleWire> wires)
    {
      // Mapping from capability title to (string) to provider (strings)
      final Map<String,List<String>> cap2providers
        = new TreeMap<String, List<String>>();

      final WireFormatter wf = new WireFormatter(wires);
      wf.requiredCapabilitiesView(cap2providers);
      appendCapabilityInfo(sb, cap2providers);
    }

    private void appendRequiredCapabilitiesEE(StringBuffer sb,
                                              List<BundleWire> wires)
    {
      // Mapping from capability title to (string) to provider (strings)
      final Map<String,List<String>> cap2providers
        = new TreeMap<String, List<String>>();

      final WireFormatter wf = new WireFormatterEE(wires);
      wf.requiredCapabilitiesView(cap2providers);
      appendCapabilityInfo(sb, cap2providers);
    }

    /**
     * Append information about the requirement side of the given wires.
     *
     * @param sb Output buffer.
     * @param wires The set of wires to present.
     */
    private void appendImportPackages(StringBuffer sb, List<BundleWire> wires)
    {
      // Mapping from capability title to (string) to provider (strings)
      final Map<String,List<String>> cap2providers
        = new TreeMap<String, List<String>>();

      final WireFormatter wf = new WireFormatterPackage(wires);
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
    // All wires shall belong to the same name-space.
    final List<BundleWire> wires;

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

    /**
     * Creates a wire formatter for a name-space.
     * @param wires all the wires in the list must belong to capabilities in
     *              the same name-space.
     */
    WireFormatter(final List<BundleWire> wires)
    {
      this.wires = wires;
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
      for (final BundleWire w : wires) {
        final BundleCapability cap = w.getCapability();
        final Map<String,Object> attrs
          = new TreeMap<String, Object>(cap.getAttributes());

        final String capName = getCapName(w.getProviderWiring(), attrs);

        final BundleWiring bw = w.getRequirerWiring();
        final String wName = getWiringName(bw);

        List<String> requesters = cap2requesters.get(capName);
        if (requesters==null) {
          requesters = new ArrayList<String>();
          cap2requesters.put(capName, requesters);
        }
        requesters.add(wName);
      }
    }

    final void requiredCapabilitiesView(Map<String, List<String>> cap2providers)
    {
      for (final BundleWire w : wires) {
        final BundleCapability cap = w.getCapability();
        final Map<String,Object> attrs
          = new TreeMap<String, Object>(cap.getAttributes());

        final String capName = getCapName(w.getProviderWiring(), attrs);

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

    WireFormatterEE(final List<BundleWire> wires)
    {
      super(wires);
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

    WireFormatterPackage(final List<BundleWire> wires)
    {
      super(wires);
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
      appendFormatedPackage(sb, pkg, version.toString(), attrs, extra);

      return sb.toString();
    }

  }
}
