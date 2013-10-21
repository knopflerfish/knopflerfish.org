package org.knopflerfish.bundle.desktop.cm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ControlPanel
  extends JPanel
{

  private static final long serialVersionUID = 1L;

  /** Label for the create button when working with non-factory PIDs. */
  private static final String CREATE_LABEL = "Create";

  /** Label for the create button when working with factory PIDs. */
  private static final String CREATE_LABEL_FACTORY = "New";

  /**
   * The owner of data that this panel presents controls for.
   */
  private final JCMService owner;

  final JButton createButton = new JButton(CREATE_LABEL);
  final JButton applyButton = new JButton("Apply");
  final JButton copyFromButton = new JButton("Copy...");
  final JButton delButton = new JButton("Delete");
  final JButton exportButton = new JButton("Export...");

  /**
   * Label that presents the optional icon from the configuration meta-data.
   */
  final JLabel iconLabel = new JLabel();

  /**
   * Icon label presenting the description of the current OCD if any.
   * The description is presented as a tool-tip on top of the icon.
   */
  final JLabel infoLabel = new JLabel(CMDisplayer.infoIcon);


  /**
   * Panel with controls for CRUD operations on the current configuration.
   *
   * @param owner The model to make call-backs to.
   */
  public ControlPanel(final JCMService owner)
  {
    this.owner = owner;

    // This panel is horizontal box.
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

    add(applyButton);
    add(createButton);
    add(copyFromButton);
    add(delButton);
    add(Box.createHorizontalGlue());
    add(exportButton);
    add(Box.createHorizontalStrut(9));
    add(iconLabel);
    add(infoLabel);

    createButton.setToolTipText("Create a new configuration from values below");
    createButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        ControlPanel.this.owner.createConfig();
      }
    });


    applyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        ControlPanel.this.owner.applyConfig();
      }
    });
    applyButton
        .setToolTipText("Applies changes made to the selected configuration and saves it.");


    delButton.setToolTipText("Delete configuration");
    delButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        ControlPanel.this.owner.deleteConfig();
      }
    });

    exportButton.setToolTipText("Exports the selected configuration "
                                + "to a cm_data XML document.");
    exportButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        ControlPanel.this.owner.exportConfiguration();
      }
    });

    copyFromButton.setToolTipText("Copy property values from "
                                  +"another configuration to "
                                  +"the configuration editor below.");
    copyFromButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        ControlPanel.this.owner.copyConfiguration();
      }
    });
  }

  /**
   * Update the states of our controls to match the state of the configuration
   * selected in the target panel.
   *
   * @param cfgExists
   *          If the selected configuration exists or not.
   * @param isFactory
   *          If the selected configuration is a factory configuration or not.
   */
  void updateState(final boolean cfgExists, boolean isFactory)
  {
    applyButton.setEnabled(cfgExists);

    createButton.setEnabled(!cfgExists || isFactory);
    createButton.setText(isFactory ? CREATE_LABEL_FACTORY : CREATE_LABEL);

    delButton.setEnabled(cfgExists);
    exportButton.setEnabled(cfgExists);

    copyFromButton.setVisible(true);

    invalidate();
    repaint();
  }

  /**
   * Update the OCD icon form a stream.
   *
   * @param in
   *          optional stream to load the icon from.
   */
  void updateIcon(InputStream in)
  {
    if (in == null) {
      iconLabel.setVisible(false);
    } else {
      try {
        final ImageIcon icon = new ImageIcon(loadStream(in));
        iconLabel.setIcon(icon);
        iconLabel.setVisible(true);
      } catch (final Exception e) {
        Activator.log.error("Failed to load icon", e);
        iconLabel.setVisible(false);
      }
    }
    invalidate();
    repaint();
  }

  /**
   * Load a stream into a byte array.
   */
  private byte[] loadStream(InputStream is)
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

  /**
   * Update the description text presented as a tool tip on the info label and
   * also on the icon label when present.
   *
   * @param description
   *          Optional description for the current OCD.
   */
  void updateDescription(String description)
  {
    if (description != null && !"".equals(description)) {
      final String tt = "<html>" + description + "</html>";
      infoLabel.setToolTipText(tt);
      infoLabel.setVisible(true);
      if (iconLabel.isVisible()) {
        iconLabel.setToolTipText(tt);
      }
    } else {
      infoLabel.setVisible(false);
    }

    invalidate();
    repaint();
  }
}
