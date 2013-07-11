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


package org.knopflerfish.bundle.soap.desktop;

import org.knopflerfish.service.desktop.*;

import javax.swing.*;
import org.osgi.framework.*;

/**
 * Very simple wrapper class which converts the JSOAPUI class into
 * a Desktop plugin.
 *
 * <p>
 * Bundle selection events are completely ignored.
 * </p>
 */
public class SOAPDisplayer implements SwingBundleDisplayer {
  
  public JComponent createJComponent() {
    int port = 80;

    // figure out the port the web server is running on
    // for initial SOAP host URL
    try {
      ServiceReference sr = Activator.bc.getServiceReference("org.osgi.service.http.HttpService");
      
      Object obj = null;
      if(sr != null) {
	obj = sr.getProperty("port");
	if(obj == null) {
	  obj = sr.getProperty("openPort");
	}
      }
      if(obj == null) {
	obj = System.getProperty("org.osgi.service.http.port", "80");
      }
      
      port = Integer.parseInt(obj.toString());
    } catch (Exception e) {
	e.printStackTrace();
    }
    
    String base = "http://localhost:" + port + "/axis/services/";
    return new JSOAPUI(base);
  }

  public void       disposeJComponent(JComponent comp) {
    JSOAPUI ui = (JSOAPUI)comp;

    ui.close();
  }

  public void       setBundleSelectionModel(BundleSelectionModel model) {
    // ignore
  }

  public void showBundle(Bundle b) {
      // NYI
  }

  public Icon       getLargeIcon() {
    return null;
  }

  public Icon       getSmallIcon() {
    return null;
  }

  public void       setTargetBundleContext(BundleContext bc) {
    // noop
  }
}
