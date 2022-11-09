/*
 * Copyright (c) 2013-2022, KNOPFLERFISH project
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

import java.awt.Component;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import org.knopflerfish.util.metatype.AD;

/**
 * Vertical box presenting the properties of a configuration.
 *
 * @author ekolin
 */
public class PropertiesPanel
  extends JPanel
{
  private static final long serialVersionUID = 1L;

  /** Current property values to present. */
  private final Map<String, JCMProp> props = new HashMap<>();

  public PropertiesPanel()
  {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  }

  /**
   * Rebuild this panel to show inputs for the given Object Class Definition.
   *
   * @param ocd
   *          The object class definition to present.
   */
  void rebuild(ObjectClassDefinition ocd)
  {
    removeAll();
    props.clear();

    if (ocd != null) {
      final Dictionary<String, Object> configProps = new Hashtable<>();

      final AttributeDefinition[] reqAttrs =
        ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED);
      addAttribs(reqAttrs, configProps, "");
      final AttributeDefinition[] optAttrs =
        ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
      addAttribs(optAttrs, configProps, " (optional)");
    }

    // Must use a panel as filler since the component returned by
    // BoxcreateGlue() does not paint the background.
    final JComponent filler = new JPanel();
    filler.setAlignmentX(Component.LEFT_ALIGNMENT);
    add(filler);
  }

  /**
   * Add input controls for each of the attribute definitions in the given list to this panel.
   *
   * @param ads
   *          The attribute definitions to present.
   * @param configProps
   *          Current values of the attributes.
   * @param info
   *          Extra information to add to the tool tip for the attribute label.
   */
  void addAttribs(AttributeDefinition[] ads,
                  Dictionary<String, Object> configProps,
                  String info)
  {
    for (final AttributeDefinition ad : ads) {
      JLabelled item;
      try {
        final JCMProp jcmProp = new JCMProp(ad, configProps);
        props.put(ad.getID(), jcmProp);

        String className = AD.getClass(ad.getType()).getName();
        if (ad.getCardinality() < 0) {
          className = "Vector of " + className;
        } else if (ad.getCardinality() > 0) {
          className = className + "[]";
        }

        final StringBuilder toolTip = new StringBuilder(200);
        toolTip.append("<table width=\"400px\">");
        toolTip.append("<tr><th align=\"left\">");
        toolTip.append(ad.getName());
        toolTip.append("</th></tr>");
        if (!info.isEmpty()) {
          toolTip.append("<tr><td align=\"right\">");
          toolTip.append(info);
          toolTip.append("</td></tr>");
        }
        if (ad.getDescription() != null && !ad.getDescription().isEmpty()) {
          toolTip.append("<tr><td>");
          toolTip.append(ad.getDescription());
          toolTip.append("</td></tr>");
        }
        toolTip.append("<tr><td align=\"left\">(");
        toolTip.append(className);
        toolTip.append(")</td></tr>");
        toolTip.append("</table>");

        item =
          new JLabelled(ad.getName(), toolTip.toString(), jcmProp, 100);
      } catch (final Exception e) {
        final String msg = "Failed to create ui for " + ad;
        Activator.log.error(msg, e);
        item =
          new JLabelled(ad.getName(), msg, new JLabel(e.getMessage()), 100);
      }
      add(item);
    }
  }

  /**
   * Get the properties presented by this panel as a configuration dictionary.
   *
   * @return the property values presented in this panel.
   *
   * @throws IllegalArgumentException
   *           if the values entered in the GUI can not be converted into
   *           objects of the expected type.
   */
  Dictionary<String, Object> getProps()
      throws IllegalArgumentException
  {
    final Hashtable<String, Object> res = new Hashtable<>();

    int errCount = 0;
    // Avoid concurrent updates.
    synchronized (props) {
      for (final Entry<String, JCMProp> entry : props.entrySet()) {
        final String name = entry.getKey();
        final JCMProp jcmProp = entry.getValue();
        try {
          final Object val = jcmProp.getValue();
          res.put(name, val);
          jcmProp.setErr(null);
        } catch (final Exception e) {
          errCount++;
          jcmProp.setErr(e.getMessage());
          jcmProp.invalidate();
          Activator.log.error("Failed to convert value for '" + name + "', "
                              + e, e);
        }
      }
    }
    revalidate();
    repaint();

    if (errCount > 0) {
      throw new IllegalArgumentException("Failed to convert " + errCount
                                         + " values");
    }
    return res;
  }

  /**
   * Update the presented values according to the given data.
   * Clear all properties from errors.
   *
   * @param in
   *          The current values to present.
   */
  void setProps(Dictionary<String, Object> in)
  {
    if (in == null) {
      in = new Hashtable<>();
    }
    // Avoid concurrent updates.
    synchronized (props) {
      for (final Entry<String, JCMProp> entry : props.entrySet()) {
        final String name = entry.getKey();
        final JCMProp jcmProp = entry.getValue();
        try {
          jcmProp.setErr(null);
          jcmProp.setValue(in.get(name));
          jcmProp.invalidate();
        } catch (final Exception e) {
          jcmProp.setErr(e.getMessage());
        }
      }

    }

    revalidate();
    repaint();
  }

}
