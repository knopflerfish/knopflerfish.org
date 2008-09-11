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

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;
import org.knopflerfish.bundle.desktop.swing.Activator;


/**
 * Factory class for creating JValue instances
 */
public class JValueFactory {

  private static final String[] TYPES = new String[] {
    ExtPreferences.TYPE_BOOLEAN,
    ExtPreferences.TYPE_STRING,
    ExtPreferences.TYPE_DOUBLE,
    ExtPreferences.TYPE_INT,
    ExtPreferences.TYPE_LONG,
    ExtPreferences.TYPE_COLOR,
  };

  /**
   * Get array of supported type names such that
   * <tt>createJValue</tt> is capable of creating
   * a renderer.
   */
  public static String[] getSupportedTypes() {
    return TYPES;
  }

  /**
   * Create a renderer matching a key in a prefs node.
   */
  public static JValue createJValue(Preferences node, String key) {
    try {
      String type = null;
      if(node instanceof ExtPreferences) {
        type = ((ExtPreferences)node).getProperty(key, ExtPreferences.PROP_TYPE, null);
      }

      if(type == null) {
        type = node.get(JValue.TYPE_PREFIX + key, "");
      }

      if(type == null || "".equals(type)) {
        String val  = node.get(key, "");
        if(JValueBoolean.isBoolean(val)) {
          type = ExtPreferences.TYPE_BOOLEAN;
        } else if(JValueInteger.isInteger(val)) {
          type = ExtPreferences.TYPE_INT;
        } else if(JValueColor.isColor(val)) {
          type = ExtPreferences.TYPE_COLOR;
        } else {
          type = ExtPreferences.TYPE_STRING;
        }
      }
      if(ExtPreferences.TYPE_STRING.equals(type)) {
        return new JValueString(node, key);
      } else if(ExtPreferences.TYPE_COLOR.equals(type)) {
        return new JValueColor(node, key);
      } else if(ExtPreferences.TYPE_BOOLEAN.equals(type)) {
        return new JValueBoolean(node, key);
      } else if(ExtPreferences.TYPE_INT.equals(type)) {
        return new JValueInteger(node, key);
      } else if(ExtPreferences.TYPE_DOUBLE.equals(type)) {
        return new JValueDouble(node, key);
      } else if(ExtPreferences.TYPE_LONG.equals(type)) {
        return new JValueLong(node, key);
      } else {
        // Activator.log.warn("Node " + node.absolutePath() + "/" + key + ", unknown type=" + type + ", using string");
        return new JValueString(node, key);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to create jvalue from" + node + ", key=" + key, e);
    }
  }
}
