/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
import org.osgi.service.packageadmin.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;

public class ShutdownPanel extends Panel implements Runnable {
  Fzz fzz;

  public ShutdownPanel() {
    setLayout(new BorderLayout());
    Panel row = new Panel(new BorderLayout());
    Button button = new Button("Shutdown");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          startShutdown();
        }
      });
    row.add(button, BorderLayout.WEST);
    row.add(new Label("All bundles will be stopped"), BorderLayout.CENTER);
    add(row, BorderLayout.NORTH);
    fzz = new Fzz();
    add(fzz, BorderLayout.CENTER);
  }

  Thread runner = null;

  public void startShutdown() {
    try {
      if(runner == null) {
        runner = new Thread(this);
        runner.start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void run() {
    long t0 = System.currentTimeMillis();
    long time = 1000;

    fzz.kx = 1.0;
    try {
      while(System.currentTimeMillis() - t0 < time) {
        fzz.update();
        fzz.repaint();
        Thread.sleep(20);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      runner = null;
      try {
        Activator.bc.getBundle(0).stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  class Fzz extends DBContainer {
    
    Random rnd = new Random();
    
    double kx = 1.0;
    
    
    public void update() {
      kx *= .98;
      bNeedRedraw = true;
    }

    public void paintComponent(Graphics g) {
      Dimension size = getSize();
      if(kx == 1.0) {
        g.setColor(getBackground());
        g.fillRect(0, 0, size.width, size.height);
        return;
      }

      g.setColor(Color.black);
      g.fillRect(0, 0, size.width, size.height);

      int lastX = -1;
      int lastY = 0;
      int steps = 20;
      int totalX = (int)(kx * size.width);
      int leftX = (size.width - totalX) / 2;

      g.setColor(Color.yellow);
      for(int i = 0; i < steps; i++) {
        double k  = (double)i / steps;
        double ky = 2 * kx * (k >= .5 ? 1 - k : k);
        ky *= ky;
        int x = (int)(leftX + totalX * k);
        
        int y = (int)(size.height / 2 + 2 * ky * (rnd.nextDouble()-.5) * size.height / 20);
        
        if(lastX > 0) {
          g.drawLine(lastX, lastY, x, y);
        }
        lastX = x;
        lastY = y;
      }
    }
  }
}
