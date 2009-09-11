/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

  Set views = new HashSet();

  public EventDisplayer(BundleContext bc) {
    super(bc, "Events", "Show events", true);

    // We're not interested in bundle events, nor in service events
    bUseListeners = false;
    allTopics.addElement("*");

    allKeys.addElement(EventConstants.TIMESTAMP);
    allKeys.addElement(EventConstants.EVENT_TOPIC);
    allKeys.addElement(EventConstants.EVENT_FILTER);
    allKeys.addElement(EventConstants.BUNDLE_SIGNER);
    allKeys.addElement(EventConstants.BUNDLE_SYMBOLICNAME);
    allKeys.addElement(EventConstants.BUNDLE_ID);
    allKeys.addElement(EventConstants.BUNDLE);
    allKeys.addElement(EventConstants.EVENT);
    allKeys.addElement(EventConstants.EXCEPTION);
    allKeys.addElement(EventConstants.EXCEPTION_CLASS);
    allKeys.addElement(EventConstants.EXCEPTION_MESSAGE);
    allKeys.addElement(EventConstants.MESSAGE);
    allKeys.addElement(EventConstants.SERVICE);
    allKeys.addElement(EventConstants.SERVICE_ID);
    allKeys.addElement(EventConstants.SERVICE_OBJECTCLASS);
    allKeys.addElement(EventConstants.TIMESTAMP);
    allKeys.addElement("service.pid");
    allKeys.addElement("log.entry");
    allKeys.addElement("log.level");

    selectedKeys.add(EventConstants.TIMESTAMP);
    selectedKeys.add(EventConstants.EVENT_TOPIC);
    selectedKeys.add(EventConstants.BUNDLE_ID);
    selectedKeys.add(EventConstants.MESSAGE);
  }

  ServiceRegistration reg = null;

  public void open() {
    super.open();

    if(reg == null) {
      Hashtable props = new Hashtable();
      props.put(EventConstants.EVENT_TOPIC,  new String[] { "*" });
      reg = bc.registerService(EventHandler.class.getName(),
                               eventHandler, props);
    }
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
    JEvent je = new JEvent();
    views.add(je);
    return je;
  }

  public void newFramedJComponent() {
    JFrame frame = new JFrame();
    final JEvent je = new JEvent(frame);

    frame.setTitle("Events: *");
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          je.close();
        }
      });
    frame.getContentPane().add(je);

    frame.pack();
    frame.setVisible(true);

    views.add(je);
  }

  public void valueChanged(long  bid) {
    Bundle[] bl = Activator.desktop.getSelectedBundles();

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JEvent comp = (JEvent)it.next();
      comp.valueChanged(bl);
    }
  }



  class JEvent extends JPanel {
    JEventPanel           eventPanel;

    JEventEntryDetail     eventDetail;
    EventTableModel       eventModel;
    EventReaderDispatcher eventDispatcher;
    JFrame frame;
    Set panels = new HashSet();

    JEvent() {
      this(null);
    }

    JEvent(JFrame _frame) {
      this.frame = _frame;

      setLayout(new BorderLayout());

      eventModel = new EventTableModel(bc);
      eventDispatcher = new EventReaderDispatcher(bc, eventModel);
      eventModel.setDispatcher(eventDispatcher);
      eventDispatcher.open();
      eventDispatcher.getAll();

      // construct in two steps
      eventDetail = new JEventEntryDetail(null, null);
      eventPanel  = new JEventPanel(allTopics,
                                    allKeys,
                                    selectedKeys,
                                    eventModel, eventDetail, false) {
          public void setTopic(String topicS) {
            super.setTopic(topicS);
            if(frame != null) {
              frame.setTitle("Events: " + topicS);
            }
          }

          public void newWindow() {
            newFramedJComponent();
          }
        };

      eventDetail.setParentAndEntry(eventPanel.getJEventTable(), null);
      eventDetail.setModel(eventModel);

      JSplitPane splitPane =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                       eventPanel,
                       eventDetail);

      splitPane.setDividerLocation(170);

      add(splitPane, BorderLayout.CENTER);
    }

    public void close() {
      eventDispatcher.close();
      eventPanel.close();
      eventDetail.close();
      if(frame != null) {
        frame.setVisible(false);
        frame.dispose();
      }
      frame = null;
    }

    public void valueChanged(Bundle[] bl) {
      // NOOP
    }
  }
}
