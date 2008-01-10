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

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class KnopflerfishLookAndFeel extends MetalLookAndFeel {

  public static String name = "Knopflerfish";

  public String getDescription() {
    return "The Knopflerfish Look And Feel";
  }

  public String getID() {
    return getClass().getName();
  }

  public String getName() {
    return name;
  }

  public boolean isNativeLookAndFeel() {
    return false;
  }

  public boolean isSupportedLookAndFeel() {
    return true;
  }

  public void initialize() {
    super.initialize();
  }

  public void uninitialize() {
    super.uninitialize();
    setCurrentTheme(new DefaultMetalTheme());
  }

  protected void createDefaultTheme() {
    setCurrentTheme(new KnopflerfishTheme());
  }

  protected void initComponentDefaults(UIDefaults table) {
    super.initComponentDefaults( table );

    // I'll have to copy some of the resource definitions here, since the
    // original code in BasicLookAndFeel (from which we inherit) uses 
    // getClass() to find its resources. That will fail since my 
    // classloader does not have these resources.
    //
    // So, the trick is to replace getClass() with MetalLookAndFeel.class

    Object[] defaults = {
      "OptionPane.errorIcon",       LookAndFeel.makeIcon(MetalLookAndFeel.class, "icons/Error.gif"),
      "OptionPane.informationIcon", LookAndFeel.makeIcon(MetalLookAndFeel.class, "icons/Inform.gif"),
      "OptionPane.warningIcon",     LookAndFeel.makeIcon(MetalLookAndFeel.class, "icons/Warn.gif"),
      "OptionPane.questionIcon",    LookAndFeel.makeIcon(MetalLookAndFeel.class, "icons/Question.gif"),

      "InternalFrame.icon",         LookAndFeel.makeIcon(BasicLookAndFeel.class, "icons/JavaCup.gif"),


      // Button margin slightly smaller than metal to save space
      "Button.margin", new InsetsUIResource(1, 11, 1, 11),


    };
    table.putDefaults(defaults);
  }
}
