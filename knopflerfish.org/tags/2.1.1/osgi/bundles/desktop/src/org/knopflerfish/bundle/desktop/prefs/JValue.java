/*
 * Copyright (c) 2008, KNOPFLERFISH project
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


package org.knopflerfish.bundle.desktop.prefs;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.awt.Toolkit;

import org.knopflerfish.bundle.desktop.swing.Activator;

/**
 * JComponent subclass capable of editing a preference key/value
 */
public class JValue 
  extends JPanel {

  protected String      type;
  protected String      key;
  protected JLabel      label;
  protected Preferences node;
  protected JPopupMenu  popup;
  protected int         width       = 150;
  protected boolean     bEditable   = true;
  protected boolean     bNeedUpdate = false;
  protected JLabel      errComp;

  /**
   * Used for prefixing type keys.
   *
   * <p>
   * Value is "__type_"
   * </p>
   */
  public static final String TYPE_PREFIX = "__type_";
  public static final String DESC_PREFIX = "__desc_";

  static Color defaultedCol = new Color(120, 120, 120);

  JValue(Preferences node, String key, String _type) {
    super(new BorderLayout());
    this.key  = key;
    this.node = node;
    this.type = _type;

    label = new JLabel(key);
    label.setPreferredSize(new Dimension(width, 
                                         label.getPreferredSize().height));

    String desc = null;

    StringBuffer extProps = new StringBuffer();

    // add extra info if ExtPreferences
    if(node instanceof ExtPreferences) {
      ExtPreferences ep = (ExtPreferences)node;
      desc = ep.getProperty(key, ExtPreferences.PROP_DESC, null);
      String[] extNames = ep.getExtPropNames(key);

      if("true".equals(ep.getProperty(key, "defaulted", null))) {
        label.setForeground(defaultedCol);
      }

      // add all ext properties except for type and description
      for(int i = 0; extNames != null && i < extNames.length; i++) {
        if(ExtPreferences.PROP_DESC.equals(extNames[i]) ||
           ExtPreferences.PROP_TYPE.equals(extNames[i])) {
        } else {
          if(extProps.length() > 0) {
            extProps.append("<br>\n");
          }
          extProps.append(extNames[i]);
          extProps.append(": ");
          extProps.append(ep.getProperty(key, extNames[i], ""));
        }
      }
    }
    
    String shortType = type;
    int ix = shortType.lastIndexOf(".");
    if(ix != -1) {
      shortType = shortType.substring(ix + 1);
    }


    StringBuffer tt = new StringBuffer();
    tt.append("<html>");
    tt.append("<b>");
    tt.append(key);
    tt.append("</b>");
    tt.append(" (");
    tt.append(shortType);
    tt.append(")");
    if(desc != null) {
      tt.append("<br>");
      tt.append(" ");
      tt.append(desc);
    }
    if(extProps.length() > 0 ) {
      tt.append("<br>\n");
      tt.append(extProps.toString());
    }
    tt.append("</html>");
    label.setToolTipText(tt.toString());
    
    popup = new JPopupMenu();
    
    JMenuItem mi;

    if(ExtPreferences.TYPE_COLOR.equals(type)) {
      mi = new JMenuItem("Select color") {{
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              if(JValue.this instanceof JValueColor) {
                JValueColor jcCol = (JValueColor)JValue.this;
                jcCol.selectColor();
              }
            }
          });
      }};
      popup.add(mi);
    }

    mi = new JMenuItem("Copy path to clipboard") {{
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            doCopyPath();
          }
        });
    }};
    popup.add(mi);

    mi = new JMenuItem("Remove") {{
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            doRemove();
          }
        });
    }};
    popup.add(mi);


    mi = new JMenuItem("Show props") {{
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            showExtProps();
          }
        });
    }};
    popup.add(mi);

    popup.add(new JPopupMenu.Separator());

    String[] types = JValueFactory.getSupportedTypes();

    for(int i = 0; i < types.length; i++) {
      final String t = types[i];
      final String n = types[i];
      mi = new JCheckBoxMenuItem(n) {{
      setSelected(t.equals(type));
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            doSetType(t);
          }
        });
      }};
      popup.add(mi);
    }

    label.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if(bEditable) {
            if(e.getButton() == MouseEvent.BUTTON1) {
              return;
            }
            popup.show(e.getComponent(), e.getX(), e.getY());    
          }
        }
      });
    errComp = new JLabel();

    add(label, BorderLayout.WEST);
  }

  boolean bErr = false;
  

  /**
   * Get the node being edited.
   */
  public Preferences getPreferences() {
    return node;
  }

  /**
   * Get the key being edited.
   */
  public String getKey() {
    return key;
  }


  /**
   * Set an error string on the component.
   *
   * @param msg error string or <tt>null</tt> if
   *            no error should be displayed.
   */
  protected void setErr(String msg) {
    if(msg != null) {
      if(!bErr) {
        bErr = true;
        add(errComp, BorderLayout.SOUTH);
        revalidate();
        repaint();
      }
      errComp.setText(msg);
    } else {
      if(bErr) {
        remove(errComp);
        revalidate();
        repaint();
        bErr = false;
      }
    }
  }

  public boolean getNeedUpdate() {
    return bNeedUpdate;
  }

  public void setNeedUpdate(boolean b) {
    bNeedUpdate = b;
  }

  protected void doCopyPath() {
    String path = node.absolutePath();

    StringSelection sel  = new StringSelection(path);
    Clipboard       clip = Toolkit.getDefaultToolkit().getSystemClipboard();
    
    clip.setContents(sel, sel);
  }

  protected void doRemove() {
    try {
      node.remove(key);
      node.flush();
    } catch (Exception e) {
      throw new RuntimeException("Failed to remove " + node, e);
    }
  }

  protected void doSetType(String type) {
    try {
      if(!this.type.equals(type)) {
        bNeedUpdate = true;
        node.put(TYPE_PREFIX + key, type);
        node.flush();
      } 
    } catch (Exception e) {
      Activator.log.warn("Failed to set type on " + node.absolutePath() + " to " + type, e);
    }
  }

  public void update() {
  }

  public void cleanup() {
  }

  public boolean isReadonly() {
    if(node instanceof ExtPreferences) {
      ExtPreferences ep = (ExtPreferences)node;
      if("true".equals(ep.getProperty(key, "frozen", "false"))) {
        return true;
      }
    }
    return false;
  }

  public void setEditable(boolean b) {
    bEditable = b;
  }

  void showExtProps() {
    JExtPropsPanel comp = new JExtPropsPanel(node, key);

    JScrollPane scroll = new JScrollPane(comp);
    scroll.setPreferredSize(new Dimension(300, 
                                          20 + Math.min(comp.getPreferredSize().height, 300)));
    
    JOptionPane.showMessageDialog(null, 
                                  scroll,
                                  node.absolutePath() + "/" + key,
                                  JOptionPane.INFORMATION_MESSAGE);
  }
  
}
/*
 */
class JExtPropsPanel extends JPanel {
  public JExtPropsPanel(Preferences node, String key) {
    super();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    setTarget(node, key);
  }

  public void setTarget(Preferences node, String key) {
    removeAll();
    
    if(node instanceof ExtPreferences) {
      ExtPreferences ep = (ExtPreferences)node;
      String[] extNames = ep.getExtPropNames(key);

      for(int i = 0; extNames != null && i < extNames.length; i++) {
        String val = ep.getProperty(key, extNames[i], "");

        JLabel     keyL = new JLabel(extNames[i]);
        JTextField valL = new JTextField(val);
        valL.setEditable(false);
        valL.setBorder(null);

        keyL.setPreferredSize(new Dimension(100, keyL.getPreferredSize().height));
        keyL.setToolTipText(extNames[i]);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
        row.add(keyL);
        row.add(valL);
        
        add(row);
      }

      Dimension minSize = new Dimension(1, 1);
      Dimension prefSize = new Dimension(1, 1);
      Dimension maxSize = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
      
      JComponent filler = new Box.Filler(minSize, prefSize, maxSize);
      add(filler);
      revalidate();
      invalidate();
      repaint();
    }
  }
}
