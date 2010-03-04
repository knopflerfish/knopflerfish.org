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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.knopflerfish.util.Text;


public class JTips extends JPanel {
  JFrame frame = null;
  String title = "Knopflerfish OSGi: tips";

  java.util.List tips = new ArrayList(); // String

  static final String sep = "<p>----</p>";

  JButton nextButton;
  JButton prevButton;

  JComponent  ctrlPanel;
  JTextPane   html;
  JScrollPane scroll;

  int tipIx = 0;

  public JTips(String tipFile) {
    super(new BorderLayout());

    BufferedReader in = null;
    try {
      URL url = getClass().getResource(tipFile);
      if(url != null) {
	in = new BufferedReader(new InputStreamReader(url.openStream()));
	String       line = null;
	StringBuffer sb   = new StringBuffer();
	while(null != (line = in.readLine())) {
	  if(sep.equals(line)) {
	    addTip(sb.toString());
	    sb = new StringBuffer();
	  } else {
	    sb.append(line);
	    sb.append("\n");
	  }
	}
	if(sb != null && sb.length() > 1) {
	  addTip(sb.toString());
	}
      } else {
	Activator.log.warn("No tip file: " + tipFile);
      }
    } catch (Exception e) {
      Activator.log.error("Failed to load tips from " + tipFile, e);
    } finally {
      try { in.close(); } catch (Exception ignored) { }
    }
    
    if(tips.size() == 0) {
      tips.add(new Tip("", "No tips found", ""));
    } else {
      Activator.log.info("loaded " + tips.size() + " tips");
    }

    html = new JTextPane();

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
	      Activator.log.warn("Failed to open external url=" + url, e);
	    }
	  }
	}
      });

    scroll = new JScrollPane(html);
    scroll.setPreferredSize(new Dimension(350, 200));

    scroll.setBorder(BorderFactory
		     .createCompoundBorder(BorderFactory
					   .createEmptyBorder(5, 5, 5, 5),
					   BorderFactory
					   .createLoweredBevelBorder()));

    final ActionListener nextAction = new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  setTip((tipIx + 1) % tips.size());
	}
      };
    
    JButton closeButton = new JButton(Strings.get("close"));
    closeButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  if(frame != null) {
	    frame.setVisible(false);
	  }
	}
      });

    nextButton = new JButton(Strings.get("next_tip"));
    nextButton.addActionListener(nextAction);

    prevButton = new JButton(Strings.get("prev_tip"));
    prevButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  setTip((tipIx + tips.size() - 1) % tips.size());
	}
      });

    JPanel bottomPanel = new JPanel(new BorderLayout());
    JPanel topPanel    = new JPanel(new BorderLayout());

    ctrlPanel = new JPanel(new FlowLayout());

    ctrlPanel.add(closeButton);
    ctrlPanel.add(prevButton);
    ctrlPanel.add(nextButton);

    bottomPanel.add(ctrlPanel,    BorderLayout.EAST);

    JLabel titleC = new JLabel("Did you know that...");
    titleC.setFont(new Font("Dialog", Font.BOLD, 15));
    titleC.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

    topPanel.add(titleC, BorderLayout.WEST);

    JLabel icon = new JLabel(Activator.desktop.tipIcon);
    icon.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent ev) {
	  nextAction.actionPerformed(null);
	}
      });
    icon.setToolTipText(nextButton.getText());

    add(icon,        BorderLayout.WEST);
    add(scroll,      BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.SOUTH);
    add(topPanel,    BorderLayout.NORTH);

    setTip((int)(Math.random() * tips.size()));

  }

  void addTip(String s) {
    tips.add(new Tip(s));
  }

  void setTip(int ix) {
    tipIx = ix;

    Tip    tip = (Tip)tips.get(tipIx % tips.size());
    String s = tip.toHTML();
    s   = Text.replace(s, "<img src=\"", "<img src=\"bundle://$(BID)/");
    s   = Text.replace(s, "$(BID)", Long.toString(Activator.getBC().getBundle().getBundleId()));
    
    setHTML(Util.fontify(s));
  }

  void setHTML(final String s) {
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

  void setTitle(String s) {
    this.title = s;
    if(frame != null) {
      frame.setTitle(this.title);
    }
  }

  public void setVisible(boolean b) {
    if(b) {
      if(frame == null) {
	frame = new JFrame(title);
	Activator.desktop.setIcon(frame, "/kf_");

	frame.getContentPane().setLayout(new BorderLayout());
	frame.getContentPane().add(this, BorderLayout.CENTER);
	frame.pack();
	Point p = Activator.desktop.frame.getLocationOnScreen();
	Dimension size = Activator.desktop.frame.getSize();
	Dimension mySize = frame.getSize();
	Point p2 = new Point(p.x + size.width  / 2 - mySize.width / 2,
			     p.y + size.height / 2 - mySize.height / 2);
	
	frame.setLocation(p2);
      }
      frame.setVisible(true);
    } else {
      if(frame != null) {
	frame.setVisible(false);
      }
    }
  }
}

class Tip {
  public String id;
  public String name;
  public String main;

  public Tip(String id, String name, String main) {
    this.id   = id;
    this.name = name;
    this.main = main;
  }

  public Tip(String s) {
    int ix = s.indexOf("\n");
    if(ix != -1) {
      name = s.substring(0, ix);      
      main = s.substring(ix + 1);
      ix = name.indexOf(" ");

      id   = name.substring(0, ix);
      name = name.substring(ix + 1);

    } else {
      throw new IllegalArgumentException("Bad tip format: " + s);
    }
  }

  

  public String toHTML() {
    return 
      "<b>" + name + "</b>" + 
      "<p>" + main + "</p>" + 
      "<p align=right><font size=-2>#" + id + "</font></p>";
  }

  public String toString() {
    return "Tip[" + 
      "id=" + id + 
      ", name=" + name + 
      "]";
  }

  
  public boolean equals(Object other) {
    if(other == null || !(other instanceof Tip)) {
      return false;
    }
    
    Tip t = (Tip)other;
    return id.equals(t.id);
  }

  public int hashCode() {
    return id.hashCode();
  }
}
