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

package org.knopflerfish.bundle.jini;

import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class JiniExporter implements ServiceTrackerCustomizer{

  public Object addingService(ServiceReference serviceReference) {
    RMICodeBaseService.getRMICodeBaseService().setCodebaseForBundle(
        serviceReference.getBundle()
    );
    JiniExportedService jiniExportedService = null;
    try {
      jiniExportedService =
          new JiniExportedService(serviceReference);
    }
    catch (Exception ex) {
      Debug.printDebugInfo(10,"Could not export service to jini : " +
                         serviceReference, ex);
    }
    return jiniExportedService;
  }

  public void modifiedService(ServiceReference serviceReference, Object object) {
    JiniExportedService jiniExportedService = (JiniExportedService) object;
    if (jiniExportedService != null)
      jiniExportedService.update(serviceReference);
  }

  public void removedService(ServiceReference serviceReference, Object object) {
    JiniExportedService jiniExportedService = (JiniExportedService) object;
    if (jiniExportedService != null) jiniExportedService.cancel();
    RMICodeBaseService.getRMICodeBaseService().removeCodebaseForBundle(
        serviceReference.getBundle()
    );
  }
}
