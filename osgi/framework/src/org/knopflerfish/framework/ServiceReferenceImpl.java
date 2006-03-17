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

package org.knopflerfish.framework;

import java.lang.reflect.*;
import java.security.*;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.*;

/**
 * Implementation of the ServiceReference object.
 *
 * @see org.osgi.framework.ServiceReference
 * @author Jan Stein
 */
public class ServiceReferenceImpl implements ServiceReference
{

  /**
   * Link to registration object for this reference.
   */
  private ServiceRegistrationImpl registration;


  /**
   * Construct a ServiceReference based on given ServiceRegistration.
   *
   * @param reg ServiceRegistration pointed to be this reference.
   */
  ServiceReferenceImpl(ServiceRegistrationImpl reg) {
    registration = reg;
  }

  //
  // ServiceReference interface
  //

  /**
   * Get the value of a service's property.
   *
   * @see org.osgi.framework.ServiceReference#getProperty
   */
  public Object getProperty(String key) {
    synchronized (registration.properties) {
      if (registration.properties != null) {
	return cloneObject(registration.properties.get(key));
      } else {
	return null;
      }
    }
  }


  /**
   * Get the list of key names for the service's properties.
   *
   * @see org.osgi.framework.ServiceReference#getPropertyKeys
   */
  public String[] getPropertyKeys() {
    synchronized (registration.properties) {
      return registration.properties.keyArray();
    }
  }


  /**
   * Return the bundle which registered the service.
   *
   * @see org.osgi.framework.ServiceReference#getBundle
   */
  public Bundle getBundle() {
    return registration.bundle;
  }


  /**
   * Test if ServiceReferences points to same service.
   *
   * @see org.osgi.framework.ServiceReference
   */
  public boolean equals(Object o) {
    if (o instanceof ServiceReferenceImpl) {
      return registration == ((ServiceReferenceImpl)o).registration;
    }
    return false;
  }


  /**
   * Return a hashcode for the service.
   *
   * @see org.osgi.framework.ServiceReference
   */
  public int hashCode() {
    return registration.hashCode();
  }

  //
  // ServiceReference interface OSGI R2
  //

  /**
   * Return the bundles that are using the service wrapped by this
   * ServiceReference, i.e., whose usage count for this service
   * is greater than zero.
   *
   * @return array of bundles whose usage count for the service wrapped by
   * this ServiceReference is greater than zero, or <tt>null</tt> if no
   * bundles currently are using this service
   *
   * @since 1.1
   */
  public Bundle[] getUsingBundles() {
    synchronized (registration.properties) {
      if (registration.reference != null && registration.dependents.size() > 0) {
	Set bs = registration.dependents.keySet();
	Bundle[] res =  new Bundle[bs.size()];
	return (Bundle[])bs.toArray(res);
      } else {
	return null;
      }
    }
  }

  //
  // Package methods
  //

  /**
   * Get the service object.
   *
   * @param bundle requester of service.
   * @return Service requested or null in case of failure.
   */
  Object getService(final BundleImpl bundle) {
    Object s = null;
    synchronized (registration.properties) {
      if (registration.available) {
	Integer ref = (Integer)registration.dependents.get(bundle);
	if (ref == null) {
	  String[] classes = (String[])registration.properties.get(Constants.OBJECTCLASS);
	  bundle.framework.perm.checkGetServicePerms(classes);
	  if (registration.service instanceof ServiceFactory) {
	    try {
              s = bundle.framework.perm.callGetService((ServiceFactory)registration.service, bundle, registration);
	    } catch (Throwable pe) {
	      bundle.framework.listeners.frameworkError(registration.bundle, pe);
	      return null;
	    }
	    if (s == null) {
	      return null;
	    }
	    BundleClassLoader bcl = (BundleClassLoader)registration.bundle.getClassLoader();
	    for (int i = 0; i < classes.length; i++) {
	      Class c = null;
	      try {
		c = bcl.loadClass(classes[i], true);
	      } catch (ClassNotFoundException ignore) { } // Already checked
	      if (!c.isInstance(s)) {
		bundle.framework.listeners.frameworkError(registration.bundle, new BundleException("ServiceFactory produced an object that did not implement: " + classes[i]));
		return null;
	      }
	    }
	    registration.serviceInstances.put(bundle, s);
	  } else {
	    s = registration.service;
	  }
	  registration.dependents.put(bundle, new Integer(1));
	} else {
	  int count = ref.intValue();
	  registration.dependents.put(bundle, new Integer(count + 1));
	  if (registration.service instanceof ServiceFactory) {
	    s = registration.serviceInstances.get(bundle);
	  } else {
	    s = registration.service;
	  }
	}
      }
    }
    return s;
  }


