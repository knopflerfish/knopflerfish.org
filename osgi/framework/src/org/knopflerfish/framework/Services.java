/*
 * Copyright (c) 2003-2016, KNOPFLERFISH project
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;


/**
 * Here we handle all the services that are registered in framework.
 *
 * @author Jan Stein, Philippe Laporte, Gunnar Ekolin
 */
class Services {

  /**
   * All registered services in the current framework.
   * Mapping of registered service to class names under which service
   * is registered.
   */
  HashMap<ServiceRegistrationImpl<?>, String[]> services
    = new HashMap<ServiceRegistrationImpl<?>, String[]>();

  /**
   * Mapping of class name to registered service.
   * The List of registered service are order in with highest
   * ranked service first.
   */
  private final HashMap<String,List<ServiceRegistrationImpl<?>>> classServices
    = new HashMap<String, List<ServiceRegistrationImpl<?>>>();

  /**
   * Handle to secure call class.
   */
  private PermissionOps secure;


  FrameworkContext framework;

  Services(FrameworkContext fwCtx, PermissionOps perm) {
    this.framework = fwCtx;
    secure = perm;
  }

  void clear()
  {
    services.clear();
    classServices.clear();
    secure = null;
    framework = null;
  }

  /**
   * Register a service in the framework wide register.
   *
   * @param bundle The bundle registering the service.
   * @param classes The class names under which the service can be located.
   * @param service The service object.
   * @param properties The properties for this service.
   * @return A {@link ServiceRegistration} object.
   * @exception java.lang.IllegalArgumentException If one of the following is true:
   * <ul>
   * <li>The service object is null.</li>
   * <li>The defining class of the service parameter is not owned by the bundle.</li>
   * <li>The service parameter is not a ServiceFactory and is not an
   * instance of all the named classes in the classes parameter.</li>
   * </ul>
   */
  @SuppressWarnings("deprecation")
  ServiceRegistration<?> register(BundleImpl bundle,
                                  String[] classes,
                                  Object service,
                                  Dictionary<String, ?> properties)
  {
    if (service == null) {
      throw new IllegalArgumentException("Can't register null as a service");
    }
    String scope;
    if (service instanceof ServiceFactory) {
      scope = service instanceof PrototypeServiceFactory ? Constants.SCOPE_PROTOTYPE : Constants.SCOPE_BUNDLE;
    } else {
      scope = Constants.SCOPE_SINGLETON;
    }
    // Check if service implements claimed classes and that they exist.
    for (final String cls : classes) {
      if (cls == null) {
        throw new IllegalArgumentException("Can't register as null class");
      }
      secure.checkRegisterServicePerm(cls);
      if (bundle.id != 0) {
        if (cls.equals(org.osgi.service.packageadmin.PackageAdmin.class.getName())) {
          throw new IllegalArgumentException
            ("Registeration of a PackageAdmin service is not allowed");
        }
        if (cls.equals(PermissionAdmin.class.getName())) {
          throw new IllegalArgumentException
            ("Registeration of a PermissionAdmin service is not allowed");
        }
        if (cls.equals(ConditionalPermissionAdmin.class.getName())) {
          throw new IllegalArgumentException
            ("Registeration of a ConditionalPermissionAdmin service is not allowed");
        }
      }
      if (scope == Constants.SCOPE_SINGLETON) {
        if (!checkServiceClass(service, cls)) {
          throw new IllegalArgumentException
            ("Service object is not an instance of " + cls);
        }
      }
    }

    @SuppressWarnings("rawtypes")
    final ServiceRegistrationImpl<?> res =
      new ServiceRegistrationImpl(bundle, service, scope,
                                  new PropertiesDictionary(properties, classes, null, new Long(bundle.id), scope));
    synchronized (this) {
      services.put(res, classes);
      for (final String clazz : classes) {
        List<ServiceRegistrationImpl<?>> s
          = classServices.get(clazz);
        if (s == null) {
          s = new ArrayList<ServiceRegistrationImpl<?>>(1);
          classServices.put(clazz, s);
        }
        final int ip = Math.abs(Util.binarySearch(s, sComp, res) + 1);
        s.add(ip, res);
      }
    }
    final ServiceReference<?> r = res.getReference();
    bundle.fwCtx.perm
      .callServiceChanged(bundle.fwCtx,
                          bundle.fwCtx.listeners.getMatchingServiceListeners(r),
                          new ServiceEvent(ServiceEvent.REGISTERED, r),
                          null);
    return res;
  }


