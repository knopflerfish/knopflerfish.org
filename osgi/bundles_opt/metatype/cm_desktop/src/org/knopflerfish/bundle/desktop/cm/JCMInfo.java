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

package org.knopflerfish.bundle.desktop.cm;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.util.tracker.*;
import java.util.*;
import org.knopflerfish.service.desktop.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import org.knopflerfish.util.metatype.*;
import org.osgi.service.metatype.*;
import java.net.URL;

public class JCMInfo extends JPanel {
  MetaTypeProvider mtp;
  PIDProvider      pp;

  JPanel main;
  JCMService jcmService;

  public JCMInfo() {
    super(new BorderLayout());

    main = new JPanel(new BorderLayout());
    jcmService = new JCMService();

    add(main, BorderLayout.CENTER);
  }

  JComboBox servicePIDBox = null;
  JComboBox factoryPIDBox = null;

  void setProvider(MetaTypeProvider _mtp, PIDProvider _pp,
		   Bundle bundle) {
    this.mtp = _mtp;
    this.pp  = _pp;
    main.removeAll();

    if(mtp != null) {
      
      servicePIDBox = null;
      factoryPIDBox = null;
      
      String[] servicePIDs = pp.getPids();


      if(servicePIDs != null && servicePIDs.length > 0) {
	servicePIDBox = new JComboBox(servicePIDs);
	servicePIDBox.addActionListener(new ActionListener() {	  
	    public void actionPerformed(ActionEvent ev) {	    
	      int ix = servicePIDBox.getSelectedIndex();
	      if(ix == -1) {
		return;
	      } else {
		String pid = (String)servicePIDBox.getSelectedItem();
		setServiceOCD(pid);
	      }
	    }
	  });
      }
      
      String[] factoryPIDs = pp.getFactoryPids();

      if(factoryPIDs != null && factoryPIDs.length > 0) {
	factoryPIDBox = new JComboBox(factoryPIDs);
	factoryPIDBox.addActionListener(new ActionListener() {	  
	    public void actionPerformed(ActionEvent ev) {	    
	      int ix = factoryPIDBox.getSelectedIndex();
	      if(ix == -1) {
		return;
	      } else {
		String pid = (String)factoryPIDBox.getSelectedItem();
		setFactoryOCD(pid);
	      }
	    }
	  });
      }
      
      JPanel upperBox = new JPanel(new GridLayout(0, 1));
      
      String title = "Available PIDs";
      if(mtp instanceof MTP) {
	title = title + " in " + ((MTP)mtp).getId();
      }
      upperBox.setBorder(makeBorder(this, title));
      
      if(servicePIDBox != null) {
	upperBox.add(new JLabelled("Services", 
				   "PIDs representing ManagedServices",
				   servicePIDBox, 100));
      }
      if(factoryPIDBox != null) {
	upperBox.add(new JLabelled("Factories", 
				   "PIDs representing ManagedServiceFactories",
				   factoryPIDBox, 100));
      }
      
      main.add(upperBox,   BorderLayout.NORTH);
      main.add(jcmService, BorderLayout.CENTER);

      // Set either the first service or the first factory as displayed
      if(servicePIDs != null && servicePIDs.length > 0) {
	setServiceOCD(servicePIDs[0]);
      } else {
	if(factoryPIDs != null && factoryPIDs.length > 0) {
	  setFactoryOCD(factoryPIDs[0]);
	} else {
	  // Neither service nor factory found in provider
	}
      }
    } else {
      JHTML jhtml = new JHTML();
      StringBuffer sb = new StringBuffer();
      sb.append("<html>");
      sb.append("<table border=0 width=\"100%\">\n");
      sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
      Util.startFont(sb, "-1");
      sb.append(getBundleSelectedHeader(bundle));
      sb.append("</font>\n");
      sb.append("</td>\n");
      sb.append("</tr>\n");
      sb.append("</table>\n");

      sb.append("<p>");
      Util.startFont(sb, "-2");
      sb.append("No CM metatype found in bundle.<br>");
      sb.append("See <a href=\"http://www.knopflerfish.org/XMLMetatype/\">http://www.knopflerfish.org/XMLMetatype/</a> for details on how to add metatype and default values.");
      sb.append("</font>");
      sb.append("</p>");
      
      
      sb.append("</html>\n");
      
      jhtml.setHTML(sb.toString());
      
      main.add(jhtml, BorderLayout.CENTER);
    }
    invalidate();
    revalidate();
    repaint();
  }

  static Border makeBorder(JComponent comp, String title) {
    Color borderCol = comp.getBackground().brighter();
    return BorderFactory
      .createTitledBorder(BorderFactory
			  .createMatteBorder(comp.getFont().getSize() + 4,0,0,0, borderCol),
			  title);
    
  }

  String getBundleSelectedHeader(Bundle b) {
    return "#" + b.getBundleId() + "  " +  Util.getBundleName(b);
  }

  void setServiceOCD(String pid) {
    try {
      ObjectClassDefinition ocd = 
	(ObjectClassDefinition)mtp.getObjectClassDefinition(pid, null);
      
      jcmService.setServiceOCD(ocd);
    } catch (Throwable t) {
      Activator.log.error("Failed to set service pid=" + pid, t);
    }
  }

  void setFactoryOCD(String pid) {
    ObjectClassDefinition ocd = 
      (ObjectClassDefinition)mtp.getObjectClassDefinition(pid, null);

    jcmService.setFactoryOCD(ocd);
  }
}

class JHTML extends JPanel {
  JTextPane html;
  JScrollPane scroll;

  JHTML() {
    super(new BorderLayout());

    html = new JTextPane();
    html.setText("");
    html.setContentType("text/html");
    
    html.setEditable(false);
    
    html.addHyperlinkListener(new HyperlinkListener() 
      {
	public void hyperlinkUpdate(HyperlinkEvent ev) {
	  if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	    URL url = ev.getURL();
	    try {
	      Util.openExternalURL(url);
	    } catch (Exception e) {
	      Activator.log.error("Failed to show " + url, e);
	    }
	  }
	}
      });
    scroll = 
      new JScrollPane(html, 
		      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      
    html.setPreferredSize(new Dimension(300, 300));
    
    add(scroll, BorderLayout.CENTER);
  }

  void setHTML(String s) {
    html.setText(s);
    
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  JViewport vp = scroll.getViewport();
	  if(vp != null) {
	    vp.setViewPosition(new Point(0,0));
	    scroll.setViewport(vp);
	  }  
	}
      });
  }
}
