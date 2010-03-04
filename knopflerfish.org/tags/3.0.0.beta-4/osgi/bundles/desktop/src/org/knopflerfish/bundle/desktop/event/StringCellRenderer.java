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

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.FontMetrics;

/**
 * TableCellRenderer which displays better clipped strings.
 */
public class StringCellRenderer extends  DefaultTableCellRenderer {

  public StringCellRenderer() {
  }

  public Component getTableCellRendererComponent(JTable  table,
						 Object  obj, 
						 boolean isSelected,
						 boolean hasFocus,
						 int row, 
						 int column) {


    
    JLabel label = (JLabel)super.getTableCellRendererComponent(table, 
                                                               obj, 
                                                               isSelected, 
                                                               hasFocus,
                                                               row, column);

    String s = obj != null ? obj.toString() : "";
    int w = table.getTableHeader().getColumnModel().getColumn(column).getWidth();

    FontMetrics fm = label.getFontMetrics(label.getFont());
    
    int fmW = fm.stringWidth(s);
    
    // s = w + "/" + fmW + ":" + s;

    int nChars = 3;
    int m = s.length() / 2;
    String s2 = s;
    while((fmW+10) > w && s2.length() > nChars && s2.length() > 5) {      
      int n1 = nChars / 2;
      int n2 = nChars - n1;
      String left  = s.substring(0, m - n1);
      String right = s.substring(m + n2);
      s2 = left + "..." + right;      
      fmW = fm.stringWidth(s2);
      nChars++;
    }
    
    label.setText(s2);
    
    return label;
  }
}

