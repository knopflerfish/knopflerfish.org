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

import java.util.*;
import java.net.URL;

import java.io.*;
import org.knopflerfish.bundle.desktop.swing.Activator;

/**
 * Panel using the <tt>JPrefsTree</tt> and <tt>JPrefsPanel</tt>
 * to display/edit a Preferences node and its subnodes.
 *
 * <p>
 * <div>
 * <img src="prefsedit.png">
 * <br><i>Example of JPrefsEditor</i>
 * </div>
 * </p>
 * @see JPrefsTree
 * @see JPrefsPanel
 */
public class JPrefsEditor extends JPanel {
  JPrefsTree  tree;
  JPrefsPanel panel;  
  JSplitPane  splitPane;

  public JPrefsEditor() {
    super(new BorderLayout());

    tree  = new JPrefsTree();
    panel = new JPrefsPanel();

    panel.setEditable(true);
    panel.setPreferredSize(new Dimension(400, 300));

    tree.addTreeSelectionListener(new TreeSelectionListener() {
        public void 	valueChanged(TreeSelectionEvent ev) {
          TreePath path = ev.getPath();
          Object   node = path.getLastPathComponent();
          
          if(node instanceof PrefsTreeNode) {
            try {
              PrefsTreeNode pNode = (PrefsTreeNode)node;
              if(pNode.getPrefs().nodeExists("")) {
                Preferences prefs = pNode.getPrefs();
                String[] keys = prefs.keys();
                panel.setPreferences(prefs);
              }
            } catch (BackingStoreException e) {
              Activator.log.warn("Failed to get keys", e);
            } catch (IllegalStateException e) {
              Activator.log.warn("hohum", e);
            }
          }
        }
      });
    
    JScrollPane treeScroll  = new JScrollPane(tree);
    treeScroll.setPreferredSize(new Dimension(200, 400));
    
    JSplitPane  splitPane   = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT ,
                                             treeScroll,
                                             panel);
    splitPane.setDividerLocation(200);

    add(splitPane, BorderLayout.CENTER);
  }

  public void setPreferences(Preferences node) {
    tree.setPreferences(node);
    panel.setPreferences(node);
  }

  public void setEditable(boolean b) {
    tree.setEditable(b);
    panel.setEditable(b);
  }

  public JPrefsTree getJPrefsTree() {
    return tree;
  }

  public JPrefsPanel getJPrefsPanel() {
    return panel;
  }

  /**
   * For standalone tesing.
   *
   * <p>
   * Creates a JFrame and adds a JPrefsEditor.
   * </p>
   */
  public static void main(String[] argv) {

    try {
      JFrame frame = new JFrame();

      frame.addWindowListener(new WindowAdapter() {
        public void  windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });


      Preferences root = null;
      
      root = Preferences.systemRoot();

      if("system".equals(argv[0])) {
        root = Preferences.systemRoot();
      } else if("user".equals(argv[0])) {
        root = Preferences.userRoot();
      } else if("load".equals(argv[0])) {
        InputStream in = new FileInputStream(argv[1]);
        Preferences.importPreferences(in);
        System.exit(0);
      }
      
      if(argv.length > 1) {
        root = root.node(argv[1]);
      }

      frame.setTitle(root.absolutePath());
      
      JPrefsEditor editor = new JPrefsEditor();
      editor.setEditable("true".equals(System.getProperty("editable", "true")));
      editor.setPreferences(root);

      frame.getContentPane().add(editor);
      
      frame.pack();
      frame.setVisible(true);
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
