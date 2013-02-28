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
        sb.append("<b>Provided Wires</b><br>");

        groupWiresOnNameSpace(nswMap, pws);

        for (final Entry<String, List<BundleWire>> entry : nswMap.entrySet()) {
          final String nameSpace = entry.getKey();
          sb.append("<em>");
          sb.append(nameSpace);
          sb.append("</em>:<br>");

          if (BundleRevision.PACKAGE_NAMESPACE.equals(nameSpace)) {
            appendExportPackages(sb, entry.getValue());
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
        sb.append("<b>Required Wires</b><br>");

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
      for (final BundleWire w : wires) {
        final BundleCapability cap = w.getCapability();

        sb.append("&nbsp;&nbsp;");
        sb.append(cap.getAttributes());
        sb.append("<br>");

        final BundleWiring rbw = w.getRequirerWiring();
        final BundleRevision rbr = rbw.getRevision();
        final Bundle rb = rbr.getBundle();
        sb.append("&nbsp;&nbsp;");
        Util.bundleLink(sb, rb);
        sb.append("<br>");
      }
    }

    private void appendProvidedCapabilitiesEE(StringBuffer sb,
                                              List<BundleWire> wires)
    {
      // Mapping from provided EE name and version string to requester strings
      final Map<String,List<String>> ees = new TreeMap<String, List<String>>();
      final StringBuffer sb1 = new StringBuffer(50);
      for (final BundleWire w : wires) {
        final BundleCapability cap = w.getCapability();
        final Map<String,Object> attrs
          = new TreeMap<String, Object>(cap.getAttributes());
        final String eeName = (String) attrs.remove("osgi.ee");
        @SuppressWarnings("unchecked")
        final List<Version> versions = (List<Version>) attrs.remove(Constants.VERSION_ATTRIBUTE);

        sb1.setLength(0);
        appendFormatedPackage(sb1, eeName, versions.toString(), attrs);
        final String key = sb1.toString();

        sb1.setLength(0);
        final BundleWiring rbw = w.getRequirerWiring();
        final BundleRevision rbr = rbw.getRevision();
        final Bundle rb = rbr.getBundle();
        Util.bundleLink(sb1, rb);

        List<String> requireingBundles = ees.get(key);
        if (requireingBundles==null) {
          requireingBundles = new ArrayList<String>();
          ees.put(key, requireingBundles);
        }
        requireingBundles.add(sb1.toString());
      }

      appendPackages(sb, ees);
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
      for (final BundleWire w : wires) {
        final BundleCapability cap = w.getCapability();

        sb.append("&nbsp;&nbsp;");
        sb.append(cap.getAttributes());
        sb.append("<br>");

        final BundleWiring pbw = w.getProviderWiring();
        final BundleRevision pbr = pbw.getRevision();
        final Bundle pb = pbr.getBundle();
        sb.append("&nbsp;&nbsp;");
        Util.bundleLink(sb, pb);
        sb.append("<br>");
      }
    }

    private void appendRequiredCapabilitiesEE(StringBuffer sb,
                                              List<BundleWire> wires)
    {
      // Mapping from required EE name and version string to provided EE strings
      final Map<String,List<String>> reqEEs = new TreeMap<String, List<String>>();
      final StringBuffer sb1 = new StringBuffer(50);
      for (final BundleWire w : wires) {
        final BundleCapability cap = w.getCapability();
        final Map<String,Object> attrs
          = new TreeMap<String, Object>(cap.getAttributes());
        final String eeName = (String) attrs.remove("osgi.ee");
        @SuppressWarnings("unchecked")
        final List<Version> versions = (List<Version>) attrs.remove(Constants.VERSION_ATTRIBUTE);

        sb1.setLength(0);
        appendFormatedPackage(sb1, eeName, versions.toString(), attrs);
        final String expKey = sb1.toString();

        sb1.setLength(0);
        final BundleWiring pbw = w.getProviderWiring();
        final BundleRevision pbr = pbw.getRevision();
        final Bundle pb = pbr.getBundle();
        Util.bundleLink(sb1, pb);

        List<String> exporters = reqEEs.get(expKey);
        if (exporters==null) {
          exporters = new ArrayList<String>();
          reqEEs.put(expKey, exporters);
        }
        exporters.add(sb1.toString());
      }

      appendPackages(sb, reqEEs);
    }

    /**
     * Append information about the provider side of the given wires.
     *
     * @param sb Output buffer.
     * @param wires The set of wires to present.
     */
    private void appendExportPackages(StringBuffer sb, List<BundleWire> wires)
    {
      // Mapping from exported package name and version string to importer strings
      final Map<String,List<String>> exports = new TreeMap<String, List<String>>();
      final StringBuffer sb1 = new StringBuffer(50);
      for (final BundleWire w : wires) {
        final BundleCapability cap = w.getCapability();
        final Map<String,Object> attrs
          = new TreeMap<String, Object>(cap.getAttributes());
        final String pkg = (String) attrs.remove(BundleRevision.PACKAGE_NAMESPACE);
        final Version version = (Version) attrs.remove(Constants.VERSION_ATTRIBUTE);
        attrs.remove(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        attrs.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);

        sb1.setLength(0);
        appendFormatedPackage(sb1, pkg, version.toString(), attrs);
        final String expKey = sb1.toString();

        sb1.setLength(0);
        final BundleWiring rbw = w.getRequirerWiring();
        final BundleRevision rbr = rbw.getRevision();
        final Bundle rb = rbr.getBundle();
        Util.bundleLink(sb1, rb);

        List<String> importers = exports.get(expKey);
        if (importers==null) {
          importers = new ArrayList<String>();
          exports.put(expKey, importers);
        }
        importers.add(sb1.toString());
      }

      appendPackages(sb, exports);
    }

    /**
     * Append information about the requirement side of the given wires.
     *
     * @param sb Output buffer.
     * @param wires The set of wires to present.
     */
    private void appendImportPackages(StringBuffer sb, List<BundleWire> wires)
    {
      // Mapping from imported package name and version string to exporter strings
      final Map<String,List<String>> imports = new TreeMap<String, List<String>>();
      final StringBuffer sb1 = new StringBuffer(50);
      for (final BundleWire w : wires) {
        final BundleCapability cap = w.getCapability();
        final Map<String,Object> attrs
          = new TreeMap<String, Object>(cap.getAttributes());
        final String pkg = (String) attrs.remove(BundleRevision.PACKAGE_NAMESPACE);
        final Version version = (Version) attrs.remove(Constants.VERSION_ATTRIBUTE);
        attrs.remove(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        attrs.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);

        sb1.setLength(0);
        appendFormatedPackage(sb1, pkg, version.toString(), attrs);
        final String expKey = sb1.toString();

        sb1.setLength(0);
        final BundleWiring pbw = w.getProviderWiring();
        final BundleRevision pbr = pbw.getRevision();
        final Bundle pb = pbr.getBundle();
        Util.bundleLink(sb1, pb);

        List<String> exporters = imports.get(expKey);
        if (exporters==null) {
          exporters = new ArrayList<String>();
          imports.put(expKey, exporters);
        }
        exporters.add(sb1.toString());
      }

      appendPackages(sb, imports);
    }

    /**
     * Appends a formated string for the given package to the output buffer.
     * @param sb Output buffer.
     * @param pkg Name of the package.
     * @param version Version of the package.
     * @param attrs Other attributes for the package.
     */
    private void appendFormatedPackage(StringBuffer sb,
                                       String pkg,
                                       String version,
                                       Map<String, Object> attrs)
    {
      sb.append("<font color=\"#444444\">");
      sb.append(pkg);
      sb.append("&nbsp;");
      sb.append(version);
      if (attrs != null && !attrs.isEmpty()) {
        sb.append("&nbsp;");
        sb.append(attrs);
      }
      sb.append("</font>");
    }

  }


  /**
   * Appends data for provided or required packages to the output buffer.
   * @param sb The output buffer.
   * @param pkgInfos Map with the pre-formated package information to output.
   */
  private void appendPackages(StringBuffer sb,
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
