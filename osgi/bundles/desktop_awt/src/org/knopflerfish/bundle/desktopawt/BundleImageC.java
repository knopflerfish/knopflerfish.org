/*
 * Copyright (c) 2003, 2004 KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktopawt;

import org.osgi.framework.*;

import java.util.*;
import java.io.*;
import java.net.URL;

import java.awt.*;

public class BundleImageC extends ImageLabel {

  static Image activeIcon       = null;
  static Image installedIcon    = null;
  static Image resolvedIcon     = null;
  static Image startingIcon     = null;
  static Image stoppingIcon     = null;
  static Image uninstalledIcon  = null;

  Bundle bundle;

  public BundleImageC(Bundle bundle, 
                      String url) {
    super(url, 2, Color.white);

    this.bundle = bundle;
    bDoHighlight = false;

    if(activeIcon == null) {
      activeIcon      = Desktop.loadImage("/player_play_14x14.png");
      
      installedIcon   = null;
      resolvedIcon    = null;
      startingIcon    = null; 
      stoppingIcon    = null;
      uninstalledIcon = null;
    }
  }
  
  public void paint(Graphics g) { 
    super.paint(g);
    //    System.out.println("paint " + bundle.getBundleId() + ", state=" + bundle.getState());
    Image overlay = null;

    switch(bundle.getState()) {
    case Bundle.ACTIVE:
      overlay = activeIcon;
      break;
    case Bundle.INSTALLED:
      overlay = installedIcon;
      break;
    case Bundle.RESOLVED:
      overlay = resolvedIcon;
      break;
    case Bundle.STARTING:
      overlay = startingIcon;
      break;
    case Bundle.STOPPING:
      overlay = stoppingIcon;
      break;
    case Bundle.UNINSTALLED:
      overlay = uninstalledIcon;
      break;
    default:
    }

    Dimension size = getSize();

    if(overlay != null) {
      int w = overlay.getWidth(null);
      int h = overlay.getHeight(null);

      int x = 0;
      int y = 0;

      int x1 = x + (size.width - w - 2);
      int y1 = y + (size.height -  h - 2);


      g.setColor(Color.white);
      g.fillRect(x1-1, y1-1, w, h);
      g.setColor(Color.gray);
      g.draw3DRect(x1-1, y1-1, w, h, true);
      g.drawImage(overlay, x1, y1, null);
    }

  }
}

