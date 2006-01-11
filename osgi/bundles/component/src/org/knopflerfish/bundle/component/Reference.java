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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import org.osgi.util.tracker.ServiceTracker;

public class Reference extends ServiceTracker {

  private boolean optional;
  private boolean multiple;
  private boolean dynamic;

  private Object bound;
  
  public Reference(String refName, Filter filter,
                   boolean optional, boolean multiple, boolean dynamic,
                   String bind, String unbind,
                   BundleContext bc) {

    super(bc, filter, null);
    this.optional = optional;
    this.multiple = multiple;
    this.dynamic = dynamic;
  }

  public Object addingService(ServiceReference ref, Object service) {
    boolean wasSatisfied = isSatisfied();
    if (bound != null && multiple) {
      // TODO: bind
    }
    Object obj = super.addingService(ref);
    if (!wasSatisfied && isSatisfied()) {
      // TODO: Report change
    }
    return obj;
  }

  /* public void modifiedService() */

  public void removedService(ServiceReference ref, Object service) {
    boolean wasSatisfied = isSatisfied();
    if (bound != null) {
      // TODO: unbind
    }
    super.removedService(ref, service);
    /* try to remove this service,
       possibly disabling the component */
    if (wasSatisfied && !isSatisfied()) {
      // TODO: Report change
    }
  }

  public boolean isSatisfied() {
    return getTrackingCount() > 0 || optional;
  }

  public void bind() {
    if (multiple) {
      // TODO: bind all from getServiceReferences()/getServices()
      // Is the order the same in getServiceReferences() and getServices()?
      // TODO: set bound
    } else { // unary
      bound = getServiceReference(); // TODO: or corresponding object?
      // TODO: bind bound or corresponding object
    }
  }

  
  public void unbind() {
    if (bound == null) return;
    if (multiple) {
      // TODO: unbind all from getServiceReferences()/getServices() 
    } else {
      // TODO: unbind bound
    }
    bound = null;
  }

}