  /**
   * Service ranking changed, reorder registered services
   * according to ranking.
   *
   * @param serviceRegistration The serviceRegistration object.
   * @param rank New rank of object.
   */
  synchronized void updateServiceRegistrationOrder(ServiceRegistrationImpl<?> sr,
                                                   String[] classes)
  {
    for (final String clazz : classes) {
      final List<ServiceRegistrationImpl<?>> s = classServices.get(clazz);
      s.remove(sr);
      s.add(Math.abs(Util.binarySearch(s, sComp, sr) + 1), sr);
    }
  }


  /**
   * Checks that a given service object is an instance of the given
   * class name.
   *
   * @param service The service object to check.
   * @param cls     The class name to check for.
   * @throws IllegalArgumentException if the given class is not an
   *            instance of the given class name.
   */
  boolean checkServiceClass(Object service, String cls)
  {
    final Class<?> sc = service.getClass();
    final ClassLoader scl = secure.getClassLoaderOf(sc);
    Class<?> c = null;
    boolean ok = false;
    try {
      if (scl != null) {
        c = scl.loadClass(cls);
      } else {
        c = Class.forName(cls);
      }
      ok = c.isInstance(service);
    } catch (final ClassNotFoundException e) {
      for (Class<?> csc = sc; csc != null; csc = csc.getSuperclass()) {
        if (cls.equals(csc.getName())) {
          ok = true;
          break;
        } else {
          final Class<?> [] ic = csc.getInterfaces();
          for (int iic = ic.length - 1; iic >= 0; iic--) {
            if (cls.equals(ic[iic].getName())) {
              ok = true;
              break;
            }
          }
        }
      }
    }
    return ok;
  }


  /**
   * Get all service implementing a certain class.
   * Only used internally by framework.
   *
   * @param clazz The class name of requested service.
   * @return A sorted list of {@link ServiceRegistrationImpl} objects
   *         or null if no services is available.
   */
  synchronized List<ServiceRegistrationImpl<?>> get(String clazz) {
    final List<ServiceRegistrationImpl<?>> v = classServices.get(clazz);
    if (v != null) {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      final List<ServiceRegistrationImpl<?>> res
        = (List<ServiceRegistrationImpl<?>>) ((ArrayList) v).clone();
      return res;
    }
    return null;
  }


  /**
   * Get a service implementing a certain class.
   *
   * @param bundle bundle requesting reference
   * @param clazz The class name of requested service.
   * @param doAssignableToTest whether to if the bundle that registered the service
   * referenced by this ServiceReference and the specified bundle are both wired to
   * same source for the registration class.
   * @return A {@link ServiceReference} object.
   */
  synchronized ServiceReference<?> get(BundleImpl bundle, String clazz, boolean doAssignableToTest) {
    try {
      final ServiceReference<?>[] srs = get(clazz, null, bundle, doAssignableToTest);
      if (framework.debug.service_reference) {
        framework.debug.println("get service ref " + clazz + " for bundle " + bundle.location
                                + " = " + (srs != null ? srs[0] : null));
      }
      if (srs != null) {
        return srs[0];
      }
    } catch (final InvalidSyntaxException _never) { }
    return null;
  }


