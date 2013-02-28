/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project All rights reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;



public class PackageHTMLDisplayer extends DefaultSwingBundleDisplayer {

  public PackageHTMLDisplayer(BundleContext bc) {
    super(bc, "Packages", "Shows bundle packages", true);

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

    for (final JComponent jComponent : components) {
      final JHTML comp = (JHTML) jComponent;
      comp.valueChanged(bl);
    }
  }

  class JHTML extends JHTMLBundle {
    private static final long serialVersionUID = 1L;

    JHTML(DefaultSwingBundleDisplayer displayer) {
      super(displayer);
    }

    @Override
    public StringBuffer  bundleInfo(Bundle b) {
      final StringBuffer sb = new StringBuffer();


      startFont(sb);

      final Desktop desktop = Activator.desktop;
      if (null!=desktop) {
        final PackageManager pm = desktop.pm;
        if (null!=pm) {
          final PackageAdmin pkgAdmin = pm.getPackageAdmin();

          if(pkgAdmin == null) {
            sb.append("No PackageAdmin service found");
          } else {
            // Array with all bundles that can be required.
            final RequiredBundle[] rbl = pkgAdmin.getRequiredBundles(null);
            boolean useParagraph = false;

            final Bundle[] fragmentBundles = pm.getFragments(b); // pkgAdmin.getFragments(b);
            if (fragmentBundles.length>0) {
              if (useParagraph) {
                sb.append("<p>");
              }
              sb.append("<b>Host bundle with attached fragments</b>");
              for (final Bundle fragmentBundle : fragmentBundles) {
                sb.append("<br>");
                sb.append("&nbsp;&nbsp");
                Util.bundleLink(sb, fragmentBundle);
                final Bundle[] hosts = pm.getHosts(fragmentBundle);
                if (hosts.length==0 || b.getBundleId()!=hosts[0].getBundleId()) {
                  sb.append("&nbsp;<i>pending refresh</i>");
                }
              }
              if (useParagraph) {
                sb.append("</p>");
              }
              useParagraph = true;
            }

            final Bundle[] hostBundles = pm.getHosts(b);
            if (hostBundles.length>0) {
              if (useParagraph) {
                sb.append("<p>");
              }
              sb.append("<b>Fragment attached to</b>");
              for (final Bundle hostBundle : hostBundles) {
                sb.append("<br>");
                sb.append("&nbsp;&nbsp");
                Util.bundleLink(sb, hostBundle);
              }
              if (useParagraph) {
                sb.append("</p>");
              }
              useParagraph = true;
            }

            final RequiredBundle rb = pm.getRequiredBundle(rbl, b);
            final Bundle[] requiringBundles = rb!=null
              ? rb.getRequiringBundles() : null;
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
          }

          appendExportedPackages(sb, b, true);
          appendImportedPackages(sb, b, true);
          appendMissingImports(sb, b);
          appendRequiredPackages(sb, b, true);
        }

      }

      sb.append("</font>");

      return sb;
    }

    void appendExportedPackages(StringBuffer sb, Bundle b,
                                boolean useParagraph) {
      final PackageManager pm = Activator.desktop.pm;
      final Collection<ExportedPackage> pkgs = pm.getExportedPackages(b);
      if (useParagraph) {
        sb.append("<p>");
      }
      if(pkgs.size() > 0) {
        sb.append("<b>Exported packages</b>");
        final List<String> exportDescr  = new ArrayList<String>();
        for (final ExportedPackage pkg : pkgs) {
          final StringBuffer   sb1  = new StringBuffer();

          sb1.append(formatPackage(pkg, false));

          if (!pkg.isRemovalPending() && !pm.isWired(pkg.getName(), b)) {
            // An exporting bundle that imports a package from itself
            // will not have a wire for that package and thus not be
            // present amongst the bundles returned by
            // pkg.getImportingBundles().  There is one exception to
            // this: When removal pending is set in an pkg, that pkg
            // is from an older generation of the bundle and there
            // must be a wire from the bundle to the old pkg and thus
            // the bundle will be present in the list returned by
            // pkg.getImportingBundles().
            sb1.append("<br>");
            sb1.append("&nbsp;&nbsp;");
            Util.bundleLink(sb1, b);
          }

          final Bundle[] bl = pkg.getImportingBundles();
          for(int j = 0; bl != null && j < bl.length; j++) {
            sb1.append("<br>");
            sb1.append("&nbsp;&nbsp;");
            Util.bundleLink(sb1, bl[j]);
          }
          exportDescr.add(sb1.toString());
        }
        Collections.sort(exportDescr);
        for (final String string : exportDescr) {
          sb.append(string);
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
      final PackageManager pm = Activator.desktop.pm;
      final Collection<ExportedPackage> importedPkgs = pm.getImportedPackages(b);

      if (useParagraph) {
        sb.append("<p>");
      }
      if(importedPkgs.size() > 0) {
        sb.append("<b>Imported packages</b>");
        for (final ExportedPackage epkg : importedPkgs) {
          sb.append(formatPackage( epkg, false ));
          sb.append("<br>");
          sb.append("&nbsp;&nbsp;");
          final Bundle exporter = epkg.getExportingBundle();
          if (exporter != null) {
            Util.bundleLink(sb, exporter);
          } else {
            sb.append("STALE");
          }
        }
      } else {
        sb.append("<b>No imported packages</b>");
      }
      if (useParagraph) {
        sb.append("</p>");
      }
    }

    void appendMissingImports(StringBuffer sb, Bundle b) {
      final PackageManager pm = Activator.desktop.pm;
      final Collection<String> missingImports = pm.getMissingImports(b);
      if(missingImports.size() > 0) {
        sb.append("<p>");
        sb.append("<b>Missing packages</b>");
        for (final String missingImport : missingImports) {
          sb.append("<br>\n");
          sb.append(formatPackage(missingImport));
          sb.append("</p>");
        }
      }
    }

    void appendRequiredPackages(StringBuffer sb, Bundle b,
                                boolean useParagraph) {
      final PackageManager pm = Activator.desktop.pm;
      final Collection<ExportedPackage> requiredPkgs = pm.getRequiredPackages(b);
      final Collection<ExportedPackage> importedPkgs = pm.getImportedPackages(b);

      if(requiredPkgs.size() > 0) {
        if (useParagraph) {
          sb.append("<p>");
        }
        sb.append("<b>Packages available from required bundles</b>");
        for (final ExportedPackage epkg : requiredPkgs) {
          sb.append(formatPackage( epkg, isPkgInList(epkg, importedPkgs)));
        }
        if (useParagraph) {
          sb.append("</p>");
        }
        useParagraph = true;
      }
    }

    /**
     * Check if a package given by epkg.getName() is present in the list
     * of ExportedPackage objects named importPkgs.
     */
    private boolean isPkgInList(ExportedPackage epkg,
                                Collection<ExportedPackage> importedPkgs)
    {
      for (final ExportedPackage ipkg : importedPkgs) {
        if (epkg.getName().equals(ipkg.getName())) {
          return true;
        }
      }
      return false;
    }

    private String formatPackage(String name) {
      final StringBuffer sb = new StringBuffer();
      sb.append("<font color=\"#444444\">");
      sb.append(name);
      sb.append("</font>");
      return sb.toString();
    }

    private String formatPackage(ExportedPackage epkg, boolean isShadowed)
    {
      final StringBuffer sb = new StringBuffer();

      sb.append("<br>");

      sb.append("<font color=\"#444444\">");
      sb.append(epkg.getName());

      final Version version = epkg.getVersion();
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

      return sb.toString();
    }
  }
}
