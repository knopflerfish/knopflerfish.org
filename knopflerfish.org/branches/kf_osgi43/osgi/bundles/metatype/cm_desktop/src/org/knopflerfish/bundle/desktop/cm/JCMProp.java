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

package org.knopflerfish.bundle.desktop.cm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Dictionary;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.osgi.service.metatype.AttributeDefinition;

import org.knopflerfish.util.metatype.AD;

public class JCMProp
  extends JPanel
{
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  AttributeDefinition ad;

  JComponent comp = null;
  JLabel err;

  @SuppressWarnings("unchecked")
  public JCMProp(AttributeDefinition ad, Dictionary<String,?> props)
  {
    super(new BorderLayout());

    this.ad = ad;

    comp = null;
    err = new JLabel();
    err.setForeground(Color.red);

    final int card = ad.getCardinality();
    int maxItems = Integer.MAX_VALUE;

    if (card == Integer.MAX_VALUE || card == Integer.MIN_VALUE) {
      maxItems = Integer.MAX_VALUE;
    } else if (card < 0) {
      maxItems = -card;
    } else {
      maxItems = card;
    }

    if (card < 0) {
      comp =
        new JVector(ad, (Vector<Object>) props.get(ad.getID()), "Vector items",
                    maxItems);
    } else if (card > 0) {
      comp =
        new JVector(ad, Util.toVector(props.get(ad.getID())), "Array items",
                    maxItems);
    } else {
      if (ad.getOptionValues() != null) {
        if (ad.getOptionLabels() != null) {
          comp = new JComboBox(ad.getOptionLabels());
        } else {
          comp = new JComboBox(ad.getOptionValues());
        }
      } else {
        switch (ad.getType()) {
        case AttributeDefinition.STRING:
          comp = new JTextField();
          break;
        case AttributeDefinition.BOOLEAN:
          comp = new JCheckBox();
          break;
        case AttributeDefinition.INTEGER:
        case AttributeDefinition.SHORT:
        case AttributeDefinition.LONG:
        case AttributeDefinition.DOUBLE:
        case AttributeDefinition.FLOAT:
          comp = new JNumber(ad);
          break;
        default:
          comp = new JTextField();
        }
      }
    }

    add(comp, BorderLayout.NORTH);
    add(err, BorderLayout.SOUTH);

    final Object obj = props.get(ad.getID());

    // System.out.println(ad.getID() + "=" + obj + " " + (obj != null ?
    // obj.getClass().getName() : "null"));
    if (ad.getCardinality() == 0) {
      setValue(AD.toString(obj != null ? obj : ad.getDefaultValue()));
    } else {
      // done in constructor for JVector
    }
  }

  void setErr(String s)
  {
    err.setText(s);
    invalidate();
    revalidate();
    repaint();
  }

  public void setValue(Object obj)
  {
    String s = null;
    if (ad.getCardinality() == 0) {
      s = AD.toString(obj != null ? obj : ad.getDefaultValue());
    } else {
      // done in constructor for JVector
    }

    if (comp instanceof JVector) {
      final JVector jv = (JVector) comp;
      jv.setValue(obj);
    } else if (comp instanceof JNumber) {
      ((JNumber) comp).setValue(AD.parse(s, 0, ad.getType()));
    } else {
      if (comp instanceof JTextField) {
        final JTextField text = (JTextField) comp;
        text.setText(s);
      } else if (comp instanceof JCheckBox) {
        final JCheckBox cb = (JCheckBox) comp;
        cb.setSelected("true".equals(s));
      } else if (comp instanceof JComboBox) {
        final JComboBox cb = (JComboBox) comp;
        final String[] opts = ad.getOptionValues();
        for (int i = 0; i < opts.length; i++) {
          if (opts[i].equals(s)) {
            cb.setSelectedIndex(i);
            return;
          }
        }
        throw new IllegalArgumentException("Unknown option value " + obj);
      }
    }
  }

  public Object getValue()
  {
    String s = null;
    if (comp instanceof JVector) {
      final JVector jv = (JVector) comp;
      if (ad.getCardinality() < 0) {
        s = AD.toString(jv.getVector());
      } else {
        s = AD.toString(jv.getArray());
      }
    } else if (comp instanceof JNumber) {
      final JNumber number = (JNumber) comp;
      s = number.getValue().toString();
    } else if (comp instanceof JTextField) {
      final JTextField text = (JTextField) comp;
      s = text.getText();
    } else if (comp instanceof JCheckBox) {
      final JCheckBox cb = (JCheckBox) comp;
      s = "" + cb.isSelected();
    } else if (comp instanceof JComboBox) {
      final JComboBox cb = (JComboBox) comp;
      s = ad.getOptionValues()[cb.getSelectedIndex()];
    }

    // System.out.println("getValue " + ad + ", s=" + s);

    final String msg = ad.validate(s);

    // System.out.println("validate msg=" + msg);

    if (msg != null && !"".equals(msg)) {
      throw new IllegalArgumentException(s + ": " + msg);
    }
    return AD.parse(s, ad.getCardinality(), ad.getType());
  }
}
