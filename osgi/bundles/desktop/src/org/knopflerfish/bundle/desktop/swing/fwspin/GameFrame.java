/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing.fwspin;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.net.URL;

import org.knopflerfish.bundle.desktop.swing.Util;

/**
 * @author Erik Wistrand
 */
public class GameFrame {

  Frame frame;
  Spin spin = null;

  Dimension size = new Dimension(600, 400);

  Container contentPane;

  public GameFrame() {
    frame = new Frame("FWSpin");
    //    setIcon();

    contentPane = frame;
    //    frame.getContentPane().setLayout(new BorderLayout());
    contentPane.setLayout(new BorderLayout());
    init();
  }

  void init() {
    spin = new Spin();
    contentPane.add(spin, BorderLayout.CENTER);


    frame.pack();
    frame.invalidate();
    frame.setSize(size);
    frame.setVisible(true);
  }

  public void start() {
    //    spin.start();
  }

  public void stop() {
    //    spin.stop();
  }

  public void close() {
    stop();
    if(frame != null) {
      frame.dispose();
      frame = null;
    }
  }

  void setIcon() {
    String iconName = "cloud32x32.gif";
    if (Util.isWindows()) {
      iconName = "cloud16x16.gif";
    }
    String strURL = "/data/" + iconName;
    try {
      MediaTracker tracker = new MediaTracker(frame);

      URL url = getClass().getResource(strURL);

      if(url != null) {
        Image image = frame.getToolkit().getImage(url);
        tracker.addImage(image, 0);
        tracker.waitForID(0);

        frame.setIconImage(image);
      } else {
      }
    } catch (Exception e) {
    }
  }

}
