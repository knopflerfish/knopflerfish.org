/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import org.osgi.framework.*;
import org.osgi.service.startlevel.*;

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class TableDisplayer extends DefaultSwingBundleDisplayer {

  final BundleTableModel2 model;
  final ListSelectionModel rowSM;


  public TableDisplayer(BundleContext bc) {
    super(bc, "Details", "Table view of bundles", false);

    model = new BundleTableModel2();
    rowSM = new BundleTableRowSelectionModel();
  }

  public JComponent newJComponent() {
    return new JBundleTable();
  }


  public void bundleChanged(BundleEvent ev) {
    final Bundle cBundle = null!=ev ? ev.getBundle() : null;
    final long cBid = null!=cBundle ? cBundle.getBundleId() : -1;
    final Bundle[] oldBl = getBundleArray();
    super.bundleChanged(ev); // This will compute a new bundle list.
    final Bundle[] newBl = getBundleArray();

    // Fire table change events for the changes
    for (int row=0; row<newBl.length; row++) {
      if (row<oldBl.length) {
        if (oldBl[row].getBundleId() != newBl[row].getBundleId()) {
          // Fire row change for rows that now presents another bundle.
          model.fireTableRowsUpdated(row,row);
        }
        if (cBid==newBl[row].getBundleId()) {
          // Fire row change since the bundle on this row was changed.
          model.fireTableRowsUpdated(row,row);
        }
      } else {
        // The remainder are new rows.
        model.fireTableRowsInserted(row, newBl.length);
        break;
      }
    }
    if (newBl.length<oldBl.length) {
      // Some rows was removed
      model.fireTableRowsDeleted(newBl.length, oldBl.length -1);
    }

    // Update table selections to match new rows of selected bundles
    valueChanged(-1);
  }

  public void valueChanged(long bid) {
    try {
      bInValueChanged = true;

      if (null!=bundleSelModel) {

        if (0==bundleSelModel.getSelectionCount()) {
          rowSM.clearSelection();
        } else {
          // Update selection state for rows with a selection change.
          for (int row=0; row<model.getRowCount(); row++) {
            final long rowBid = model.getBundle(row).getBundleId();
            final boolean isSelected = bundleSelModel.isSelected(rowBid);
            if (isSelected != rowSM.isSelectedIndex(row)) {
              if (isSelected) {
                rowSM.addSelectionInterval(row, row);
              } else {
                rowSM.removeSelectionInterval(row, row);
              }
            }
          }
        }
      }
    } finally {
      bInValueChanged = false;
    }
  }

  class JBundleTable extends JPanel {
    JTable            table;

    public JBundleTable() {
      setLayout(new BorderLayout());


      table = new JTable() {
          public Color getGridColor() {
            return getBackground().darker();
          }
        };

      table.setModel(model);
      table.setSelectionModel(rowSM);

      //      Dimension size = new Dimension(500, 300);
      //      scroll.setPreferredSize(size);

      DefaultTableCellRenderer rightAlign =
        new DefaultTableCellRenderer();

      rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);

      table.getColumnModel().getColumn(COL_ID).setCellRenderer(rightAlign);

      setColumnWidth();

      JScrollPane scroll = new JScrollPane(table);
      add(scroll, BorderLayout.CENTER);
    }


    void setColumnWidth() {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            totalWidth = 0;
            setColWidth(COL_ID,         15);
            setColWidth(COL_NAME,       80);
            setColWidth(COL_STATE,      40);
            setColWidth(COL_STARTLEVEL, 20);
            setColWidth(COL_DESC,       100);
            setColWidth(COL_LOCATION,   80);
            setColWidth(COL_VENDOR,     60);
          }
        });
    }

    int totalWidth = 0;

    void setColWidth(int col, int w) {
      try {
        TableModel  model  = table.getModel();
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

        if(col < model.getColumnCount()) {
          TableColumn column = table.getColumnModel().getColumn(col);

          Component headerComp =
            headerRenderer
            .getTableCellRendererComponent(null, column.getHeaderValue(),
                                           false, false, 0, 0);
          int headerWidth = headerComp.getPreferredSize().width;


          w = Math.max(headerWidth, w);

          totalWidth += w;

          column.setMinWidth(10);
          column.setMaxWidth(300);

          column.setPreferredWidth(w);
        }
      } catch (Exception e) {
        Activator.log.warn("Failed to set column #" + col + " to width=" + w);
      }
    }
  }

  public static final int COL_ID         = 0;
  public static final int COL_STARTLEVEL = 1;
  public static final int COL_NAME       = 2;
  public static final int COL_STATE      = 3;
  public static final int COL_LOCATION   = 4;
  public static final int COL_DESC       = 5;
  public static final int COL_VENDOR     = 6;

  public static int COL_COUNT   = 7;

  class BundleTableModel2 extends AbstractTableModel {

    public BundleTableModel2() {
    }

    public int getRowFromBID(long bid) {
      if (-1==bid) return -1;

      final Bundle[] bl = getBundleArray();
      for(int i = 0; i < bl.length; i++) {
        if(bl[i].getBundleId() == bid) {
          return i;
        }
      }
      return -1;
    }

    public Bundle getBundle(int row) {
      return getBundleArray()[row];
    }

    public int getRowCount() {
      return getBundleArray().length;
    }

    public int getColumnCount() {
      return COL_COUNT;
    }

    public Class getColumnClass(int columnIndex) {
      Object obj = getValueAt(0, columnIndex);
      if (obj == null) {
        return Object.class;
      } else {
        return obj.getClass();
      }
    }


    public boolean isCellEditable(int row, int col) {
      return false;
    }


    public String getColumnName(int col) {
      switch(col) {
      case COL_LOCATION:   return "Location";
      case COL_ID:         return "Id";
      case COL_STATE:      return "State";
      case COL_STARTLEVEL: return "Level";
      case COL_NAME:       return "Name";
      case COL_DESC:       return "Description";
      case COL_VENDOR:     return "Vendor";
      default:             return "";
      }
    }

    public String getToolTipText(int row, int column) {
      String tt = "";

      if(column >= 0 && row >= 0) {

        Bundle b = getBundle(row);

        switch(column) {
        case COL_ID:
        case COL_LOCATION:
        case COL_STATE:
        case COL_STARTLEVEL:
        case COL_DESC:
        case COL_NAME:
        case COL_VENDOR:
          tt = Util.bundleInfo(b);
          break;
        default:
          break;
        }
        if(b.getState() == Bundle.UNINSTALLED) {
          tt = "Bundle is uninstalled";
        }
      }

      return tt;
    }


    public Object getValueAt(int row, int column) {
      Bundle b = getBundle(row);

      switch(column) {
      case COL_LOCATION:
        return Util.shortLocation(b.getLocation());
      case COL_ID:
        return Long.toString(b.getBundleId());
      case COL_STATE:
        return Util.stateName(b.getState());
      case COL_STARTLEVEL:
        {
          StartLevel sls = (StartLevel)Activator.desktop.slTracker.getService();

          if(null != sls) {
            try {
              int n = sls.getBundleStartLevel(b);
              return Integer.toString(n);
            } catch (Exception e) {
              return "not managed";
            }
          } else {
            return "no start level service";
          }
        }
      case COL_DESC:
        return Util.getHeader(b, "Bundle-Description");
      case COL_NAME:
        return Util.getHeader(b, "Bundle-Name");
      case COL_VENDOR:
        return Util.getHeader(b, "Bundle-Vendor");
      default:
        return null;
      }
    }
  }

  // A ListSelectionModel that delegates all selection operations not
  // originating from the bundleSelModel to the bundleSelModel-object.
  class BundleTableRowSelectionModel
    extends DefaultListSelectionModel
  {
    public BundleTableRowSelectionModel()
    {
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    public void addSelectionInterval(int index0, int index1)
    {
      if(!bInValueChanged) {
        final List selectedBundleIds = new ArrayList();
        for (int row=index0; row<=index1; row++) {
          final long rowBid = model.getBundle(row).getBundleId();
          selectedBundleIds.add(new Long(rowBid));
        }
        bundleSelModel.setSelected(selectedBundleIds, true);
      } else {
        super.addSelectionInterval(index0, index1);
      }
    }

    public void removeSelectionInterval(int index0, int index1)
    {
      if(!bInValueChanged) {
        final List selectedBundleIds = new ArrayList();
        for (int row=index0; row<=index1; row++) {
          final long rowBid = model.getBundle(row).getBundleId();
          selectedBundleIds.add(new Long(rowBid));
        }
        bundleSelModel.setSelected(selectedBundleIds, false);
      } else {
        super.removeSelectionInterval(index0, index1);
      }
    }

    public void setSelectionInterval(int index0, int index1)
    {
      if(!bInValueChanged) {
        bundleSelModel.clearSelection();
        final List selectedBundleIds = new ArrayList();
        final int startIx = index0<=index1 ? index0 : index1;
        final int endIx   = index0<=index1 ? index1 : index0;
        for (int row=startIx; row<=endIx; row++) {
          final long rowBid = model.getBundle(row).getBundleId();
          selectedBundleIds.add(new Long(rowBid));
        }
        bundleSelModel.setSelected(selectedBundleIds, true);
      } else {
        super.setSelectionInterval(index0, index1);
      }
    }

    public void clearSelection()
    {
      if(!bInValueChanged) {
        bundleSelModel.clearSelection();
      } else {
        super.clearSelection();
      }
    }
  }

}
