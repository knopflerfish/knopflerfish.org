/*
 * Copyright (c) 2003-2005, KNOPFLERFISH project
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

import java.security.*;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.*;


/**
 * Implementation of the ServiceRegistration object.
 *
 * @see org.osgi.framework.ServiceRegistration
 * @author Jan Stein
 */
public class ServiceRegistrationImpl implements ServiceRegistration
{
  /**
   * Bundle registering this service.
   */
  BundleImpl bundle;

  /**
   * Service or ServiceFactory object.
   */
  Object service;

  /**
   * Reference object to this service registration.
   */
  ServiceReferenceImpl reference;

  /**
   * Service properties.
   */
  PropertiesDictionary properties;

  /**
   * Bundles dependent on this service. Integer is used as
   * reference counter, counting number of unbalanced getService().
   */
  HashMap /*Bundle->Integer*/ dependents = new HashMap();

  /**
   * Object instances that factory has produced.
   */
  HashMap /*Bundle->Object*/ serviceInstances = new HashMap();

  /**
   * Is service available.
   */
  boolean available;

  /**
   * Lock object for synchronous event delivery.
   */
  private Object eventLock = new Object();

  /**
   * Avoid recursive unregistrations.
   */
  //private boolean unregistering = false;


  /**
   * Construct a ServiceRegistration for a registered service.
   *
   * @param b Bundle providing service.
   * @param s Service object.
   * @param props Properties describing service.
   */
  ServiceRegistrationImpl(BundleImpl b, Object s, PropertiesDictionary props) {
    bundle = b;
    service = s;
    properties = props;
    reference = new ServiceReferenceImpl(this);
    available = true;
  }

  //
  // ServiceRegistration interface
  //

  /**
   * Returns a ServiceReference object for this registration.
   *
   * @see org.osgi.framework.ServiceRegistration#getReference
   */
  public ServiceReference getReference() {
    ServiceReference res = reference;
    if (res != null) {
      return res;
    } else {
      throw new IllegalStateException("Service is unregistered");
    }
  }


  /**
   * Update the properties associated with this service.
   *
   * @see org.osgi.framework.ServiceRegistration#setProperties
   */
  public void setProperties(Dictionary props) {
    synchronized (eventLock) {
      synchronized (properties) {
	if (available) {
	  String[] classes = (String[])properties.get(Constants.OBJECTCLASS);
	  Long sid = (Long)properties.get(Constants.SERVICE_ID);
	  properties = new PropertiesDictionary(props, classes, sid);
	} else {
	  throw new IllegalStateException("Service is unregistered");
	}
      }
      bundle.framework.listeners.serviceChanged(new ServiceEvent(ServiceEvent.MODIFIED, reference));
    }
  }

  /**
   * Unregister the service.
   *
   * @see org.osgi.framework.ServiceRegistration#unregister
   */
  public void unregister() {
    synchronized (eventLock) {

	    if(!Framework.UNREGISTERSERVICE_VALID_DURING_UNREGISTERING)
	    {
		    unregister_removeService();
	    }

	    bundle.framework.listeners.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference));

	    if(Framework.UNREGISTERSERVICE_VALID_DURING_UNREGISTERING)
	    {
		    unregister_removeService();
	    }
    }
    bundle.framework.perm.callUnregister0(this);
    synchronized (properties) {
      bundle = null;
      dependents = null;
      reference = null;
      service = null;
      serviceInstances = null;
    }
  }

  void unregister0() {
    for (Iterator i = serviceInstances.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry)i.next();
      try {
        ((ServiceFactory)service).ungetService((Bundle)e.getKey(), this, e.getValue());
      } catch (Throwable ue) {
        bundle.framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, ue));
      }
    }
  }

  //
  // Framework internal
  //

  /**
   * Check if a bundle uses this service
   *
   * @param b Bundle to check
   * @return true if bundle uses this service
   */
  boolean isUsedByBundle(Bundle b) {
    Map deps = dependents;
    if (deps != null) {
      return deps.containsKey(b);
    } else {
      return false;
    }
  }

  /**
   * Remove the service registration for this service.
   */
  private void unregister_removeService()
  {
      synchronized (properties) {
	if (available) {
	  bundle.framework.services.removeServiceRegistration(this);
	  available = false;
	} else {
	  throw new IllegalStateException("Service is unregistered");
	}
      }
  }

}
