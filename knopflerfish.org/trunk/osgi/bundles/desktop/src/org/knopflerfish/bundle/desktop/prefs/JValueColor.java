/*
 * Copyright (c) 2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.prefs;

import org.knopflerfish.bundle.desktop.swing.Colors;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;
import java.util.*;

public class JValueColor extends JValue {
  JTextField text;

  JButton    colSelect;
  ColorIcon  icon;

  JValueColor(Preferences _node, String _key) {
    super(_node, _key, ExtPreferences.TYPE_COLOR);

    String val = node.get(key, "");
    text = new JTextField(val, 15) {{
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            node.put(key, text.getText());
          }
        });
    }};
    

    icon = new ColorIcon(Colors.getColor(node.get(key, "")), 12, 12);
    colSelect = new JButton();
    colSelect.setToolTipText("Select color from color dialog");
    colSelect.setPreferredSize(new Dimension(20, 20));
    colSelect.setIcon(icon);
    colSelect.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          selectColor();
        }
      });

    text.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if(bEditable) {
            if(e.getButton() == MouseEvent.BUTTON1) {
              return;
            }
            selectColor();
          }
        }
      });

    add(text,      BorderLayout.CENTER);
    add(colSelect, BorderLayout.EAST);
  }

  void selectColor() {
    Color col = 
      JColorChooser.showDialog(this, 
                               "Select a color for " + 
                               node.absolutePath() + "/" + key, 
                               Colors.getColor(node.get(key, "")));

    if(null != col) {
      node.put(key, Colors.toString(col));
    }
  }
  
  public void update() {
    text.setText(node.get(key, ""));
    icon.setColor(Colors.getColor(node.get(key, "")));
    colSelect.repaint();
  }

  public void setEditable(boolean b) {
    if(isReadonly()) {
      b = false;
    }

    super.setEditable(b);
    text.setEditable(b);
    colSelect.setEnabled(b);
  }

  static boolean isColor(String s) {
    boolean b = isColor0(s);
    return b;
  }

  static boolean isColor0(String s) {
    if(Colors.COLORS.containsKey(s.toLowerCase())) {
      return true;
    }
    if(s.length() == 7 && '#' == s.charAt(0)) {
      for(int i = 1; i < s.length(); i++) {
        if(-1 == "01234567890abcdef".indexOf(Character.toLowerCase(s.charAt(i)))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}

/**
 * A very simple icon that paints itself in a solid color
 */
class ColorIcon implements Icon {
  Color col;
  int   width;
  int   height;
  
  public ColorIcon(Color col, int w, int h) {
    this.col    = col;
    this.width  = w;
    this.height = h;
  }

  public int getIconWidth() {
    return width;
  }

  public int getIconHeight() {
    return height;
  }

  public void setColor(Color c) {
    this.col = c;
  }


  public void paintIcon(Component comp,Graphics g,int x,int y) {
    g.setColor(col);
    g.fillRect(x, y, getIconWidth(), getIconHeight());
  }
}
