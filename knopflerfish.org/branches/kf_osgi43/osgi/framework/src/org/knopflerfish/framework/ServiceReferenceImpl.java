/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;

/**
 * Implementation of the ServiceReference object.
 *
 * @see org.osgi.framework.ServiceReference
 * @author Jan Stein
 */
public class ServiceReferenceImpl<S> implements ServiceReference<S>
{

  /**
   * Link to registration object for this reference.
   */
  private final ServiceRegistrationImpl<S> registration;


  /**
   * Construct a ServiceReference based on given ServiceRegistration.
   *
   * @param reg ServiceRegistration pointed to be this reference.
   */
  ServiceReferenceImpl(ServiceRegistrationImpl<S> reg) {
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
    return cloneObject(registration.getProperty(key));
  }


  /**
   * Get the list of key names for the service's properties.
   *
   * @see org.osgi.framework.ServiceReference#getPropertyKeys
   */
  public String[] getPropertyKeys() {
    return registration.getProperties().keyArray();
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
  @Override
  public boolean equals(Object o) {
    if (o instanceof ServiceReferenceImpl) {
      final ServiceReferenceImpl<?> that = (ServiceReferenceImpl<?>)o;
      return registration == that.registration;
    }
    return false;
  }

  /**
   * Compare two ServiceReferences
   *
   * @see org.osgi.framework.ServiceReference
   */
  public int compareTo(Object obj) {
    final ServiceReference<?> that = (ServiceReference<?>)obj;

    boolean sameFw = false;
    if (that instanceof ServiceReferenceImpl) {
      final ServiceReferenceImpl<?> thatImpl = (ServiceReferenceImpl<?>) that;
      sameFw
        = this.registration.fwCtx == thatImpl.registration.fwCtx;
    }
    if (!sameFw) {
      throw new IllegalArgumentException("Can not compare service references "
                                         +"belonging to different framework "
                                         +"instances (this=" +this +", other="
                                         +that +").");
    }

    final Object ro1 = this.getProperty(Constants.SERVICE_RANKING);
    final Object ro2 = that.getProperty(Constants.SERVICE_RANKING);
    final int r1 = (ro1 instanceof Integer) ? ((Integer)ro1).intValue() : 0;
    final int r2 = (ro2 instanceof Integer) ? ((Integer)ro2).intValue() : 0;

    if (r1 != r2) {
      // use ranking if ranking differs
      return r1 < r2 ? -1 : 1;
    } else {
      final Long id1 = (Long)this.getProperty(Constants.SERVICE_ID);
      final Long id2 = (Long)that.getProperty(Constants.SERVICE_ID);

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
  @Override
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
    return registration.getUsingBundles();
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
  S getService(final BundleImpl bundle) {
    bundle.fwCtx.perm.checkGetServicePerms(this);
    return registration.getService(bundle);
  }


  /**
   * Unget the service object.
   *
   * @param bundle Bundle who wants remove service.
   * @return True if service was used, otherwise false.
   */
  boolean ungetService(BundleImpl bundle) {
    return registration.ungetService(bundle, true);
  }


  /**
   * Get all properties registered with this service.
   *
   * @return Dictionary containing properties or null
   *         if service has been removed.
   */
  PropertiesDictionary getProperties() {
    return registration.getProperties();
  }

  //
  // Private methods
  //

  /**
   * Clone object. Handles all service property types
   * and does this recursively.
   *
   * @param bundle Object to clone.
   * @return Cloned object.
   */
  Object cloneObject(Object val) {
    if (val instanceof Object []) {
      val = ((Object [])val).clone();
      final int len = Array.getLength(val);
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
      @SuppressWarnings({ "rawtypes", "unchecked" })
      final Vector<Object> c = (Vector)((Vector)val).clone();
      for (int i = 0; i < c.size(); i++) {
        c.setElementAt(cloneObject(c.elementAt(i)), i);
      }
      val = c;
    }
    return val;
  }


  public boolean isAssignableTo(Bundle bundle, String className) {
    final BundleImpl sBundle = registration.bundle;
    if (sBundle == null) {
        throw new IllegalStateException("Service is unregistered");
    }
    final FrameworkContext fwCtx = sBundle.fwCtx;
    if (((BundleImpl)bundle).fwCtx != fwCtx) {
      throw new IllegalArgumentException("Bundle is not from same framework as service");
    }
    // Check if bootdelegated
    if (fwCtx.isBootDelegated(className)) {
      return true;
    }
    final int pos = className.lastIndexOf('.');
    if (pos != -1) {
      final String name = className.substring(0, pos);
      final Pkg p = fwCtx.packages.getPkg(name);
      // Is package exported by a bundle
      if (p != null) {
        final BundlePackages rbp = sBundle.current().bpkgs;
        final BundlePackages pkgExporter = rbp.getProviderBundlePackages(name);
        List<BundleGeneration> pkgProvider;
        if (pkgExporter == null) {
          // Package not imported by provide, is it required
          pkgProvider = rbp.getRequiredBundleGenerations(name);
        } else {
          pkgProvider = new ArrayList<BundleGeneration>(1);
          pkgProvider.add(pkgExporter.bg);
        }
        final BundlePackages bb = ((BundleImpl)bundle).current().bpkgs;
        final BundlePackages bbp = bb.getProviderBundlePackages(name);
        List<BundleGeneration> pkgConsumer;
        if (bbp == null) {
          // Package not imported by bundle, is it required
          pkgConsumer = bb.getRequiredBundleGenerations(name);
        } else {
          pkgConsumer = new ArrayList<BundleGeneration>(1);
          pkgConsumer.add(bbp.bg);
        }
        if (pkgConsumer == null) {
          // NYI! Check dynamic import?
          if (bb.isExported(name)) {
            // If bundle only exports package, then return true if
            // bundle is provider.
            return pkgProvider!=null ? pkgProvider.contains(bb.bg) : true;
          } else {
            // If bundle doesn't import or export package, then return true and
            // assume that the bundle only uses reflection to access service.
            return true;
          }
        } else if (pkgProvider == null) {
          // Package not imported by registrar. E.g. proxy registration.
          final Object sService = registration.service;
          if (sService == null) {
            throw new IllegalStateException("Service is unregistered");
          }
          if (p.providers.size() == 1) {
            // Only one version available, allow.
            return true;
          } else if (sService instanceof ServiceFactory) {
            // Factory, allow.
            return true;
          } else {
            // Use the classloader of bundle to load the class, then check
            // if the service's class is assignable.
            final ClassLoader bCL = bb.getClassLoader();
            if (bCL!=null) {
              try {
                final Class<?> bCls = bCL.loadClass(className);
                // NYI, Handle Service Factories.
                return bCls.isAssignableFrom(sService.getClass());
              } catch (final Exception e) {
                // If we can not load, assume that we are just a proxy.
                return true;
              }
            }
          }
          // Fallback: Always OK when singleton provider of the package
        } else { // Package imported by both parties
          // Return true if we have same provider as service.
          for (final Object element : pkgProvider) {
            if (pkgConsumer.contains(element)) {
              return true;
            }
          }
        }
      } else {
        // Not a package under package control. System package?
        if (name.startsWith("java.") || sBundle == bundle) {
          return true;
        } else {
          // NYI! We have a private service, check if bundle can use it.
          // Now, allow it to handle reflection of service.
          return true;
        }
      }
    }
    return false;
  }

}
