/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import org.osgi.service.event.Event;
import org.osgi.framework.Filter;


public class JEventPanel extends JPanel implements ClipboardOwner {

  JTextArea text;
  JComboBox topicC;
  JTextField filterC;

  EventTableModel model;
  JEventTable     table;
  JScrollPane   scrollpane;
  JPopupMenu    popup;
  Color txtColor;

  DefaultComboBoxModel topicModel;
  DefaultListModel allTopics;
  DefaultListModel allKeys;

  Set selectedKeys;

  boolean popupOK = false;

  public JEventPanel(DefaultListModel allTopics,
                     DefaultListModel allKeys,
                     Set              selectedKeys,
                     EventTableModel   model,
                     JEventEntryDetail logEntryDetail,
                     boolean         bSort) {
    super(new BorderLayout());
    this.allTopics = allTopics;
    this.allKeys   = allKeys;
    this.selectedKeys = new LinkedHashSet();
    this.selectedKeys.addAll(selectedKeys);
    this.model = model;

    topicModel = new DefaultComboBoxModel();
    topicC     = new JComboBox(topicModel);
    topicC.setEditable(true);

    allTopics.addListDataListener(new ListDataListener() {
        public void     contentsChanged(ListDataEvent e) {
          updateTopics();
        }
        public void     intervalAdded(ListDataEvent e) {
          updateTopics();
        }
        public void     intervalRemoved(ListDataEvent e) {
          updateTopics();
        }
      });

    allKeys.addListDataListener(new ListDataListener() {
        public void     contentsChanged(ListDataEvent e) {
          updateKeys();
        }
        public void     intervalAdded(ListDataEvent e) {
          updateKeys();
        }
        public void     intervalRemoved(ListDataEvent e) {
          updateKeys();
        }
      });

    table = new JEventTable(model, logEntryDetail, bSort);
    scrollpane = new JScrollPane(table);

    popup = new JPopupMenu();

    MouseListener ml = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
          maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
          if (e.isPopupTrigger()) {
            makePopup();
            popup.show(e.getComponent(), e.getX(), e.getY());
          }
        }
      };

    getJEventTable().addMouseListener(ml);
    getJEventTable().getTableHeader().addMouseListener(ml);

    JLabel jl;
    JPanel tPanel = new JPanel(new BorderLayout());

    jl = new JLabel("Topic:");
    jl.setSize(new Dimension(100, jl.getSize().height));
    tPanel.add(jl, BorderLayout.WEST);

    updateTopics();

    topicModel.setSelectedItem("*");

    updateTableModel();

    topicC.setMaximumSize(new Dimension(120, 50));
    topicC.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String topicS = topicC.getSelectedItem().toString();
          setTopic(topicS);
        }
      });

    tPanel.add(topicC, BorderLayout.CENTER);



    JPanel fPanel = new JPanel(new BorderLayout());
    jl = new JLabel("Filter:");
    jl.setSize(new Dimension(100, jl.getSize().height));

    fPanel.add(jl, BorderLayout.WEST);

    filterC = new JTextField(model.getDispatcher().getFilter(), 8);
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
            JEventPanel.this.model.clear();
            JEventPanel.this.model.getDispatcher().setFilter(filterS);
          } catch (Exception ex) {
            System.out.println("bad filter " + filterS + ", " + ex.getMessage());
            filterC.setForeground(Color.red);
            filterC.setToolTipText(ex.getMessage());
          }

        }
      });

    fPanel.add(filterC, BorderLayout.CENTER);



    JButton newButton = new JButton("New");
    newButton.setToolTipText("New event view window");
    newButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          newWindow();
        }
      });



    JButton clearButton = new JButton("Clear");
    clearButton.setToolTipText("Clear event list");
    clearButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          JEventPanel.this.model.clear();
        }
      });

    JButton sendButton = new JButton("Send...");
    sendButton.setToolTipText("Send event...");
    sendButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          openSendFrame();
        }
      });

    JPanel p2 = new JPanel();
    p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));

    p2.add(tPanel);

    JPanel p3 = new JPanel();
    p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));

    p3.add(fPanel);
    p3.add(newButton);
    p3.add(clearButton);
    p3.add(sendButton);

    p2.add(p3);

    add(scrollpane, BorderLayout.CENTER);
    add(p2, BorderLayout.NORTH);
  }

  public void setTopic(String topicS) {
    if(-1 == topicModel.getIndexOf(topicS)) {
      topicModel.addElement(topicS);
    }

    JEventPanel.this.model.getDispatcher().setTopic(topicS);
    JEventPanel.this.model.clear();
  }

  JFrame sendFrame;
  JSendEventPanel sendPanel;

  void openSendFrame() {
    if(sendFrame == null) {
      sendFrame = new JFrame("Send event");
      sendFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      sendFrame.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            sendFrame.setVisible(false);
          }
        });
      sendPanel = new JSendEventPanel(table, allTopics) {
          public void doClose() {
            sendFrame.setVisible(false);
          }
        };
      sendPanel.setBorder(BorderFactory.createTitledBorder("Send Event"));
      sendFrame.getContentPane().add(sendPanel);
      sendFrame.pack();
    }
    sendFrame.setVisible(true);
  }

  JMenuItem copyItem = new JMenuItem("Copy events to clipboard") {
      {
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              copyToClipBoard();
            }
          });
      }
    };

  void updateKeys() {
    popupOK = false;
  }

  ArrayList cbList = new ArrayList();

  void makePopup() {
    if(popupOK) {
      return;
    }

    popup.removeAll();
    popup.add(copyItem);
    popup.add(new JPopupMenu.Separator());

    cbList.clear();

    Set keys = new TreeSet();
    for(int i = 0; i < allKeys.getSize(); i++) {
      String val = allKeys.getElementAt(i).toString();
      keys.add(val);
    }
    for(Iterator it = keys.iterator(); it.hasNext(); ) {
      final String val = (String)it.next();
      final JCheckBoxMenuItem cb = new JCheckBoxMenuItem(val);
      if(selectedKeys.contains(val)) {
        cb.setState(true);
      }
      cbList.add(cb);
      cb.addItemListener(new ItemListener() {
          public void   itemStateChanged(ItemEvent e) {
            if(cb.getState()) {
              selectedKeys.add(val);
            } else {
              selectedKeys.remove(val);

              // avoid zero-column table
              if(selectedKeys.size() == 0) {
                selectedKeys.add(val);
                cb.setState(true);
              }
            }
            updateTableModel();
          }
        });
      popup.add(cb);
    }

    popupOK = true;
  }

  void updateTableModel() {
    ArrayList names = new ArrayList();
    for(Iterator it = selectedKeys.iterator(); it.hasNext(); ) {
      String name = (String)it.next();
      names.add(name);
    }
    model.setColumns(names);
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
    if(topicC != null) {
      topicC.setModel(cbModel);
    }
  }

  public void newWindow() {
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
    if(sendFrame != null) {
      sendFrame.setVisible(false);
      sendFrame.dispose();
      sendFrame = null;
    }
  }
}
