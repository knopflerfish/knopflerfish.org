/*
 * Copyright (c) 2006, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;


public class DelayedComponent extends Component {

  private int refCount;

  public DelayedComponent(Config config, 
			  Dictionary overriddenProps) {
    super(config, overriddenProps);

    refCount = 0;
  }

  public void satisfied() {
    registerService();
  }

  public void unsatisfied() {
    unregisterService();
  }

  public Object getService(Bundle bundle, ServiceRegistration reg) {
   
    super.getService(bundle, reg);
    
    if (config.isServiceFactory()) {
      Component component =
	config.createComponent();

      
      // TODO: vad händer egentligen här? Är det bara att köra config.createComponent och sedan köra return componentgetService(bundle, reg) på den? Måste nog göra nåt mer annars kommer den loopa igen eftersom isServiceFactory == true.
      

      if (config.isSatisfied()) {
	component.satisfied();
	return component.getService(bundle, reg);
      } else {
	// throw new ComponentException("Could not satisfy blalba, TODO: read more on this.");
      }
    }

    if (activate()) {
      
	refCount++;
	return getInstance();
	
    } else {
      
      unregisterService();
      return null;
      
    } 
  }

  public void ungetService(Bundle bundle, ServiceRegistration reg, Object instance) {
    super.ungetService(bundle, reg, instance);
    
    if (refCount == 0)
      return ;

    refCount--;
    
    if (refCount == 0) {
      deactivate();
    }
  }
}
				
