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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import org.knopflerfish.shared.cm.CMDataReader;
import org.knopflerfish.shared.cm.CMDataWriter;
import org.knopflerfish.util.metatype.AD;

public class JCMService
  extends JPanel
  implements ConfigurationListener
{
  /** Item label for the default values of a factory configuration. */
  private static final String FACTORY_PID_DEFAULTS = " - Default Values -";
  private static final long serialVersionUID = 1L;

  /** The currently presented object class definition. */
  ObjectClassDefinition ocd;
  /** The currently presented PID. Either a factory PID or a normal PID. */
  String designatedPid;
  /** ? */
  boolean isService = true;
  /** Set to true when the designated PID is a factory PID. */
  boolean isFactory = false;

  /** Our main UI component. Hold topPanel and mainPanel. */
  JPanel main;
  /** The panel that presents the selected PID. */
  JPanel mainPanel;
  /** Panel presenting the properties of the configuration. */
  JPanel propPane;

  /** Current property values to present. */
  Map<String, JCMProp> props = new HashMap<String, JCMProp>();


  /**
   * The pid of the factory configuration instance that is presented, null
   * otherwise.
   */
  String factoryPid;

  /** Service registration for our white-board configuration listener. */
  ServiceRegistration<ConfigurationListener> srCfgListener;


  /**
   * Create an instance of this UI. Will register the white-board configuration
   * changed listener.
   */
  public JCMService()
  {
    super(new BorderLayout());

    main = new JPanel(new BorderLayout());
    add(main, BorderLayout.CENTER);

    // Listen for configuration changes so that the view can be updated if the
    // configuration it presents changes in any way.
    srCfgListener =
      Activator.bc.registerService(ConfigurationListener.class, this, null);
  }

  /**
   * Called when this UI component is no longer wanted. Will unregister the
   * white-board configuration changed listener.
   */
  void stop()
  {
    // Unregister the configuration listener.
    srCfgListener.unregister();
  }

  /**
   * Configuration changed listener call-back. Update the UI if the event is
   * about the configuration that is currently presented (designated PID matches
   * the PID or factory PID in the event.
   *
   * @param event
   *          configuration changed event.
   */
  public void configurationEvent(final ConfigurationEvent event)
  {
    Activator.log.debug("Configuration changed: pid=" + event.getPid()
                        + ", fpid=" + event.getFactoryPid() + ", type="
                        + event.getType());
    if (event.getPid().equals(designatedPid)) {
      updateOCD();
    } else {
      final String fpid = event.getFactoryPid();
      if (fpid != null && fpid.equals(designatedPid)) {
        updateOCD();
      }
    }
  }

  /**
   * Tell this component to present a configuration with metatype information.
   *
   * @param pid
   *          The PID of the configuration to present.
   * @param ocd
   *          The metatype Object Class Definition describing the configuration
   *          data.
   * @see #setFactoryOCD(String, ObjectClassDefinition)
   */
  void setServiceOCD(String pid, ObjectClassDefinition ocd)
  {
    this.ocd = ocd;
    this.designatedPid = pid;
    isService = ocd != null;
    isFactory = false;
    lastPID = null;
    updateOCD();
  }

  /**
   * Tell this component to present a factory configuration with metatype
   * information.
   *
   * @param pid
   *          The factory PID to present configurations for.
   * @param ocd
   *          The metatype Object Class Definition describing the configuration
   *          data.
   * @see #setServiceOCD(String, ObjectClassDefinition)
   */
  void setFactoryOCD(String pid, ObjectClassDefinition ocd)
  {
    this.ocd = ocd;
    this.designatedPid = pid;
    isService = false;
    isFactory = ocd != null;
    lastPID = null;
    updateOCD();
  }

  /**
   * Rebuild the UI according to the current Object Class Definition and
   * designated PID.
   */
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
        final JButton facExportButton = new JButton("Export...");

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
              facExportButton.setEnabled(exists);
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
            deleteFactoryPid(factoryPid);
          }
        });

        facExportButton
            .setToolTipText("Exports the selected factory configuration "
                            + "instance to an cm_data XML document.");
        facExportButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev)
          {
            exportConfiguration(factoryPid);
          }
        });

        ctrlPanel.add(newButton);
        ctrlPanel.add(facdelButton);
        ctrlPanel.add(facapplyButton);
        ctrlPanel.add(facExportButton);
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
          final JButton applyButton = new JButton("Apply");
          applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev)
            {
              applyConfig(designatedPid);
            }
          });
          applyButton
              .setToolTipText("Applies and stores the configuration changes");

          final JButton delButton = new JButton("Delete");
          delButton.setToolTipText("Delete configuration");
          delButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev)
            {
              deleteConfig(designatedPid);
            }
          });

          final JButton exportButton = new JButton("Export...");
          exportButton.setToolTipText("Exports the selected configuration "
                                      + "to a cm_data XML document.");
          exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev)
            {
              exportConfiguration(designatedPid);
            }
          });

          ctrlPanel.add(applyButton);
          ctrlPanel.add(delButton);
          ctrlPanel.add(exportButton);

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

  /**
   * Populate the properties part of the UI with name, description, editable
   * value for all provided attribute definitions.
   *
   * @param propPane
   *          The UI component to add row to.
   * @param ads
   *          The attribute definitions to present.
   * @param configProps
   *          Current values of the attributes.
   * @param info
   *          Extra information to add to the tool tip for the attribute label.
   */
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


  /**
   * Saves the specified (factory) configuration in a cm_data XML file.
   *
   * @param pid PID of the configuration instance to save.
   */
  private void exportConfiguration(String pid)
  {
    try {
      final Configuration cfg = CMDisplayer.getCA().getConfiguration(pid, null);

      final JFileChooser saveFC = new JFileChooser();
      final File cwd = new File(".");
      saveFC.setCurrentDirectory(cwd);
      saveFC.setSelectedFile(new File(cwd, pid + ".xml"));
      saveFC.setMultiSelectionEnabled(false);
      final FileFilterImpl filter = new FileFilterImpl();
      filter.addExtension("xml");
      filter.setDescription("cm_data");
      saveFC.setFileFilter(filter);
      saveFC.setDialogTitle("Export Configuration '" + pid + "'.");
      saveFC.setApproveButtonText("Save");

      final int returnVal = saveFC.showSaveDialog(SwingUtilities.getRoot(this));
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File file = saveFC.getSelectedFile();
        if (file.exists()) {
          final String msg =
            "The file '" + file + "' already exists.\n\n" + "Overwrite it?";
          final String title = "Overwrite '" + file + "'?";
          final int confirm =
            JOptionPane.showConfirmDialog(this, msg, title,
                                          JOptionPane.OK_CANCEL_OPTION);
          if (confirm != JOptionPane.OK_OPTION) {
            // Done since not OK to over write the file.
            return;
          }
        }
        final OutputStream out = new FileOutputStream(file);
        final OutputStreamWriter ow =
          new OutputStreamWriter(out, CMDataReader.ENCODING);
        final PrintWriter pw = new PrintWriter(ow);
        try {
          if (cfg.getFactoryPid() != null) {
            CMDataWriter.writeFactoryConfiguration(cfg.getFactoryPid(),
                                                   cfg.getPid(),
                                                   cfg.getProperties(), pw);
          } else {
            CMDataWriter.writeConfiguration(cfg.getPid(), cfg.getProperties(),
                                            pw);
          }
        } finally {
          pw.close();
        }
      }
    } catch (final Exception e) {
      final String msg =
        "Export of configuration instance failed, pid=" + pid + ", "
            + e.getMessage();
      Activator.log.error(msg, e);
      showError(msg, e);
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
      // This UI will be updated via its configuration event listener.
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
        // This UI will be updated via its configuration event listener.
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
        // This UI will be updated via its configuration event listener.
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
        // This UI will be updated via its configuration event listener.
      } catch (final Exception e) {
        Activator.log.error("Failed to create/update pid=" + pid, e);
      }
    } catch (final Exception e) {
      Activator.log.error("Failed to get props for pid=" + pid, e);
    }
  }

  // The PID to select in the factory configuration instance combo box in a call
  // to updateOCD().
  String lastPID = null;

  void applyConfig(String pid)
  {
    try {
      final Dictionary<String, Object> props = getProps();

      try {
        final Configuration conf =
          CMDisplayer.getCA().getConfiguration(pid, null);
        conf.update(props);
        // This UI will be updated via its configuration event listener.
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
    for (final Entry<String, JCMProp> entry : props.entrySet()) {
      final String name = entry.getKey();
      final JCMProp jcmProp = entry.getValue();
      try {
        final Object val = jcmProp.getValue();
        out.put(name, val);
        jcmProp.setErr(null);
      } catch (final Exception e) {
        errCount++;
        jcmProp.setErr(e.getMessage());
        jcmProp.invalidate();
        Activator.log.error("Failed to convert value for '" + name + "', " + e,
                            e);
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
