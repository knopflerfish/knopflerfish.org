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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;


public class JValueBoolean extends JValue {
  JCheckBox cb;
  
  JValueBoolean(Preferences _node, String _key) {
    super(_node, _key, ExtPreferences.TYPE_BOOLEAN);
    
    boolean val = node.getBoolean(key, false);
    cb = new JCheckBox() {{
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            node.putBoolean(key, cb.isSelected());
          }
        });
    }};
    cb.setSelected(val);
    
    add(cb, BorderLayout.CENTER);
  }

  public void update() {
    cb.setSelected(node.getBoolean(key, false));
  }

  public void setEditable(boolean b) {
    if(isReadonly()) {
      b = false;
    }

    super.setEditable(b);
    cb.setEnabled(b);
  }
  
  /**
   * Check if a string is a boolean value.
   */
  public static boolean isBoolean(String s) {
    boolean b = 
      0 == "true".compareToIgnoreCase(s) ||
      0 == "false".compareToIgnoreCase(s);

    return b;
  }
}
