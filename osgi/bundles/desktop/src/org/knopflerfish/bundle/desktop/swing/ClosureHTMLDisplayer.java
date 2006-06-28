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

import org.knopflerfish.util.Text;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.event.*;
import java.awt.*;

import java.util.*;



public class ClosureHTMLDisplayer extends DefaultSwingBundleDisplayer {

  public ClosureHTMLDisplayer(BundleContext bc) {
    super(bc, "Closure", "Shows bundle closure", true); 

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

    public void valueChanged(Bundle[] bl) {
      StringBuffer sb = new StringBuffer("<html>\n");
      
      
      if(bl == null || bl.length == 0) {
	setCurrentBID(-1);

	sb.append("<html>\n");
	sb.append("<table border=0>\n");
	sb.append("<tr><td bgcolor=\"#eeeeee\">");
	startFont(sb, "-1");
	sb.append(getNoBundleSelectedHeader());
	sb.append("</font>\n");
	sb.append("</td>\n");
	sb.append("</tr>\n");
	sb.append("</table>\n");
	
	startFont(sb);
	sb.append(getNoBundleSelectedText());
	sb.append("</font>\n" + 
		  "</p>\n" + 
		  "</html>");
      } else {

	setCurrentBID(bl[0].getBundleId());

	sb.append("<table border=0 width=\"100%\">\n");
	sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
	startFont(sb, "-1");
	for(int i = 0; i < bl.length; i++) {
	  sb.append(getBundleSelectedHeader(bl[i]));
	  if(i < bl.length - 1) {
	    sb.append("<br>");
	  }
	}
	sb.append("</font>\n");
	sb.append("</td>\n");
	sb.append("</tr>\n");
	
	sb.append("<tr><td bgcolor=\"#ffffff\">");
	sb.append(bundleInfo(bl).toString());
	sb.append("</td>\n");
	sb.append("</tr>\n");
	sb.append("</table>\n");
      }

      sb.append("\n</html>");

      setHTML(sb.toString());
    }


    public StringBuffer  bundleInfo(Bundle target) {
      return new StringBuffer("---- " + target);
    }

    public StringBuffer  bundleInfo(Bundle[] targets) {
      StringBuffer sb = new StringBuffer();
      
      startFont(sb);
      PackageAdmin pkgAdmin = (PackageAdmin)Activator.pkgTracker.getService();
      if(pkgAdmin == null) {
	sb.append("No PackageAdmin service found");
      } else {
	
	Bundle[] bl = getBundleArray();
	
	Set pkgClosure = new TreeSet(Util.bundleIdComparator);
	
	for(int i = 0; i < targets.length; i++) {
	  pkgClosure.addAll(Util.getPackageClosure(pkgAdmin, 
						   bl, 
						   targets[i], 
						   null));
	}
	
	// remove myself	
	//	pkgClosure.remove(b);
	
	if(pkgClosure.size() == 0) {
	  sb.append("No package dependecies");
	} else {

	  sb.append("<b>Static dependencies via packages</b><br>");
	  for(Iterator it = pkgClosure.iterator(); it.hasNext();) {
	    Bundle depB = (Bundle)it.next();
	    
	    sb.append("&nbsp;&nbsp;");
	    Util.bundleLink(sb, depB);
	    sb.append("<br>");
	  }
	}
	
	sb.append("<br>");
	
	Set serviceClosure = new TreeSet(Util.bundleIdComparator);

	for(int i = 0; i < targets.length; i++) {
	  serviceClosure.addAll(Util.getServiceClosure(targets[i], null));
	}
	
	// remove myself	
	//	serviceClosure.remove(b);
	
	if(serviceClosure.size() == 0) {
	  sb.append("No service dependecies");
	} else {
	  sb.append("<b>Runtime dependencies via services</b><br>");
	  
	  for(Iterator it = serviceClosure.iterator(); it.hasNext();) {
	    Bundle depB = (Bundle)it.next();
	    
	    sb.append("&nbsp;&nbsp;");
	    Util.bundleLink(sb, depB);
	    sb.append("<br>");
	  }
	}

	// Add xarsg info if we seem to be running knopflerfish
	if(targets.length > 0 && 
	   (-1 != targets[0].getClass().getName().indexOf("knopflerfish"))) {
	  
	  String xargs = Util.getXARGS(null, pkgClosure, serviceClosure).toString();
	  sb.append("<hr>");
	  startFont(sb);
	  sb.append("<b>Suggested startup .xargs file</b><br>\n");
	  sb.append("</font>");
	  
	  sb.append("<pre>");
	  sb.append("<font size=\"-2\">");
	  //	sb.append(Text.replace(xargs, "\n", "<br>"));
	  sb.append(xargs);
	  sb.append("</font>");
	  sb.append("</pre>");
	}

      }
      
      sb.append("</font>");
      
      return sb;
    }
    
  }

}

