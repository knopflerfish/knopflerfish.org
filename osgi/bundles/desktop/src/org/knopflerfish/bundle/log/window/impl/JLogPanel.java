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

package org.knopflerfish.bundle.log.window.impl;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

public class JLogPanel extends JPanel implements ClipboardOwner {
  private static final long serialVersionUID = 1L;

  private LogTableModel model;
  private JLogTable     table;
  private JPopupMenu    popup;

  public JLogPanel(LogTableModel model,
                   JLogEntryDetail logEntryDetail) {
    super(new BorderLayout());
    this.model = model;
    
    table = new JLogTable(model, logEntryDetail);
    JScrollPane scrollpane = new JScrollPane(table);
    
    popup = new JPopupMenu() {
      private static final long serialVersionUID = 1L;
      {
        add(new JMenuItem("Copy log to clipboard") {

          private static final long serialVersionUID = 1L;

          {
            addActionListener(ev -> copyToClipBoard());
          }
        });
      }
    };
    
    getJLogTable().addMouseListener(new MouseAdapter() {
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

    add(scrollpane, BorderLayout.CENTER);
  }

  void copyToClipBoard() {
    StringBuilder sb = new StringBuilder();

    for(ExtLogEntry entry : model.getEntries()) {
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

  public JLogTable getJLogTable() {
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
