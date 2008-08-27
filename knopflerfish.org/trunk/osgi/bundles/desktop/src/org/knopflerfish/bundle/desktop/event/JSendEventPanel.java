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

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
// import javax.swing.event.*;
import javax.swing.table.*;

import java.util.*;

import org.osgi.service.event.*;
import org.osgi.framework.ServiceReference;


public class JSendEventPanel extends JPanel  {
  JComboBox topicC;
  DefaultComboBoxModel topicModel;
  DefaultListModel allTopics;
  JTable propTable;

  JEventTable     table;

  public JSendEventPanel(JEventTable     _table, 
                         DefaultListModel _allTopics) {
    super(new BorderLayout());
    
    this.table     = _table;
    this.allTopics = _allTopics;

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
    
    // jl = new JLabel("Topic:");
    // jl.setSize(new Dimension(100, jl.getSize().height));
    // tPanel.add(jl, BorderLayout.WEST);
    tPanel.add(topicC, BorderLayout.CENTER);
    
    tPanel.setBorder(BorderFactory.createTitledBorder("Event topic"));


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

    JButton copyButton = new JButton("Copy info from selected");
    copyButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          int ix = table.getSelectedRow();
          if(ix != -1) {
            Event event = table.model.getEntry(ix);
            setEvent(event);
          } else {
            setEvent(null);
          }
          makeModel(templateEvent);
        }
      });
    
    JButton clearButton = new JButton("Clear");
    clearButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          data[0].clear();
          data[1].clear();
          ((AbstractTableModel)propTable.getModel()).fireTableDataChanged();
        }
      });

    JButton addButton = new JButton("Add property");
    addButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          data[0].add("");
          data[1].add("");
          ((AbstractTableModel)propTable.getModel()).fireTableDataChanged();
        }
      });
                                 
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(sendButton);
    buttonPanel.add(closeButton);

    propTable = new JTable() {
        public TableCellRenderer getCellRenderer(int row, int column) {
          Object val = getValueAt(row, column);
          
          return getDefaultRenderer(val != null ? val.getClass() : String.class);    
        }
        public TableCellEditor getCellEditor(int row, int column) {
          Object val = getValueAt(row, column);
          
          TableCellEditor ce = getDefaultEditor(val != null ? val.getClass() : String.class);    
          return ce;
        }
      };

    makeModel(null);

    JPanel propPanel = new JPanel();
    propPanel.setLayout(new BoxLayout(propPanel, BoxLayout.Y_AXIS));
    propPanel.setBorder(BorderFactory.createTitledBorder("Event properties"));

    JPanel tableButtonPanel = new JPanel();
    tableButtonPanel.setLayout(new BoxLayout(tableButtonPanel, BoxLayout.X_AXIS));
    tableButtonPanel.add(copyButton);
    tableButtonPanel.add(addButton);
    tableButtonPanel.add(clearButton);

    JScrollPane scroll = new JScrollPane(propTable);
    
    propPanel.add(scroll);
    propPanel.add(tableButtonPanel);


    add(tPanel,      BorderLayout.NORTH);
    add(propPanel,   BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  Event templateEvent;

  void setEvent(Event ev) {
    this.templateEvent = ev;
  }

  ArrayList[] data;

  void makeModel(Event template) {    

    data = new ArrayList[] { new ArrayList(), new ArrayList() };
    int extraRowCount = 10;

    if(template != null) {
      String[] names = template.getPropertyNames();
      for(int i = 0; i < names.length; i++) {
        data[0].add(names[i]);
        data[1].add(template.getProperty(names[i]));
      }
      extraRowCount = 3;
      topicC.setSelectedItem(template.getTopic());
    }
    // add some empty data
    for(int i = 0; i < extraRowCount; i++) {
      data[0].add("");
      data[1].add("");
    }
      
    AbstractTableModel model = new AbstractTableModel() {
        public String 	getColumnName(int column) {
          return column == 0 ? "Name" : "Value";
        }

        public boolean 	isCellEditable(int rowIndex, int columnIndex) {
          return true;
        }
   
        public int getColumnCount() { return 2; }
        public int getRowCount() { return data[0].size();}
        public Object getValueAt(int row, int col) { 
          return data[col].get(row);
        }
        public void setValueAt(Object val, int row, int col) { 
          Object oldVal = data[col].get(row);
          if(oldVal != null && val != null) {
            if(oldVal.getClass() != val.getClass()) {
              try {
                Object val2 = oldVal.getClass()
                  .getConstructor(new Class[] { String.class})
                  .newInstance(new Object[] { val.toString() });
                val = val2;
              } catch (Exception e) {
              }
            }
          }
          data[col].set(row, val);
        }
      };

    propTable.setModel(model);
    // propTable.invalidate();
    // propTable.repaint();
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
          props.put(name, val);
        }
      }
    }
    props.put("timestamp.generated", new Long(System.currentTimeMillis()));

    org.osgi.service.event.Event ev = 
      new org.osgi.service.event.Event(topic, props);


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

