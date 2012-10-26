/*
 * Copyright (c) 2003-2012, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import org.osgi.framework.Bundle;

/**
 * Utility swing component which display bundle info as HTML.
 *
 * <p> Intended to be used as base class.  Subclasses creates
 * presentation data for an individual bundle by overriding the method
 * {@link #bundleInfo(Bundle)}.  Subclasses that does not want the per
 * bundle presentation when multuple bundles are selected should
 * override {@link #updateView(Bundle[])}.</p>
 *
 * <p>
 * If the <tt>Util.bundleLink</tt> method is used to create
 * text for bundles, these will become selection links for
 * the bundle.
 * </p>
 */
public abstract class JHTMLBundle extends JPanel
                                  implements HierarchyListener
{
  private static final long serialVersionUID = 1L;

  JPanel      panel;
  JTextPane   html;
  JScrollPane scroll;

  DefaultSwingBundleDisplayer displayer;

  ArrayList historyBack    = new ArrayList();
  URL       historyCurrent = null;
  ArrayList historyFwd     = new ArrayList();

  JButton backButton = null;
  JButton fwdButton = null;

  private long currentBid = -1;

  private static final List /* JHTMLBundleLinkHandler */ linkHandlers
    = new ArrayList();

  JHTMLBundle(DefaultSwingBundleDisplayer _displayer) {

    setLayout(new BorderLayout());

    this.displayer = _displayer;
    if (displayer instanceof JHTMLBundleLinkHandler) {
      linkHandlers.add(displayer);
    }

    html = new JTextPane();
    html.setText(Strings.get("bundleinfo_startup"));
    html.setContentType("text/html");
    final HTMLEditorKit htmlEditor
      = (HTMLEditorKit)html.getEditorKitForContentType("text/html");
    try {
      // Call htmlEditor.setAutoFormSubmission(false); if available (Java 5+)
      final Method setAutoFormSubmissionMethod = htmlEditor.getClass()
        .getMethod("setAutoFormSubmission", new Class[]{ Boolean.TYPE});
      setAutoFormSubmissionMethod.invoke(htmlEditor,
                                         new Object[]{Boolean.FALSE});
    } catch (Throwable t) {
      Activator.log.warn("Failed to enable auto form submission for JHTMLBundle.", t);
    }

    html.setEditable(false);

    html.addHyperlinkListener(new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          final URL url = e.getURL();
          if (openURL(url)) {
            // Internal URL; add to history.
            if (null==historyCurrent) {
              historyBack.add(Util.bundleURL(getCurrentBID()));
            } else {
              historyBack.add(historyCurrent);
            }
            historyCurrent = url;
            if (!historyFwd.isEmpty()) {
              historyFwd.clear();
            }
            alignHistoryButtonEnableState();
          }
        }
      }
    });

    scroll = new JScrollPane(html, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    html.setPreferredSize(new Dimension(300, 300));

    JToolBar cmds = new JToolBar() {
      private static final long serialVersionUID = 1L;
      {
        add(backButton = new JButton(Desktop.prevIcon) {
          private static final long serialVersionUID = 1L;
          {
            addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                if (!historyBack.isEmpty()) {
                  final URL url
                    = (URL) historyBack.remove(historyBack.size() - 1);
                  historyFwd.add(historyCurrent);
                  historyCurrent = url;
                  openURL(url);
                }
                alignHistoryButtonEnableState();
              }
            });
            setToolTipText(Strings.get("tt_html_back"));
          }
        });

        add(fwdButton = new JButton(Desktop.nextIcon) {
          private static final long serialVersionUID = 1L;
          {
            addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                if (historyFwd.size() > 0) {
                  final URL url
                    = (URL) historyFwd.remove(historyFwd.size() - 1);
                  historyBack.add(historyCurrent);
                  historyCurrent = url;
                  openURL(url);
                }
                alignHistoryButtonEnableState();
              }
            });
            setToolTipText(Strings.get("tt_html_forward"));
          }
        });
        alignHistoryButtonEnableState();
      }
    };

    cmds.setFloatable(false);

    add(scroll, BorderLayout.CENTER);
    add(cmds, BorderLayout.SOUTH);

    valueChanged(null);
  }

  private void alignHistoryButtonEnableState() {
    backButton.setEnabled(historyBack.size()>0);
    fwdButton.setEnabled(historyFwd.size()>0);
  }

  public void addNotify() {
    super.addNotify();
    addHierarchyListener(this);
  }

  public void removeNotify() {
    removeHierarchyListener(this);
    super.removeNotify();
  }

  public void hierarchyChanged(HierarchyEvent e) {
    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
      // This pane has changed show-state.
      if (isShowing()) {
        // on screen: Clear history.
        historyBack.clear();
        historyCurrent = null;
        historyFwd.clear();
        alignHistoryButtonEnableState();
        if (SwingUtilities.isEventDispatchThread()) {
          new ValueUpdater().run();
        } else {
          // Views are not updated when not showing; schedule a call to
          // updateView()
          currentUpdater = new ValueUpdater();
          SwingUtilities.invokeLater(currentUpdater);
        }
      }
    }
  }

  boolean openURL(final URL url) {
    boolean isInternalUrl = true;
    if (Util.isBundleLink(url)) {
      final long bid = Util.bidFromURL(url);

      gotoBid(bid);

    } else {
      boolean handled = false;
      for (Iterator it = linkHandlers.iterator(); it.hasNext() && !handled;) {
        final JHTMLBundleLinkHandler handler
          = (JHTMLBundleLinkHandler) it.next();
        if (handler.canRenderUrl(url)) {
          final StringBuffer sb = new StringBuffer(400);
          handler.renderUrl(url, sb);
          setHTML(sb.toString());
          handled = true;
        }
      }
      if (!handled) {
        try {
          Util.openExternalURL(url);
        } catch (Exception e2) {
          Activator.log.error("Failed to open url " + url, e2);
        }
        isInternalUrl = false;
      }
    }
    return isInternalUrl;
  }

  void gotoBid(long bid) {
    displayer.getBundleSelectionModel().clearSelection();
    displayer.getBundleSelectionModel().setSelected(bid, true);
  }


  /**
   * Override this to provide bundle info in HTML format.
   */
  public abstract StringBuffer  bundleInfo(Bundle b);

  /**
   * Get header text for no selected bundle page.
   */
  public String getNoBundleSelectedHeader() {
    return "No bundle selected";
  }

  /**
   * Get main text for no selected bundle page.
   */
  public String getNoBundleSelectedText() {
    return
      "Select one or more bundles in the main view to " +
      "view detail information";
  }

  /**
   * Get header text for selected bundle page.
   */
  public String getBundleSelectedHeader(Bundle b) {
    return
      "#" + b.getBundleId() + "  " +  Util.getBundleName(b);
  }


  // Lock that must be taken before accessing the fields
  // currentSelection and currentUpdater.
  private Object valueChangedLock = new Object();
  // The most resent selection, to be used by updateView().
  private Bundle[] currentSelection = null;
  // If non-null an updateView() is scheduled on the EDT.
  private Runnable currentUpdater = null;

  /**
   * Gets notified about selection change and service registration /
   * un-registration etc. Will schedule an update of the contents of
   * this view. This method may sometimes be called very often during
   * a short time period, e.g., during an update of a bundle that
   * registers many service, thus we schedule the actual view update
   * (it may be costly, e.g., when the bundle registers many services)
   * as job on the EDT that will use the most resent bundle list when
   * it executes. I.e., all intermediate bundle lists (and thus
   * updates) will be skipped.
   *
   * Subclasses creates presentation data for an individual
   * bundle by overriding the method {@link #bundleInfo(Bundle)}.
   * Subclasses that does not want the per bundle presentation should
   * override {@link #updateView(Bundle[])}.
   */
  public final void valueChanged(Bundle[] bl) {
    synchronized(valueChangedLock) {
      currentSelection = bl;
      if (null==currentUpdater && isShowing()) {
        // Must schedule a new call to updateView()
        currentUpdater = new ValueUpdater();
        SwingUtilities.invokeLater(currentUpdater);
      }
    }
  }

  /**
   * A runnable-class that will call updateView() form the EDT.
   */
  class ValueUpdater implements Runnable
  {
    public void run()
    {
      Bundle[] bl = null;
      synchronized(JHTMLBundle.this.valueChangedLock) {
        bl = currentSelection;
        currentUpdater = null;
      }
      JHTMLBundle.this.updateView(bl);
    }
  }

  /**
   * Update this view to show info about the bundles in the given
   * bundle array. Sub-classes should normally implement the
   * {@link #bundleInfo(Bundle)} to provide the presentation for a
   * single bundle. This method may be overridden by a displayer that
   * presents something based on the entire selection like the
   * {@link ClosureHTMLDisplayer}.
   */
  public void updateView(Bundle[] bl)
  {
    if (!isShowing()) {
      // Don't update non-visible components.
      return;
    }

    final StringBuffer sb = new StringBuffer(400);
    sb.append("<html>\n");

    if(bl == null || bl.length == 0) {
      setCurrentBID(-1L);
      sb.append("<html>\n");
      sb.append("<table border=\"0\">\n");
      sb.append("<tr><td bgcolor=\"#eeeeee\">");
      startFont(sb, "-1");
      sb.append(getNoBundleSelectedHeader());
      sb.append("</font>\n");
      sb.append("</td>\n");
      sb.append("</tr>\n");
      sb.append("</table>\n");

      startFont(sb);
      sb.append(getNoBundleSelectedText());
      sb.append("</font>\n" +
                "</p>\n" +
                "</html>");
    } else {
      if(bl.length == 1) {
        if(bl[0].getBundleId() == getCurrentBID()) {
          //      System.out.println("skip already set bid=" + getCurrentBID());
          //      return;
        }
      }

      setCurrentBID(bl[0].getBundleId());

      for(int i = 0; i < bl.length; i++) {

        sb.append("<table border=\"0\" width=\"100%\">\n");
        sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
        startFont(sb, "-1");
        sb.append(getBundleSelectedHeader(bl[i]));
        stopFont(sb);
        sb.append("</td>\n");
        sb.append("</tr>\n");

        sb.append("<tr><td bgcolor=\"#ffffff\">");
//         long t0 = System.currentTimeMillis();
        StringBuffer bi = bundleInfo(bl[i]);
//         long t1 = System.currentTimeMillis();
//         if(t1 - t0 > 50) {
//           System.out.println("  " + (t1-t0) + "ms " + getClass().getName());
//         }
        sb.append(bi.toString());
        sb.append("</td>\n");
        sb.append("</tr>\n");
        sb.append("</table>\n");
      }
    }

    sb.append("\n</html>");
    setHTML(sb.toString());
  }

  protected void setCurrentBID(long bid) {
    final URL bundleUrl = Util.bundleURL(bid);
    if (historyCurrent == null) {
      // Initial selection when becoming visible
      historyCurrent = bundleUrl;
    } else if (!historyCurrent.equals(bundleUrl)) {
      // New selection, push current to history and clear history forward
      historyBack.add(historyCurrent);
      historyCurrent = bundleUrl;
      historyFwd.clear();
    }
    alignHistoryButtonEnableState();
    this.currentBid = bid;
  }

  public long getCurrentBID() {
    return currentBid;
  }

  void setHTML(String s) {
    html.setText(s);

    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try {
            JViewport vp = scroll.getViewport();
            if(vp != null) {
              vp.setViewPosition(new Point(0,0));
              scroll.setViewport(vp);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
  }


  static void appendRow(StringBuffer sb, String c1, String c2) {
    appendRow(sb, "-2", "", c1, c2);
  }

  static void appendRow(final StringBuffer sb,
                        final String size,
                        final String align,
                        final String label,
                        final String value) {
    sb.append("<tr><td ");
    sb.append(align);
    sb.append(" valign='top'>");

    JHTMLBundle.startFont(sb, size);
    sb.append("<b>");
    sb.append(label);
    sb.append("</b>");
    JHTMLBundle.stopFont(sb);

    sb.append("</td><td ");
    sb.append(align);
    sb.append(" valign='top'>");

    JHTMLBundle.startFont(sb, size);
    sb.append(value);
    JHTMLBundle.stopFont(sb);

    sb.append("</td></tr>\n");
  }

  static void startFont(final StringBuffer sb) {
    startFont(sb, "-2");
  }

  static void startFont(final StringBuffer sb, final String size) {
    sb.append("<font size=\"");
    sb.append(size);
    sb.append("\" face=\"Verdana, Arial, Helvetica, sans-serif\">");
  }

  static void stopFont(final StringBuffer sb) {
    sb.append("</font>");
  }

}
