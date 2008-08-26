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

import org.knopflerfish.bundle.desktop.swing.Activator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import org.osgi.service.event.*;
import org.osgi.framework.ServiceReference;


public class JSendEventPanel extends JPanel  {
  JComboBox topicC;
  DefaultComboBoxModel topicModel;
  DefaultListModel allTopics;
  JTable propTable;

  public JSendEventPanel(DefaultListModel allTopics) {
    super(new BorderLayout());

    this.allTopics = allTopics;

    topicModel = new DefaultComboBoxModel();
    topicC = new JComboBox(topicModel); 
    topicC.setEditable(true);
    
    topicC.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String topicS = topicC.getSelectedItem().toString();
          if(-1 == topicModel.getIndexOf(topicS)) {
            topicModel.addElement(topicS);
          }
          
        }
      });

    updateTopics();

    JLabel jl;    
    JPanel tPanel = new JPanel(new BorderLayout());
    
    jl = new JLabel("Topic:");
    jl.setSize(new Dimension(100, jl.getSize().height));
    tPanel.add(jl, BorderLayout.WEST);
    tPanel.add(topicC, BorderLayout.CENTER);
    
    add(tPanel, BorderLayout.NORTH);

    JButton sendButton = new JButton("Send");
    sendButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          doSend();
        }
      });

    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          doClose();
        }
      });

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(sendButton);
    buttonPanel.add(closeButton);

    String[][] vals = new String[10][2];

    propTable = new JTable(vals, new String[] {"Name", "Value"});

    JScrollPane scroll = new JScrollPane(propTable);

    add(scroll, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  void doSend() {
    String topic = (String)topicC.getSelectedItem();
    Hashtable props = new Hashtable();


    for(int i = 0; i < propTable.getRowCount(); i++) {
      Object key = propTable.getValueAt(i, 0);
      Object val = propTable.getValueAt(i, 1);

      if(key != null && !"".equals(key)) {
        String name = key.toString();
        if(val != null) {
          props.put(name, val.toString());
        }
      }
    }
    org.osgi.service.event.Event ev = 
      new org.osgi.service.event.Event(topic, props);

    System.out.println("doSend event=" + ev);    

    ServiceReference sr = Activator.getBC().getServiceReference(EventAdmin.class.getName());
    if(sr != null) {
      try {
        EventAdmin ea = (EventAdmin)Activator.getBC().getService(sr);
        ea.postEvent(ev);
      } finally {
        Activator.getBC().ungetService(sr);
      }
    }
  }

  void doClose() {
  }

 void updateTopics() {
    DefaultComboBoxModel cbModel = new DefaultComboBoxModel();
    for(int i = 0; i < topicModel.getSize(); i++) {
      Object val = topicModel.getElementAt(i);
      cbModel.addElement(val);
    }
    for(int i = 0; i < allTopics.getSize(); i++) {
      Object val = allTopics.getElementAt(i);
      if(-1 == cbModel.getIndexOf(val)) {
        cbModel.addElement(val);
      }
    }
    cbModel.setSelectedItem(topicModel.getSelectedItem());
    topicModel = cbModel;
    topicC.setModel(cbModel);
  }


}

