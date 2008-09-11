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

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.util.prefs.*;


import javax.swing.*;
import java.awt.event.*;
import org.knopflerfish.bundle.desktop.swing.Activator;


/**
 * Preferences implemnation taht allows mounting of preference nodes
 * at arbitrary paths.
 */
public class MountedPreferences 
  extends AbstractPreferences
  implements ExtPreferences
{
  static final String[]              EMPTY_STRINGS = new String[0];
  Preferences target;

  // Set pclSet = new LinkedHashSet();
  Set nclSet = new LinkedHashSet();

  Map mounts = new LinkedHashMap();

  public MountedPreferences() {
    super(null, "");
  }

  public MountedPreferences(AbstractPreferences parent, 
                            Preferences target,
                            String targetName) {
    super(parent, targetName);
    this.target    = target;
  }

  public Preferences getTarget() {
    return target;
  }


  public void 	addNodeChangeListener(NodeChangeListener ncl) {
    synchronized(nclSet) {
      super.addNodeChangeListener(ncl);
      nclSet.add(ncl);
    }
  }

  public void 	removeNodeChangeListener(NodeChangeListener ncl) {
    synchronized(nclSet) {
      super.removeNodeChangeListener(ncl);
      nclSet.remove(ncl);
    }
  }


  /*
  public void 	addPreferenceChangeListener(PreferenceChangeListener pcl) {
    synchronized(pclSet) {
      super.addPreferenceChangeListener(pcl);
      pclSet.add(pcl);
    }
  }

  public void 	removePreferenceChangeListener(PreferenceChangeListener pcl) {
    synchronized(pclSet) {
      super.removePreferenceChangeListener(pcl);
      pclSet.remove(pcl);
    }
  }
  */

  protected void notifyNCL(NodeChangeEvent evt, boolean bAdded) {
    synchronized(nclSet) {
      
      for(Iterator it = nclSet.iterator(); it.hasNext(); ) {
        NodeChangeListener ncl = (NodeChangeListener)it.next();
        try {
          if(bAdded) {
            ncl.childAdded(evt);
          } else {
            ncl.childRemoved(evt);
          }
        } catch (Exception e) {
          Activator.log.warn("Failed to notify " + ncl);
        }
      }
    }
  }

  /**
   * Mount the specified preference node as child to this node.
   *
   * @param prefs Preferences node to mount
   * @param name child name to mount the specified node as.
   *             This will override any previous child with the same
   *             name.
   */
  public void mount(Preferences prefs, String name) {
    synchronized(mounts) {
      int ix;
      if(-1 != (ix = name.indexOf("/"))) {
        String n1 = name.substring(0, ix);
        String n2 = name.substring(ix+1);

        // this cast is OK since childSpi always return MountedPreferences
        ((MountedPreferences)node(n1)).mount(prefs, n2);
        NodeChangeEvent evt = new NodeChangeEvent(this, prefs);
        notifyNCL(evt, true);
      } else {
        mounts.put(name, prefs);
      }
    }
  }
  
  /**
   * Unmount a child node from this node.
   *
   * @param name name of node to unmount. If the node does not
   *             exists, does nothing.
   */
  public void unmount(String name) {
    synchronized(mounts) {
      Preferences prefs = (Preferences)mounts.get(name);
      mounts.remove(name);
      if(prefs != null) {
        NodeChangeEvent evt = new NodeChangeEvent(this, prefs);
        notifyNCL(evt, false);
      }
    }
  }

  /**
   * Check if a name is a mount.
   */
  public boolean isMount(String name) {
    synchronized(mounts) {
      return mounts.containsKey(name);
    }
  }


  // AbstractPreferences implementation
  protected String[] childrenNamesSpi() throws BackingStoreException {
    Collection set = new LinkedHashSet();

    if(target != null) {
      String[] names = target.childrenNames();
      for(int i = 0; i < names.length; i++) {
        set.add(names[i]);
      }
    }
    set.addAll(mounts.keySet());

    String[] names = new String[set.size()];
    set.toArray(names);
    return names;
  }

  // AbstractPreferences implementation
  protected AbstractPreferences  childSpi(String name) {
    synchronized(mounts) {
      Preferences mount = (Preferences)mounts.get(name);
      if(mount != null) {
        return new MountedPreferences(this, mount, name);
      }
      if(target != null) {
        return new MountedPreferences(this, target.node(name), name);
      } else {
        MountedPreferences m = new MountedPreferences(this, null, name);
        mount(m, name);
        return m;
      }
    }
  }
  
  // AbstractPreferences implementation
  protected void  flushSpi() throws BackingStoreException {
    if(target != null) {
      target.flush();
    }
  }


  // AbstractPreferences implementation
  protected String getSpi(String key) {
    if(target != null) {
      return target.get(key, null);
    }
    return null;
  }

  // AbstractPreferences implementation
  protected String[] keysSpi() throws BackingStoreException {
    if(target != null) {
      return target.keys();
    }
    return EMPTY_STRINGS;
  }

  // AbstractPreferences implementation
  protected void putSpi(String key, String value) {
    if(target != null) {
      target.put(key, value);
    }
  }

  // AbstractPreferences implementation
  protected void 	removeNodeSpi() throws BackingStoreException {
    if(target != null) {
      target.removeNode();
    }
  }

  // AbstractPreferences implementation
  protected void 	removeSpi(String key) {
    if(target != null) {
      target.remove(key);
    }
  }

  // AbstractPreferences implementation
  protected void 	syncSpi() throws BackingStoreException {
    if(target != null) {
      target.sync();
    }
  }

  public String[] getExtPropNames(String key) {
    if(target != null) {
      if(target instanceof ExtPreferences) {
        return ((ExtPreferences)target).getExtPropNames(key);
      }
    }
    return null;
  }

  public String getProperty(String key, String propName, String defValue) {
    if(target != null) {
      if(target instanceof ExtPreferences) {
        return ((ExtPreferences)target).getProperty(key, propName, defValue);
      }
    }
    return defValue;
  }

  public void setProperty(String key, String propName, String val) {
    if(target != null) {
      if(target instanceof ExtPreferences) {
        ((ExtPreferences)target).setProperty(key, propName, val);
      }
    }
  }


  public boolean equals(Object obj) {
    if(null == obj) {
      return false;
    }
    if(!(obj instanceof Preferences)) {
      return false;
    }
    
    Preferences prefs = (Preferences)obj;

    return absolutePath().equals(prefs.absolutePath());
  }

  public int hashCode() {
    return absolutePath().hashCode();
  }

  public static void main(String[] argv) {

    try {
      JFrame frame = new JFrame();

      frame.addWindowListener(new WindowAdapter() {
        public void  windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });

      
      Preferences sys  = Preferences.systemRoot();
      Preferences user = Preferences.userRoot();

      MountedPreferences root = new MountedPreferences();
      root.mount((AbstractPreferences)user, "user");
      root.mount((AbstractPreferences)sys, "sys");

      MountedPreferences test = (MountedPreferences)root.node("test/tjipp");

      test.mount(sys, "more");
      test.mount(sys, "evenmore");

      test.mount(test, "loop");

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
