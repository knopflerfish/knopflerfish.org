/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Utiliy swing component which display bundle info as
 * HTML.
 *
 *<p>
 * Intended to be used as base class. Subclasses should override
 * the <tt>valueChanged</tt> method and retur an HTML string.
 *</p>
 *
 * <p>
 * If the <tt>Util.bundleLink</tt> method is used to create
 * text for bundles, these will become selection links for
 * the bundle.
 * </p>
 */
public abstract class JHTMLBundle extends JPanel  {
  JPanel      panel;
  JTextPane   html;
  JScrollPane scroll;

  DefaultSwingBundleDisplayer displayer;

  ArrayList historyBack    = new ArrayList();
  ArrayList historyFwd     = new ArrayList();

  JButton backButton = null;
  JButton fwdButton = null;

  private long currentBid = -1;

  JHTMLBundle(DefaultSwingBundleDisplayer _displayer) {

    setLayout(new BorderLayout());

    this.displayer = _displayer;

    html = new JTextPane();
    html.setText(Strings.get("bundleinfo_startup"));
    html.setContentType("text/html");

    html.setEditable(false);

      html.addHyperlinkListener(new HyperlinkListener()
        {
          public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
              URL url = e.getURL();

              if(Util.isBundleLink(url)) {
                long bid = Util.bidFromURL(url);

                if(getCurrentBID() != -1) {
                  historyBack.add(new Long(getCurrentBID()));

                  backButton.setEnabled(!historyBack.isEmpty());
                }

                displayer.getBundleSelectionModel().clearSelection();
                displayer.getBundleSelectionModel().setSelected(bid, true);


              } else if(Util.isServiceLink(url)) {
                long sid = Util.sidFromURL(url);

                if(getCurrentBID() != -1) {
                  historyBack.add(new Long(getCurrentBID()));

                  backButton.setEnabled(!historyBack.isEmpty());
                }

                setServiceHTML(sid);

              } else if(Util.isResourceLink(url)) {
                String path = Util.resourcePathFromURL(url);



                if(getCurrentBID() != -1) {
                  historyBack.add(new Long(getCurrentBID()));

                  backButton.setEnabled(!historyBack.isEmpty());

                  setResourceHTML(Activator.getBC().getBundle(getCurrentBID()), path);
                }

              } else {
                try {
                  Util.openExternalURL(url);
                } catch (Exception e2) {
                  Activator.log.error("Failed to open url " + url, e2);
                }
              }
            }
          }
        }
                                );

      scroll =
        new JScrollPane(html,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      html.setPreferredSize(new Dimension(300, 300));

      JToolBar cmds = new JToolBar() {
          {
            add(backButton = new JButton(Activator.desktop.prevIcon) {
                {
                  addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      if(!historyBack.isEmpty()) {
                        Long bid = (Long)historyBack.get(historyBack.size()-1);
                        historyBack.remove(historyBack.size()-1);

                        if(getCurrentBID() != -1) {
                          historyFwd.add(new Long(getCurrentBID()));
                        }

                        gotoBid(bid.longValue());
                      }
                      backButton.setEnabled(historyBack.size() > 0);
                      fwdButton.setEnabled(historyFwd.size() > 0);
                    }
                  });
                  setToolTipText(Strings.get("tt_html_back"));
                }
              });

            add(fwdButton = new JButton(Activator.desktop.nextIcon) {
                {
                  addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      if(historyFwd.size() > 0) {
                        Long bid = (Long)historyFwd.get(historyFwd.size()-1);
                        historyFwd.remove(historyFwd.size()-1);

                        if(getCurrentBID() != -1) {
                          historyBack.add(new Long(getCurrentBID()));
                        }

                        gotoBid(bid.longValue());
                      }
                      backButton.setEnabled(historyBack.size() > 0);
                      fwdButton.setEnabled(historyFwd.size() > 0);
                    }
                  });
                  setToolTipText(Strings.get("tt_html_back"));
                }
              });
            backButton.setEnabled(historyBack.size() > 0);
            fwdButton.setEnabled(historyFwd.size() > 0);
          }
        };

      cmds.setFloatable(false);

      add(scroll, BorderLayout.CENTER);
      add(cmds, BorderLayout.SOUTH);

      valueChanged(null);
  }

  void gotoBid(long bid) {
    displayer.getBundleSelectionModel().clearSelection();
    displayer.getBundleSelectionModel().setSelected(bid, true);
  }

  void setResourceHTML(Bundle bundle, String path) {
    StringBuffer sb = new StringBuffer();

    sb.append("<html>");

    URL url = bundle.getResource(path);

    sb.append("<table border=0>");

    sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
    startFont(sb, "-1");
    sb.append("#" + bundle.getBundleId() + " " + path);
    sb.append("</font>\n");
    sb.append("</td>\n");
    sb.append("</tr>\n");

    sb.append("<tr>");
    sb.append("<td>");
    sb.append("<pre>");
    sb.append("<font size=\"-1\">");
    try {
      byte[] bytes = Util.readStream(url.openStream());
      String value = new String(bytes);
      value = Strings.replace(value, "<", "&lt;");
      value = Strings.replace(value, ">", "&gt;");

      sb.append(value);
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      sb.append(sw.toString());
    }
    sb.append("</font>\n");
    sb.append("</pre>");
    sb.append("</td>");
    sb.append("</tr>");

    sb.append("</table>");
    sb.append("</html>");

    setHTML(sb.toString());
  }

  void setServiceHTML(long sid) {
    StringBuffer sb = new StringBuffer();

    try {
      ServiceReference[] srl =
        Activator.getTargetBC().getServiceReferences(null,
                                          "(" + Constants.SERVICE_ID + "=" + sid + ")");
      if(srl != null && srl.length == 1) {
        sb.append("<html>");
        sb.append("<table border=0>");

        sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
        startFont(sb, "-1");
        sb.append("Service #" + sid);
        sb.append(", ");
        Util.bundleLink(sb, srl[0].getBundle());
        sb.append("</font>\n");
        sb.append("</td>\n");
        sb.append("</tr>\n");
        sb.append("</table>");


        startFont(sb);
        sb.append("<b>Properties</b>");
        sb.append("</font>");
        sb.append("<table cellpadding=\"0\" cellspacing=\"1\" border=\"0\">");
        String[] keys = srl[0].getPropertyKeys();
        for(int i = 0; keys != null && i < keys.length; i++) {

          StringWriter sw = new StringWriter();
          PrintWriter  pr = new PrintWriter(sw);

          Util.printObject(pr, srl[0].getProperty(keys[i]));

          sb.append("<tr>");
          sb.append("<td valign=\"top\">");
          startFont(sb);
          sb.append(keys[i]);
          stopFont(sb);
          sb.append("</td>");

          sb.append("<td valign=\"top\">");
          sb.append(sw.toString());
          sb.append("</td>");

          sb.append("</tr>");
        }
        sb.append("</table>");

        try {
    sb.append(formatServiceObject(srl[0]).toString());
        } catch (Exception e) {
          sb.append("Failed to format service object: " + e);
        }


        sb.append("</html>");

      } else {
        sb.append("No service with sid=" + sid);
      }
    } catch (Exception e2) {
      e2.printStackTrace();
    }

    setHTML(sb.toString());
  }

  StringBuffer formatServiceObject(ServiceReference sr) {
    String[] names = (String[]) sr.getProperty(Constants.OBJECTCLASS);
    StringBuffer sb = new StringBuffer();
    startFont(sb);
    sb.append("<b>Implemented interfaces</b>");
    sb.append("<br>");
    for(int i = 0; i < names.length; i++) {
      sb.append(names[i]);
      if(i < names.length -1) {
        sb.append(", ");
      }
    }
    sb.append("</font>");
    sb.append("<br>");

    startFont(sb);
    sb.append("<b>Methods</b>");

    sb.append("<table>");
    for (int i=0; i<names.length; i++) {
      try {
        Class clazz = sr.getBundle().loadClass(names[i]);
        sb.append(formatClass(clazz).toString());
      } catch (ClassNotFoundException e) {
        sb.append("<tr><td colspan=\"3\" valign=\"top\" bgcolor=\"#eeeeee\">");
        startFont(sb);
        sb.append("Class not found: ").append(names[i]);
        sb.append("</font></td></tr>");
      }
    }
    sb.append("</table>");

    return sb;
  }

  StringBuffer formatClass(Class clazz) {
    Method[] methods = clazz.getDeclaredMethods();
    StringBuffer sb = new StringBuffer();

    sb.append("<tr>");
    sb.append("<td colspan=\"4\" valign=\"top\" bgcolor=\"#eeeeee\">");
    startFont(sb);
    sb.append(clazz.getName());
    sb.append("</font></td></tr>");

    for(int i = 0; i < methods.length; i++) {
      if(!Modifier.isPublic(methods[i].getModifiers())) {
        continue;
      }
      Class[] params = methods[i].getParameterTypes();
      sb.append("<tr>");

      sb.append("<td valign=\"top\" colspan=\"3\">");
      startFont(sb);
      sb.append(className(methods[i].getReturnType().getName()));

      sb.append("&nbsp;");
      sb.append(methods[i].getName());

      sb.append("(");
      for(int j = 0; j < params.length; j++) {
        sb.append(className(params[j].getName()));
        if(j < params.length - 1) {
          sb.append(",&nbsp;");
        }
      }
      sb.append(");&nbsp;");
      sb.append("</font>");
      sb.append("</td>");

      sb.append("</tr>");
    }
    return sb;
  }

  String className(String name) {
    if(name.startsWith("[L") && name.endsWith(";")) {
      name = name.substring(2, name.length() - 1) + "[]";
    }

    if(name.startsWith("java.lang.")) {
      name = name.substring(10);
    }

    return name;
  }

  /**
   * Override this to provide special bundle info in HTML
   * format.
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
   */
  public void valueChanged(Bundle[] bl) {
    // final StringBuffer sbb = new StringBuffer(200);
    // sbb.append("valueChanged(");
    // if (null==bl) {
    //   sbb.append("null");
    // } else {
    //   sbb.append("[");
    //   for(int i = 0; i < bl.length; i++) {
    //     sbb.append(bl[i].getBundleId());
    //   }
    //   sbb.append("]");
    // }
    // sbb.append(") ");
    // sbb.append(this.getClass().getName());
    // System.out.println(sbb.toString());

    synchronized(valueChangedLock) {
      currentSelection = bl;
      if (null==currentUpdater) {
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
   * bundle array.
   */
  private void updateView(Bundle[] bl)
  {
    //System.out.println("updateView() " +this.getClass().getName());

    final StringBuffer sb = new StringBuffer(400);
    sb.append("<html>\n");

    if(bl == null || bl.length == 0) {
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
        sb.append("</font>\n");
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


  void appendRow(StringBuffer sb, String c1, String c2) {
    sb.append("<tr>" +
              " <td valign=\"top\"><b>");
    startFont(sb);
    sb.append(c1);
    sb.append("</font>");
    sb.append("</b></td>\n");
    sb.append(" <td valign=\"top\">");
    startFont(sb);
    sb.append(c2);
    sb.append("</td>\n" +
              "</tr>\n");
  }

  void startFont(StringBuffer sb) {
    startFont(sb, "-2");
  }

  void stopFont(StringBuffer sb) {
    sb.append("</font>");
  }

  void startFont(StringBuffer sb, String size) {
    sb.append("<font size=\"" + size + "\" face=\"Verdana, Arial, Helvetica, sans-serif\">");
  }


}
