/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

import java.awt.Font;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class KnopflerfishTheme extends DefaultMetalTheme {
  final ColorUIResource primary1 = new ColorUIResource(160, 133, 95);
  final ColorUIResource primary2 = new ColorUIResource(213, 185, 145);
  final ColorUIResource primary3 = new ColorUIResource(255, 238, 185);

  final ColorUIResource secondary1 = new ColorUIResource(112, 112, 112);
  final ColorUIResource secondary2 = new ColorUIResource(163, 163, 163);
  final ColorUIResource secondary3 = new ColorUIResource(229, 229, 220);

  FontUIResource controlFont;
  FontUIResource systemFont;
  FontUIResource userFont;
  FontUIResource smallFont;

  public String getName() {
    return "Knopflerfish";
  }

  public KnopflerfishTheme() {
  }
  
  // these are blue in Metal Default Theme
  protected ColorUIResource getPrimary1() {
    return primary1;
  }
  
  protected ColorUIResource getPrimary2() {
    return primary2;
  }

  protected ColorUIResource getPrimary3() {
    return primary3;
  }

  // these are gray in Metal Default Theme
  protected ColorUIResource getSecondary1() {
    return secondary1;
  }

  protected ColorUIResource getSecondary2() {
    return secondary2;
  }

  protected ColorUIResource getSecondary3() {
    return secondary3;
  }

  public ColorUIResource getSystemTextColor() {
    return MetalLookAndFeel.getBlack();
  }

  public FontUIResource getControlTextFont() {
    if (controlFont == null) {
      try {
        controlFont = new FontUIResource(Font.getFont("swing.plaf.metal.controlFont", new Font("Dialog", Font.PLAIN, 12)));
      }
      catch (Exception e) {
        controlFont = new FontUIResource("Dialog", Font.PLAIN, 12);
      }
    }
    return controlFont;
  }

  /*
  public FontUIResource getSystemTextFont() {
    if (systemFont == null) {
      try {
        systemFont = new FontUIResource(Font.getFont("swing.plaf.metal.systemFont", new Font("Verdana", Font.PLAIN, 12)));
      }
      catch (Exception e) {
        systemFont = new FontUIResource("Verdana", Font.PLAIN, 12);
      }
    }
    return systemFont;
  }

  public FontUIResource getUserTextFont() {
    if (userFont == null) {
      try {
        userFont = new FontUIResource(Font.getFont("swing.plaf.metal.userFont", new Font("Verdana", Font.PLAIN, 12)));
      }
      catch (Exception e) {
        userFont = new FontUIResource("Verdana", Font.PLAIN, 12);
      }
    }
    return userFont;
  }

  public FontUIResource getMenuTextFont() {
    if (controlFont == null) {
      try {
        controlFont = new FontUIResource(Font.getFont("swing.plaf.metal.controlFont", new Font("Verdana", Font.PLAIN, 12)));
      }
      catch (Exception e) {
        controlFont = new FontUIResource("Verdana", Font.PLAIN, 12);
      }
    }
    return controlFont;
  }

  public FontUIResource getWindowTitleFont() {
    if (controlFont == null) {
      try {
        controlFont = new FontUIResource(Font.getFont("swing.plaf.metal.controlFont", new Font("Verdana", Font.BOLD, 12)));
      }
      catch (Exception e) {
        controlFont = new FontUIResource("Verdana", Font.BOLD, 12);
      }
    }
    return controlFont;
  }

  public FontUIResource getSubTextFont() {
    if (smallFont == null) {
      try {
        smallFont = new FontUIResource(Font.getFont("swing.plaf.metal.smallFont", new Font("Verdana", Font.PLAIN, 10)));
      }
      catch (Exception e) {
        smallFont = new FontUIResource("Verdana", Font.PLAIN, 10);
      }
    }
    return smallFont;
  }
  */
}
