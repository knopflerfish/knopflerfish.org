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

import javax.swing.*;
import java.util.*;

public class LFManager {

  public Hashtable customLF = new Hashtable();

  boolean bUseSystemLF = true;

  public LFManager() {
    try {
      String s = System.getProperty("org.knopflerfish.osgi.desktop.usesystemlf");
      bUseSystemLF = s == null || "true".equals(s);
    } catch (Exception e) {
      Activator.log.error("Failed to get property", e);
    }
  }
  
  public void init() {
    if(!customLF.containsKey(KnopflerfishLookAndFeel.class.getName())) {
      customLF.put(KnopflerfishLookAndFeel.class.getName(),
		   new KnopflerfishLookAndFeel());
      Activator.log.debug("Created Knopflerfish L&F");
    }

    // check if swing handles this all by itself...
    try {
      if(!"".equals(System.getProperty("swing.defaultlaf", ""))) {
        return;
      }
    } catch (Exception ignored) { }

    LookAndFeel origLF = UIManager.getLookAndFeel();
    if(origLF != null && !customLF.containsKey(origLF.getClass().getName())) {
      customLF.put(origLF.getClass().getName(), origLF);
      Activator.log.debug("Saved orig L&F " + origLF.getClass().getName());
    }
    try {
      boolean bUseCustomLF = false;
      if(origLF == null) { 
	Activator.log.debug("Setting Knopflerfish L&F since no LF was set");
	bUseCustomLF = true;
      } else {
	if(origLF.getClass().getName().startsWith("com.l2fprod.gui.plaf.skin")) {
	  Activator.log.debug("Skipping Knopflerfish L&F since SkinLF seems to be active");
	} else if(-1 != origLF.getClass().getName().indexOf("apple.")) {
	  Activator.log.debug("Skipping Knopflerfish L&F since Apple LF seems to be active");
	} else {
	  Activator.log.debug("Overriding LF " + origLF.getClass().getName() + 
			     " with Knopflerfish LF");
	  bUseCustomLF = true;
	}
      }
      if(bUseSystemLF) {
	String systemLF = UIManager.getSystemLookAndFeelClassName();
	Activator.log.debug("Setting system L&F " + systemLF);
	UIManager.setLookAndFeel(systemLF);
      } else if(bUseCustomLF) {
	Activator.log.debug("Setting Knopflerfish L&F...");
	UIManager.setLookAndFeel((LookAndFeel)
				 customLF.get(KnopflerfishLookAndFeel.class.getName()));
      }
    } catch (Exception e) {
      Activator.log.error("Failed to set default LF");
      customLF.remove(KnopflerfishLookAndFeel.class.getName());
    }
  }
}
