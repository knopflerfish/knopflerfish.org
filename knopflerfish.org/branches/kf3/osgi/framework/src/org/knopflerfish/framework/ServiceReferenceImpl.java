/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
   * Compare two ServiceReferences
   *
   * @see org.osgi.framework.ServiceReference
   */
  public int compareTo(Object obj) {
    ServiceReference that = (ServiceReference)obj;

    boolean sameFw = false;
    if (that instanceof ServiceReferenceImpl) {
      ServiceReferenceImpl thatImpl = (ServiceReferenceImpl) that;
      sameFw
        = this.registration.bundle.fwCtx == thatImpl.registration.bundle.fwCtx;
    }
    if (!sameFw) {
      throw new IllegalArgumentException("Can not compare service references "
                                         +"belonging to different framework "
                                         +"instances (this=" +this +", other="
                                         +that +").");
    }

    Object ro1 = this.getProperty(Constants.SERVICE_RANKING);
    Object ro2 = that.getProperty(Constants.SERVICE_RANKING);
    int r1 = (ro1 instanceof Integer) ? ((Integer)ro1).intValue() : 0;
    int r2 = (ro2 instanceof Integer) ? ((Integer)ro2).intValue() : 0;

    if (r1 != r2) {
      // use ranking if ranking differs
      return r1 < r2 ? -1 : 1;
    } else {
      Long id1 = (Long)this.getProperty(Constants.SERVICE_ID);
      Long id2 = (Long)that.getProperty(Constants.SERVICE_ID);

      // otherwise compare using IDs,
      // is less than if it has a higher ID.
      return id2.compareTo(id1);
    }
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
      if (registration.available
          && (!registration.unregistering
              || bundle.fwCtx.props.UNREGISTERSERVICE_VALID_DURING_UNREGISTERING) ) {
        Integer ref = (Integer)registration.dependents.get(bundle);
        if (ref == null) {
          String[] classes =
            (String[])registration.properties.get(Constants.OBJECTCLASS);
          bundle.fwCtx.perm.checkGetServicePerms(classes);
          registration.dependents.put(bundle, new Integer(1));
          if (registration.service instanceof ServiceFactory) {
            try {
              s = bundle.fwCtx.perm.callGetService
                ((ServiceFactory)registration.service, bundle, registration);
            } catch (Throwable pe) {
              bundle.fwCtx.listeners.frameworkError
                (registration.bundle,
                 new ServiceException("ServiceFactory throw an exception",
                                      ServiceException.FACTORY_EXCEPTION, pe));
              return null;
            }
            if (s == null) {
              bundle.fwCtx.listeners.frameworkError
                (registration.bundle,
                 new ServiceException("ServiceFactory produced null",
                                      ServiceException.FACTORY_ERROR));
              return null;
            }
            final Class sc = s.getClass();
            for (int i = 0; i < classes.length; i++) {
              final String cls = classes[i];
              if (!Services.checkServiceClass(s, cls)) {
                bundle.fwCtx.listeners.frameworkError
                  (registration.bundle,
                   new ServiceException("ServiceFactory produced an object " +
                                        "that did not implement: " + cls,
                                        ServiceException.FACTORY_ERROR));
                return null;
              }
            }
            registration.serviceInstances.put(bundle, s);
          } else {
            s = registration.service;
          }
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
      boolean hadReferences = false;
      if (registration.reference != null) {
        boolean removeService = false;

        Object countInteger = registration.dependents.get(bundle);
        int count = countInteger != null ? ((Integer) countInteger).intValue() : 0;
        if (count > 0) {
          hadReferences = true;
        }

        if(checkRefCounter) {
            if (count > 1) {
              registration.dependents.put(bundle, new Integer(count - 1));
            } else if(count == 1) {
                removeService = true;
            }
        } else {
          removeService = true;
        }

        if(removeService) {
          Object sfi = registration.serviceInstances.remove(bundle);
          if (sfi != null) {
            try {
              ((ServiceFactory) registration.service).ungetService(bundle,
                  registration, sfi);
            } catch (Throwable e) {
              bundle.fwCtx.listeners.frameworkError(registration.bundle,
                  e);
            }
          }
          registration.dependents.remove(bundle);
        }
      }
      return hadReferences;
    }
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
      final String name = className.substring(0, pos);
      final Pkg p = registration.bundle.fwCtx.packages.getPkg(name);
      // NYI! check if bootdelegated=
      if (p != null) {
        final BundlePackages pkgExporter
          = registration.bundle.bpkgs.getProviderBundlePackages(name);
        final BundlePackages bb = ((BundleImpl)bundle).bpkgs;
        final BundlePackages bbp = bb.getProviderBundlePackages(name);
        if (bbp == null) {
          // Package not imported by bundle

          if (bb.getExport(name) != null) {
            // If bundle only exports package, then return true if
            // bundle is provider.
            return bb == pkgExporter;

          } else {
            // If bundle doesn't import or export package, then return true and
            // assume that the bundle only uses reflection to access service.
            return true;
          }
        } else if (pkgExporter == null) {
          // Package not imported by registrar. E.g. proxy registration.

          // Use the classloader of bundle to load the class, then check
          // if the service's class is assignable.
          ClassLoader bCL = bbp.getClassLoader();
          if (bCL!=null) {
            try {
              Class bCls = bCL.loadClass(className);
              return bCls.isAssignableFrom(registration.service.getClass());
            } catch (Exception e) {
              //e.printStackTrace();
            }
          }
          // Fallback: Allways Ok when singleton provider of the package
          return p.providers.size()==1;
        } else { // Package imported by both parties
          // Return true if we have same provider as service.
          return pkgExporter == bbp;
        }
      } else {
        // Not a package under package control. System package?
        if (name.startsWith("java.")) {
          return true;
        } else {
          // NYI! Check if pkg comes from system or framework.
          return true;
          // return registration.bundle == bundle;
        }
      }
    }
    return false;
  }

}
