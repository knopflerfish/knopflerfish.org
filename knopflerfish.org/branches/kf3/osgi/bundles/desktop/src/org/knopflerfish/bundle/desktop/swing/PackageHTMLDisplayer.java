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
import java.util.Collection;
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

  public PackageHTMLDisplayer(BundleContext bc) {
    super(bc, "Packages", "Shows bundle packages", true);

    bUseListeners          = true;
    bUpdateOnBundleChange  = true;
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


      PackageManager pm = Activator.desktop.pm;

      PackageAdmin pkgAdmin = pm.getPackageAdmin();
      
      if(pkgAdmin == null) {
        sb.append("No PackageAdmin service found");
      } else {
        // Array with all bundles that can be required.
        RequiredBundle[] rbl = pkgAdmin.getRequiredBundles(null);
        boolean useParagraph = false;

        Bundle[] fragmentBundles = pm.getFragments(b); // pkgAdmin.getFragments(b);
        if (fragmentBundles.length>0) {
          if (useParagraph) {
            sb.append("<p>");
          }
          sb.append("<b>Host bundle with attached fragments</b>");
          for (int j=0; j<fragmentBundles.length; j++){
            sb.append("<br>");
            sb.append("&nbsp;&nbsp");
            Util.bundleLink(sb, fragmentBundles[j]);
            Bundle[] hosts = pm.getHosts(fragmentBundles[j]);
            if (hosts.length==0 || b.getBundleId()!=hosts[0].getBundleId()) {
              sb.append("&nbsp;<i>pending refresh</i>");
            }
          }
          if (useParagraph) {
            sb.append("</p>");
          }
          useParagraph = true;
        }

        Bundle[] hostBundles = pm.getHosts(b);
        if (hostBundles.length>0) {
          if (useParagraph) {
            sb.append("<p>");
          }
          sb.append("<b>Fragment attached to</b>");
          for (int j=0; j<hostBundles.length; j++){
            sb.append("<br>");
            sb.append("&nbsp;&nbsp");
            Util.bundleLink(sb, hostBundles[j]);
          }
          if (useParagraph) {
            sb.append("</p>");
          }
          useParagraph = true;
        }

        RequiredBundle rb = pm.getRequiredBundle(rbl, b);
        Bundle[] requiringBundles = rb!=null ? rb.getRequiringBundles() : null;
        if (requiringBundles!=null && requiringBundles.length>0) {
          if (useParagraph) {
            sb.append("<p>");
          }
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
          if (useParagraph) {
            sb.append("</p>");
          }
          useParagraph = true;
        }

        appendExportedPackages(sb, b, true);
        appendImportedPackages(sb, b, true);
        appendMissingImports(sb, b);
        appendRequiredPackages(sb, b, true);

      }

      sb.append("</font>");

      return sb;
    }

    void appendExportedPackages(StringBuffer sb, Bundle b, 
                                boolean useParagraph) {
      PackageManager pm = Activator.desktop.pm;
      Collection pkgs = pm.getExportedPackages(b);
      if (useParagraph) {
        sb.append("<p>");
      }
      if(pkgs.size() > 0) {
        sb.append("<b>Exported packages</b>");
        List exportDescr  = new ArrayList();
        for (Iterator it = pkgs.iterator(); it.hasNext(); ) {
          ExportedPackage pkg = (ExportedPackage)it.next();
          StringBuffer   sb1  = new StringBuffer();
          
          sb1.append(formatPackage(pkg, false));
          
          Bundle[] bl = pkg.getImportingBundles();
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
      if (useParagraph) {
        sb.append("</p>");
      }
    }

    void appendImportedPackages(StringBuffer sb, Bundle b, 
                                boolean useParagraph) {
      PackageManager pm = Activator.desktop.pm;
      Collection importedPkgs = pm.getImportedPackages(b);
      
      if (useParagraph) {
        sb.append("<p>");
      }
      if(importedPkgs.size() > 0) {
        sb.append("<b>Imported packages</b>");
        for (Iterator it = importedPkgs.iterator(); it.hasNext(); ) {
          sb.append(formatPackage( (ExportedPackage) it.next(), false ));
        }
      } else {
        sb.append("<b>No imported packages</b>");
      }
      if (useParagraph) {
        sb.append("</p>");
      }
    }

    void appendMissingImports(StringBuffer sb, Bundle b) {
      PackageManager pm = Activator.desktop.pm;
      Collection missingImports = pm.getMissingImports(b);
      if(missingImports.size() > 0) {
        sb.append("<p>");
        sb.append("<b>Missing packages</b>");
        for (Iterator it = missingImports.iterator(); it.hasNext(); ) {
          sb.append("<br>\n");
          sb.append(formatPackage( (String) it.next()));
          sb.append("</p>");
        }
      }
    }

    void appendRequiredPackages(StringBuffer sb, Bundle b,
                                boolean useParagraph) {
      PackageManager pm = Activator.desktop.pm;
      Collection requiredPkgs = pm.getRequiredPackages(b);
      Collection importedPkgs = pm.getImportedPackages(b);

      if(requiredPkgs.size() > 0) {
        if (useParagraph) sb.append("<p>");
        sb.append("<b>Packages available from required bundles</b>");
        for (Iterator it = requiredPkgs.iterator(); it.hasNext(); ) {
          ExportedPackage epkg = (ExportedPackage) it.next();
          sb.append(formatPackage( epkg, isPkgInList(epkg, importedPkgs)));
        }
        if (useParagraph) sb.append("</p>");
        useParagraph = true;
      }
    }

    /**
     * Check if a package given by epkg.getName() is present in the list
     * of ExportedPackage objects named importPkgs.
     */
    private boolean isPkgInList(ExportedPackage epkg, Collection importedPkgs)
    {
      for (Iterator it = importedPkgs.iterator(); it.hasNext(); ) {
        ExportedPackage ipkg = (ExportedPackage) it.next();
        if (epkg.getName().equals(ipkg.getName())) return true;
      }
      return false;
    }

    private String formatPackage(String name) {
      StringBuffer sb = new StringBuffer();
      sb.append("<font color=\"#444444\">");
      sb.append(name);
      sb.append("</font>");
      return sb.toString();
    }

    private String formatPackage(ExportedPackage epkg, boolean isShadowed)
    {
      StringBuffer sb = new StringBuffer();

      sb.append("<br>");

      sb.append("<font color=\"#444444\">");
      sb.append(epkg.getName());

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
      sb.append("</font>");
      sb.append("<br>");
      sb.append("&nbsp;&nbsp;");
      Bundle b = epkg.getExportingBundle();
      if (b != null) {
        Util.bundleLink(sb, b);
      } else {
        sb.append("STALE");
      }
      return sb.toString();
    }
  }
}
