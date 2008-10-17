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

import java.util.*;

import javax.swing.table.*;
import javax.swing.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

/**
 * Table model which keeps track of <tt>Event</tt> items.
 *
 * <p>
 * New entries should be added using <tt>logged</tt>
 * </p>
 */
public class EventTableModel extends DefaultTableModel {
  
  private ArrayList entries = new ArrayList();

  EventReaderDispatcher dispatcher;

  DefaultListModel allKeys;
  
  /**
   * Name of column headers
   */
  String[] headers = { 
    EventConstants.TIMESTAMP,
    EventConstants.EVENT_TOPIC,
    "bundle.id", 
    "message", 
  };

  public EventTableModel() {
    super();
    this.allKeys = allKeys;
  }

  public EventReaderDispatcher getDispatcher() {
    return dispatcher;
  }

  public void setDispatcher(EventReaderDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  
  /*
  public Class getColumnClass(int c) {
    return clazzes[c];
  }
  */

  public boolean isCellEditable(int row, int col) {
    return false;
  }

  public void setColumns(java.util.List names) {


    String[] h2 = new String[names.size()];
    {
      int i = 0;
      for(Iterator it = names.iterator(); it.hasNext(); ) {
        h2[i++] = it.next().toString();
      }
      
    }
    boolean bChanged = false;
    if(h2.length == headers.length) {
      for(int i = 0; i < headers.length; i++) {
        if(!h2[i].equals(headers[i])) {
          bChanged = true;
        }
      }
    } else {
      bChanged = true;
    }
    if(bChanged) {
      headers = h2;
      fireTableStructureChanged();
    } else {
    }
  }

  public int getColumnCount() {
    return headers.length;
  }

  public String getColumnName(int i) {
    return headers[i];
  }

  public java.util.List getEntries() {
    return entries;
  }

  Event getEntry(int row) {
    Event e = (Event)entries.get(row);
    return e;
  }


  public Object getValueAt(int row, int col) {
    Object val = getValueAt(getEntry(row), col);

    if(-1 != headers[col].indexOf("timestamp") && (val instanceof Long)) {
      return new Date(((Long)val).longValue());
    }

    return val;
  }

  public Object getValueAt(Event e, int col) {

    if(col >= getColumnCount()) { 
      return "";
    }

    return e.getProperty(headers[col]);
  }

  public int getRowCount() {
    return entries != null ? entries.size() : 0;
  }

  public void logged(Event entry) {
    entries.add(entry);
    fireTableRowsInserted(entries.size() - 1, entries.size());
  }

  public void clear() {
    entries.clear();
    fireTableDataChanged();
  }
}
