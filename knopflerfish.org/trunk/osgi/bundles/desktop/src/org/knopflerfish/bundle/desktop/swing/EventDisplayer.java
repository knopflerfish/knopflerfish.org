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
import java.util.*;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTable;
import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;


import org.knopflerfish.bundle.desktop.event.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.*;

public class EventDisplayer extends DefaultSwingBundleDisplayer {


  DefaultListModel          allTopics = new DefaultListModel();
  DefaultListModel          allKeys   = new DefaultListModel();

  Set selectedKeys = new LinkedHashSet();
  
  public EventDisplayer(BundleContext bc) {
    super(bc, "Events", "Show events", true); 

    // We're not interested in bundle events, nor in service events
    bUseListeners = false;
    allTopics.addElement("*");

    allKeys.addElement(EventConstants.TIMESTAMP);
    allKeys.addElement(EventConstants.EVENT_TOPIC);
    allKeys.addElement("bundle.id");
    allKeys.addElement("message");
    
    selectedKeys.add(EventConstants.TIMESTAMP);
    selectedKeys.add(EventConstants.EVENT_TOPIC);
    selectedKeys.add("bundle.id");
    selectedKeys.add("message");
  }

  ServiceRegistration reg = null;

  public void open() {
    super.open();

    if(reg == null) {
      Hashtable props = new Hashtable();
      props.put(EventConstants.EVENT_TOPIC,  new String[] { "*" });
      reg = bc.registerService(EventHandler.class.getName(), eventHandler, props);
    }

    // System.out.println("****** open: " + eventModel + ", " + eventModel.getDispatcher());
  }


  
  EventHandler eventHandler = new EventHandler() {
      public void handleEvent(final Event ev) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              addTopic(ev.getTopic());
              addKeyNames(ev.getPropertyNames());
            }
          });
      }
    };

  void addTopic(String topic) {
    if(!allTopics.contains(topic)) {
      allTopics.addElement(topic);
    }
  }

  void addKeyNames(String[] keys) {
    for(int i = 0; i < keys.length; i++) {
      if(!allKeys.contains(keys[i])) {
        allKeys.addElement(keys[i]);
      }
    }
  }


  
  public void close() {

    if(reg != null) {
      reg.unregister();
      reg = null;
    }

    for(Iterator it = views.iterator(); it.hasNext(); ) {
      JEvent je = (JEvent)it.next();
      je.close();
    }
    views.clear();

    super.close();
  }
  
  public JComponent newJComponent() {
    return new JEvent();
  }

  public void valueChanged(long  bid) {
    Bundle[] bl = Activator.desktop.getSelectedBundles();
    
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JEvent comp = (JEvent)it.next();
      comp.valueChanged(bl);
    }
  }
  
  Set views = new HashSet();

  class JEvent extends JPanel {
    JEventPanel           eventPanel;

    JEventEntryDetail     eventDetail;
    // FilterEventTableModel filterEventModel;
    EventTableModel       eventModel;
    EventReaderDispatcher eventDispatcher;
    JFrame frame;

    JEvent() {
      setLayout(new BorderLayout());

      eventModel = new EventTableModel();
      eventDispatcher = new EventReaderDispatcher(bc, eventModel);
      eventModel.setDispatcher(eventDispatcher);
      eventDispatcher.open();
      eventDispatcher.getAll();

      // filterEventModel = new FilterEventTableModel(eventModel);
      // filterEventModel.setBundles(null);

      // construct in two steps
      eventDetail = new JEventEntryDetail(null, null);
      eventPanel  = new JEventPanel(allTopics, 
                                    allKeys, 
                                    selectedKeys,
                                    eventModel, eventDetail, false) {
          public void newWindow() {
            frame = new JFrame();
            JEvent newPanel = new JEvent();
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                  frame.dispose();
                }
              });
            frame.getContentPane().add(newPanel);

            frame.pack();
            frame.setVisible(true);
          }
        };

      eventDetail.setParentAndEntry(eventPanel.getJEventTable(), null);
      eventDetail.setModel(eventModel);
      
      JSplitPane splitPane = 
	new JSplitPane(JSplitPane.VERTICAL_SPLIT,
		       eventPanel,
		       eventDetail);
      
      splitPane.setDividerLocation(120);

      add(splitPane, BorderLayout.CENTER);

      views.add(this);
    }

    public void close() {
      eventDispatcher.close();
      if(frame != null) {
        frame.setVisible(false);
      }
      frame = null;
    }

    public void valueChanged(Bundle[] bl) {
      /*
      filterEventModel.setBundles(bl);

      JTable table = eventPanel.getJEventTable();

      if(table.getRowCount() > 0) {
	table.setRowSelectionInterval(0, 0);
      }
      */
    }
  }
}

