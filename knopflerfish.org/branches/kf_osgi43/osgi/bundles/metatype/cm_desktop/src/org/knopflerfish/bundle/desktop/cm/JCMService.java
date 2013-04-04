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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import org.knopflerfish.util.metatype.AD;

public class JCMService
  extends JPanel
{
  private static final String FACTORY_PID_DEFAULTS = " - Default Values -";
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  ObjectClassDefinition ocd;
  String designatedPid;
  JPanel main;

  boolean isService = true;
  boolean isFactory = false;

  JPanel propPane;
  JPanel mainPanel;

  // String -> AttributeDefinition
  Map<String, JCMProp> props = new HashMap<String, JCMProp>();
  String factoryPid;

  public JCMService()
  {
    super(new BorderLayout());

    main = new JPanel(new BorderLayout());
    add(main, BorderLayout.CENTER);
  }

  void setServiceOCD(String pid, ObjectClassDefinition ocd)
  {
    this.ocd = ocd;
    this.designatedPid = pid;
    isService = ocd != null;
    isFactory = false;
    lastPID = null;
    updateOCD();

  }

  void setFactoryOCD(String pid, ObjectClassDefinition ocd)
  {
    this.ocd = ocd;
    this.designatedPid = pid;
    isService = false;
    isFactory = ocd != null;
    lastPID = null;
    updateOCD();
  }

  void updateOCD()
  {
    main.removeAll();
    props.clear();
    factoryPid = null;

    if (ocd != null) {
      main.setBorder(JCMInfo.makeBorder(this, designatedPid));

      Dictionary<String, Object> configProps = null;
      try {
        final Configuration conf = CMDisplayer.getConfig(designatedPid);
        configProps = conf.getProperties();
      } catch (final Exception e) {
        configProps = new Hashtable<String, Object>();
      }

      mainPanel = new JPanel(new BorderLayout());

      propPane = new JPanel();
      final BoxLayout box = new BoxLayout(propPane, BoxLayout.Y_AXIS);
      propPane.setLayout(box);
      final AttributeDefinition[] reqAttrs =
        ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED);
      final AttributeDefinition[] optAttrs =
        ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
      addAttribs(propPane, reqAttrs, configProps, "");
      addAttribs(propPane, optAttrs, configProps, " (optional)");

      final JPanel propOuter = new JPanel(new BorderLayout());
      propOuter.add(propPane, BorderLayout.NORTH);
      propOuter.add(new JPanel(), BorderLayout.CENTER);

      final JScrollPane scroll = new JScrollPane(propOuter);
      scroll.setPreferredSize(propPane.getPreferredSize());

      final JPanel topPanel = new JPanel(new BorderLayout());

      final JPanel ctrlPanel = new JPanel(new FlowLayout());

      final JButton applyButton = new JButton("Apply");
      applyButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev)
        {
          applyConfig(designatedPid);
        }
      });
      applyButton
          .setToolTipText("Applies and stores the configuration changes");

      if (isFactory) {
        // Controls for a factory configuration
        Configuration[] configs = null;
        try {
          configs =
            CMDisplayer.getCA().listConfigurations("(service.factoryPid="
                                                       + designatedPid + ")");
        } catch (final Exception e) {
        }

        final JButton newButton = new JButton("New");
        final JButton facapplyButton = new JButton("Apply");
        final JButton facdelButton = new JButton("Delete");

        final TreeSet<String> fpids = new TreeSet<String>();
        fpids.add(FACTORY_PID_DEFAULTS);
        for (int i = 0; configs != null && i < configs.length; i++) {
          fpids.add(configs[i].getPid());
        }
        final JComboBox fbox = new JComboBox(fpids.toArray());
        fbox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev)
          {
            final int ix = fbox.getSelectedIndex();
            if (ix == -1) {
              return;
            } else {
              final String pid = (String) fbox.getSelectedItem();
              final boolean exists = !FACTORY_PID_DEFAULTS.equals(pid);
              facapplyButton.setEnabled(exists);
              facdelButton.setEnabled(exists);
              showFactoryConfig(pid);
            }
          }
        });

        newButton.setToolTipText("Create a new factory configuration");
        newButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev)
          {
            newFactoryConfig(designatedPid);
          }
        });

        facapplyButton
            .setToolTipText("Applies the currect changes to the factory config");
        facapplyButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev)
          {
            applyFactoryConfig(factoryPid);
          }
        });

        facdelButton
            .setToolTipText("Delete the selected factory configuration");
        facdelButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev)
          {
            if (factoryPid == null) {
              factoryPid = (String) fbox.getSelectedItem();
            }
            deleteFactoryPid(factoryPid);
          }
        });

        ctrlPanel.add(newButton);
        ctrlPanel.add(facdelButton);
        ctrlPanel.add(facapplyButton);
        ctrlPanel.add(fbox);

        if (lastPID != null) {
          fbox.setSelectedItem(lastPID);
          showFactoryConfig(lastPID);
        } else {
          fbox.setSelectedIndex(fbox.getModel().getSize()-1);
        }
      } else {
        // Controls for a non-factory configuration
        if (CMDisplayer.configExists(designatedPid)) {
          final JButton delButton = new JButton("Delete");
          delButton.setToolTipText("Delete configuration");
          delButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev)
            {
              deleteConfig(designatedPid);
            }
          });
          ctrlPanel.add(applyButton);
          ctrlPanel.add(delButton);

        } else {
          final JButton createButton = new JButton("Create");
          createButton.setToolTipText("Create configuration from values below");
          createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev)
            {
              createConfig(designatedPid);
            }
          });

          final JLabel createInfo = new JLabel("Create from values below");
          ctrlPanel.add(createButton);
          ctrlPanel.add(createInfo);
        }
      }

      // ctrlPanel.add(new JLabel(designatedPid), BorderLayout.CENTER);

      scroll.setBorder(null);
      mainPanel.add(scroll, BorderLayout.CENTER);
      topPanel.add(ctrlPanel, BorderLayout.WEST);
      final JPanel icons = new JPanel(new FlowLayout());
      JLabel iconLabel = null;
      InputStream iconStream = null;
      try {
        iconStream = ocd.getIcon(16);
        if (iconStream != null) {
          try {
            final ImageIcon icon = new ImageIcon(loadStream(iconStream));
            iconLabel = new JLabel(icon);
            icons.add(iconLabel);
          } catch (final Exception e) {
            Activator.log.error("Failed to load icon", e);
          }
        }
      } catch (final Exception e) {
        Activator.log.error("Failed to get icon stream", e);
      }

      final String desc = ocd.getDescription();
      if (desc != null && !"".equals(desc)) {
        final JLabel infoLabel = new JLabel(CMDisplayer.infoIcon);
        final String tt = "<html>" + desc + "</html>";
        infoLabel.setToolTipText(tt);
        icons.add(infoLabel);
        if (iconLabel != null) {
          iconLabel.setToolTipText(tt);
        }
      }

      topPanel.add(icons, BorderLayout.EAST);

      main.add(topPanel, BorderLayout.NORTH);
      main.add(mainPanel, BorderLayout.CENTER);
    }

    invalidate();
    revalidate();
    repaint();
  }

  void addAttribs(JComponent propPane,
                  AttributeDefinition[] ads,
                  Dictionary<String, Object> configProps,
                  String info)
  {
    for (final AttributeDefinition ad : ads) {
      JLabelled item = null;
      try {
        final JCMProp jcmProp = new JCMProp(ad, configProps);
        props.put(ad.getID(), jcmProp);

        String className = AD.getClass(ad.getType()).getName();
        if (ad.getCardinality() < 0) {
          className = "Vector of " + className;
        } else if (ad.getCardinality() > 0) {
          className = className + "[]";
        }

        final StringBuffer toolTip = new StringBuffer(200);
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
      propPane.add(item);
    }
  }

  /**
   * Load a stream into a byte array.
   */
  byte[] loadStream(InputStream is)
      throws IOException
  {
    final int bufSize = 1024 * 2;
    final byte[] buf = new byte[bufSize];

    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    BufferedInputStream in = null;
    try {
      in = new BufferedInputStream(is);
      int n;
      while ((n = in.read(buf)) > 0) {
        bout.write(buf, 0, n);
      }
      return bout.toByteArray();
    } finally {
      try {
        in.close();
      } catch (final Exception ignored) {
      }
    }
  }

  void showFactoryConfig(String pid)
  {
    if (FACTORY_PID_DEFAULTS.equals(pid)) {
      setProps(new Hashtable<String, Object>());
      factoryPid = null;
      // TODO: Enable/disable controls
    } else {
      try {
        final Configuration conf =
          CMDisplayer.getCA().getConfiguration(pid, null);

        setProps(conf.getProperties());
        factoryPid = pid;
      } catch (final Exception e) {
        Activator.log.error("show factory failed pid=" + pid, e);
      }
    }
  }

  void deleteFactoryPid(String pid)
  {
    // System.out.println("deleteFactoryConfig " + pid);
    try {
      final Configuration conf =
        CMDisplayer.getCA().getConfiguration(pid, null);
      conf.delete();
      lastPID = null;
      updateOCD();
    } catch (final Exception e) {
      Activator.log.error("delete factory failed pid=" + pid, e);

    }
  }

  void applyFactoryConfig(String pid)
  {
    // System.out.println("applyFactoryConfig " + pid);
    try {
      final Dictionary<String, Object> props = getProps();

      // System.out.println("props=" + props);
      try {
        final Configuration conf =
          CMDisplayer.getCA().getConfiguration(pid, null);
        conf.update(props);
        lastPID = conf.getPid();
        updateOCD();
      } catch (final Exception e) {
        Activator.log.error("apply factory failed pid=" + pid, e);
      }
    } catch (final Exception e) {
      Activator.log.error("failed to get props pid=" + pid, e);
    }
  }

  void newFactoryConfig(String pid)
  {
    // System.out.println("newFactoryConfig " + pid);

    try {
      final Dictionary<String, Object> props = getProps();

      // System.out.println("props=" + props);
      try {
        final Configuration conf =
          CMDisplayer.getCA().createFactoryConfiguration(pid, null);
        lastPID = conf.getPid();
        conf.update(props);
        updateOCD();
      } catch (final Exception e) {
        showError("new factory failed pid=" + pid, e);
      }
    } catch (final Exception e) {
      Activator.log.error("failed to get props pid=" + pid, e);
    }
  }

  void deleteConfig(String pid)
  {
    // System.out.println("deleteConfig " + pid);
    lastPID = null;
    try {
      final Configuration conf =
        CMDisplayer.getCA().getConfiguration(pid, null);
      conf.delete();
      updateOCD();
    } catch (final Exception e) {
      showError("Delete failed pid=" + pid, e);

    }
  }

  void showError(String msg, Throwable t)
  {
    Activator.log.error(msg, t);
    JOptionPane.showMessageDialog(this, msg + "\n" + t.toString(), msg,
                                  JOptionPane.ERROR_MESSAGE);
  }

  void createConfig(String pid)
  {
    try {
      final Dictionary<String, Object> props = getProps();

      try {
        final Configuration conf =
          CMDisplayer.getCA().getConfiguration(pid, null);
        conf.update(props);
        lastPID = pid;
        updateOCD();
      } catch (final Exception e) {
        Activator.log.error("Failed to create/update pid=" + pid, e);
      }
    } catch (final Exception e) {
      Activator.log.error("Failed to get props for pid=" + pid, e);
    }
  }

  String lastPID = null;

  void applyConfig(String pid)
  {
    try {
      final Dictionary<String, Object> props = getProps();

      try {
        final Configuration conf =
          CMDisplayer.getCA().getConfiguration(pid, null);
        conf.update(props);
        lastPID = pid;
        updateOCD();
      } catch (final Exception e) {
        Activator.log.error("Failed to apply/update pid=" + pid, e);
      }
    } catch (final Exception e) {
      Activator.log.error("Failed to get props for pid=" + pid, e);
    }
  }

  void setProps(Dictionary<String, Object> in)
  {
    for (final Entry<String,JCMProp> entry : props.entrySet()) {
      final String name = entry.getKey();
      final JCMProp jcmProp = entry.getValue();
      try {
        jcmProp.setErr(null);
        jcmProp.setValue(in.get(name));
      } catch (final Exception e) {
        jcmProp.setErr(e.getMessage());
      }
    }

    mainPanel.invalidate();
    mainPanel.revalidate();
    mainPanel.repaint();

  }

  Dictionary<String, Object> getProps()
  {
    final Hashtable<String, Object> out = new Hashtable<String, Object>();

    int errCount = 0;
    for (final Object element : props.keySet()) {
      final String name = (String) element;
      final JCMProp jcmProp = props.get(name);
      try {
        final Object val = jcmProp.getValue();

        out.put(name, val);
        jcmProp.setErr(null);
      } catch (final Exception e) {
        errCount++;
        jcmProp.setErr(e.getMessage());
        // System.out.println(name + ": " + e);
        Activator.log.error("Failed to convert value in " + name, e);
      }
    }

    mainPanel.invalidate();
    mainPanel.revalidate();
    mainPanel.repaint();

    if (errCount > 0) {
      throw new IllegalArgumentException("Failed to convert " + errCount
                                         + " values");
    }
    return out;
  }
}
