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
import java.awt.event.*;
import java.util.*;

public class ImageLabel extends Component {
  Image     img;
  boolean   bFocus = false;
  boolean   bEnabled   = true;
  boolean   bDoHighlight = true;

  Dimension imgSize;
  Dimension minSize;

  Hashtable actionListeners = new Hashtable();

  LF        lf         = LF.getLF();

  int id = 0;

  public ImageLabel(String url, int pad, Color bg) {
    img = Desktop.loadImage(url);
    imgSize = new Dimension(img.getWidth(null),
                            img.getHeight(null));
    minSize = new Dimension(imgSize.width + pad * 2,
                            imgSize.height + pad * 2);
    setBackground(bg);
    
    addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          if(isEnabled()) {
            setFocus(true);
          }
        }
        public void mouseExited(MouseEvent e) {
          if(isEnabled()) {
            setFocus(false);
          }
        }
        public void mouseClicked(MouseEvent e) {
          if(isEnabled()) {
            setFocus(false);
            sendActionEvent(new ActionEvent(ImageLabel.this, id++, ""));
          }
        }
      });
  }

  public void addActionListener(ActionListener l) {
    actionListeners.put(l, l);
  }

  public void removeActionListener(ActionListener l) {
    actionListeners.remove(l);
  }

  public void sendActionEvent(ActionEvent ev) {
    for(Enumeration e = actionListeners.keys(); e.hasMoreElements();) {
      ActionListener l = (ActionListener)e.nextElement();
      try {
        l.actionPerformed(ev);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public boolean isEnabled() {
    return bEnabled;
  }

  public void setEnabled(boolean b) {
    bEnabled = b;
  }

  public void setFocus(boolean b) {
    bFocus = b;
    repaint();
  }

  public boolean isFocus() {
    return bFocus;
  }

  public Dimension getPreferredSize() {
    return minSize;
  }

  public Dimension getMinimumSize() {
    return minSize;
  }

  public Dimension preferredSize() {
    return getPreferredSize();
  }

  Color alphaCol = new Color(0,0,0,255);

  public void paint(Graphics g) {
    Dimension size = getSize();
    
    if(bFocus && bDoHighlight) {
      Color hiliteCol1 = lf.stdHiliteCol1.brighter();
      lf.paintButton(g, 
                     size, 
                     getBackground(), 
                     lf.stdHiliteCol1,
                     lf.stdHiliteCol2,
                     lf.stdSelectedCol,
                     false);
    } else {
      g.setColor(getBackground());
      g.fillRect(0,0, size.width, size.height);
    }
    int w = img.getWidth(null);
    int h = img.getHeight(null);


    g.drawImage(img,
                size.width / 2 - w / 2,
                size.height / 2 - h / 2,
                null);
    
  }
}
