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
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.osgi.service.event.Event;

public class JEventEntryDetail extends JPanel {
  Event entry;

  JEventTable table;

  JToolBar   cmdPanel;
  
  JButton  nextButton;
  JButton  prevButton;
  JButton  lastButton;
  JButton  firstButton;

  ImageIcon          arrowUpIcon;
  ImageIcon          arrowDownIcon;
  ImageIcon          arrowUp2Icon;
  ImageIcon          arrowDown2Icon;
  ImageIcon          reloadIcon;

  JScrollPane scrollPane;

  JDetail  info;

  EventTableModel model;
  
  public JEventEntryDetail(JEventTable table, Event entry) {
    //    super(new Date(entry.getTime()).toString() + ": " + entry.getMessage());

    this.entry  = entry;
    this.table  = table;

    setLayout(new BorderLayout());

    arrowUpIcon    = new ImageIcon(getClass().getResource("/1uparrow.png"));
    arrowUp2Icon   = new ImageIcon(getClass().getResource("/2uparrow.png"));
    arrowDownIcon  = new ImageIcon(getClass().getResource("/1downarrow.png"));
    arrowDown2Icon = new ImageIcon(getClass().getResource("/2downarrow.png"));
    reloadIcon     = new ImageIcon(getClass().getResource("/reload_green16.png"));    
    info = new JDetail(entry);

    scrollPane = new JScrollPane(info);
    //    contentPane.setPreferredSize(new Dimension(400, 200));

    cmdPanel = new JToolBar(JToolBar.VERTICAL);
    cmdPanel.setFloatable(false);

    nextButton = new JButton(arrowDownIcon) {	{
      setToolTipText("Next log entry");
      addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    showNext(1);
	  }
	});
    }};
    
    prevButton = new JButton(arrowUpIcon) { {
      setToolTipText("Previous log entry");
      addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    showNext(-1);
	  }
	});
    }};

    firstButton = new JButton(arrowUp2Icon) {	{
      setToolTipText("First log entry");
      addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    showFirst();
	  }
	});
    }};
    
    lastButton = new JButton(arrowDown2Icon) {	{
      setToolTipText("Last log entry");
      addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    showLast();
	  }
	});
    }};

    /*
    JButton reloadButton = new JButton(reloadIcon) {	{
      setToolTipText("Clear event list");
      addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
            JEventEntryDetail.this.model.clear();
	  }
	});
    }};
    */

    cmdPanel.add(firstButton);
    cmdPanel.add(prevButton);
    cmdPanel.add(nextButton);
    cmdPanel.add(lastButton);
    // cmdPanel.add(reloadButton);

    //    cmdPanel.add(reloadButton);

    add(scrollPane, BorderLayout.CENTER);
    add(cmdPanel,   BorderLayout.WEST);    

    syncUI();
  }

  public void setModel(EventTableModel model) {
    this.model = model;
  }

  synchronized public void setParentAndEntry(JEventTable table,
					     Event entry) {
    this.table = table;
    setEntry(entry);
    syncUI();
  }

  synchronized public void setEntry( Event _entry) {
    this.entry = _entry;
    
    info.setEntry(entry);

    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  JViewport vp = scrollPane.getViewport();
	  if(vp != null) {
	    vp.setViewPosition(new Point(0,0));
	    scrollPane.setViewport(vp);
	  } 
	  if(table != null && model !=  null) {
	    int row = model.getEntries().indexOf(entry);
	    Rectangle r = table.getCellRect(row, 0, true);
	    table.scrollRectToVisible(r);
	    revalidate();
	  }
	}
      });

  }


  void showNext(int delta) {
    if(entry != null && table != null) {
      Event e = table.getEventEntry(entry, delta);
      if(e != null) {
	setEntry(e);
	
	syncUI();
      }
    }
  }
  
  void showFirst() {
    if(entry != null && table != null) {
      try {
	java.util.List entries = table.model.getEntries();
	Event e = (Event)entries.get(0);
	if(e != null) {
	  setEntry(e);
	  
	  syncUI();
	}
      } catch (Exception e) {
      }
    }
  }

  void showLast() {
    if(entry != null && table != null) {
      try {
	java.util.List entries = table.model.getEntries();
	Event e = (Event)entries.get(entries.size() - 1);
	if(e != null) {
	  setEntry(e);
	  
	  syncUI();
	}
      } catch (Exception e) {
      }
    }
  }
  
  void syncUI() {
    if(entry != null && table != null) {
      Event prevE = table.getEventEntry(entry, -1);
      Event nextE = table.getEventEntry(entry, 1);
      
      prevButton.setEnabled(prevE != null);
      
      nextButton.setEnabled(nextE != null);
      
      table.setSelectedRowFromEntry(entry);
    } else {
      prevButton.setEnabled(false);
      nextButton.setEnabled(false);
    }
  }

  public void close() {
  }

}

class JDetail extends JTextPane {
  public JDetail(Event e) {
    super();
    setBackground(Color.white);
    setEditable(false);
    setContentType("text/html");

    setEntry(e);
  }

  void setEntry(Event e) {
    String s = "";
    if(e != null) {
      s = Util.toHTML(e);
      // String s = Util.toString(e);
    }
    super.setText(s);
  }
}
