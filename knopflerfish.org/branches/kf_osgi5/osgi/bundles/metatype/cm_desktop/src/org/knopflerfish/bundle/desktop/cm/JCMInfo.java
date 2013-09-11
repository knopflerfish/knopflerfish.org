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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

import org.knopflerfish.shared.cm.CMDataReader;
import org.knopflerfish.util.metatype.MTP;

public class JCMInfo
  extends JPanel
{
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  MetaTypeInformation mti;

  JPanel main;
  JCMService jcmService;

  public JCMInfo()
  {
    super(new BorderLayout());

    main = new JPanel(new BorderLayout());
    jcmService = new JCMService();
    // Initialize presentation as if nothing is selected.
    setProvider(null, null);

    add(main, BorderLayout.CENTER);
  }

  JComboBox servicePIDBox = null;
  JComboBox factoryPIDBox = null;

  void setProvider(MetaTypeInformation mti, Bundle bundle)
  {
    this.mti = mti;
    main.removeAll();
    if (mti != null) {
      renderBundleWithMetaTypeInformation(bundle);
    } else {
      final JHTML jhtml = renderBundleWithoutMetaTypeInformation(bundle);
      main.add(jhtml, BorderLayout.CENTER);
    }
    invalidate();
    revalidate();
    repaint();
  }

  private void renderBundleWithMetaTypeInformation(final Bundle bundle)
  {
    final String[] servicePIDs = mti.getPids();
    servicePIDBox = new JComboBox(servicePIDs);
    servicePIDBox.setEnabled(servicePIDs != null && servicePIDs.length > 0);
    servicePIDBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final int ix = servicePIDBox.getSelectedIndex();
        if (ix == -1) {
          return;
        } else {
          final String pid = (String) servicePIDBox.getSelectedItem();
          setServiceOCD(pid, bundle);
        }
      }
    });

    final String[] factoryPIDs = mti.getFactoryPids();
    factoryPIDBox = new JComboBox(factoryPIDs);
    factoryPIDBox.setEnabled(factoryPIDs != null && factoryPIDs.length > 0);
    factoryPIDBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final int ix = factoryPIDBox.getSelectedIndex();
        if (ix == -1) {
          return;
        } else {
          final String pid = (String) factoryPIDBox.getSelectedItem();
          setFactoryOCD(pid, bundle);
        }
      }
    });

    final JPanel upperBox = new JPanel(new GridLayout(0, 1));
    upperBox.add(new JLabelled("PIDs", "PIDs representing ManagedServices",
                               servicePIDBox, 100));
    upperBox.add(new JLabelled("Factory PIDs",
                               "PIDs representing ManagedServiceFactories",
                               factoryPIDBox, 100));
    String title = getBundleSelectedHeader(bundle);
    if (mti instanceof MTP) {
      title = title + " (" + ((MTP) mti).getId() + ")";
    }
    upperBox.setBorder(makeBorder(this, title));
    main.add(upperBox, BorderLayout.NORTH);

    // Set either the first service or the first factory as displayed
    if (servicePIDs != null && servicePIDs.length > 0) {
      main.add(jcmService, BorderLayout.CENTER);
      setServiceOCD(servicePIDs[0], bundle);
    } else if (factoryPIDs != null && factoryPIDs.length > 0) {
      main.add(jcmService, BorderLayout.CENTER);
      setFactoryOCD(factoryPIDs[0], bundle);
    } else {
      // Neither service PID nor factory PID found in provider; leave the rest
      // of the main-panel empty.
    }
  }

  /**
   * Create a {@link JHTML}-component that presents a bundle without any meta
   * type information.
   *
   * @param bundle
   *          The bundle to present. May be {@code null}.
   * @return component presenting a bundle without meta type information.
   */
  private JHTML renderBundleWithoutMetaTypeInformation(Bundle bundle)
  {
    final StringBuffer sb = new StringBuffer();
    sb.append("<html>\n");
    sb.append("<body>\n");
    sb.append("<table border=0 width=\"100%\">\n");
    sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
    Util.startFont(sb, "-1");
    sb.append(getBundleSelectedHeader(bundle));
    sb.append("</font>\n");
    sb.append("</td>\n");
    sb.append("</tr>\n");
    sb.append("</table>\n");

    sb.append("<p>");
    Util.startFont(sb, "-2");
    if (bundle==null) {
      sb.append("Select a bundle in the main view to view CM metatype information for it.<br>");
    } else {
      sb.append("No CM metatype found in <em>");
      sb.append(Util.getBundleName(bundle));
      sb.append("</em>.");
    }
    sb.append("</font>");
    sb.append("</p>");

    renderSystemMTP(sb);
    renderCmMTP(sb);
    renderMetaTypeBundles(sb);
    sb.append("<p></p>");
    renderImportConfigruations(sb);

    sb.append("</body>\n");
    sb.append("</html>\n");

    return new JHTML(sb.toString());
  }

  private void renderSystemMTP(final StringBuffer sb)
  {
    final Bundle systemBundle = Activator.bc.getBundle(0);
    if (Activator.getMTP(systemBundle) != null) {
      sb.append("<p>");
      Util.startFont(sb, "-2");
      sb.append("Select the ");
      Util.bundleLink(sb, systemBundle);
      sb.append(" to see all configurations that can be created or edited based on associated metatype information.");
      sb.append("</font></p>");
    }
  }

  private void renderCmMTP(final StringBuffer sb)
  {
    final Bundle cmBundle =
      CMDisplayer.cmTracker.getServiceReference().getBundle();
    if (cmBundle != null && Activator.getMTP(cmBundle) != null) {
      sb.append("<p>");
      Util.startFont(sb, "-2");
      sb.append("Select the ");
      Util.bundleLink(sb, cmBundle);
      sb.append(" bundle to see and edit all exisiting configurations.");
      sb.append("</font></p>");
    }
  }

  private void renderMetaTypeBundles(StringBuffer sb)
  {
    final Bundle[] bundles = CMDisplayer.getAllBundles();
    sb.append("<p>");
    Util.startFont(sb, "-2");
    sb.append("Bundles with associated metatype information: ");
    boolean first = true;
    for (final Bundle bundle : bundles) {
      if (Activator.getMTI(bundle) != null) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        Util.bundleLink(sb, bundle);
      }
    }
    sb.append("</font></p>");
  }

  /**
   * Draw the {@code Import...} button.
   *
   * @param sb
   *          buffer to append the HTML to.
   */
  private void renderImportConfigruations(final StringBuffer sb)
  {
    // Must place the form contents in a table to get the input-button
    // vertically centered!
    sb.append("<table border=0 cellspacing=1 cellpadding=1>\n");
    sb.append("<tr><td valign='middle'>");
    Util.startFont(sb, "-2");
    sb.append("Import configurations from a cm_data XML document: ");
    Util.stopFont(sb);
    sb.append("</td><td valign='middle'>");
    sb.append("<form action=\"");
    sb.append(Util.URL_CM);
    sb.append("\" method=\"get\">");
    sb.append("<input type=\"submit\" name=\"");
    sb.append(Util.URL_CM_CMD);
    sb.append("\" value=\"");
    sb.append(Util.URL_CM_CMD_IMPORT);
    sb.append("\">");
    sb.append("</form>");
    sb.append("</td></tr>");
    sb.append("</table>");
  }

  /**
   * Import configurations from a cm_data XML file. Pop-up a dialog asking for
   * the name of the file to import, do the actual import work then show a
   * confirmation dialog with the PIDs of the imported configurations.
   *
   * @param comp
   */
  static void importCfg(Component comp)
  {
    File file = null;
    try {
      System.out.println("importCfg()");
      final JFileChooser importFC = new JFileChooser();
      final File cwd = new File(".");
      importFC.setCurrentDirectory(cwd);
      importFC.setMultiSelectionEnabled(false);
      final FileFilterImpl filter = new FileFilterImpl();
      filter.addExtension("xml");
      filter.setDescription("cm_data");
      importFC.setFileFilter(filter);
      importFC.setDialogTitle("Import Configurations from cm_data docuements.");
      importFC.setApproveButtonText("Import");

      final int returnVal =
        importFC.showOpenDialog(SwingUtilities.getRoot(comp));
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        file = importFC.getSelectedFile();
        if (file.exists()) {
          final URL url = file.toURI().toURL();
          PushbackReader reader = null;
          try {
            final CMDataReader cmDataReader = new CMDataReader();
            reader =
              new PushbackReader(
                                 new BufferedReader(
                                                    new InputStreamReader(
                                                                          url.openStream(),
                                                                          CMDataReader.ENCODING),
                                                    8192), 8);
            final Hashtable<String, Object>[] configs =
              cmDataReader.readCMDatas(reader);

            final StringBuffer sb = new StringBuffer(100*configs.length);
            sb.append("<html>Imported configurations with PIDs:<ul>");
            for (final Hashtable<String, Object> props : configs) {
              final String pid = (String) props.get(CMDataReader.SERVICE_PID);
              final String fpid = (String) props.get(CMDataReader.FACTORY_PID);
              Configuration cfg;
              if (fpid == null) {
                cfg = CMDisplayer.getCA().getConfiguration(pid, null);
                sb.append("<li>");
                sb.append(pid);
              } else {
                cfg =
                  CMDisplayer.getCA().createFactoryConfiguration(fpid, null);
                sb.append("<li>");
                sb.append(cfg.getPid());
                sb.append(" (factory PID ");
                sb.append(fpid);
                sb.append(")");
              }
              cfg.update(props);
            }
            sb.append("</ul></html>");
            JOptionPane.showMessageDialog(comp, sb.toString(),
                                          "Imported Configurations",
                                          JOptionPane.INFORMATION_MESSAGE);
            sb.setLength(0);
          } finally {
            if (reader != null) {
              reader.close();
            }
          }
        }
      }
    } catch (final Exception e) {
      final String msg =
        "Import of cm_data configuration document failed, file="
            + (file != null ? file.getAbsolutePath() : "-") + ", "
            + e.getMessage();
      Activator.log.error(msg, e);
      JCMService.showError(comp, msg, e);
    }

  }

  static Border makeBorder(JComponent comp, String title)
  {
    return BorderFactory.createTitledBorder(title);
  }

  String getBundleSelectedHeader(Bundle b)
  {
    if (b==null) {
      return "No bundle selected";
    }
    return "#" + b.getBundleId() + "  " + Util.getBundleName(b);
  }

  void setServiceOCD(String pid, Bundle bundle)
  {
    try {
      final ObjectClassDefinition ocd = mti.getObjectClassDefinition(pid, null);

      jcmService.setServiceOCD(pid, bundle, ocd);
    } catch (final Throwable t) {
      Activator.log.error("Failed to set service pid=" + pid, t);
    }
  }

  void setFactoryOCD(String pid, Bundle bundle)
  {
    final ObjectClassDefinition ocd = mti.getObjectClassDefinition(pid, null);

    jcmService.setFactoryOCD(pid, bundle, ocd);
  }

  void stop()
  {
    jcmService.stop();
  }
}

