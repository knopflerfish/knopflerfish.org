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

package org.knopflerfish.bundle.log.window.impl;

import java.util.*;
import java.text.*;

import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.knopflerfish.service.log.LogRef;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;

/**
 * Table subclass which displays <tt>ExtLogEntry</tt> items.
 */
public class JLogTable extends JTable {

  LogTableModel    model;
  JLogEntryDetail logEntryDetail;

  public JLogTable(LogTableModel   model, 
		   JLogEntryDetail logEntryDetail,
		   boolean         bSort) {
    super();
    
    this.logEntryDetail = logEntryDetail;
    this.model          = model;

    if(false) {
      TableSorter sorter = new TableSorter(model);
      
      setModel(sorter);
      sorter.addMouseListenerToHeaderInTable(this);
    } else {
      setModel(model);
    }

    setDefaultRenderer(Date.class, 
		       new DateCellRenderer("HH:mm:ss, MMM dd"));
    
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    ListSelectionModel rowSM = getSelectionModel();

    rowSM.addListSelectionListener(new ListSelectionListener() {
	public void valueChanged(ListSelectionEvent e) {

	  if (e.getValueIsAdjusting()) {
	    return;
	  }

	  ListSelectionModel lsm = (ListSelectionModel)e.getSource();

	  if (lsm.isSelectionEmpty()) {
	    //
	  } else {	    
            int ix = lsm.getMinSelectionIndex();

	    ExtLogEntry entry = JLogTable.this.model.getEntry(ix);

	    if(entry != null) {
	      JLogTable.this.logEntryDetail.setParentAndEntry(JLogTable.this, entry);
	    }
	    
	  }
	}
      });

    getColumnModel().getColumn(LogTableModel.COL_ID).setPreferredWidth(30);
    getColumnModel().getColumn(LogTableModel.COL_BID).setPreferredWidth(30);
    getColumnModel().getColumn(LogTableModel.COL_LEVEL).setPreferredWidth(40);
    getColumnModel().getColumn(LogTableModel.COL_MESSAGE).setPreferredWidth(150);    
  }

  ExtLogEntry getLogEntry(LogEntry e, int delta) {
    int ix = model.getEntries().indexOf(e);
    if(ix != -1) {
      int i = ix + delta;
      if(i >= 0 && i < model.getEntries().size()) {
	return (ExtLogEntry)model.getEntries().get(i);
      }
    }
    return null;
  }

  void setSelectedRowFromEntry(ExtLogEntry e) {
    int ix = model.getEntries().indexOf(e);
    if(ix != -1) {
      setRowSelectionInterval(ix, ix);
    }
  }

  public Color getGridColor() {
    return getBackground().darker();
  }

  public void close() {


  }
}


