/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

import org.osgi.framework.*;
import org.osgi.service.startlevel.*;
import org.osgi.service.packageadmin.*;
import org.osgi.util.tracker.*;

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.event.*;
import java.awt.*;

import java.util.*;



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
	ExportedPackage[] pkgs = pkgAdmin.getExportedPackages(b);

	if(pkgs != null && pkgs.length > 0) {
	  sb.append("<b>Exported packages</b>");
	} else {
	  sb.append("<b>No exported packages</b>");
	}
	for(int i = 0; pkgs != null && i < pkgs.length; i++) {
	  sb.append("<br>");
	  sb.append("<b>" + pkgs[i].getName() + "</b>");
	  String version = pkgs[i].getSpecificationVersion();
	  if(version != null) {
	    sb.append(" " + version);
	  }
	  Bundle[] bl = pkgs[i].getImportingBundles();
	  for(int j = 0; bl != null && j < bl.length; j++) {
	    sb.append("<br>");
	    sb.append("&nbsp;&nbsp;");
	    Util.bundleLink(sb, bl[j]);
	  }
	}

	StringBuffer sb3 = new StringBuffer();

	Bundle[] bl = getBundleArray();

	for(int i = 0; i < bl.length; i++) {
	  ExportedPackage[] pkgs2 = pkgAdmin.getExportedPackages(bl[i]);

	  for(int j = 0; pkgs2 != null && j < pkgs2.length; j++) {
	    Bundle[] bl2 = pkgs2[j].getImportingBundles();
	    StringBuffer sb2 = new StringBuffer();

	    for(int k = 0; bl2 != null && k < bl2.length; k++) {
	      if(bl2[k].getBundleId() == b.getBundleId()) {
		sb2.append("<br>");
		sb2.append("<b>" + pkgs2[j].getName() + "</b>");
	      }
	    }
	    if(sb2.length() > 0) {
	      sb3.append(sb2.toString());
	      sb3.append("<br>");
	      sb3.append("&nbsp;&nbsp;");
	      Util.bundleLink(sb3, pkgs2[j].getExportingBundle());
	    }
	  }
	}

	sb.append("<p>");
	if(sb3.length() > 0) {
	  sb.append("<b>Imported packages</b>");
	  sb.append(sb3.toString());
	} else {
	  sb.append("<b>No imported packages</b>");
	}
	sb.append("</p>");
      }

      sb.append("</font>");

      return sb;
    }
  }
}

