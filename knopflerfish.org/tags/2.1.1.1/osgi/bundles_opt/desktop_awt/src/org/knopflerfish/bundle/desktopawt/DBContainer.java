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
import java.awt.*;

 public class DBContainer extends Panel {

   Dimension memSize = new Dimension(0,0);
   Graphics  memG;
   Image     memImage;
   boolean   bNeedRedraw = true;
   
   static int count = 0;
   
   protected Graphics makeOff(Graphics g) {
     Dimension size = getSize();
     if(memSize.width != size.width ||
        memSize.height != size.height) {
       memSize = size;
       memImage = createImage(size.width, size.height);
       memG     = null;
       bNeedRedraw = true;
       if(memImage != null) {
         memG  = memImage.getGraphics();
       } else {
         System.out.println(getClass().getName() + ": failed to create memory buffer, size=" + size);
       }        
     }
     return memG != null ? memG : g;
   }
   
   public void paintComponent(Graphics g) {
     super.paint(g);
   }

   public boolean isOpaque() {
     return true;
   }

   public void update(Graphics g) {
     paintDB(g);
   }

   long paintTime = 10000;

   public void paint(Graphics g) {
     long t0 = System.currentTimeMillis();
     paintDB(g);
     long t1 = System.currentTimeMillis();
     paintTime = t1 - t0;
   }

   public void paintDB(Graphics g) {
     count++;

     makeOff(g);
     if(bNeedRedraw) {
       if(memG != null) {
         paintComponent(memG);
         bNeedRedraw = false;
       } else {
         paintComponent(g);
       }
     }

     
     if(memG != null) {
       g.drawImage(memImage, 0, 0, null);
     }
   }

   Color bgColor = null;
   
   public Color getBackground() {
     if(bgColor == null) {
       return getParent().getBackground();
     } else {
       return bgColor;
     }
   }
   
   /**
    * Override this without firing any propchangeevent as the
    * Component class does.
    * <p>
    * This is to avoid peer to clear background area. I do this better
    * myself.
    * </p>
    */
   public void setBackground(Color bg) {
     this.bgColor = bg;
   }

 }
