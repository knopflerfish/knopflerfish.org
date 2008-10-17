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

public class JValueInteger extends JValue {
  JTextField text;
  JSpinner spinner;

  SpinnerNumberModel model;

  JValueInteger(Preferences _node, String _key) {
    super(_node, _key, ExtPreferences.TYPE_INT);

    int val = node.getInt(key, 0);
    /*
    text = new JTextField("" + val, 20) {{
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              node.putInt(key, Integer.parseInt(text.getText()));
              setErr(null);
            } catch (Exception e) {
              setErr(e.getClass().getName() + " " + e.getMessage());
            }
          }
        });
    }};
    */

    model = 
      new SpinnerNumberModel(new Integer(val),
                             new Integer(Integer.MIN_VALUE),
                             new Integer(Integer.MAX_VALUE),
                             new Integer(1));
    
    spinner = new JSpinner(model);
    spinner.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          try {
            node.putInt(key, model.getNumber().intValue());
            setErr(null);
          } catch (Exception e) {
            setErr(e.getClass().getName() + " " + e.getMessage());
          }
            }
      });
    add(spinner, BorderLayout.CENTER);
  }

  public void update() {
    // text.setText("" + node.getInt(key, 0));
    model.setValue(new Integer(node.getInt(key, 0)));
  }

  public void setEditable(boolean b) {
    if(isReadonly()) {
      b = false;
    }
    super.setEditable(b);
    // text.setEditable(b);
    spinner.setEnabled(b);
  }

  /**
   * Check if a string is an integer.
   */
  public static boolean isInteger(String s) {
    if(s.length() == 0) {
      return false;
    }
    for(int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if(c == '-' && i == 0) {
        continue;
      }
      if(!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

}
