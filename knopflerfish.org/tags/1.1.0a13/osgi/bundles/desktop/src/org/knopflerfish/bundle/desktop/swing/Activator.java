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

import org.osgi.framework.*;
import org.osgi.util.tracker.*;
import org.osgi.service.packageadmin.*;
import org.knopflerfish.service.log.*;

import java.util.Hashtable;
import org.knopflerfish.service.desktop.*;

public class Activator implements BundleActivator {

  static public LogRef        log;
  static public BundleContext bc;

  static public Desktop desktop;

  static ServiceTracker pkgTracker;

  
  public void start(BundleContext _bc) {
    this.bc  = _bc;
    this.log = new LogRef(bc);

    pkgTracker = new ServiceTracker(bc, PackageAdmin.class.getName(), null);
    pkgTracker.open();
    
    // Spawn to avoid racing conditions in resource loading
    Thread t = new Thread(new Runnable() {
	public void run() {
	  desktop = new Desktop();
	  desktop.start();

	  DefaultSwingBundleDisplayer disp;

	  // bundle displayers
	  disp = new LargeIconsDisplayer(bc);
	  disp.open();
	  disp.register();

	  disp = new TimeLineDisplayer(bc);
	  disp.open();
	  disp.register();

	  disp = new TableDisplayer(bc);
	  disp.open();
	  disp.register();

	  disp = new SpinDisplayer(bc);
	  disp.open();
	  disp.register();

	  // detail displayers
	  disp = new ManifestHTMLDisplayer(bc);
	  disp.open();
	  disp.register();

	  disp = new ClosureHTMLDisplayer(bc);
	  disp.open();
	  disp.register();

	  disp = new ServiceHTMLDisplayer(bc);
	  disp.open();
	  disp.register();

	  disp = new PackageHTMLDisplayer(bc);
	  disp.open();
	  disp.register();


	  disp = new LogDisplayer(bc);
	  disp.open();
	  disp.register();


	  // We really want this one to be display.
	  desktop.bundlePanel.showTab("Large Icons");

	}
      }, "desktop startup");
    
    t.start();

  }

  public void stop(BundleContext bc) {
    try {
      if(desktop != null) {
	desktop.stop();
	desktop = null;
      }
      
      if(log != null) {
	log = null;
      }
      
      this.bc = null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
