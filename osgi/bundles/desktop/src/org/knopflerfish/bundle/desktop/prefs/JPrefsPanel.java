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

import java.util.*;



import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;

import org.knopflerfish.bundle.desktop.swing.Activator;

/**
 * Panel capable of displaying/editing the keys in a preferences node.
 */
public class JPrefsPanel extends JPanel 
  implements PreferenceChangeListener {
  
  Preferences  node;
  Map          nodeMap = new HashMap();
  JPanel       valuePanel;
  boolean      bEditable = true;
  JButton      addButton;
  Component    filler;
  JLabel       header;

  /**
   * Create an empty preferences panel.
   *
   * <p>
   * As soon <tt>setPreferences</tt> is called, the panel is filled
   * with all keys and values from the node. As default,
   * the keys are also editable.
   * </p>
   */
  public JPrefsPanel() {
    super(new BorderLayout());


    valuePanel = new JPanel();
    valuePanel.setBorder(new EmptyBorder(3,3,3,3));
    valuePanel.setLayout(new BoxLayout(valuePanel, BoxLayout.Y_AXIS));
    
    JScrollPane scroll = new JScrollPane(valuePanel);
    
    add(scroll, BorderLayout.CENTER);
    
    JPanel cmdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
    
    addButton = new JButton("Add key") {{
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            doAddKey();
          }
        });
    }};
    addButton.setToolTipText("Add new key");
    // cmdPanel.add(addButton);


    header = new JLabel();

    add(cmdPanel, BorderLayout.SOUTH);
    add(header, BorderLayout.NORTH);
  }
  
  /**
   * Set if the panel is editable.
   * <p>
   * An editable panel allows modification, adding and removing of 
   * keys.
   * </p>
   */
  public void setEditable(boolean b) {
    bEditable = b;
    
    addButton.setEnabled(b);
    synchronized(nodeMap) {
      for(Iterator it = nodeMap.keySet().iterator(); it.hasNext(); ) {
        String   key  = (String)it.next();
        JValue   jv   = (JValue)nodeMap.get(key);
        jv.setEditable(bEditable);
      }
    }
  }
  
  protected void doAddKey() {
    try {
      String name = JOptionPane.showInputDialog(this, 
                                                "New key name",
                                                "New key...",
                                                JOptionPane.YES_NO_OPTION);
      if(name != null && !"".equals(name)) {  
        node.put(name, node.get(name, ""));
        node.flush();
      }
      setPreferences(node);
    } catch (Exception e) {
      Activator.log.warn("Failed to add key", e);
    }
  }
  
  
  /*
  public void childAdded(NodeChangeEvent ev) {
    log.info("JPrefsPanel.childAdded parent=" + ev.getParent() + 
             "child=" + ev.getChild());
    setPreferences(node);
  }
  
  public void 	childRemoved(NodeChangeEvent ev) {
    log.info("JPrefsPanel.childRemoved parent=" + ev.getParent() + 
             "child=" + ev.getChild());
    setPreferences(node);
  }
  */

  public void 	preferenceChange(PreferenceChangeEvent ev) {
  // log.info("JPrefsPanel.preferenceChange ev=" + ev);

    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
  
          boolean bNeedUpdate = false;
          
          synchronized(nodeMap) {
            HashSet removeSet = new HashSet();
            
            for(Iterator it = nodeMap.keySet().iterator(); it.hasNext(); ) {
              String   key  = (String)it.next();
              if(null == node.get(key, null)) {
                removeSet.add(key);
              } else {
                JValue   jv   = (JValue)nodeMap.get(key);
                jv.update();
              }
            }
            
            for(Iterator it = removeSet.iterator(); it.hasNext(); ) {
              String   key  = (String)it.next();
              JValue   jv   = (JValue)nodeMap.get(key);
              nodeMap.remove(key);
              valuePanel.remove(jv);
              bNeedUpdate = true;
            }
            
            try {
              String[] keys = getKeys(node);
              for(int i = 0; i < keys.length; i++) {    
                if(!nodeMap.containsKey(keys[i])) {
                  bNeedUpdate = true;
                } else {
                  JValue   jv   = (JValue)nodeMap.get(keys[i]);
                  bNeedUpdate |= jv.getNeedUpdate();
                  jv.setNeedUpdate(false);
                }
              }
            } catch (Exception e) {
              bNeedUpdate = true;
            }
          }
          
          // log.info("bNeedUpdate=" + bNeedUpdate);

          if(bNeedUpdate) {
            setPreferences(node);
          }
        }
      });
  }

  static private String[] getKeys(Preferences node) {
    ArrayList a = new ArrayList();
    try {
      String[] keys = node.keys();
      for(int i = 0; i < keys.length; i++) {
        if(!isHidden(keys[i])) {
          a.add(keys[i]);
        }
      }
    } catch (Exception e) {
      Activator.log.warn("failed to get keys from " + node, e);
    }
    String[] keys = new String[a.size()];
    a.toArray(keys);
    return keys;
  }
  
  static private boolean isHidden(String key) {
    return 
      key.startsWith(JValue.TYPE_PREFIX) ||
      key.startsWith(JValue.DESC_PREFIX);
    
  }

  /**
   * Set the node to be displayed.
   */
  public void setPreferences(Preferences node) {
    if(this.node != null) {
      try {
        if(this.node.nodeExists("")) {
          this.node.removePreferenceChangeListener(this);
          // this.node.parent().removeNodeChangeListener(this);
        }
      } catch (Exception ignored) {
        // this is really annoying. I just want to make sure the node
        // is valid.
      }
    }
    this.node  = node;

    header.setText(node.absolutePath());
    
    synchronized(nodeMap) {
      for(Iterator it = nodeMap.keySet().iterator(); it.hasNext(); ) {
        String   key  = (String)it.next();
        JValue   jv   = (JValue)nodeMap.get(key);
        jv.cleanup();
        valuePanel.remove(jv);
      }
      
      if(filler != null) {
        valuePanel.remove(filler);
        filler = null;
      }
      try {
        if(node.nodeExists("")) {
          node.addPreferenceChangeListener(this);
          // node.parent().addNodeChangeListener(this);
          
          String[] keys = getKeys(node);
          for(int i = 0; i < keys.length; i++) {
            String val = node.get(keys[i], "");
            JValue jv = JValueFactory.createJValue(node, keys[i]);
            jv.setEditable(bEditable);
            jv.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));

            nodeMap.put(keys[i], jv);
            valuePanel.add(jv);
          }
          Dimension minSize = new Dimension(1, 1);
          Dimension prefSize = new Dimension(1, 1);
          Dimension maxSize = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
          
          filler = new Box.Filler(minSize, prefSize, maxSize);
          valuePanel.add(filler);
          revalidate();
          invalidate();
          repaint();
        } else {
          valuePanel.add(new JLabel("Node is removed"));
        }
      } catch (BackingStoreException e) {
        throw new RuntimeException("Failed to load prefs", e);
      }
    }
  }
}
