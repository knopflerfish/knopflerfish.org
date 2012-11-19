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

package org.knopflerfish.bundle.desktop.cm;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.util.tracker.*;
import java.util.*;
import org.knopflerfish.service.desktop.*;
import org.knopflerfish.util.metatype.*;
import org.osgi.service.metatype.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class JVector extends JPanel {
  JTable              table;
  AbstractTableModel  model;
  Vector              v;
  AttributeDefinition ad;
  String              header;
  int                 maxItems;

  static final int COL_ID   = 0;
  static final int COL_ITEM = 1;


  JButton addRowButton;
  JButton delRowButton;

  JVector(AttributeDefinition _ad, 
	  Vector inValues,
	  String _header,
	  int maxItems) {
    super(new BorderLayout());
    this.ad       = _ad;
    this.header   = _header;
    this.maxItems = maxItems;

    // Set to empty vector initially
    v = new Vector();

    model = new  AbstractTableModel() {
	public int getRowCount() {
	  return v.size();
	}

	public String getColumnName(int col) {
	  switch(col) {
	  case COL_ITEM: return header;
	  case COL_ID:   return "#";
	  default:
	    throw new IllegalArgumentException("Col to large");
	  }
	}

	public Class getColumnClass(int col) {
	  switch(col) {
	  case COL_ITEM: return String.class;
	  case COL_ID: return Integer.class;
	  default:
	    throw new IllegalArgumentException("Col to large");
	  }
	}

	public boolean isCellEditable(int row, int col) {
	  switch(col) {
	  case COL_ITEM: return true;
	  case COL_ID: return false;
	  default:
	    throw new IllegalArgumentException("Col to large");
	  }
	}

	public int getColumnCount() {
	  return 2;
	}

	public Object getValueAt(int row, int col) {
	  switch(col) {
	  case COL_ITEM: 
	    {
	      Object val = v.elementAt(row);
	      String s = val.toString();
	      
	      return s;
	    }
	  case COL_ID: 
	    {
	      return new Integer(row);
	    }
	  default:
	    throw new IllegalArgumentException("Col to large");
	  }
	}
	
	public void setValueAt(Object val, int row, int col) {
	  switch(col) {
	  case COL_ITEM: 
	    {
	      String s = val.toString();
	      
	      Object obj = AD.parse(s, 0, ad.getType());
	      v.setElementAt(obj, row);
	    }
	    break;
	  default:
	    throw new IllegalArgumentException("Col to large");
	  }
	}
      };
    
    table = new JTable(model);

    setColWidth(COL_ID, 30);

    JScrollPane scroll = new JScrollPane(table);

    scroll.setPreferredSize(new Dimension(100, 80));

    JPanel cmds = new JPanel(new BorderLayout());
    
    addRowButton = new JButton("Add");
    addRowButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  addRow();
	}
      });
    addRowButton.setToolTipText("Add a new row to sequence");

    delRowButton = new JButton("Delete");
    delRowButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  deleteRow();
	}
      });
    delRowButton.setToolTipText("Delete the selected (or first) row from sequence");
    


    JPanel cmdEast = new JPanel(new FlowLayout());

    cmdEast.add(addRowButton);
    cmdEast.add(delRowButton);

    cmds.add(cmdEast, BorderLayout.EAST);

    add(scroll, BorderLayout.CENTER);    
    add(cmds, BorderLayout.SOUTH);

    setValue(inValues);
    syncUI();
  }

  void syncUI() {
    addRowButton.setEnabled(v.size() < maxItems);
  }

  void setColWidth(int col, int w) {
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
      
      //      totalWidth += w;
      
      column.setMinWidth(10);
      column.setMaxWidth(300);
      
      column.setPreferredWidth(w);
    }
  }

  void setVector(Vector _v) {
    this.v = _v != null ? (Vector)_v.clone() : new Vector();

    if(v.size() > maxItems) {
      throw new IllegalArgumentException("Size too large (got " + v.size() + 
					 ", max=" + maxItems);
    }

    model.fireTableDataChanged();
    
    syncUI();
    invalidate();
    revalidate();
    repaint();
  }

  Vector getVector() {
    return (Vector)v.clone();
  }
  
  Object getArray() {
    return Util.toArray(v, AD.getPrimitiveClass(ad.getType()));
  }

  void setArray(Object array) {
    setVector(Util.toVector(array));
  }

  /**
   * Set item values from vector or array object.
   *
   * <p>
   * If object is null, use default values from AD
   * </p>
   */
  void setValue(Object obj) {

    //    System.out.println("setValue " + ad.getID() + " " + obj);
    if(obj == null) {
      Vector def = new Vector();
      String[] defValues = ad.getDefaultValue();
      for(int i = 0; i < defValues.length; i++) {
	def.addElement(AD.parse(defValues[i], 0, ad.getType()));
      }
      if(ad.getCardinality() < 0) {
	obj = def;
      } else {
	obj = Util.toArray(def, AD.getPrimitiveClass(ad.getType()));
      }
    }

    if(obj instanceof Vector) {
      setVector((Vector)obj);
    } else if(obj.getClass().isArray()) {
      setArray(obj);
    } else {
      throw new IllegalArgumentException("Object must be vector or array, " + 
					 "found " + obj.getClass().getName());
    }
  }

  void addRow() {
    int row = table.getSelectedRow();
    String[] def = ad.getDefaultValue();
    if(def == null) {
      //System.out.println("No default values in " + ad);
      return;
    }

    if(def.length == 0) {
      //System.out.println("Zero default values in " + ad);
      return;
    }

    /*
    for(int i = 0; def != null && i < def.length; i++) {
      System.out.println("def[" + i + "]=" + def[i]);
    }
    */
    addRow(def[0]);
  }
  
  public void addRow(String newVal) {
    Object obj = AD.parse(newVal, 0, ad.getType());
    v.addElement(obj);
    setVector(v);
  }
  
  void deleteRow() {
    int row = table.getSelectedRow();
    if(row == -1 && v.size() > 0) {
      row = 0;
    }
    if(row != -1) {
      v.removeElementAt(row);
      setVector(v);
    }
  }
}

