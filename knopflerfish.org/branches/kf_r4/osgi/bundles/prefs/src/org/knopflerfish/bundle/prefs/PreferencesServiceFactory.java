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

package org.knopflerfish.bundle.prefs;

import org.osgi.framework.*;
import org.osgi.service.prefs.*;
import org.knopflerfish.service.log.LogRef;
import java.util.*;


public class PreferencesServiceFactory implements ServiceFactory {
  ServiceRegistration reg = null;

  // Bundle -> PreferencesServiceImpl
  Map prefsMap = new HashMap();
  
  PreferencesServiceFactory() {
  }
  
  public Object getService(Bundle bundle, 
			   ServiceRegistration reg) {
    synchronized(prefsMap) {
      PreferencesServiceImpl prefs 
	= (PreferencesServiceImpl)prefsMap.get(bundle);
      
      if(prefs == null) {
	prefs = new PreferencesServiceImpl(bundle);
	prefsMap.put(bundle, prefs);
      }
      return prefs;
    }
  }

  public void ungetService(Bundle bundle, 
			   ServiceRegistration registration, 
			   Object service) {
    synchronized(prefsMap) {
      PreferencesServiceImpl prefs 
	= (PreferencesServiceImpl)prefsMap.get(bundle);
      
      if(prefs != null) {
	prefs.flush();
	prefsMap.remove(bundle);
      } else {
	Activator.log.warn("No prefs for unget from bundle #" + bundle.getBundleId());
      }
    }
  }

  void register() {
    if(reg == null) {
      Hashtable props = new Hashtable();
      reg = Activator.bc.registerService(PreferencesService.class.getName(),
					 this, 
					 props);
    }
  }
}
