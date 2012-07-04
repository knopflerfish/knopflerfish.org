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

package org.knopflerfish.bundle.log.window.impl;

import java.util.ArrayList;
import java.util.Date;

import javax.swing.table.AbstractTableModel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


/**
 * Table model which keeps track of <tt>ExtLogEntry</tt> items.
 *
 * <p>
 * New entries should be added using <tt>logged</tt>
 * </p>
 */
public class LogTableModel extends AbstractTableModel {

  private static final String CAPACITY_PROP_NAME
    = LogTableModel.class.getName() + ".capacity";

  private int capacity = 342;

  private final ArrayList entries = new ArrayList();

  static final int COL_ID        = 0;
  static final int COL_BID       = 1;
  static final int COL_TIME      = 2;
  static final int COL_LEVEL     = 3;
  static final int COL_MESSAGE   = 4;
  static final int COL_EXCEPTION = 5;

  /**
   * Name of column headers
   */
  final String[] headers = {
    "#",
    "bid",
    "Time",
    "Level",
    "Message",
    "Exception",
  };

  /**
   * Column classes.
   */
  final Class [] clazzes = {
    Long.class, // ID
    Long.class, // BID
    Date.class,   // TIME
    String.class, // LEVEL
    String.class, // MESSAGE
    Throwable.class, // EXCEPTION
  };


  public LogTableModel(BundleContext bc) {
    super();

    final String capacityS = bc.getProperty(CAPACITY_PROP_NAME);
    if (null!=capacityS && capacityS.length()>0) {
      try {
        capacity = Integer.parseInt(capacityS.trim());
      } catch (NumberFormatException nfe){
      }
    }
    if (capacity>0) {
      entries.ensureCapacity(capacity);
    }
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

  ExtLogEntry getEntry(int row) {
    final ExtLogEntry e = (ExtLogEntry)entries.get(row);
    return e;
  }


  public Object getValueAt(int row, int col) {
    return getValueAt(getEntry(row), col);
  }

  public Object getValueAt(ExtLogEntry e, int col) {

    if(col >= getColumnCount()) {
      throw new ArrayIndexOutOfBoundsException
        ("Column " +col +" is larger than " +(getColumnCount() - 1));
    }

    switch(col) {
    case COL_ID:
      return new Long(e.getId());
    case COL_BID:
      final Bundle b = e.getBundle();
      return null!=b ? (Object) new Long(b.getBundleId()) : (Object)"";
    case COL_LEVEL:
      return Util.levelString(e.getLevel());
    case COL_TIME:
      return new Date(e.getTime());
    case COL_MESSAGE:
      return e.getMessage();
    case COL_EXCEPTION:
      return e.getException();
    default:
      return null;
    }
  }

  public int getRowCount() {
    return entries.size();
  }

  public void logged(ExtLogEntry entry) {
    if (capacity>0 && capacity == entries.size()) {
      // List is full; remove oldest (first) entry.
      ExtLogEntry oldEntry = (ExtLogEntry) entries.remove(0);
      fireTableRowsDeleted(0, 0);
    }
    entries.add(entry);
    fireTableRowsInserted(entries.size() - 1, entries.size());
  }

  public void clear() {
    entries.clear();
    fireTableDataChanged();
  }
}
