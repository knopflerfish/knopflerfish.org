/*
 * Copyright (c) 2003, KNOPFLERFISH project
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
import java.util.Iterator;


public class TableDisplayer extends DefaultSwingBundleDisplayer {

  BundleTableModel2 model;

  public TableDisplayer(BundleContext bc) {
    super(bc, "Details", "Table view of bundles", false);

    model = new BundleTableModel2();
  }
  
  public JComponent newJComponent() {
    return new JBundleTable();
  }


  public void bundleChanged(BundleEvent ev) {
    super.bundleChanged(ev);

    model.fireTableStructureChanged();
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JBundleTable comp = (JBundleTable)it.next();
      comp.setColumnWidth();
    }
  }

  public void valueChanged(long bid) {
    try {
      bInValueChanged = true;
      int row = model.getRowFromBID(bid);
      
      if(row == -1) {
        return;
      }
      for(Iterator it = components.iterator(); it.hasNext(); ) {
        JBundleTable comp = (JBundleTable)it.next();
        
        comp.table.setRowSelectionInterval(row, row);
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
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      
      //      Dimension size = new Dimension(500, 300);
      
      //      scroll.setPreferredSize(size);
      

      DefaultTableCellRenderer rightAlign = 
	new DefaultTableCellRenderer();

      rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);

      table.getColumnModel().getColumn(COL_ID).setCellRenderer(rightAlign);
      
      setColumnWidth();

      ListSelectionModel rowSM = table.getSelectionModel();
      rowSM.addListSelectionListener(new ListSelectionListener() {
	  public void valueChanged(ListSelectionEvent e) {

	    if (e.getValueIsAdjusting()) {
	      return;
	    }

	    ListSelectionModel lsm =  (ListSelectionModel)e.getSource();
	    if (!lsm.isSelectionEmpty()) {
	      int row = lsm.getMinSelectionIndex();
	      
	      Bundle b = model.getBundle(row);
              if(!bInValueChanged) {
                bundleSelModel.clearSelection();
                bundleSelModel.setSelected(b.getBundleId(), true);
              }
            }
	  }
	});

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
      Bundle[] bl = getBundleArray();
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
}
