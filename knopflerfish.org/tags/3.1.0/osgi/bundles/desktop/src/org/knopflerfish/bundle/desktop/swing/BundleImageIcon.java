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

package org.knopflerfish.bundle.desktop.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.osgi.framework.Bundle;

public class BundleImageIcon extends ImageIcon {

  static ImageIcon activeIcon       = null;
  static ImageIcon installedIcon    = null;
  static ImageIcon resolvedIcon     = null;
  static ImageIcon startingIcon     = null;
  static ImageIcon stoppingIcon     = null;
  static ImageIcon uninstalledIcon  = null;

  Bundle bundle;

  /**
   * Image icon for a bundle. The paint method will draw different
   * overlays based on the bundles state.
   *
   * @param bundle The bundle that this icon represents.
   * @param url URL to icon image, should be 32x32. If the icon image
   *            has another size it will be scaled to 32x32.
   */
  public BundleImageIcon(Bundle bundle,
                         URL url)
  {
    super(url);

    this.bundle = bundle;


    // Force image to load;  so that we can check the size
    loadImage(getImage());
    if (32<getIconWidth() || 32<getIconHeight()) {
      // Image too large; scale it down to 32x32.
      setImage(getScaledImage(getImage(), 32, 32));
    }

    if(activeIcon == null) {
      activeIcon      =
        // Activator.desktop.startIcon;
        new ImageIcon(getClass().getResource("/player_play_14x14.png"));

      installedIcon   = null;
      //        new ImageIcon(getClass().getResource("/overlay_installed.gif"));

      resolvedIcon = null;
      //        new ImageIcon(getClass().getResource("/overlay_resolved.gif"));

      startingIcon    = null;
      //        new ImageIcon(getClass().getResource("/overlay_starting.gif"));

      stoppingIcon    = null;
      //        new ImageIcon(getClass().getResource("/overlay_stopping.gif"));

      uninstalledIcon = null;
      //        new ImageIcon(getClass().getResource("/overlay_uninstalled.gif"));

    }
  }

  public void paintIcon(Component c,
                        Graphics g,
                        int x,
                        int y) {
    super.paintIcon(c, g, x, y);

    Icon overlay = null;

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

    if(overlay != null) {
      int x1 = x + (getIconWidth() - overlay.getIconWidth());
      int y1 = y + (getIconHeight() - overlay.getIconHeight());

      int w = overlay.getIconWidth();
      int h = overlay.getIconHeight();

      g.setColor(Color.white);
      g.fill3DRect(x1-1, y1-1, w+2, h+2, true);
      overlay.paintIcon(c, g, x1, y1);
    }
  }

  /**
   * Resizes an image using a Graphics2D object backed by a BufferedImage.
   *
   * @param srcImg - source image to scale
   * @param w - desired width
   * @param h - desired height
   * @return - the new resized image
   */
  private static Image getScaledImage(Image srcImg, int w, int h) {
    BufferedImage resizedImg = new BufferedImage(w, h,
                                                 BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = resizedImg.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.drawImage(srcImg, 0, 0, w, h, null);
    g2.dispose();
    return resizedImg;
  }

}
