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

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

/**
 * TableCellRenderer which display formatted dates.
 */
public class DateCellRenderer extends JTextField implements TableCellRenderer {
  JTextField label;
  
  SimpleDateFormat tf;

  public DateCellRenderer(String format) {
    tf = new SimpleDateFormat(format);

    label = new JTextField();
    label.setBorder(null);
      //      label.setEditable(false);


  }

  public Component getTableCellRendererComponent(JTable  table,
						 Object  obj, 
						 boolean isSelected,
						 boolean hasFocus,
						 int row, 
						 int column) {

    Color bg;
    Color fg;
    
    if(isSelected) {
      bg = table.getSelectionBackground();
      fg = table.getSelectionForeground();

      // remove any transparency in background. I'm amazed.
      bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue());
    } else {
      try {
        // Get the default string renderer, just to clone the colors
        Component defComp = table.getDefaultRenderer(String.class)
          .getTableCellRendererComponent(table, 
    				     "dummy",
    				     isSelected,
    				     hasFocus,
    				     row, 
    				     column);
        bg = defComp.getBackground();
        fg = defComp.getForeground();
      } catch (Throwable e) {
        // It has been reported that the previous call can throw something.
        // In that case, we'll just set the colors manually:
        bg = Color.WHITE;
        fg = Color.BLACK;
      }
        
    }

    // paint by setting the formatted date
    if(obj != null) {
      label.setText(tf.format((Date)obj));
    } else {
      label.setText("");
    }

    label.setBackground(bg);
    label.setForeground(fg);

    return label;
  }
}

