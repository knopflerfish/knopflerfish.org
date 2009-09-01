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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Table model which filters events from a set of bundles.
 *
 */
public class FilterLogTableModel
  extends    LogTableModel
  implements TableModelListener
{

  Object lock = new Object();

  LogTableModel model;
  Set           bundles         = new HashSet();
  ArrayList     filteredEntries = new ArrayList();

  public FilterLogTableModel(BundleContext bc,
                             LogTableModel model)
  {
    super(bc);
    this.model = model;
    model.addTableModelListener(this);
  }

  public void setBundles(Bundle[] bl) {
    synchronized(lock) {
      bundles.clear();
      for(int i = 0; bl != null && i < bl.length; i++) {
        bundles.add(bl[i]);
      }
    }
    fireTableDataChanged();
  }

  public Class getColumnClass(int c) {
    return model.getColumnClass(c);
  }

  public boolean isCellEditable(int row, int col) {
    synchronized(lock) {
      return model.isCellEditable(row, col);
    }
  }


  public int getColumnCount() {
    return model.getColumnCount();
  }

  public String getColumnName(int i) {
    return model.getColumnName(i);
  }

  /**
   * Return true if an entry matches any of the filter bundles.
   */
  boolean isValidEntry(ExtLogEntry e) {
    synchronized(lock) {
      if(bundles.size() == 0) {
        return true;
      }

      for(Iterator it = bundles.iterator(); it.hasNext();) {
        Bundle b = (Bundle)it.next();
        if((e.getBundle() != null) &&
           (b.getBundleId() == e.getBundle().getBundleId())) {
          return true;
        }
      }

      return false;
    }
  }

  public Object getValueAt(int row, int col) {
    synchronized(lock) {
      return model.getValueAt(getEntry(row), col);
    }
  }

  public java.util.List getEntries() {
    synchronized(lock) {
      return filteredEntries;
    }
  }

  public int getRowCount() {
    synchronized(lock) {
      return filteredEntries.size();
    }
  }


  public void clear() {
    synchronized(lock) {
      model.getEntries().clear();
    }
    fireTableDataChanged();
  }


  public ExtLogEntry getEntry(int row) {
    synchronized(lock) {
      ExtLogEntry e = (ExtLogEntry)filteredEntries.get(row);

      return e;
    }
  }


  private void filterEntries() {
    synchronized(lock) {
      filteredEntries.clear();

      for(Iterator it = model.getEntries().iterator(); it.hasNext(); ) {
        ExtLogEntry e = (ExtLogEntry)it.next();
        if(isValidEntry(e)) {
          filteredEntries.add(e);
        }
      }
    }
  }

  public void fireTableDataChanged() {
    filterEntries();
    super.fireTableDataChanged();
  }


  public void tableChanged(TableModelEvent e) {
    fireTableDataChanged();
  }
}
