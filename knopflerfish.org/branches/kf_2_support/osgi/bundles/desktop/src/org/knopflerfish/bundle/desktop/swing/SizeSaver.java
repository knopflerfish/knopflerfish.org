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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.util.prefs.Preferences;
import javax.swing.event.*;

public class SizeSaver extends ComponentAdapter {
  String id;

  public static final String KEY_X        = "x";
  public static final String KEY_Y        = "y";
  public static final String KEY_WIDTH    = "width";
  public static final String KEY_HEIGHT   = "height";
  public static final String KEY_STATE    = "state";
  public static final String KEY_SPLITPOS = "splitpos";

  public static final String NODE_NAME    = "sizes";

  Component comp;
  Dimension defSize;
  Dimension savedSize;
  int defSplit;
  int errCount = 0;
  int maxErr = 10;

  ComponentListener splitListener;

  public SizeSaver(String id, Dimension defSize, int defSplit) {
    if(id.length() == 0 || id.indexOf("/") != -1) {
      throw new
        IllegalArgumentException("Bad id string '" + id + "'" +
                                 ", must be non-zero lenght and no '/'");
    }
    this.id      = id;
    this.defSplit = defSplit;
    this.defSize = defSize != null ? new Dimension(defSize.width, defSize.height) : null;
    this.savedSize = defSize;
  }

  public void attach(Component _comp) {
    if(this.comp != null) {
      throw new IllegalStateException("SizeSaver can only be attach to one compoent. current component is " + this.comp);
    }
    this.comp = _comp;

    Dimension size = getSize();

    if(size != null) {
      // System.out.println("attach " + id + " size=" + size);
      comp.setSize(size);
      if(comp instanceof JComponent) {
        ((JComponent)comp).setPreferredSize(size);
      }
    }

    Preferences prefs = getPrefs();

    if(comp instanceof JFrame) {
      Toolkit tk = comp.getToolkit();
      if(tk.isFrameStateSupported(Frame.MAXIMIZED_VERT) ||
         tk.isFrameStateSupported(Frame.MAXIMIZED_HORIZ) ||
         tk.isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
        int state = prefs.getInt(KEY_STATE, Frame.NORMAL);
        ((Frame)comp).setExtendedState(state);
      }
      int x = prefs.getInt(KEY_X, 0);
      int y = prefs.getInt(KEY_Y, 0);
      // System.out.println("attach " + id + " pos=" + x + ", " + y);
      comp.setLocation(x, y);
    }

    if(comp instanceof JSplitPane) {
      JSplitPane split = (JSplitPane)comp;
      int pos = prefs.getInt(KEY_SPLITPOS, defSplit);
      if(pos != -1) {
        // System.out.println("attach " + id + " split=" + pos);
        split.setDividerLocation(pos);
        // Tell components that they may want to redo its layout
        Component parent = split.getParent();
        if (null!=parent) {
          parent.invalidate();
        } else {
          split.invalidate();
        }
      }

      splitListener = new ComponentAdapter() {
          public void 	componentResized(ComponentEvent e) {
            store();
          }
          public void 	componentMoved(ComponentEvent e) {
            store();
          }
        };

      split.getLeftComponent().addComponentListener(splitListener);
    }

    this.comp.addComponentListener(this);
  }

  public void detach() {

    if(comp != null) {
      if(comp instanceof JSplitPane) {
        JSplitPane split = (JSplitPane)comp;
        if(splitListener != null) {
          split.getLeftComponent().removeComponentListener(splitListener);
          splitListener = null;
        }
      }
      comp.removeComponentListener(this);
      comp = null;
    }
  }

  public void 	componentMoved(ComponentEvent e) {
    handleCompChange();
  }

  public void 	componentResized(ComponentEvent e) {
    handleCompChange();
  }

  void handleCompChange() {
    if(this.comp == null) {
      return;
    }
    if(!this.comp.isVisible()) {
      return;
    }

    store();
  }

  Preferences getPrefs() {
    Preferences prefsBase = Preferences.userNodeForPackage(getClass());

    Map    props = Activator.getSystemProperties();
    String spid  = (String)props.get("org.osgi.provisioning.spid");

    if(spid == null) {
      spid = "default";
    }

    Preferences prefs     = prefsBase.node(NODE_NAME + "/" + spid + "/" + id);
    try {
      prefs.sync(); // Get the latest version of the node.
    } catch (Exception e) {
      errCount++;
      if(errCount < maxErr) {
        Activator.log.warn("Failed to get id=" + id, e);
      }
    }
    return prefs;
  }

  public void store() {
    Dimension size = comp.getSize();
    try {
      savedSize = new Dimension(size.width, size.height);
      Preferences prefs = getPrefs();


      if(comp instanceof JFrame) {
        Point p =  ((JFrame)comp).getLocationOnScreen();
        // System.out.println(id + ": store pos " + p);
        prefs.put(KEY_X, Integer.toString((int)p.getX()));
        prefs.put(KEY_Y, Integer.toString((int)p.getY()));

        Toolkit tk = Toolkit.getDefaultToolkit();
        if(tk.isFrameStateSupported(Frame.MAXIMIZED_VERT) ||
           tk.isFrameStateSupported(Frame.MAXIMIZED_HORIZ) ||
           tk.isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
          int state = ((Frame)comp).getExtendedState();
          prefs.put(KEY_STATE, Integer.toString(state));
        }

      }

      // System.out.println(id + ": store " + size);
      prefs.put(KEY_WIDTH, Integer.toString(size.width));
      prefs.put(KEY_HEIGHT, Integer.toString(size.height));

      if(comp instanceof JSplitPane) {
        JSplitPane split = (JSplitPane)comp;
        prefs.put(KEY_SPLITPOS, Integer.toString(split.getDividerLocation()));
        // System.out.println(id + ": store split=" + split.getDividerLocation());
      }

      prefs.flush();
    } catch (Exception e) {
      errCount++;
      if(errCount < maxErr) {
        Activator.log.warn("Failed to store id=" + id + ", size=" + size, e);
      }
    }
  }

  public Dimension getSize() {
    if(defSize == null) {
      return null;
    }
    try {

      Preferences prefs = getPrefs();
      int w = prefs.getInt(KEY_WIDTH, defSize.width);
      int h = prefs.getInt(KEY_HEIGHT, defSize.height);

      Dimension size = new Dimension(w, h);
      return size;
    } catch (Exception e) {
      errCount++;
      if(errCount < maxErr) {
        Activator.log.warn("Failed to get id=" + id, e);
      }
    }
    return savedSize;
  }
}
