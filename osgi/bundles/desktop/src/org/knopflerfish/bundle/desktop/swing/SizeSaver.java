/*
 * Copyright (c) 2003-2020, KNOPFLERFISH project
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

public class SizeSaver extends ComponentAdapter {
  public static final String NODE_NAME    = "sizes";

  public static final String KEY_X        = "x";
  public static final String KEY_Y        = "y";
  public static final String KEY_WIDTH    = "width";
  public static final String KEY_HEIGHT   = "height";
  public static final String KEY_STATE    = "state";
  public static final String KEY_SPLITPOS = "splitpos";

  private final String id;

  private Component comp;
  private Dimension defSize;
  private Dimension savedSize;
  private int defSplit;
  private int errCount = 0;
  private int maxErr = 10;

  private SizeSaver(String id, Component newComponent) {
    if (id.length() == 0 || id.contains("/")) {
      throw new IllegalArgumentException("Bad id string '" + id + "', must be non-zero length and no '/'");
    }
    this.id = id;

    comp = newComponent;
    comp.addComponentListener(this);
  }

  public SizeSaver(String id, int defSplit, JSplitPane splitPane) {
    this(id, splitPane);
    this.defSplit = defSplit;
    initDividerLocation(splitPane);
    splitPane.getLeftComponent().addComponentListener(this);
  }

  public SizeSaver(String id, int defWidth, int defHeight, JFrame frame) {
    this(id, frame);
    defSize = new Dimension(defWidth, defHeight);
    savedSize = defSize;
    initComponentSize();
    initFrameStateAndLocation(frame);
  }

  private void initComponentSize() {
    Dimension size = getSize();

    if (size != null) {
      comp.setSize(size);
      comp.setPreferredSize(size);
    }
  }

  private void initDividerLocation(JSplitPane split) {
    Preferences prefs = getPrefs();
    int pos = prefs.getInt(KEY_SPLITPOS, defSplit);
    if (pos != -1) {
      split.setDividerLocation(pos);
      invalidate(split);
    }
  }

  private void initFrameStateAndLocation(Frame frame) {
    Preferences prefs = getPrefs();
    Toolkit toolkit = frame.getToolkit();
    if (toolkit.isFrameStateSupported(Frame.MAXIMIZED_VERT) ||
       toolkit.isFrameStateSupported(Frame.MAXIMIZED_HORIZ) ||
       toolkit.isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
      int state = prefs.getInt(KEY_STATE, Frame.NORMAL);
      frame.setExtendedState(state);
    }
    int x = prefs.getInt(KEY_X, 0);
    int y = prefs.getInt(KEY_Y, 0);
    frame.setLocation(x, y);
  }

  private void invalidate(JSplitPane splitPane) {
    // Tell components that they may want to redo its layout
    Component parent = splitPane.getParent();
    if (parent != null) {
      parent.invalidate();
    } else {
      splitPane.invalidate();
    }
  }

  public void detach() {
    if(comp != null) {
      if (comp instanceof JSplitPane) {
        JSplitPane splitPane = (JSplitPane) comp;
        splitPane.getLeftComponent().removeComponentListener(this);
      }
      comp.removeComponentListener(this);
      comp = null;
    }
  }

  @Override
  public void componentMoved(ComponentEvent e) {
    handleCompChange();
  }

  @Override
  public void componentResized(ComponentEvent e) {
    handleCompChange();
  }

  private void handleCompChange() {
    if (comp != null && comp.isVisible()) {
      store();
    }
  }

  private Preferences getPrefs() {
    Preferences prefsBase = Preferences.userNodeForPackage(getClass());
    Preferences prefs     = prefsBase.node(NODE_NAME + "/" + getSpid() + "/" + id);
    getLatestVersionOfNode(prefs);
    return prefs;
  }

  private String getSpid() {
    String spid  = Activator.getBC().getProperty("org.osgi.provisioning.spid");
    return spid == null ? "default" : spid;
  }

  private void getLatestVersionOfNode(Preferences prefs) {
    try {
      prefs.sync();
    } catch (Exception e) {
      errCount++;
      if(errCount < maxErr) {
        Activator.log.warn("Failed to get id=" + id, e);
      }
    }
  }

  private void store() {
    Dimension size = comp.getSize();
    try {
      savedSize = new Dimension(size.width, size.height);
      Preferences prefs = getPrefs();


      if(comp instanceof JFrame) {
        Point p = comp.getLocationOnScreen();
        prefs.put(KEY_X, Integer.toString((int)p.getX()));
        prefs.put(KEY_Y, Integer.toString((int)p.getY()));

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit.isFrameStateSupported(Frame.MAXIMIZED_VERT) ||
            toolkit.isFrameStateSupported(Frame.MAXIMIZED_HORIZ) ||
            toolkit.isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
          int state = ((Frame)comp).getExtendedState();
          prefs.put(KEY_STATE, Integer.toString(state));
        }
      }

      prefs.put(KEY_WIDTH, Integer.toString(size.width));
      prefs.put(KEY_HEIGHT, Integer.toString(size.height));

      if (comp instanceof JSplitPane) {
        JSplitPane splitPane = (JSplitPane) comp;
        prefs.put(KEY_SPLITPOS, Integer.toString(splitPane.getDividerLocation()));
      }

      prefs.flush();
    } catch (Exception e) {
      errCount++;
      if(errCount < maxErr) {
        Activator.log.warn("Failed to store id=" + id + ", size=" + size, e);
      }
    }
  }

  private Dimension getSize() {
    if (defSize == null) {
      return null;
    }
    try {

      Preferences prefs = getPrefs();
      int w = prefs.getInt(KEY_WIDTH, defSize.width);
      int h = prefs.getInt(KEY_HEIGHT, defSize.height);

      return new Dimension(w, h);
    } catch (Exception e) {
      errCount++;
      if(errCount < maxErr) {
        Activator.log.warn("Failed to get id=" + id, e);
      }
    }
    return savedSize;
  }
}
