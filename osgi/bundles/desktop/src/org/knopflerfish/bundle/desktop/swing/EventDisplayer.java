/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.knopflerfish.bundle.desktop.event.EventReaderDispatcher;
import org.knopflerfish.bundle.desktop.event.EventTableModel;
import org.knopflerfish.bundle.desktop.event.JEventEntryDetail;
import org.knopflerfish.bundle.desktop.event.JEventPanel;

public class EventDisplayer extends DefaultSwingBundleDisplayer {
  private DefaultListModel<String> allTopics = new DefaultListModel<>();
  private DefaultListModel<String> allKeys   = new DefaultListModel<>();
  private Set<String> selectedKeys = new LinkedHashSet<>();
  private Set<JEvent> views = new HashSet<>();

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

  ServiceRegistration<EventHandler> reg = null;

  public void open() {
    super.open();

    if(reg == null) {
      Dictionary<String, String[]> props = new Hashtable<>();
      props.put(EventConstants.EVENT_TOPIC,  new String[] { "*" });
      reg = bc.registerService(EventHandler.class,
                               eventHandler, props);
    }
  }



  EventHandler eventHandler = ev -> SwingUtilities.invokeLater(() -> {
    addTopic(ev.getTopic());
    addKeyNames(ev.getPropertyNames());
  });

  void addTopic(String topic) {
    if(!allTopics.contains(topic)) {
      allTopics.addElement(topic);
    }
  }

  void addKeyNames(String[] keys) {
    for (String key : keys) {
      if (!allKeys.contains(key)) {
        allKeys.addElement(key);
      }
    }
  }




  public void close() {

    if(reg != null) {
      reg.unregister();
      reg = null;
    }

    for (JEvent je : views) {
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

    for (JComponent component : components) {
      JEvent comp = (JEvent) component;
      comp.valueChanged(bl);
    }
  }

  class JEvent extends JPanel {
    private static final long serialVersionUID = 1L;

    JEventPanel           eventPanel;

    JEventEntryDetail     eventDetail;
    EventTableModel       eventModel;
    EventReaderDispatcher eventDispatcher;
    JFrame frame;

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
          private static final long serialVersionUID = 1L;

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
