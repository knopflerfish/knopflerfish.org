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

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.event.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

import java.util.*;
import java.net.URL;

import java.io.*;


public class ServiceHTMLDisplayer extends DefaultSwingBundleDisplayer {

  public ServiceHTMLDisplayer(BundleContext bc) {
    super(bc, "Services", "Shows bundle services", true); 
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
     
      try {
	ServiceReference[] srl = Activator.getTargetBC().getServiceReferences(null, null);
	
	int nExport = 0;
	int nImport = 0;
	for(int i = 0; srl != null && i < srl.length; i++) {
	  if(srl[i].getBundle().getBundleId() == b.getBundleId()) {
	    nExport++;
	  }
	  Bundle[] bl = srl[i].getUsingBundles();
	  for(int j = 0; bl != null && j < bl.length; j++) {
	    if(bl[j].getBundleId() == b.getBundleId()) {
	      nImport++;
	    }
	  }
	}

	startFont(sb);

	if(nExport > 0) {
	  sb.append("<b>Exported services</b>");
	  
	  for(int i = 0; srl != null && i < srl.length; i++) {
	    if(srl[i].getBundle().getBundleId() == b.getBundleId()) {
	      String[] cl = (String[])srl[i].getProperty(Constants.OBJECTCLASS);
	      Bundle[] bl = srl[i].getUsingBundles();
	      
	      for(int j = 0; j < cl.length; j++) {
		sb.append("<br>");
		sb.append("#");
		Util.serviceLink(sb, srl[i], "" + srl[i].getProperty(Constants.SERVICE_ID));
		sb.append(" ");
		sb.append(cl[j]);
	      }
	      
	      if(bl != null && bl.length > 0) {
		//	      sb.append("<b>Used by</b><br>");
		for(int j = 0; bl != null && j < bl.length; j++) {
		  sb.append("<br>");
		  sb.append("&nbsp;&nbsp;");
		  Util.bundleLink(sb, bl[j]);
		}
	      }
	    }
	  }
	}

	if(nImport > 0) {
	  sb.append("<br><b>Imported services</b>");
	  for(int i = 0; srl != null && i < srl.length; i++) {
	    Bundle[] bl = srl[i].getUsingBundles();
	    for(int j = 0; bl != null && j < bl.length; j++) {
	      if(bl[j].getBundleId() == b.getBundleId()) {
		String[] cl = (String[])srl[i].getProperty(Constants.OBJECTCLASS);
		for(int k = 0; k < cl.length; k++) {
		  sb.append("<br>");
		  sb.append("#");
		  Util.serviceLink(sb, srl[i], "" + srl[i].getProperty(Constants.SERVICE_ID));
		  sb.append(" ");
		  sb.append(cl[k]);
		}
		sb.append("<br>");
		sb.append("&nbsp;&nbsp;");
		Util.bundleLink(sb, bl[j]);
	      }
	    }
	  }
	}
	sb.append("</font>");
      } catch (Exception e) {
	e.printStackTrace();
      }
      Dictionary headers = b.getHeaders();
      
      sb.append("<table border=0 cellspacing=1 cellpadding=0>\n");
      
      sb.append("</table>");
      return sb;
    }

  }
  
}

