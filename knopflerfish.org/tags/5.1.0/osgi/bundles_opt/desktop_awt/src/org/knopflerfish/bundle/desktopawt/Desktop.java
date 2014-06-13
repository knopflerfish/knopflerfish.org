/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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
import java.awt.*;
import java.net.URL;

class Desktop {
  static Frame        frame;
  DesktopPanel dp;

  static MediaTracker tracker;
  static int trackerId = 0;

  static Image activeIcon = null;
  static Image bundleImage;
  static Image libImage;

  Desktop() {
    frame       = new Frame("Knopflerfish Desktop");
    tracker     = new MediaTracker(frame);
    activeIcon  = loadImage("/player_play_14x14.png");
    bundleImage = loadImage("/bundle.png");
    libImage    = loadImage("/lib.png");


    setIcon(frame, "/kf_");
  }

  static Image loadImage(String urlS) {
    try {
      URL url = Activator.bc.getBundle().getResource(urlS);

      if(url == null) {
        throw new RuntimeException("No image " + urlS);
      }

      Image img =  Toolkit.getDefaultToolkit().createImage(url);
      int id = trackerId++;
      tracker.addImage(img, id);
      tracker.waitForID(id); 

      return img;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to load image " +  urlS + ", " + e);
    }
  }

  public void open() {
    dp    = new DesktopPanel();
    frame.add(dp);
    frame.setSize(new Dimension(200, 400));
    
    frame.pack();
    frame.show();
  }

  public void close() {
    if(frame != null) {
      frame.hide();
      frame   = null;
      tracker = null;
    } 
  }


  public void setIcon(Frame frame, String baseName) {
    String iconName = baseName + "32x32.gif";
    if (System.getProperty( "os.name", "" ).startsWith("Win")) {
      iconName = baseName + "16x16.gif";
    }
    String strURL = iconName;
    try {
      Image image = loadImage(strURL);
      frame.setIconImage(image);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