class JHTML
  extends JPanel
{
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  JTextPane html;
  JScrollPane scroll;

  JHTML()
  {
    this("");
  }

  JHTML(String s)
  {
    super(new BorderLayout());

    html = new JTextPane();
    html.setEditable(false); // need to set this explicitly to fix swing 1.3 bug
    html.setCaretPosition(0);
    html.setContentType("text/html");

    // Enable posting of form submit events to the hyper link listener
    final HTMLEditorKit htmlEditor
    = (HTMLEditorKit)html.getEditorKitForContentType("text/html");
    try {
      // Call htmlEditor.setAutoFormSubmission(false); if available (Java 5+)
      final Method setAutoFormSubmissionMethod = htmlEditor.getClass()
        .getMethod("setAutoFormSubmission", new Class[]{ Boolean.TYPE});
      setAutoFormSubmissionMethod.invoke(htmlEditor,
                                         new Object[]{Boolean.FALSE});
    } catch (final Throwable t) {
      Activator.log.warn("Failed to enable auto form submission for JHTMLBundle.", t);
    }

    html.setText(s);
    html.setCaretPosition(0);

    html.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent ev)
      {
        if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          final URL url = ev.getURL();
          try {
            if (Util.isBundleLink(url)) {
              final long bid = Util.bidFromURL(url);
              Activator.disp.getBundleSelectionModel().clearSelection();
              Activator.disp.getBundleSelectionModel().setSelected(bid, true);
            } else if (Util.isImportLink(url)) {
              JCMInfo.importCfg(JHTML.this);
            } else {
              Util.openExternalURL(url);
            }
          } catch (final Exception e) {
            Activator.log.error("Failed to show " + url, e);
          }
        }
      }
    });

    scroll =
      new JScrollPane(html, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    html.setPreferredSize(new Dimension(300, 300));

    add(scroll, BorderLayout.CENTER);
  }

  void setHTML(String s)
  {
    try {
      html.setText(s);
    } catch (final Exception e) {
      Activator.log.error("Failed to set html", e);
    }
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run()
      {
        try {
          final JViewport vp = scroll.getViewport();
          if (vp != null) {
            vp.setViewPosition(new Point(0, 0));
            scroll.setViewport(vp);
          }
        } catch (final Exception e) {
          Activator.log.error("Failed to set html", e);
        }
      }
    });
  }
}
