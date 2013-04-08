/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;


public class TableDisplayer extends DefaultSwingBundleDisplayer {

  final BundleTableModel2 model;
  final ListSelectionModel rowSM;


  public TableDisplayer(BundleContext bc) {
    super(bc, "Table", "Table view of bundles", false);

    model = new BundleTableModel2();
    rowSM = new BundleTableRowSelectionModel();
  }

  @Override
  public JComponent newJComponent() {
    return new JBundleTable();
  }


  @Override
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

    // Update column widths to handle columns with dynamic max-width.
    for (final JComponent jComp : components) {
      final JBundleTable table = (JBundleTable) jComp;
      table.setColumnWidth();
    }

    // Update table selections to match new rows of selected bundles
    valueChanged(-1);
  }

  @Override
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
    private static final long serialVersionUID = 1L;
    JTable            table;

    public JBundleTable() {
      setLayout(new BorderLayout());


      table = new JTable() {
          private static final long serialVersionUID = 1L;

          @Override
          public Color getGridColor() {
            return getBackground().darker();
          }
        };

      table.setModel(model);
      table.setSelectionModel(rowSM);

      //      Dimension size = new Dimension(500, 300);
      //      scroll.setPreferredSize(size);

      final DefaultTableCellRenderer rightAlign =
        new DefaultTableCellRenderer();
      rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);

      table.getColumnModel().getColumn(COL_ID).setCellRenderer(rightAlign);
      table.getColumnModel().getColumn(COL_STARTLEVEL).setCellRenderer(rightAlign);

      setColumnWidth();

      final JScrollPane scroll = new JScrollPane(table);
      add(scroll, BorderLayout.CENTER);
    }

    // Avoid scheduling multiple setColumnWidth jobs.
    private final Object setColumnWidthLock = new Object();
    private boolean setColumnWidthScheduled = false;

    void setColumnWidth()
    {
      synchronized (setColumnWidthLock) {
        if (!setColumnWidthScheduled) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
              synchronized (setColumnWidthLock) {
                setColumnWidthScheduled = false;
              }
              setColWidth(COL_ID, 20, 30);
              setColWidth(COL_NAME, 80, 200);
              setColWidth(COL_STATE, 40, 60);
              setColWidth(COL_STARTLEVEL, 20, 30);
              setColWidth(COL_DESC, 100, 300);
              setColWidth(COL_LOCATION, 80, 200);
              setColWidth(COL_VENDOR, 60, 150);
              setColWidth(COL_VERSION, 45, -1);
              setColWidth(COL_SYMBOLIC_NAME, 80, -1);
            }
          });
          setColumnWidthScheduled = true;
        }
      }
    }

    /**
     * Configure column widths.
     *
     * @param col The column to set widths for.
     * @param w preferred width.
     * @param max The value -1 means compute the max with based on table contents.
     */
    void setColWidth(final int col, final int w, int max) {
      int pw = w;
      try {
        final TableModel  model  = table.getModel();
        final TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

        if(col < model.getColumnCount()) {
          final TableColumn column = table.getColumnModel().getColumn(col);

          final Component headerComp =
            headerRenderer
            .getTableCellRendererComponent(null, column.getHeaderValue(),
                                           false, false, 0, 0);
          final int headerWidth = headerComp.getPreferredSize().width;
          pw = Math.max(headerWidth, pw);

          if (max == -1) {
            // Set max with to the width of the widest row.
            max = pw;
            for (int row = 0; row < model.getRowCount(); row++) {
              final Object value = model.getValueAt(row, col);
              final TableCellRenderer cellRenderer =
                table.getCellRenderer(row, col);
              final Component rComp =
                cellRenderer.getTableCellRendererComponent(table, value, false,
                                                           false, row, col);
              final int width = rComp.getPreferredSize().width;
              // Must add 2 to the preferred width to avoid that the renderer
              // replaces the end of the item with a ".."
              max = Math.max(width + 2, max);
            }
          } else {
            // Ensure that max width is at least as large as the preferred width.
            max = Math.max(pw, max);
          }

          column.setMinWidth(10);
          column.setPreferredWidth(pw);
          column.setMaxWidth(max);
        }
      } catch (final Exception e) {
        Activator.log.warn("Failed to set column #" + col + " to width=" + pw);
      }
    }
  }

  public static final int COL_ID         = 0;
  public static final int COL_STARTLEVEL = 1;
  public static final int COL_NAME       = 2;
  public static final int COL_STATE      = 3;
  public static final int COL_SYMBOLIC_NAME = 4;
  public static final int COL_VERSION    = 5;
  public static final int COL_LOCATION   = 6;
  public static final int COL_DESC       = 7;
  public static final int COL_VENDOR     = 8;

  public static int COL_COUNT   = 9;

  class BundleTableModel2 extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    public BundleTableModel2() {
    }

    public int getRowFromBID(long bid) {
      if (-1==bid) {
        return -1;
      }

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

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      final Object obj = getValueAt(0, columnIndex);
      if (obj == null) {
        return Object.class;
      } else {
        return obj.getClass();
      }
    }


    @Override
    public boolean isCellEditable(int row, int col) {
      return false;
    }


    @Override
    public String getColumnName(int col) {
      switch(col) {
      case COL_LOCATION:   return "Location";
      case COL_ID:         return "Id";
      case COL_STATE:      return "State";
      case COL_STARTLEVEL: return "Level";
      case COL_NAME:       return "Name";
      case COL_DESC:       return "Description";
      case COL_VENDOR:     return "Vendor";
      case COL_SYMBOLIC_NAME: return "Symbolic Name";
      case COL_VERSION:    return "Version";
      default:             return "";
      }
    }

    public String getToolTipText(int row, int column) {
      String tt = "";

      if(column >= 0 && row >= 0) {

        final Bundle b = getBundle(row);

        switch(column) {
        case COL_ID:
        case COL_LOCATION:
        case COL_STATE:
        case COL_STARTLEVEL:
        case COL_DESC:
        case COL_NAME:
        case COL_VENDOR:
        case COL_SYMBOLIC_NAME:
        case COL_VERSION:
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
      final Bundle b = getBundle(row);

      switch(column) {
      case COL_LOCATION:
        return Util.shortLocation(b.getLocation());
      case COL_ID:
        return Long.toString(b.getBundleId());
      case COL_STATE:
        return Util.stateName(b.getState());
      case COL_STARTLEVEL:
        {
          final BundleStartLevel bsl = b.adapt(BundleStartLevel.class);

          if(null != bsl) {
            try {
              final int n = bsl.getStartLevel();
              return Integer.toString(n);
            } catch (final Exception e) {
              return "not managed";
            }
          } else {
            return "-";
          }
        }
      case COL_DESC:
        return Util.getHeader(b, Constants.BUNDLE_DESCRIPTION);
      case COL_NAME:
        return Util.getHeader(b, Constants.BUNDLE_NAME);
      case COL_VENDOR:
        return Util.getHeader(b, Constants.BUNDLE_VENDOR);
      case COL_SYMBOLIC_NAME:
        return b.getSymbolicName();
      case COL_VERSION:
        return b.getVersion();
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
    private static final long serialVersionUID = 1L;

    public BundleTableRowSelectionModel()
    {
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    @Override
    public void addSelectionInterval(int index0, int index1)
    {
      if(!bInValueChanged) {
        final List<Long> selectedBundleIds = new ArrayList<Long>();
        for (int row=index0; row<=index1; row++) {
          final long rowBid = model.getBundle(row).getBundleId();
          selectedBundleIds.add(new Long(rowBid));
        }
        bundleSelModel.setSelected(selectedBundleIds, true);
      } else {
        super.addSelectionInterval(index0, index1);
      }
    }

    @Override
    public void removeSelectionInterval(int index0, int index1)
    {
      if(!bInValueChanged) {
        final List<Long> selectedBundleIds = new ArrayList<Long>();
        for (int row=index0; row<=index1; row++) {
          final long rowBid = model.getBundle(row).getBundleId();
          selectedBundleIds.add(new Long(rowBid));
        }
        bundleSelModel.setSelected(selectedBundleIds, false);
      } else {
        super.removeSelectionInterval(index0, index1);
      }
    }

    @Override
    public void setSelectionInterval(int index0, int index1)
    {
      if(!bInValueChanged) {
        bundleSelModel.clearSelection();
        final List<Long> selectedBundleIds = new ArrayList<Long>();
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

    @Override
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