  /**
   * Get all services implementing a certain class and then
   * filter these with a property filter.
   *
   * @param clazz The class name of requested service.
   * @param filter The property filter.
   * @param bundle bundle requesting reference.
   * @param doAssignableToTest whether to if the bundle that registered the service
   * referenced by this ServiceReference and the specified bundle are both wired to
   * same source for the registration class.
   * @return An array of {@link ServiceReference} object.
   */
  synchronized ServiceReference<?>[] get(String clazz, String filter, BundleImpl bundle, boolean doAssignableToTest)
    throws InvalidSyntaxException {
    Iterator<ServiceRegistrationImpl<?>> s;
    LDAPExpr ldap = null;
    if (clazz == null) {
      if (filter != null) {
        ldap = new LDAPExpr(filter);
        final Set<String> matched = ldap.getMatchedObjectClasses();
        if (matched != null) {
          List<ServiceRegistrationImpl<?>> v = null;
          boolean vReadOnly = true;;
          for (final String match : matched) {
            final List<ServiceRegistrationImpl<?>> cl = classServices.get(match);
            if (cl != null) {
              if (v == null) {
                v = cl;
              } else {
                if (vReadOnly) {
                  v = new ArrayList<ServiceRegistrationImpl<?>>(v);
                  vReadOnly = false;
                }
                v.addAll(cl);
              }
            }
          }
          if (v != null) {
            s = v.iterator();
          } else {
            return null;
          }
        } else {
          s = services.keySet().iterator();
        }
      } else {
        s = services.keySet().iterator();
      }
    } else {
      final List<ServiceRegistrationImpl<?>> v = classServices.get(clazz);
      if (v != null) {
        s = v.iterator();
      } else {
        return null;
      }
      if (filter != null) {
        ldap = new LDAPExpr(filter);
      }
    }
    Collection<ServiceReference<?>> res
      = new ArrayList<ServiceReference<?>>();
    while (s.hasNext()) {
      final ServiceRegistrationImpl<?> sr = s.next();
      ServiceReference<?> sri = sr.getReference();
      if (!secure.okGetServicePerms(sri)) {
        continue; //sr not part of returned set
      }
      if (filter == null || ldap.evaluate(sr.getProperties(), false)) {
        if (doAssignableToTest) {
          final String[] classes = services.get(sr);
          for (int i = 0; i < classes.length; i++) {
            if (!sri.isAssignableTo(bundle, classes[i])){
              sri = null;
              break;
            }
          }
        }
        if (sri != null) {
          res.add(sri);
        }
      }
    }
    if (res.isEmpty()) {
      return null;
    } else {
      ArrayList<ServiceReference<?>> allSaved = null;
      if (bundle == framework.systemBundle) {
        allSaved = new ArrayList<ServiceReference<?>>(res);
      }
      if (doAssignableToTest) {
        framework.serviceHooks.filterServiceReferences(bundle.bundleContext,
                                                clazz, filter, false, res);
      } else {
        framework.serviceHooks.filterServiceReferences(null, clazz, filter, true, res);
      }
      if (allSaved != null) {
        res = allSaved;
      }
      if (res.isEmpty()) {
        return null;
      } else {
        return res.toArray(new ServiceReference [res.size()]);
      }
    }
  }


  /**
   * Remove a registered service.
   *
   * @param sr The ServiceRegistration object that is registered.
   */
  synchronized void removeServiceRegistration(ServiceRegistrationImpl<?> sr) {
    final String[] classes = (String[]) sr.getProperty(Constants.OBJECTCLASS);
    services.remove(sr);
    for (final String clazz : classes) {
      final List<ServiceRegistrationImpl<?>> s = classServices.get(clazz);
      if (s.size() > 1) {
        s.remove(sr);
      } else {
        classServices.remove(clazz);
      }
    }
  }


  /**
   * Get all services that a bundle has registered.
   *
   * @param b The bundle
   * @return A set of {@link ServiceRegistration} objects
   */
  synchronized Set<ServiceRegistrationImpl<?>> getRegisteredByBundle(Bundle b) {
    final HashSet<ServiceRegistrationImpl<?>> res = new HashSet<ServiceRegistrationImpl<?>>();
    for (final ServiceRegistrationImpl<?> sr : services.keySet()) {
      if (sr.bundle == b) {
        res.add(sr);
      }
    }
    return res;
  }


  /**
   * Get all services that a bundle uses.
   *
   * @param b The bundle
   * @return A set of {@link ServiceRegistration} objects
   */
  synchronized Set<ServiceRegistrationImpl<?>> getUsedByBundle(Bundle b) {
    final HashSet<ServiceRegistrationImpl<?>> res = new HashSet<ServiceRegistrationImpl<?>>();
    for (final ServiceRegistrationImpl<?> sr : services.keySet()) {
      if (sr.isUsedByBundle(b)) {
        res.add(sr);
      }
    }
    return res;
  }


  /**
   * Get all services registered.
   *
   * @return A set of {@link ServiceRegistration} objects
   */
  synchronized Set<ServiceRegistrationImpl<?>> getAllRegistered() {
    return new HashSet<ServiceRegistrationImpl<?>>(services.keySet());
  }


  static final Util.Comparator<ServiceRegistrationImpl<?>,ServiceRegistrationImpl<?>> sComp
  = new Util.Comparator<ServiceRegistrationImpl<?>, ServiceRegistrationImpl<?>>() {
      /**
       * Name compare two ServiceRegistrationImpl objects according
       * to the ServiceReference compareTo.
       *
       * @param a ServiceRegistrationImpl to compare.
       * @param b ServiceRegistrationImpl to compare.
       * @return Return 0 if equals, negative if first object is less than second
       *         object and positive if first object is larger then second object.
       */
    public int compare(ServiceRegistrationImpl<?> a,
                       ServiceRegistrationImpl<?> b)
    {
      return a.reference.compareTo(b.reference);
    }
    };

}
