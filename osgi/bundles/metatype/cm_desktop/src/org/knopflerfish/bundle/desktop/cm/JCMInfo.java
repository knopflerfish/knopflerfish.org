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
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

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

  void setProvider(MetaTypeInformation _mti, Bundle bundle)
  {
    this.mti = _mti;
    main.removeAll();
    if (mti != null) {
      renderBundleWithMetadata(bundle);
    } else {
      final JHTML jhtml = renderBundleWithoutMetadata(bundle);
      main.add(jhtml, BorderLayout.CENTER);
    }
    invalidate();
    revalidate();
    repaint();
  }

  private void renderBundleWithMetadata(final Bundle bundle)
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
   * Create a {@link JHTML}-component that presents a bundle without any metadata.
   * @param bundle The bundle to present. May be {@code null}.
   * @return component presenting a bundle without metadata.
   */
  private JHTML renderBundleWithoutMetadata(Bundle bundle)
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
      sb.append("No CM metatype found in bundle.<br>");
      sb.append("See <a href=\"http://www.knopflerfish.org/XMLMetatype/\">http://www.knopflerfish.org/XMLMetatype/</a> for details on how to add metatype and default values.<br>");
    }
    sb.append("</font>");
    sb.append("</p>");

    sb.append("<p>");
    Util.startFont(sb, "-2");
    sb.append("Select the ");
    Util.bundleLink(sb, Activator.bc.getBundle(0));
    sb.append(" to see all configurations that can be created or edited based on associated metatype information.");
    sb.append("</font></p>");

    sb.append("<p>");
    Util.startFont(sb, "-2");
    sb.append("Select the ");
    final Bundle cmBundle =
      CMDisplayer.cmTracker.getServiceReference().getBundle();
    Util.bundleLink(sb, cmBundle);
    sb.append(" bundle to see and edit all exisiting configurations.");
    sb.append("</font></p>");

    sb.append("</body>\n");
    sb.append("</html>\n");

    return new JHTML(sb.toString());
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
