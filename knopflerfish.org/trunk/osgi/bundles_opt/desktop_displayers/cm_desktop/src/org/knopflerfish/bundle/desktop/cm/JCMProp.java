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

public class JCMProp extends JPanel {
  AttributeDefinition ad;

  JComponent comp = null;
  JLabel     err;

  public JCMProp(AttributeDefinition ad, Dictionary props) {
    super(new BorderLayout());

    this.ad = ad;
    
    comp = null;
    err  = new JLabel();
    err.setForeground(Color.red);

    switch(ad.getType()) {
    case AttributeDefinition.STRING:
      comp = new JTextField();
      break;
    case AttributeDefinition.BOOLEAN:
      comp = new JCheckBox();
      break;
    default:
      comp = new JTextField();
    }
    
    add(comp, BorderLayout.CENTER);
    add(err,  BorderLayout.SOUTH);
    
    Object obj = props.get(ad.getID());
    setValue(AD.toString(obj != null ? obj : ad.getDefaultValue()));
  } 
  
  
  void setErr(String s) {
    err.setText(s);
    invalidate();
    revalidate();
    repaint();
  }

  public void setValue(Object obj) {
    String s = AD.toString(obj);

    if(comp instanceof JTextField) {
      JTextField text = (JTextField)comp;
      text.setText(s);
    } else if(comp instanceof JCheckBox) {
      JCheckBox cb = (JCheckBox)comp;
      cb.setSelected("true".equals(s));
    }
  }

  public Object getValue() {
    String s = null;
    if(comp instanceof JTextField) {
      JTextField text = (JTextField)comp;
      s = text.getText();
    } else if(comp instanceof JCheckBox) {
      JCheckBox cb = (JCheckBox)comp;
      s = "" + cb.isSelected();
    }

    //    System.out.println("getValue " + ad + ", s=" + s);

    String msg = ad.validate(s);
    
    //    System.out.println("validate msg=" + msg);

    if(msg != null && !"".equals(msg)) {
      throw new IllegalArgumentException(s + ": " + msg);
    }
    return AD.parse(s, ad.getCardinality(), ad.getType());
  }
}
