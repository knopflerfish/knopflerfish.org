/*
 * Copyright (c) 2008-2022, KNOPFLERFISH project
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

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.tree.DefaultMutableTreeNode;

public class PrefsTreeNode extends DefaultMutableTreeNode {
  private static final long serialVersionUID = 1L;

  protected  boolean     bLoaded          = false;
  protected  boolean     bHasBeenExpanded = false;
  protected  Preferences prefs;
  protected  String      name;

  public PrefsTreeNode(Preferences prefs) {
    this(prefs, false, null);
  }

  public PrefsTreeNode(Preferences prefs, boolean bLoad, String name) {
    super(prefs);
    this.prefs = prefs;
    this.name = name;

    if(bLoad) {
      addChildren();
    } else {
      try {
        if(prefs != null) {
          String[] children = prefs.childrenNames();
          if(children.length > 0) {
            add(new DummyNode());
          }
        }
      } catch (BackingStoreException e) {
        // Who the f*** decided this should be a checked exception?
        throw new RuntimeException("Failed to load prefs", e);
      }
    }
  }

  public void rescan() {
    removeAllChildren();
    addChildren();
  }

  public void assertLoad() {
    if(!bLoaded) {
      addChildren();
    }
  }

  protected void addChildren() {
    try {
      if(prefs != null) {
        String[] children = prefs.childrenNames();
        for (String s : children) {
          Preferences child = prefs.node(s);
          add(new PrefsTreeNode(child));
        }
      }
      bLoaded = true;
    } catch (BackingStoreException e) {
      // Who the f*** decided this should be a checked exception?
      throw new RuntimeException("Failed to load prefs", e);
    }
  }

  public String toString() {
    return name != null ? name : (prefs != null ? prefs.name() : "empty");
  }

  public int hashCode() {
    return prefs != null ? prefs.hashCode() : 0;
  }

  public Preferences getPrefs() {
    return prefs;
  }

  public boolean equals(Object obj) {

    if(obj == null) {
      return false;
    }
    // System.out.println(this.getClass().getName() + ".equals(" + obj.getClass().getName() + ")");
    if(!(obj instanceof PrefsTreeNode)) {
      return false;
    }
    PrefsTreeNode p = (PrefsTreeNode)obj;
    if(prefs == null) {
      return p.prefs == null;
    }
    return prefs.equals(p.prefs);
  }

  public void setHasBeenExpanded(boolean b) {
    this.bHasBeenExpanded = b;
  }
  
  static class DummyNode extends DefaultMutableTreeNode {
    private static final long serialVersionUID = 1L;

    public DummyNode() {
      super("dummy");
    }
  }
}
