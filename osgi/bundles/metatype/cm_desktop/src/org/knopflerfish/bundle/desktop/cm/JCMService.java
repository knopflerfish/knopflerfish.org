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
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.ObjectClassDefinition;

import org.knopflerfish.bundle.desktop.cm.TargetPanel.ConfigurationAlternative;
import org.knopflerfish.shared.cm.CMDataReader;
import org.knopflerfish.shared.cm.CMDataWriter;

/**
 * UI to work with configurations for one OCD (Object Class Definition).
 *
 * @author ekolin
 */
public class JCMService
  extends JPanel
{
  private static final long serialVersionUID = 1L;

  /**
   * Panel to place controls etc. in placed in the north slot of the main panel.
   */
  final Box northPanel = Box.createVerticalBox();

  /** Panel presenting the properties of the configuration. */
  final PropertiesPanel propPanel = new PropertiesPanel();

  /**
   * Scroll pane scrolling the {@code #propPane}.
   */
  final JScrollPane propScroll = new JScrollPane(propPanel);

  final TargetPanel targetPanel = new TargetPanel(this);
  final ControlPanel controlPanel = new ControlPanel(this);


  /**
   * Create an instance of this UI. Will register the white-board configuration
   * changed listener.
   */
  public JCMService()
  {
    super(new BorderLayout());

    northPanel.add(targetPanel);
    northPanel.add(controlPanel);

    final JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(northPanel, BorderLayout.NORTH);

    propScroll.setBorder(null);

    add(topPanel, BorderLayout.NORTH);
    add(propScroll, BorderLayout.CENTER);

  }

  /**
   * Called when this UI component is no longer wanted.
   */
  void stop()
  {
    targetPanel.stop();
  }

  /**
   * Tell this component to present a configuration with metatype information.
   *
   * @param pid
   *          The PID of the configuration to present.
   * @param bundle
   *          The select bundle to present targeted versions of the PID for.
   * @param ocd
   *          The metatype Object Class Definition describing the configuration
   *          data.
   * @see #setFactoryOCD(String, ObjectClassDefinition)
   */
  void setServiceOCD(String pid, Bundle bundle, ObjectClassDefinition ocd)
  {
    updateOCD(pid, ocd);
    targetPanel.updateTargeted(pid, bundle, false);
  }

  /**
   * Tell this component to present a factory configuration with metatype
   * information.
   *
   * @param pid
   *          The factory PID to present configurations for.
   * @param bundle
   *          The select bundle to present targeted versions of the factory PID
   *          for.
   * @param ocd
   *          The metatype Object Class Definition describing the configuration
   *          data.
   * @see #setServiceOCD(String, ObjectClassDefinition)
   */
  void setFactoryOCD(String pid, Bundle bundle, ObjectClassDefinition ocd)
  {
    updateOCD(pid, ocd);
    targetPanel.updateTargeted(pid, bundle, true);
  }

  /**
   * Rebuild the UI according to the current Object Class Definition and
   * designated PID.
   *
   * @param pid
   *          The base (factory) PID of the configuration to present.
   * @param ocd
   *          The metatype Object Class Definition describing the configuration
   *          data.
   */
  void updateOCD(String pid, ObjectClassDefinition ocd)
  {
    if (ocd != null) {
      setBorder(JCMInfo.makeBorder(this, pid));

      // Rebuild the props pane presenting default values.
      propPanel.rebuild(ocd);

      // The icon from current OCD if any.
      try {
        controlPanel.updateIcon(ocd.getIcon(16));
      } catch (final IOException e) {
        controlPanel.updateIcon(null);
        Activator.log.error("Failed to get icon stream form OCD-oibject: "
                                + e.getMessage(), e);
      }

      // The description from current OCD if any.
      controlPanel.updateDescription(ocd.getDescription());
    } else {
      controlPanel.updateIcon(null);
      controlPanel.updateDescription(null);
      propPanel.rebuild(null);
    }

    invalidate();
    revalidate();
    repaint();
  }

  /**
   * Callback from the {@code #targetPanel} that informs about a target
   * selection change. Update control panel buttons and the property
   * presentation to show the current configuration.
   */
  void targetSelectionChanged()
  {
    final Configuration cfg = targetPanel.getSelectedConfiguration();
    controlPanel.updateState(cfg != null, targetPanel.isFactoryPid());

    final Dictionary<String, Object> newProps =
      cfg != null ? cfg.getProperties() : null;
    propPanel.setProps(newProps);
  }

  /**
   * Saves the current configuration in a cm_data XML file.
   */
  void exportConfiguration()
  {
    final String pid = targetPanel.getSelectedPid();

    try {
      final Configuration cfg = targetPanel.getSelectedConfiguration();

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
      showError(this, msg, e);
    }
  }

  /**
   * Update property values in the current props pane with those from the
   * selected configuration.
   */
  void copyConfiguration()
  {
    try {
      final Set<ConfigurationAlternative> cfgAlternatives =
        targetPanel.getAlternatives();

      final ConfigurationAlternative selectedValue =
        (ConfigurationAlternative) JOptionPane
            .showInputDialog(null, "Choose configuration to copy from",
                             "Select original to copy form",
                             JOptionPane.INFORMATION_MESSAGE, null,
                             cfgAlternatives.toArray(), cfgAlternatives
                                 .iterator().next());
      if (selectedValue != null) {
        // If no cfg present in the selected alternative use the default (call
        // setProps with an empty dictionary)
        final Dictionary<String, Object> newProps =
          selectedValue.cfg != null
            ? selectedValue.cfg.getProperties()
            : new Hashtable<String, Object>();
        propPanel.setProps(newProps);
      }
    } catch (final Exception e) {
      final String msg = "Copy configuration values failed, " + e.getMessage();
      Activator.log.error(msg, e);
      showError(this, msg, e);
    }
  }

  void createConfig()
  {
    try {
      targetPanel.createSelectedConfiguration(propPanel.getProps());
    } catch (final Exception e) {
      showError(this, "Failed to create configuration, pid: '"
                                  + targetPanel.getSelectedPid() + "'.", e);
    }
  }

  void deleteConfig()
  {
    final String pid = targetPanel.getSelectedPid();

    try {
      targetPanel.deleteSelectedConfiguration();
    } catch (final Exception e) {
      showError(this, "Failed to delete configuration, pid: '" + pid +"'.", e);
    }
  }

  /**
   * Applies the configuration data in the properties view to the current PID.
   */
  void applyConfig()
  {
    try {
      final Configuration conf =
        targetPanel.getSelectedConfiguration(false, propPanel.getProps());
      if (conf == null) {
        showError(this, "Can not update non-exisiting configuration", null);
      }
      // UI will be updated by the configuration listener in the target panel.
    } catch (final Exception e) {
      Activator.log.error("Failed to apply new values to configuration; " +e.getMessage(), e);
    }
  }

  static void showError(Component component, String msg, Throwable t)
  {
    Activator.log.error(msg, t);
    JOptionPane.showMessageDialog(component, msg + "\n" + t.toString(), msg,
                                  JOptionPane.ERROR_MESSAGE);
  }

}