  /**
   * Unget the service object.
   *
   * @param bundle Bundle who wants remove service.
   * @param checkRefCounter If true decrement refence counter and remove service
   *                        if we reach zero. If false remove service without
   *                        checking refence counter.
   * @return True if service was remove or false if only refence counter was
   *         decremented.
   */
  boolean ungetService(BundleImpl bundle, boolean checkRefCounter) {
    synchronized (registration.properties) {
      if (registration.reference != null) {
	Object countInteger = registration.dependents.remove(bundle);
	if (countInteger != null) {
	  int count = ((Integer) countInteger).intValue();
	  if (checkRefCounter && count > 1) {
	    registration.dependents.put(bundle, new Integer(count - 1));
	    return true;
	  } else {
	    Object sfi = registration.serviceInstances.remove(bundle);
	    if (sfi != null) {
	      try {
		((ServiceFactory)registration.service).ungetService(bundle, registration, sfi);
	      } catch (Throwable e) {
		bundle.framework.listeners.frameworkError(registration.bundle, e);
	      }
	    }
	  }
	}
      }
    }
    return false;
  }


  /**
   * Get all properties registered with this service.
   *
   * @return Dictionary containing properties or null
   *         if service has been removed. 
   */
  PropertiesDictionary getProperties() {
    return registration.properties;
  }

  //
  // Private methods
  //    
    
  /**
   * Clone object. Handles all service property types
   * and does this recursivly.
   *
   * @param bundle Object to clone.
   * @return Cloned object.
   */
  Object cloneObject(Object val) {
    if (val instanceof Object []) {
      val = ((Object [])val).clone();
      int len = Array.getLength(val);
      if (len > 0 && Array.get(val, 0).getClass().isArray()) {
	for (int i = 0; i < len; i++) {
	  Array.set(val, i, cloneObject(Array.get(val, i)));
	}
      }
    } else if (val instanceof boolean []) {
      val = ((boolean [])val).clone();
    } else if (val instanceof byte []) {
      val = ((byte [])val).clone();
    } else if (val instanceof char []) {
      val = ((char [])val).clone();
    } else if (val instanceof double []) {
      val = ((double [])val).clone();
    } else if (val instanceof float []) {
      val = ((float [])val).clone();
    } else if (val instanceof int []) {
      val = ((int [])val).clone();
    } else if (val instanceof long []) {
      val = ((long [])val).clone();
    } else if (val instanceof short []) {
      val = ((short [])val).clone();
    } else if (val instanceof Vector) {
      Vector c = (Vector)((Vector)val).clone();
      for (int i = 0; i < c.size(); i++) {
	c.setElementAt(cloneObject(c.elementAt(i)), i);
      }
      val = c;
    }
    return val;
  }

  public boolean isAssignableTo(Bundle bundle, String className) {
    int pos = className.lastIndexOf('.');
    if (pos != -1) {
      String name = className.substring(0, pos);
      Pkg p = registration.bundle.framework.packages.getPkg(name);
      if (p != null) {
        if (p.providers.size() > 1) {
          BundlePackages pkgExporter = registration.bundle.bpkgs.getProviderBundlePackages(name);
          BundlePackages bb = ((BundleImpl)bundle).bpkgs.getProviderBundlePackages(name);
          // TBD Should we fail if bundle doesn't have an import?
          return bb == null || pkgExporter == bb;
        } else {
          // Since we do not have multiple providers its no problem
          return true;
        }
      } else {
        // Not a package under package control. System package?
        if (name.startsWith("java.")) {
          return true;
        } else {
          return true;
          // return registration.bundle == bundle;
        }
      }
    }
    return false;
  }

}
