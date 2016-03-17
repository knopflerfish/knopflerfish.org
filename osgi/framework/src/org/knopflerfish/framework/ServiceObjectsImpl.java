/*
 * Copyright (c) 2016-2016, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;


/**
 * Implementation of the ServiceObjects object.
 *
 * @see org.osgi.framework.ServiceObjects
 * @author Jan Stein
 */
public class ServiceObjectsImpl<S> implements ServiceObjects<S>
{
  final BundleContextImpl bc;
  final ServiceReferenceImpl<S> sr;

  ServiceObjectsImpl(BundleContextImpl bc, ServiceReferenceImpl<S> sr) {
    this.bc = bc;
    this.sr = sr;
  }

  /**
   * Returns a service object for the {@link #getServiceReference()
   * associated} service.
   * 
   * @see org.osgi.framework.ServiceObjects#getService()
   */
  public S getService() {
    bc.checkValid(); 
    return sr.getService(bc.bundle, true);
  }

  /**
   * Releases a service object for the {@link #getServiceReference()
   * associated} service.
   *
   * @see org.osgi.framework.ServiceObjects#ungetService()
   */
  public void ungetService(S service) {
    bc.checkValid(); 
    if (service == null) {
      throw new IllegalArgumentException("null Service is not valid input to ungetService(Object)");
    }
    sr.ungetService(bc.bundle, service);
  }

  /**
   * Returns the {@link ServiceReference} for the service associated with this
   * {@code ServiceObjects} object.
   * 
   * @see org.osgi.framework.ServiceObjects#getServiceReference()
   */
  public ServiceReference<S> getServiceReference() {
    return sr;
  }

}
