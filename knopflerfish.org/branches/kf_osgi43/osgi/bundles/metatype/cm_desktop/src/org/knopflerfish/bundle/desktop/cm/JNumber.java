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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.osgi.service.metatype.AttributeDefinition;

import org.knopflerfish.util.metatype.AD;

public class JNumber
  extends JPanel
{
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  Number num;
  AttributeDefinition ad;

  JTextField text;
  JSlider slider = null;

  double max;
  double min;

  boolean bUseSlider = false;

  static final int RANGE = 1000;

  JNumber(AttributeDefinition _ad)
  {
    super(new BorderLayout());
    this.ad = _ad;

    text = new JTextField();
    if (bUseSlider) {
      slider = new JSlider();
    }

    switch (ad.getType()) {
    case AttributeDefinition.INTEGER:
      min = 0;
      max = 65535;
      break;
    case AttributeDefinition.LONG:
      min = 0;
      max = 65535;
      break;
    case AttributeDefinition.SHORT:
      min = 0;
      max = 65535;
      break;
    case AttributeDefinition.DOUBLE:
      min = 0;
      max = 65535;
      break;
    case AttributeDefinition.FLOAT:
      min = 0;
      max = 65535;
      break;
    default:
      throw new IllegalArgumentException("Unsupported type=" + ad.getType());
    }

    text = new JTextField("");
    if (bUseSlider) {
      slider = new JSlider(0, RANGE);
    }

    // Set the preferred size of test so that 10 digits fits in it.
    text.setText("0000000000");
    final Dimension tDimension = text.getPreferredSize();
    text.setPreferredSize(tDimension);
    text.setText("");

    text.setHorizontalAlignment(SwingConstants.TRAILING);
    add(text, BorderLayout.WEST);
    if (slider != null) {
      add(slider, BorderLayout.CENTER);
    }
    text.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev)
      {
        final Number n = (Number) AD.parse(text.getText(), 0, ad.getType());
        setValue(n);
      }
    });

    if (slider != null) {
      slider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e)
        {
          final int v = slider.getValue();
          switch (ad.getType()) {
          case AttributeDefinition.INTEGER:
          case AttributeDefinition.SHORT:
          case AttributeDefinition.LONG: {
            final long val = (long) (min + (max - min) * v / RANGE);
            text.setText(Long.toString(val));
          }
            break;
          case AttributeDefinition.DOUBLE:
          case AttributeDefinition.FLOAT: {
            final double val = min + (max - min) * v / RANGE;
            text.setText(Double.toString(val));
          }
            break;
          }
        }
      });
    }
  }

  void syncUI()
  {
  }

  Object getValue()
  {
    try {
      switch (ad.getType()) {
      case AttributeDefinition.INTEGER:
        return new Integer(text.getText());
      case AttributeDefinition.LONG:
        return new Long(text.getText());
      case AttributeDefinition.SHORT:
        return new Short(text.getText());
      case AttributeDefinition.DOUBLE:
        return new Double(text.getText());
      case AttributeDefinition.FLOAT:
        return new Float(text.getText());
      default:
        throw new IllegalArgumentException("Unsupported type=" + ad.getType());
      }
    } catch (final NumberFormatException nfe) {
      final String msg = "Invalid " + getTypeName(ad.getType()) +" value '" + text.getText() + "'.";
      throw (NumberFormatException) new NumberFormatException(msg)
          .initCause(nfe);
    }
  }

  private String getTypeName(int type)
  {
    switch (ad.getType()) {
    case AttributeDefinition.INTEGER:
      return "integer";
    case AttributeDefinition.LONG:
      return "long";
    case AttributeDefinition.SHORT:
      return "short";
    case AttributeDefinition.DOUBLE:
      return "double";
    case AttributeDefinition.FLOAT:
      return "float";
    default:
      return "unknown AD type=" + ad.getType();
    }
  }

  /**
   * Set item values from vector or array object.
   *
   * <p>
   * If object is null, use default values from AD
   * </p>
   */
  void setValue(Object obj)
  {
    if (obj instanceof Number) {
      final Number n = (Number) obj;
      final double val = n.doubleValue();
      // double val = min + (max - min) * (double)v / RANGE;
      final int v = (int) (RANGE * (val - min) / (max - min));
      if (slider != null) {
        slider.setValue(v);
      }
      text.setText(n.toString());
    } else {
      throw new IllegalArgumentException("Value is not a Number, is "
                                         + obj.getClass().getName() + ", "
                                         + obj);
    }
  }
}
