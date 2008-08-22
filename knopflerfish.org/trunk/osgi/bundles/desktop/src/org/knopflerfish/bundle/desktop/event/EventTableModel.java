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

import java.util.ArrayList;
import java.util.Date;

import javax.swing.table.AbstractTableModel;
import org.osgi.service.event.Event;

/**
 * Table model which keeps track of <tt>Event</tt> items.
 *
 * <p>
 * New entries should be added using <tt>logged</tt>
 * </p>
 */
public class EventTableModel extends AbstractTableModel {
  
  private ArrayList entries = new ArrayList();

  static final int COL_TIME      = 0;
  static final int COL_TOPIC     = 1;
  static final int COL_BUNDLE    = 2;
  static final int COL_MESSAGE   = 3;

  EventReaderDispatcher dispatcher;

  
  /**
   * Name of column headers
   */
  String[] headers = { 
    "Time", 
    "Topic", 
    "Bundle", 
    "Message", 
  };

  /**
   * Column classes.
   */
  Class [] clazzes = {
    Date.class,   // TIME
    String.class, // TOPIC
    String.class, // MESSAGE
    String.class, // BUNDLE
  };


  public EventTableModel() {
    super();
  }

  public EventReaderDispatcher getDispatcher() {
    return dispatcher;
  }


  public void setDispatcher(EventReaderDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  
  public Class getColumnClass(int c) {
    return clazzes[c];
  }

  public boolean isCellEditable(int row, int col) {
    return false;
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
    return getValueAt(getEntry(row), col);
  }

  public Object getValueAt(Event e, int col) {

    if(col >= getColumnCount()) { 
      throw new ArrayIndexOutOfBoundsException("Column " + col + 
					       " is larger than " + 
					       (getColumnCount() - 1));
    }

    switch(col) {
    case COL_TIME:
      return new Date(Util.getTime(e));
    case COL_TOPIC:
      return e.getTopic();
    case COL_BUNDLE:
      return Util.shortName(Util.getBundle(e));
    case COL_MESSAGE:
      return Util.getMessage(e);
    default:
      return null;
    }
  }

  public int getRowCount() {
    return entries.size();
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
