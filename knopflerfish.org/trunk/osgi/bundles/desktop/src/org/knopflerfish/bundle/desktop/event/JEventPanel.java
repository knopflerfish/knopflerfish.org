/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.event;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.osgi.service.event.Event;
import org.osgi.framework.Filter;

public class JEventPanel extends JPanel implements ClipboardOwner {
  
  JTextArea text;
  JTextField topicC;
  JTextField filterC;

  EventTableModel model;
  JEventTable     table;
  JScrollPane   scrollpane;
  JPopupMenu    popup;
  Color txtColor;

  public JEventPanel(EventTableModel   model, 
		   JEventEntryDetail logEntryDetail,
		   boolean         bSort) {
    super(new BorderLayout());
    this.model = model;
    
    table = new JEventTable(model, logEntryDetail, bSort);
    scrollpane = new JScrollPane(table);


    popup = new JPopupMenu() {
	  {
	    add(new JMenuItem("Copy log to clipboard") {
		{
		  addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ev) {
			copyToClipBoard();
		      }
		    });
		}
	      });
	  }
      };
    getJEventTable().addMouseListener(new MouseAdapter() {
	public void mousePressed(MouseEvent e) {
	  maybeShowPopup(e);
	}
	
	public void mouseReleased(MouseEvent e) {
	  maybeShowPopup(e);
	}
	
	private void maybeShowPopup(MouseEvent e) {
	  if (e.isPopupTrigger()) {
	    popup.show(e.getComponent(), e.getX(), e.getY());
	  }
	}
      });
    
    JPanel p1 = new JPanel(new FlowLayout());
    p1.add(new JLabel("Topic:"));

    topicC = new JTextField(model.getDispatcher().getTopic(), 15);
    topicC.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          JEventPanel.this.model.getDispatcher().setTopic(topicC.getText());
        }
      });

    p1.add(topicC, BorderLayout.CENTER);

    p1.add(new JLabel("Filter:"));

    filterC = new JTextField(model.getDispatcher().getFilter(), 12);
    txtColor = filterC.getForeground();
    filterC.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String filterS = filterC.getText().trim();
          try {
            if(filterS.length() > 0) {
              Filter f = org.knopflerfish.bundle.desktop.swing.Activator.getBC().createFilter(filterS);
            }
            filterC.setToolTipText("Event filter");
            filterC.setForeground(txtColor);
            JEventPanel.this.model.getDispatcher().setFilter(filterS);
          } catch (Exception ex) {
            filterC.setForeground(Color.red);
            filterC.setToolTipText(ex.getMessage());
          }

        }
      });

    p1.add(filterC);

    add(scrollpane, BorderLayout.CENTER);
    add(p1, BorderLayout.NORTH);
  }

  void copyToClipBoard() {
    StringBuffer sb = new StringBuffer();

    for(Iterator it = model.getEntries().iterator(); it.hasNext();) {
      Event entry = (Event)it.next();
      sb.append(entry.toString());
      sb.append("\n");
    }

    setClipboardContents(sb.toString());
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents) {
  }
  
  
  void setClipboardContents( String str ){
    StringSelection stringSelection = new StringSelection( str );
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents( stringSelection, this );
  }

  public JEventTable getJEventTable() {
    return table;
  }

  public void updateUI() {
    setUI(UIManager.getUI(this));
  }


  public void open() {
  }

  public void close() {
    table.close();
  }
}
