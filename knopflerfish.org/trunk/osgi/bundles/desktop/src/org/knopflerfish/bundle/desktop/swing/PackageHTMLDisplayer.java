/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project All rights reserved.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.util.tracker.ServiceTracker;



public class PackageHTMLDisplayer extends DefaultSwingBundleDisplayer {

  ServiceTracker pkgTracker;

  public PackageHTMLDisplayer(BundleContext bc) {
    super(bc, "Packages", "Shows bundle packages", true);

    pkgTracker = new ServiceTracker(bc, PackageAdmin.class.getName(), null);
    pkgTracker.open();
  }

  public JComponent newJComponent() {
    return new JHTML(this);
  }

  public void valueChanged(long bid) {
    Bundle[] bl = Activator.desktop.getSelectedBundles();

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JHTML comp = (JHTML)it.next();
      comp.valueChanged(bl);
    }
  }

  class JHTML extends JHTMLBundle {

    JHTML(DefaultSwingBundleDisplayer displayer) {
      super(displayer);
    }

    public StringBuffer  bundleInfo(Bundle b) {
      StringBuffer sb = new StringBuffer();

      startFont(sb);
      PackageAdmin pkgAdmin = (PackageAdmin)pkgTracker.getService();
      if(pkgAdmin == null) {
        sb.append("No PackageAdmin service found");
      } else {
        // Array with all bundles that can be required.
        RequiredBundle[] rbl = pkgAdmin.getRequiredBundles(null);
        boolean useParagraph = false;

        Bundle[] fragmentBundles = pkgAdmin.getFragments(b);
        if (fragmentBundles!=null && fragmentBundles.length>0) {
          if (useParagraph) sb.append("<p>");
          sb.append("<b>Host bundle with attached fragments</b>");
          for (int j=0; fragmentBundles!=null && j<fragmentBundles.length; j++){
            sb.append("<br>");
            sb.append("&nbsp;&nbsp");
            Util.bundleLink(sb, fragmentBundles[j]);
            Bundle[] hosts = pkgAdmin.getHosts(fragmentBundles[j]);
            if (hosts==null || b.getBundleId()!=hosts[0].getBundleId()) {
              sb.append("&nbsp;<i>pending refresh</i>");
            }
          }
          if (useParagraph) sb.append("</p>");
          useParagraph = true;
        }

        Bundle[] hostBundles = pkgAdmin.getHosts(b);
        if (hostBundles!=null && hostBundles.length>0) {
          if (useParagraph) sb.append("<p>");
          sb.append("<b>Fragment attached to</b>");
          for (int j=0; hostBundles!=null && j<hostBundles.length; j++){
            sb.append("<br>");
            sb.append("&nbsp;&nbsp");
            Util.bundleLink(sb, hostBundles[j]);
          }
          if (useParagraph) sb.append("</p>");
          useParagraph = true;
        }

        RequiredBundle rb = getRequiredBundle(rbl, b);
        Bundle[] requiringBundles = rb!=null ? rb.getRequiringBundles() : null;
        if (requiringBundles!=null && requiringBundles.length>0) {
          if (useParagraph) sb.append("<p>");
          sb.append("<b>Required by</b>");
          if (rb.isRemovalPending()) {
            sb.append(" (<i>pending removal on refresh</i>)");
          }
          for (int j=0; requiringBundles!=null && j<requiringBundles.length;
               j++) {
            sb.append("<br>");
            sb.append("&nbsp;&nbsp");
            Util.bundleLink(sb, requiringBundles[j]);
          }
          if (useParagraph) sb.append("</p>");
          useParagraph = true;
        }

        ExportedPackage[] pkgs = pkgAdmin.getExportedPackages(b);
        if (useParagraph) sb.append("<p>");
        if(pkgs != null && pkgs.length > 0) {
          sb.append("<b>Exported packages</b>");
          List exportDescr  = new ArrayList();
          for (int i = 0; i < pkgs.length; i++) {
            StringBuffer sb1 = new StringBuffer();
            sb1.append("<br>");
            sb1.append("<b>" + pkgs[i].getName() + "</b>");
            Version version = pkgs[i].getVersion();
            if (version != null) {
              sb1.append(" " + version);
            }
            if (pkgs[i].isRemovalPending()) {
              sb1.append(" ").append("<i>pending removal on refresh</i>");
            }
            Bundle[] bl = pkgs[i].getImportingBundles();
            for(int j = 0; bl != null && j < bl.length; j++) {
              sb1.append("<br>");
              sb1.append("&nbsp;&nbsp;");
              Util.bundleLink(sb1, bl[j]);
            }
            exportDescr.add(sb1.toString());
          }
          Collections.sort(exportDescr);
          for (Iterator it = exportDescr.iterator(); it.hasNext(); ) {
            sb.append(it.next());
          }
        } else {
          sb.append("<b>No exported packages</b>");
        }
        if (useParagraph) sb.append("</p>");
        useParagraph = true;

        List importedPkgs = new ArrayList();
        List requiredPkgs = new ArrayList();

        ExportedPackage[] allEpkgs = pkgAdmin.getExportedPackages((Bundle)null);
        for(int i = 0; allEpkgs != null && i < allEpkgs.length; i++) {
          Bundle[] bl2 = allEpkgs[i].getImportingBundles();

          for(int k = 0; bl2 != null && k < bl2.length; k++) {
            if(bl2[k].getBundleId() == b.getBundleId()) {
              Bundle exporter = allEpkgs[i].getExportingBundle();
              if (isBundleRequiredBy(rbl, exporter, b)) {
                requiredPkgs.add(allEpkgs[i]);
              } else {
                importedPkgs.add(allEpkgs[i]);
              }
            }
          }
        }

        if (useParagraph) sb.append("<p>");
        if(importedPkgs.size() > 0) {
          sb.append("<b>Imported packages</b>");
          Collections.sort(importedPkgs, new ExportedPackageComparator());
          for (Iterator it = importedPkgs.iterator(); it.hasNext(); ) {
            sb.append(formatPackage( (ExportedPackage) it.next(), false ));
          }
        } else {
          sb.append("<b>No imported packages</b>");
        }
        if (useParagraph) sb.append("</p>");
        useParagraph = true;

        if(requiredPkgs.size() > 0) {
          if (useParagraph) sb.append("<p>");
          sb.append("<b>Packages available from required bundles</b>");
          Collections.sort(requiredPkgs, new ExportedPackageComparator());
          for (Iterator it = requiredPkgs.iterator(); it.hasNext(); ) {
            ExportedPackage epkg = (ExportedPackage) it.next();
            sb.append(formatPackage( epkg, isPkgInList(epkg, importedPkgs)));
          }
          if (useParagraph) sb.append("</p>");
          useParagraph = true;
        }

      }

      sb.append("</font>");

      return sb;
    }

    /**
     * Check if a package given by epkg.getName() is present in the list
     * of ExportedPackage objects named importPkgs.
     */
    private boolean isPkgInList(ExportedPackage epkg, List importedPkgs)
    {
      for (Iterator it = importedPkgs.iterator(); it.hasNext(); ) {
        ExportedPackage ipkg = (ExportedPackage) it.next();
        if (epkg.getName().equals(ipkg.getName())) return true;
      }
      return false;
    }

    private String formatPackage(ExportedPackage epkg, boolean isShadowed)
    {
      StringBuffer sb = new StringBuffer();

      sb.append("<br>");
      sb.append("<b>" + epkg.getName() + "</b>");
      Version version = epkg.getVersion();
      if (version != null) {
        sb.append(" ").append(version);
      }
      if (isShadowed) {
        sb.append(" <i>shadowed</i>");
      }
      if (epkg.isRemovalPending()) {
        if (isShadowed) {
          sb.append(",");
        }
        sb.append(" <i>pending removal on refresh</i>");
      }
      sb.append("<br>");
      sb.append("&nbsp;&nbsp;");
      Util.bundleLink(sb, epkg.getExportingBundle());
      return sb.toString();
    }

    /**
     * Check if one given bundle is required by another specified bundle.
     *
     * @param rbl List of required bundles as returend by package admin.
     * @param requiredBundle The bundle to check if it is required.
     * @param requiringBundle The bundle to check that it requires.
     * @return <tt>true</tt> if requiredbundle is required by
     *         requiringBundle, <tt>false</tt> otherwsie.
     */
    private boolean isBundleRequiredBy(RequiredBundle[] rbl,
                                       Bundle requiredBundle,
                                       Bundle requiringBundle)
    {
      RequiredBundle rb = getRequiredBundle(rbl, requiredBundle);
      Bundle[] requiringBundles = rb!=null ? rb.getRequiringBundles() : null;
      for (int j=0; requiringBundles!=null && j<requiringBundles.length;j++){
        if (requiringBundles[j].getBundleId()==requiringBundle.getBundleId()){
          return true;
        }
      }
      return false;
    }

    /**
     * Get the RequiredBundle object for this bundle.
     *
     * @param rbl List of required bundles as returend by package admin.
     * @param bundle The bundle to get requiring bundles for.
     * @return The RequiredBundle object for the given bundle or
     *         <tt>null</tt> if the bundle is not required.
     */
    private RequiredBundle getRequiredBundle(RequiredBundle[] rbl,
                                             Bundle b)
    {
      for (int i=0; rbl!=null && i<rbl.length; i++) {
        if (rbl[i].getBundle().getBundleId()==b.getBundleId()) {
          return rbl[i];
        }
      }
      return null;
    }

  }

  static class ExportedPackageComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      ExportedPackage ep1 = (ExportedPackage) o1;
      ExportedPackage ep2 = (ExportedPackage) o2;

      if (ep1.getName().equals(ep2.getName())) {
        ep1.getVersion().compareTo(ep2.getVersion());
      }
      return ep1.getName().compareTo(ep2.getName());
    }
    public boolean equals(Object o)
    {
      return o instanceof ExportedPackageComparator;
    }

  }
}
