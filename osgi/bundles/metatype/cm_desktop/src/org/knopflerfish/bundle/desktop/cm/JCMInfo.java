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
  MetaTypeInformation mtp;

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

  void setProvider(MetaTypeInformation _mtp, Bundle bundle) {
    this.mtp = _mtp;
    
    main.removeAll();

    if(mtp != null) {
      servicePIDBox = null;
      factoryPIDBox = null;
      
      String[] servicePIDs = mtp.getPids();


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
      
      String[] factoryPIDs = mtp.getFactoryPids();

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
      try {
	StringBuffer sb = new StringBuffer();
	sb.append("<html>\n");
	sb.append("<body>\n");
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
	sb.append("<p>See <a href=\"http://www.knopflerfish.org/XMLMetatype/\">http://www.knopflerfish.org/XMLMetatype/</a> for details on how to add metatype and default values.</p>");
	sb.append("<p>The ");
	Util.bundleLink(sb, Activator.bc.getBundle(0));
	sb.append(" shows all available configurations</p>");
	sb.append("</font>");
	sb.append("</p>");
	
	
	sb.append("</body>\n");
	sb.append("</html>\n");

	JHTML jhtml = new JHTML(sb.toString());

	main.add(jhtml, BorderLayout.CENTER);
	
      } catch (Exception e) {
	e.printStackTrace();
      }
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
      
      jcmService.setServiceOCD(pid, ocd);
    } catch (Throwable t) {
      Activator.log.error("Failed to set service pid=" + pid, t);
    }
  }

  void setFactoryOCD(String pid) {
    ObjectClassDefinition ocd = 
      (ObjectClassDefinition)mtp.getObjectClassDefinition(pid, null);

    jcmService.setFactoryOCD(pid, ocd);
  }
}

class JHTML extends JPanel {
  JTextPane html;
  JScrollPane scroll;

  JHTML() {
    this("");
  }

  JHTML(String s) {
    super(new BorderLayout());

    html = new JTextPane();
    html.setEditable(false); // need o set this explicitly to fix swing 1.3 bug
    html.setCaretPosition(0);
    html.setContentType("text/html");
    html.setText(s);
    html.setCaretPosition(0);

    html.addHyperlinkListener(new HyperlinkListener() 
      {
	public void hyperlinkUpdate(HyperlinkEvent ev) {
	  if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	    URL url = ev.getURL();
	    try {
	      if(Util.isBundleLink(url)) {
		long bid = Util.bidFromURL(url);
		Activator.disp.getBundleSelectionModel().clearSelection();
		Activator.disp.getBundleSelectionModel().setSelected(bid, true);
	      } else {
		Util.openExternalURL(url);
	      }
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
    try {
      html.setText(s);
    } catch (Exception e) {
      Activator.log.error("Failed to set html", e);
    }
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  try {
	    JViewport vp = scroll.getViewport();
	    if(vp != null) {
	      vp.setViewPosition(new Point(0,0));
	      scroll.setViewport(vp);
	    }  
	  } catch (Exception e) {
	    Activator.log.error("Failed to set html", e);
	  }
	}
      });
  }
}
