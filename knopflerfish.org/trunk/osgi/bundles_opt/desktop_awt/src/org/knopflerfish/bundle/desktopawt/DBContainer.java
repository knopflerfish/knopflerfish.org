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
   
   boolean bNeedRedraw = true;
   
   void makeOff(Graphics g) {
     Dimension size = getSize();
     if(memSize.width != size.width ||
        memSize.height != size.height) {
       memSize = size;
       memImage = createImage(size.width, size.height);
       memG     = null;
       if(memImage != null) {
         memG  = memImage.getGraphics();
       } else {
         System.out.println(getClass().getName() + ": failed to create memory buffer, size=" + size);
       }        
     }
   }
   
   public void paintComponent(Graphics g) {
     super.paint(g);
   }

   public void paint(Graphics _g) {
     makeOff(_g);
     
     Graphics g = memG != null ? memG : _g;
     if(bNeedRedraw || memG == null) {
       paintComponent(g);
     }
     
     bNeedRedraw = false;
     
     if(memG != null) {
       _g.drawImage(memImage, 0, 0, null);
     }
   }
 }
