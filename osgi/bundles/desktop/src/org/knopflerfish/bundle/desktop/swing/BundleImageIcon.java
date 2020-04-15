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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.osgi.framework.Bundle;

import org.knopflerfish.framework.Util.HeaderEntry;

/**
 * Image icon for a bundle that paints different overlays based on the bundles
 * state.
 */
public class BundleImageIcon
  extends ImageIcon
{
  private static final long serialVersionUID = 1L;

  /**
   * The name of the bundle icon manifest header.
   *
   * This constant should be in {@link org.osgi.framework.Constants} but is
   * not...
   */
  final static String BUNDLE_ICON = "Bundle-Icon";

  // Image icons for the bundle state overlays:
  static final ImageIcon OVERLAY_ACTIVE =
    new ImageIcon(BundleImageIcon.class.getResource("/overlay_active.png"));
  static final ImageIcon OVERLAY_INSTALLED = null; // No overlay for installed
  static final ImageIcon OVERLAY_RESOLVED =
    new ImageIcon(BundleImageIcon.class.getResource("/overlay_resolved.png"));
  static final ImageIcon OVERLAY_STARTING =
    new ImageIcon(BundleImageIcon.class.getResource("/overlay_starting.png"));
  static final ImageIcon OVERLAY_STOPPING =
    new ImageIcon(BundleImageIcon.class.getResource("/overlay_stopping.png"));
  static final ImageIcon OVERLAY_UNINSTALLED = null; // No overlay for uninstalled
  public static final int MAX_ICON_SIZE = 32;

  /**
   * Get the bundle icon URL for icon with size 32 from the manifest header
   * "Bundle-Icon".
   *
   * <p>
   * Care must be taken to not trigger a resolve of the bundle when reading the
   * icon image from inside it.
   * </p>
   *
   * @param bundle
   *          The bundle to get icon URL for.
   *
   */
  private static URL getBundleIconURL(final Bundle bundle)
  {
    URL res = null;

    final String bih = bundle.getHeaders().get(BUNDLE_ICON);
    if (null != bih && 0 < bih.length()) {
      // Re-uses the manifest entry parser from the KF-framework
      try {
        String iconName = null;
        int iconSize = -1;
        // We prefer a 32x32 size icon.
        for (final HeaderEntry headerEntry : org.knopflerfish.framework.Util
            .parseManifestHeader(BUNDLE_ICON, bih, false, true, false)) {
          final List<String> icons = headerEntry.getKeys();
          final String sizeS = (String) headerEntry.getAttributes().get("size");

          if (null == sizeS) {
            // Icon with unspecified size; use it if no other icon
            // has been found.
            if (null == iconName) {
              iconName = icons.get(0);
            }
          } else {
            int size = -1;
            try {
              size = Integer.parseInt(sizeS);
            } catch (final NumberFormatException ignored) {
            }
            if (-1 < size) {
              if (-1 == iconSize) {
                // First icon with a valid size; start with it.
                iconName = icons.get(0);
                iconSize = size;
              } else if (Math.abs(size - MAX_ICON_SIZE) < Math.abs(iconSize - MAX_ICON_SIZE)) {
                // Icon is closer in size to optimal size than old icon; use it
                iconName = icons.get(0);
                iconSize = size;
              }
            }
          }
        }
        if (null != iconName) {
          try {
            try {
              res = new URL(iconName);
            } catch (final MalformedURLException mfe) {
              // iconName is not a valid URL; assume it is a resource path
              // Use bundle.getEntry() to avoid resolving the bundle.
              res = bundle.getEntry(iconName);
              if (null == res) {
                Activator.log.warn("Failed to load icon with name '" + iconName
                                   + "' from bundle #" + bundle.getBundleId()
                                   + " (" + Util.getBundleName(bundle)
                                   + "): No such entry.");
              }
            }
          } catch (final Exception e) {
            Activator.log.error("Failed to load icon with name '" + iconName
                                    + "' from bundle #" + bundle.getBundleId()
                                    + " (" + Util.getBundleName(bundle) + "): "
                                    + e.getMessage(), e);
          }
        }
      } catch (final IllegalArgumentException iae) {
        Activator.log.error("Failed to parse Bundle-Icon header for #"
                                + bundle.getBundleId() + " ("
                                + Util.getBundleName(bundle) + "): "
                                + iae.getMessage(), iae);
      }
    }
    return res;
  }

  /**
   * Get the bundle icon URL for icon with size 32 from the Knopflerfish defined
   * manifest header "Application-Icon".
   *
   * @param bundle
   *          the bundle to get an application icon for.
   */
  private static URL getApplicationIconURL(final Bundle bundle)
  {
    URL res = null;

    String iconName = bundle.getHeaders().get("Application-Icon");
    if (iconName != null) {
      iconName = iconName.trim();
    }

    if (iconName != null && 0 < iconName.length()) {
      try {
        res = bundle.getEntry(iconName);
        if (null == res) {
          Activator.log.warn("Failed to load icon with name '" + iconName
                             + "' from bundle #" + bundle.getBundleId() + " ("
                             + Util.getBundleName(bundle) + "): No such resource.");
        }
      } catch (final Exception e) {
        Activator.log.error("Failed to load icon with name '" + iconName
                            + "' from bundle #" + bundle.getBundleId() + " ("
                            + Util.getBundleName(bundle) + "): " + e.getMessage(), e);
      }
    }
    return res;
  }

  /**
   * Get the URL of the icon to use for the given bundle.
   *
   * @param bundle
   *          the bundle to create a bundle image icon for.
   * @return URL to the icon for for the given bundle.
   */
  private static URL getIconUrl(Bundle bundle)
  {
    URL appURL = getBundleIconURL(bundle);
    if (null == appURL) {
      appURL = getApplicationIconURL(bundle);
    }

    if (appURL == null) {
      if (Util.hasMainClass(bundle)) {
        appURL = Util.class.getResource("/jarexec.png");
      } else if (Util.hasFragment(bundle)) {
        appURL = Util.class.getResource("/frag.png");
      } else if (Util.hasComponent(bundle)) {
        appURL = Util.class.getResource("/component.png");
      } else if (Util.hasActivator(bundle)) {
        appURL = Util.class.getResource("/bundle.png");
      } else {
        appURL = Util.class.getResource("/bundle-lib.png");
      }
    }
    return appURL;
  }

  /**
   * The bundle that this icon image belongs to.
   */
  Bundle bundle;

  /**
   * The last modified time stamp for the bundle. Used to detect when image
   * needs to be updated.
   */
  long lastModified;

  /**
   * Create a 32 by 32 image icon for a bundle using the icon specified in the
   * bundles manifest.
   *
   * <p>
   * If the bundles has not specified an icon image then the default images
   * defined by the desktop bundle will be used.
   * </p>
   *
   * @param bundle
   *          The bundle that this icon represents.
   */
  public BundleImageIcon(Bundle bundle)
  {
    this(bundle, getIconUrl(bundle));
  }

  /**
   * Image icon for a bundle. The paint method will draw different overlays
   * based on the bundles state.
   *
   * @param bundle
   *          The bundle that this icon represents.
   * @param url
   *          URL to icon image, should be 32x32. If the icon image has another
   *          size it will be scaled to 32x32.
   */
  public BundleImageIcon(Bundle bundle, URL url)
  {
    super(url);

    this.bundle = bundle;
    this.lastModified = Long.MIN_VALUE;

    // Load the icon and ensure size.
    updateIcon();
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y)
  {
    updateIcon();

    super.paintIcon(c, g, x, y);

    Icon overlay = null;

    switch (bundle.getState()) {
    case Bundle.ACTIVE:
      overlay = OVERLAY_ACTIVE;
      break;
    case Bundle.INSTALLED:
      overlay = OVERLAY_INSTALLED;
      break;
    case Bundle.RESOLVED:
      overlay = OVERLAY_RESOLVED;
      break;
    case Bundle.STARTING:
      overlay = OVERLAY_STARTING;
      break;
    case Bundle.STOPPING:
      overlay = OVERLAY_STOPPING;
      break;
    case Bundle.UNINSTALLED:
      overlay = OVERLAY_UNINSTALLED;
      break;
    default:
    }

    if (overlay != null) {
      final int x1 = x + (getIconWidth() - overlay.getIconWidth());
      final int y1 = y + (getIconHeight() - overlay.getIconHeight());

      final int w = overlay.getIconWidth();
      final int h = overlay.getIconHeight();

      g.setColor(Color.white);
      g.fill3DRect(x1 - 1, y1 - 1, w + 2, h + 2, true);
      overlay.paintIcon(c, g, x1, y1);
    }
  }

  /**
   * If the bundle has been updated since the last call to this method we must
   * reload the icon image to ensure that the current image is used.
   */
  private void updateIcon()
  {
    if (lastModified < bundle.getLastModified()) {
      // Load new icon
      final URL iconUrl = getIconUrl(bundle);
      final ImageIcon ii = new ImageIcon(iconUrl);
      // Force image to load; so that we can check the size
      setImage(ii.getImage());
      lastModified = bundle.getLastModified();
      loadImage(getImage());
      if (MAX_ICON_SIZE < getIconWidth() || MAX_ICON_SIZE < getIconHeight()) {
        // Image too large; scale it down.
        setImage(getScaledImage(getImage()));
      }
    }
  }

  /**
   * Resizes an image using a Graphics2D object backed by a BufferedImage.
   *
   * @param srcImg
   *          - source image to scale
   * @return - the new resized image
   */
  private static Image getScaledImage(Image srcImg)
  {
    final BufferedImage resizedImg =
      new BufferedImage(MAX_ICON_SIZE, MAX_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = resizedImg.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.drawImage(srcImg, 0, 0, MAX_ICON_SIZE, MAX_ICON_SIZE, null);
    g2.dispose();
    return resizedImg;
  }

}
