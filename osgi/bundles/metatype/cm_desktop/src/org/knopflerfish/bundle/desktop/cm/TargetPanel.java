package org.knopflerfish.bundle.desktop.cm;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/**
 * A {@link JPanel} that displays PID target selection options.
 *
 * @author Gunnar Ekolin
 *
 */
public class TargetPanel
  extends JPanel
  implements ConfigurationListener
{

  private static final long serialVersionUID = 1L;

  /**
   * Names of the four levels or targeting that may be used for a PID.
   */
  private final static String[] TARGET_LEVEL_NAMES =
    new String[] { "plain PID", "symbolic name", "version", "location" };
  /**
   * Names of the four levels or targeting used in the configuration
   * alternatives instances.
   */
  private final static String[] ALTERNATIVE_TARGET_LEVEL_NAMES =
    new String[] { "PID (no target specified)",
                  "PID targeted on symbolic name",
                  "PID targeted on symbolic name and version",
                  "PID targeted on symbolic name, version and location" };
  /**
   * Tool tip texts for the for levels of PID targeting.
   */
  private static final String[] TARGET_LEVEL_PID_TOOLTIPS =
    new String[] {
                  "<html><p>PID without any bundle target specification.<br/>Actual PID: <code>",
                  "<html><p>PID with target specification for<br/> the bundles symbolic name.<br/>Actual PID: <code>",
                  "<html><p>PID with target specification for<br/> the bundles symbolic name and version.<br/>Actual PID: <code>",
                  "<html><p>PID with target specification for<br/> the bundles symbolic name, version and location.<br/>Actual PID: <code>", };

  private static final String[] TARGET_LEVEL_FACOTRY_PID_TOOLTIPS =
    new String[] {
                  "<html><p>Factory PID without any bundle target specification.<br/>Actual factory PID: <code>",
                  "<html><p>Factory PID with target specification for<br/> the bundles symbolic name.<br/>Actual factory PID: <code>",
                  "<html><p>Factory PID with target specification for<br/> the bundles symbolic name and version.<br/>Actual factory PID: <code>",
                  "<html><p>Factory PID with target specification for<br/> the bundles symbolic name, version and location.<br/>Actual factory PID: <code>", };

  private static final String CONFIGURATION_TARGET_LEVEL_TOOLTIP =
    "<html><body><p>Selects the targeted PID for the selected bundle "
        + "to present below.</p></body></html>";

  /**
   * Maximum number of targeted PIDs generated for one PID. Includes the PID
   * itself.
   */
  public static final int MAX_SIZE = TARGET_LEVEL_NAMES.length;

  /**
   * The currently selected targeting level.
   */
  private int selectedTargetLevel = 0;

  /**
   * Array with the targeted PIDs for each target level. Index 0 contains the
   * plain PID.
   */
  private final String[] targetedPids = new String[MAX_SIZE];

  /**
   * Mapping from (factory) configuration instance PID to the actual
   * configuration.
   */
  private final Map<String, Configuration> pid2Cfg =
    new HashMap<String, Configuration>();

  /**
   * A horizontal box that presents all instances of the selected factory PID.
   */
  private final Box factoryPidInstanceSelectionBox = Box.createHorizontalBox();

  /**
   * A horizontal box with choices for targeting the PID to a specific bundle.
   */
  private final Box targetSelectionBox = Box.createHorizontalBox();

  /**
   * Array with the radio buttons for each target level.
   */
  private final JRadioButton[] rbs = new JRadioButton[MAX_SIZE];

  /**
   * The button group linking the radio buttons together
   */
  private final ButtonGroup bg = new ButtonGroup();

  /**
   * Array with icons showing if a configuration for that targeted PID exists or
   * not.
   */
  private final JLabel[] icons = new JLabel[MAX_SIZE];

  /** Item label for the default values of a factory configuration. */
  private static final String FACTORY_PID_DEFAULTS = " - Default Values -";

  /** Tool tip for the factory PID instance selector. */
  private static final String FACTORY_PID_INSTANCE_SELECTOR_TOOLTIP =
    "Select the factory PID instance to work with.";

  /**
   * Combo box that displays PIDs of the factory configuration instances
   * belonging to {@code #targetedPids[0]}.
   */
  final JComboBox fbox = new JComboBox();

  /**
   * Icon for an existing target configuration.
   */
  private final Icon openDocumentIcon;

  /**
   * Icon for an non-existing target configuration.
   */
  private final Icon newDocumentIcon;

  /**
   * The model (owner) to be notified when the selection changes.
   */
  private final JCMService owner;

  /**
   * If {@code true} then the PID is a factory PID.
   */
  private boolean isFactoryPid;

  /** Service registration for our white-board configuration listener. */
  ServiceRegistration<ConfigurationListener> srCfgListener;

  /**
   * The factory configuration instance to select when
   * {@link #updateSelection(boolean)} is called with false.
   */
  private String nextFactoryPidToSelect;

  /**
   * The configuration instance to select when {@link #updateSelection(boolean)}
   * is called with false.
   */
  private String nextPidToSelect;

  /**
   * Creates a targeted PID selection panel.
   *
   * @param owner
   *          Owner to be notified when the selection changes.
   */
  public TargetPanel(JCMService owner)
  {
    this.owner = owner;

    // Load icons.
    openDocumentIcon =
      new ImageIcon(this.getClass().getResource("document-open.png"),
                    "Open configuration.");
    newDocumentIcon =
      new ImageIcon(this.getClass().getResource("document-new.png"),
                    "Create new configuration.");

    buildTargetSelectionBox();
    buildFactoryPidInstanceSelectionBox();

    // This panel is vertical box with two items.
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(factoryPidInstanceSelectionBox);
    add(targetSelectionBox);

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

  private void buildTargetSelectionBox()
  {
    final JLabel targetedLabel = new JLabel("Configuration target level: ");
    targetedLabel.setToolTipText(CONFIGURATION_TARGET_LEVEL_TOOLTIP);
    targetedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    targetedLabel.setAlignmentY(Component.LEFT_ALIGNMENT);

    targetSelectionBox.add(targetedLabel);
    targetSelectionBox.add(Box.createHorizontalStrut(30));

    for (int i = 0; i < MAX_SIZE; i++) {
      rbs[i] = new JRadioButton(TARGET_LEVEL_NAMES[i]);
      bg.add(rbs[i]);
      rbs[i].setActionCommand(String.valueOf(i));
      rbs[i].setAlignmentX(Component.CENTER_ALIGNMENT);
      rbs[i].setAlignmentY(Component.LEFT_ALIGNMENT);
      rbs[i].addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent event)
        {
          // If this is a factory PID then changing target level must be handled
          // as a request for a new instance with the selected binding.
          // Thus we change the selected PID in fbox to the default entry. We
          // must do this without triggering an action event, thus replace the
          // model with a new empty model, change selection in the real model
          // and set it back as the model of the combo-box.
          final ComboBoxModel cbm = fbox.getModel();
          fbox.setModel(new DefaultComboBoxModel());
          cbm.setSelectedItem(FACTORY_PID_DEFAULTS);
          fbox.setModel(cbm);

          selectedTargetLevel = Integer.parseInt(event.getActionCommand());
          TargetPanel.this.owner.targetSelectionChanged();
        }
      });
      targetSelectionBox.add(rbs[i]);

      icons[i] = new JLabel(newDocumentIcon);
      icons[i].setAlignmentX(Component.BOTTOM_ALIGNMENT);
      icons[i].setAlignmentY(Component.LEFT_ALIGNMENT);
      targetSelectionBox.add(icons[i]);
      targetSelectionBox.add(Box.createHorizontalGlue());
    }
  }

  private void buildFactoryPidInstanceSelectionBox()
  {
    final JLabel factoryInstanceLabel = new JLabel("Factory PID instance: ");
    factoryInstanceLabel.setToolTipText(FACTORY_PID_INSTANCE_SELECTOR_TOOLTIP);
    factoryInstanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    factoryInstanceLabel.setAlignmentY(Component.LEFT_ALIGNMENT);

    fbox.setAlignmentX(Component.CENTER_ALIGNMENT);
    fbox.setAlignmentY(Component.LEFT_ALIGNMENT);
    fbox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final String slectedPid = (String) fbox.getSelectedItem();
        updateSelectionFactoryPID(slectedPid);
        TargetPanel.this.owner.targetSelectionChanged();
      }
    });

    factoryPidInstanceSelectionBox.add(factoryInstanceLabel);
    factoryPidInstanceSelectionBox.add(Box.createHorizontalStrut(30));
    factoryPidInstanceSelectionBox.add(fbox);
  }

  /**
   * Configuration changed listener call-back. Update the selection if the event
   * is about one of the configurations that is currently presented.
   *
   * @param event
   *          configuration changed event.
   */
  @Override
  public void configurationEvent(final ConfigurationEvent event)
  {
    final String msg =
      "Configuration changed: pid=" + event.getPid() + ", fpid="
          + event.getFactoryPid() + ", type=" + event.getType();
    Activator.log.debug(msg);

    if (ConfigurationEvent.CM_DELETED == event.getType()) {
      if (event.getPid().equals(getSelectedPid())) {
        // Current selection is deleted, select another configuration and update
        // the UI.
        updateSelection(true);
      } else if (isTargetedPid(event.getPid())
                 || isTargetedPid(event.getFactoryPid())) {
        // Another visible configuration has been deleted, update UI without
        // changing selection.
        updateSelection(false);
      }
    } else if (ConfigurationEvent.CM_UPDATED == event.getType()) {
      if (isTargetedPid(event.getPid()) || isTargetedPid(event.getFactoryPid())) {
        // A visible configuration has been deleted, update UI without changing
        // selection.
        updateSelection(false);
      }
    }
    // Events of other types (i.e., ConfigurationEvent.LOCATION_CHANGED) does
    // not effect the presentation so skip them.
  }

  /**
   * Update this panel to show configurations for another PID and bundle.
   *
   * @param pid
   *          The (non-targeted) PID to show in this panel.
   * @param bundle
   *          The bundle to do targeted PIDs.
   * @param isFactoryPid
   *          Set to {@code true} to indicate that the specified PID is a
   *          factory PID.
   */
  void updateTargeted(final String pid,
                      final Bundle bundle,
                      boolean isFactoryPid)
  {
    targetedPids[0] = pid;
    this.isFactoryPid = isFactoryPid;

    if (bundle != null) {
      targetedPids[1] = targetedPids[0] + "|" + bundle.getSymbolicName();
      targetedPids[2] = targetedPids[1] + "|" + bundle.getVersion().toString();
      targetedPids[3] = targetedPids[2] + "|" + bundle.getLocation();
    } else {
      targetedPids[1] = null;
      targetedPids[2] = null;
      targetedPids[3] = null;
    }

    // No target selection for the system bundle or the CM bundle when handling
    // a non-factory PID.
    targetSelectionBox.setVisible(!CMDisplayer.isCmBundle(bundle)
                                  && !CMDisplayer.isSystemBundle(bundle));

    updateSelection();
  }

  /**
   * Updates the components of this UI from the model. Resets selection to the
   * most specific of the existing targeted PIDs.
   */
  private void updateSelection()
  {
    updateSelection(true);
  }

  /**
   * Updates the components of this UI from the model. Optionally resets
   * selection to the most specific of the existing targeted PIDs.
   *
   * @param selectBest
   *          If {@code true} reset the selection to targeted PID that CM would
   *          use for the bundle. If {@code false} the current selection is kept
   *          unless the selected configuration does not exists or it is a
   *          factory configuration and {@link #nextFactoryPidToSelect} names
   *          the PID of an existing configuration instance.
   */
  void updateSelection(boolean selectBest)
  {
    if (isFactoryPid) {
      updateSelectionFactoryPID(selectBest ? "" : getSelectedPid());
    } else {
      updateSelectionPID(selectBest);
    }
    factoryPidInstanceSelectionBox.setVisible(isFactoryPid);
    revalidate();
    repaint();
    owner.targetSelectionChanged();
  }

  /**
   * For a non-factory PID load all targeted configurations and select the best
   * one.
   *
   * @param selectBest
   *          If {@code true} reset the selection to targeted PID that CM would
   *          use for the bundle. If {@code false} the current selection is
   *          kept.
   */
  private void updateSelectionPID(boolean selectBest)
  {
    synchronized (pid2Cfg) {
      if (selectBest) {
        selectedTargetLevel = 0;
      }
      pid2Cfg.clear();

      // Find the targeted PIDs that has a configuration and handle
      // nextPidToSelect
      String tpid = null;
      for (int i = 0; i < MAX_SIZE && null != (tpid = targetedPids[i]); i++) {
        rbs[i].setToolTipText(TARGET_LEVEL_PID_TOOLTIPS[i] + tpid
                              + "</code></p></html>");

        final Configuration cfg = CMDisplayer.getConfig(tpid);
        if (cfg != null) {
          pid2Cfg.put(cfg.getPid(), cfg);
          if (tpid.equals(nextPidToSelect)) {
            selectedTargetLevel = i;
            rbs[i].setSelected(true);
            nextPidToSelect = null;
          }
          icons[i].setIcon(openDocumentIcon);
          icons[i].setToolTipText("exists");
        } else {
          icons[i].setIcon(newDocumentIcon);
          icons[i].setToolTipText("to be created");
        }
      }
      nextPidToSelect = null;

      // Update selection to the best configuration when requested or needed.
      if (selectBest || pid2Cfg.get(targetedPids[selectedTargetLevel]) == null) {
        for (int i = 0; i < MAX_SIZE && null != (tpid = targetedPids[i]); i++) {
          if (pid2Cfg.get(tpid) != null) {
            selectedTargetLevel = i;
            rbs[i].setSelected(true);
          }
        }
      }
    }
  }

  /**
   * Load all targeted factory configuration instances and update user interface
   * to show them.
   *
   * @param selectedPid
   *          If this PID is available then select it, otherwise select the last
   *          PID. If {@link #nextFactoryPidToSelect} is non-null then select
   *          that configuration instance and set the field to {@code null}.
   */
  private void updateSelectionFactoryPID(String selectedPid)
  {
    synchronized (pid2Cfg) {
      pid2Cfg.clear();
      if (selectedPid == null) {
        selectedPid = "";
      }

      for (int i = 0; i < MAX_SIZE; i++) {
        try {
          final Configuration[] configs =
            CMDisplayer.getCA().listConfigurations("(service.factoryPid="
                                                       + targetedPids[i] + ")");
          if (configs != null) {
            for (final Configuration cfg : configs) {
              pid2Cfg.put(cfg.getPid(), cfg);
            }
          }
        } catch (final Exception e) {
          Activator.log
              .error("Faile to load factory configuration instances for fpid '"
                     + targetedPids[i] + "': " + e.getMessage(), e);
        }
      }

      final SortedSet<String> instancePIDs =
        new TreeSet<String>(pid2Cfg.keySet());
      instancePIDs.add(FACTORY_PID_DEFAULTS);

      final DefaultComboBoxModel model =
        new DefaultComboBoxModel(instancePIDs.toArray());
      if (nextFactoryPidToSelect != null) {
        if (instancePIDs.contains(nextFactoryPidToSelect)) {
          selectedPid = nextFactoryPidToSelect;
        }
        nextFactoryPidToSelect = null;
      } else if (!instancePIDs.contains(selectedPid)) {
        // New selection needed, use last PID.
        selectedPid = (String) model.getElementAt(model.getSize() - 1);
      }
      model.setSelectedItem(selectedPid);
      fbox.setModel(model);
      final Configuration selectedCfg = pid2Cfg.get(selectedPid);

      // Update the targeted PID selectors to match the target selectors in the
      // factory PID of the selected instance.
      final String fpid =
        selectedCfg != null ? selectedCfg.getFactoryPid() : targetedPids[0];
      String tpid = null;
      for (int i = 0; i < MAX_SIZE && null != (tpid = targetedPids[i]); i++) {
        rbs[i].setToolTipText(TARGET_LEVEL_FACOTRY_PID_TOOLTIPS[i] + tpid
                              + "</code></p></html>");

        if (fpid.equals(targetedPids[i])) {
          rbs[i].setSelected(true);
          selectedTargetLevel = i;
          if (selectedCfg != null) {
            icons[i].setIcon(openDocumentIcon);
            icons[i].setToolTipText("exists");
          } else {
            icons[i].setIcon(newDocumentIcon);
            icons[i].setToolTipText("to be created");
          }
        } else {
          icons[i].setIcon(newDocumentIcon);
          icons[i].setToolTipText("to be created");
        }
      }
    }
  }

  /**
   * Is this panel displaying a factory configuration or not.
   *
   * @return {@code true} if the PID this panel handles is a factory PID and
   *         {@code false} otherwise.
   */
  boolean isFactoryPid()
  {
    return isFactoryPid;
  }

  /**
   * Check if the given PID is one of the PIDs handled by this object.
   *
   * @param pid
   *          The PID to check (factory PID for factory configurations).
   * @return {@code true} if the specified PID is in the list of targeted PIDs.
   */
  boolean isTargetedPid(String pid)
  {
    for (final String targetedPid : targetedPids) {
      if (pid.equals(targetedPid)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the targeted PID for the selected target level.
   *
   * @return The targeted PID for the selected target level.
   */
  String getSelectedPid()
  {
    if (isFactoryPid) {
      return (String) fbox.getSelectedItem();
    } else {
      return targetedPids[selectedTargetLevel];
    }
  }

  /**
   * Fetch the selected configuration.
   *
   * @return The selected configuration or {@code null} if it does not exist.
   */
  Configuration getSelectedConfiguration()
  {
    Configuration res = pid2Cfg.get(getSelectedPid());
    if (res != null && isFactoryPid
        && !res.getFactoryPid().equals(targetedPids[selectedTargetLevel])) {
      // Found factory configuration instance with a target specification that
      // differs from the one currently selected. Nothing to return.
      res = null;
    }
    return res;
  }

  /**
   * Gets and optionally creates, updates the selected configuration.
   *
   * @param create
   *          If {@code true} create the configuration if it does not exists or
   *          if we are working with a factory PID.
   * @param newProps
   *          Update the configuration with these new props before returning it.
   *
   * @return The (updated) selected configuration or {@code null} if
   *         {@code create} is {@code false} and there is no configuration for
   *         the current selection.
   * @throws IOException
   */
  Configuration getSelectedConfiguration(boolean create,
                                         Dictionary<String, Object> newProps)
      throws IOException
  {
    Configuration cfg = getSelectedConfiguration();
    if (create && (cfg == null || isFactoryPid)) {
      if (isFactoryPid) {
        if (newProps == null) {
          final String msg =
            "getSelectedConfigriation(true,null) for factory "
                + "configuration is not supported.";
          throw new IOException(msg);
        }
        final String fpid = targetedPids[selectedTargetLevel];
        cfg = CMDisplayer.getCA().createFactoryConfiguration(fpid, null);

        // Select the new PID in next call to updateSelection(false).
        nextFactoryPidToSelect = cfg.getPid();
      } else {
        cfg =
          CMDisplayer.getCA()
              .getConfiguration(targetedPids[selectedTargetLevel], null);
        // Let the next call to updateSelection(false) select the new PID.
        nextPidToSelect = cfg.getPid();
      }
    }

    if (cfg != null && newProps != null) {
      // Update the configuration with the given set of properties
      cfg.update(newProps);
      // UI will be updated from the ConfgurationListener callback.
    }

    return cfg;
  }

  /**
   * Update the selected configuration with the given set of property values.
   * The configuration will be created if needed.
   *
   * @param props
   *          Properties to update the selected configuration with.
   * @throws Exception
   */
  void createSelectedConfiguration(Dictionary<String, Object> props)
      throws Exception
  {
    try {
      getSelectedConfiguration(true, props);

      // UI will be updated from the ConfgurationListener callback.
    } catch (final Exception e) {
      Activator.log.error("Failed to create/update pid=" + getSelectedPid(), e);
      throw e;
    }
  }

  /**
   * Deletes the currently selected configuration.
   *
   * @throws IOException
   */
  void deleteSelectedConfiguration()
      throws IOException
  {
    final Configuration cfg = getSelectedConfiguration();
    if (cfg != null) {
      cfg.delete();
      // UI will be updated from the ConfgurationListener callback.
    } else {
      owner.showError("Can not delete non-existing configuration.", null);
    }
  }

  /**
   * Get a sorted set with the existing configurations that are not currently
   * selected.
   *
   * @return set of alternative configurations to copy to the current one.
   */
  Set<ConfigurationAlternative> getAlternatives()
  {
    final Set<ConfigurationAlternative> res =
      new TreeSet<ConfigurationAlternative>();

    // The default configuration is always an option.
    res.add(new ConfigurationAlternative("Default", null));

    // Existing (targeted) configurations except the selected one are the other
    // alternatives.
    if (isFactoryPid) {
      for (final Configuration cfg : pid2Cfg.values()) {
        res.add(new ConfigurationAlternative(cfg.getPid(), cfg));
      }
    } else {
      for (int i = 0; i < TargetPanel.MAX_SIZE; i++) {
        if (i == selectedTargetLevel) {
          continue;
        }
        final Configuration cfg = pid2Cfg.get(targetedPids[i]);
        if (cfg != null) {
          res.add(new ConfigurationAlternative(
                                               ALTERNATIVE_TARGET_LEVEL_NAMES[i],
                                               cfg));
        }
      }
    }
    return res;
  }

  /**
   * Class representing one configuration to copy from.
   */
  static class ConfigurationAlternative
    implements Comparable<ConfigurationAlternative>
  {
    /**
     * The name of this configuration alternative.
     */
    public final String label;
    /**
     * The configuration or null (for the default configuration).
     */
    public final Configuration cfg;

    public ConfigurationAlternative(String label, Configuration cfg)
    {
      super();
      this.label = label;
      this.cfg = cfg;
    }

    @Override
    public String toString()
    {
      return label;
    }

    @Override
    public int compareTo(ConfigurationAlternative other)
    {
      return label.compareTo(other.label);
    }
  }

}
