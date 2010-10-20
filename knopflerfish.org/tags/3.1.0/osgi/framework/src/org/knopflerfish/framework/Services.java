/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.util.Set;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;


/**
 * Here we handle all the services that are registered in framework.
 *
 * @author Jan Stein, Philippe Laporte, Gunnar Ekolin
 */
class Services {

  /**
   * All registered services in the current framework.
   * Mapping of registered service to class names under which service
   * is registerd.
   */
  HashMap /* serviceRegistration -> Array of Class Names */ services = new HashMap();

  /**
   * Mapping of classname to registered service.
   * The List of registered service are order in with highest
   * ranked service first.
   */
  private HashMap /* String->List(ServiceRegistration) */ classServices = new HashMap();

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
   * <li>The defining class of the service paramater is not owned by the bundle.</li>
   * <li>The service parameter is not a ServiceFactory and is not an
   * instance of all the named classes in the classes parameter.</li>
   * </ul>
   */
  ServiceRegistration register(BundleImpl bundle,
                               String[] classes,
                               Object service,
                               Dictionary properties) {
    if (service == null) {
      throw new IllegalArgumentException("Can't register null as a service");
    }
    // Check if service implements claimed classes and that they exist.
    for (int i = 0; i < classes.length; i++) {
      String cls = classes[i];
      if (cls == null) {
        throw new IllegalArgumentException("Can't register as null class");
      }
      secure.checkRegisterServicePerm(cls);
      if (bundle.id != 0) {
        if (cls.equals(PackageAdmin.class.getName())) {
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
      if (!(service instanceof ServiceFactory)) {
        if (!checkServiceClass(service, cls)) {
          throw new IllegalArgumentException
            ("Service object is not an instance of " + cls);
        }
      }
    }

    ServiceRegistration res =
      new ServiceRegistrationImpl(bundle, service,
                                  new PropertiesDictionary(properties, classes, null));
    synchronized (this) {
      services.put(res, classes);
      for (int i = 0; i < classes.length; i++) {
        ArrayList s = (ArrayList) classServices.get(classes[i]);
        if (s == null) {
          s = new ArrayList(1);
          classServices.put(classes[i], s);
        }
        int ip = Math.abs(Util.binarySearch(s, sComp, res) + 1);
        s.add(ip, res);
      }
    }
    ServiceReference r = res.getReference();
    bundle.fwCtx.perm
      .callServiceChanged(bundle.fwCtx,
                          bundle.fwCtx.listeners.getMatchingServiceListeners(r),
                          new ServiceEvent(ServiceEvent.REGISTERED, r),
                          null);
    return res;
  }


  /**
   * Service ranking changed reorder registered services
   * according to ranking.
   *
   * @param serviceRegistration The serviceRegistration object.
   * @param rank New rank of object.
   */
  synchronized void updateServiceRegistrationOrder(ServiceRegistrationImpl sr,
                                                   String[] classes) {
    for (int i = 0; i < classes.length; i++) {
      ArrayList s = (ArrayList) classServices.get(classes[i]);
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
    final Class sc = service.getClass();
    final ClassLoader scl = secure.getClassLoaderOf(sc);
    Class c = null;
    boolean ok = false;
    try {
      if (scl != null) {
        c = scl.loadClass(cls);
      } else {
        c = Class.forName(cls);
      }
      ok = c.isInstance(service);
    } catch (ClassNotFoundException e) {
      for (Class csc = sc; csc != null; csc = csc.getSuperclass()) {
        if (cls.equals(csc.getName())) {
          ok = true;
          break;
        } else {
          Class [] ic = csc.getInterfaces();
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
   * Only used internaly by framework.
   *
   * @param clazz The class name of requested service.
   * @return A sorted list of {@link ServiceRegistrationImpl} objects
   *         or null if no services is available.
   */
  synchronized ArrayList get(String clazz) {
    ArrayList v = (ArrayList) classServices.get(clazz);
    if (v != null) {
      return (ArrayList)v.clone();
    }
    return null;
  }


  /**
   * Get a service implementing a certain class.
   *
   * @param bundle bundle requesting reference
   * @param clazz The class name of requested service.
   * @return A {@link ServiceReference} object.
   */
  synchronized ServiceReference get(BundleImpl bundle, String clazz) {
    try {
      ServiceReference [] srs = get(clazz, null, bundle);
      if (framework.debug.service_reference) {
        framework.debug.println("get service ref " + clazz + " for bundle " + bundle.location
                                + " = " + (srs != null ? srs[0] : null));
      }
      if (srs != null) {
        return srs[0];
      }
    } catch (InvalidSyntaxException _never) { }
    return null;
  }


  /**
   * Get all services implementing a certain class and then
   * filter these with a property filter.
   *
   * @param clazz The class name of requested service.
   * @param filter The property filter.
   * @param bundle bundle requesting reference. can be null if doAssignableToTest is false
   * (this is not an interface class so don't check)
   * @param isAssignableToTest whether to if the bundle that registered the service
   * referenced by this ServiceReference and the specified bundle are both wired to
   * same source for the registration class.
   * @return An array of {@link ServiceReference} object.
   */
  synchronized ServiceReference[] get(String clazz, String filter, BundleImpl bundle)
    throws InvalidSyntaxException {
    Iterator s;
    LDAPExpr ldap = null;
    if (clazz == null) {
      if (filter != null) {
        ldap = new LDAPExpr(filter);
        Set matched = ldap.getMatchedObjectClasses();
        if (matched != null) {
          ArrayList v = null;
          boolean vReadOnly = true;;
          for (Iterator i = matched.iterator(); i.hasNext(); ) {
            ArrayList cl = (ArrayList) classServices.get(i.next());
            if (cl != null) {
              if (v == null) {
                v = cl;
              } else {
                if (vReadOnly) {
                  v = new ArrayList(v);
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
      ArrayList v = (ArrayList) classServices.get(clazz);
      if (v != null) {
        s = v.iterator();
      } else {
        return null;
      }
      if (filter != null) {
        ldap = new LDAPExpr(filter);
      }
    }
    Collection res = new ArrayList();
    while (s.hasNext()) {
      ServiceRegistrationImpl sr = (ServiceRegistrationImpl)s.next();
      ServiceReference sri = sr.getReference();
      if (!secure.okGetServicePerms(sri)) {
        continue; //sr not part of returned set
      }
      if (filter == null || ldap.evaluate(sr.properties, false)) {
        if (bundle != null) {
          String[] classes = (String[]) services.get(sr);
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
      if (bundle != null) {
        framework.hooks.filterServiceReferences(bundle.bundleContext,
                                                clazz, filter, false, res);
      } else {
        framework.hooks.filterServiceReferences(null, clazz, filter, true, res);
      }
      if (res.isEmpty()) {
        return null;
      } else {
        return (ServiceReference [])res.toArray(new ServiceReference [res.size()]);
      }
    }
  }


  /**
   * Remove a registered service.
   *
   * @param sr The ServiceRegistration object that is registered.
   */
  synchronized void removeServiceRegistration(ServiceRegistrationImpl sr) {
    String[] classes = (String[]) sr.properties.get(Constants.OBJECTCLASS);
    services.remove(sr);
    for (int i = 0; i < classes.length; i++) {
      ArrayList s = (ArrayList) classServices.get(classes[i]);
      if (s.size() > 1) {
        s.remove(sr);
      } else {
        classServices.remove(classes[i]);
      }
    }
  }


  /**
   * Get all services that a bundle has registered.
   *
   * @param b The bundle
   * @return A set of {@link ServiceRegistration} objects
   */
  synchronized Set getRegisteredByBundle(Bundle b) {
    HashSet res = new HashSet();
    for (Iterator e = services.keySet().iterator(); e.hasNext();) {
      ServiceRegistrationImpl sr = (ServiceRegistrationImpl)e.next();
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
  synchronized Set getUsedByBundle(Bundle b) {
    HashSet res = new HashSet();
    for (Iterator e = services.keySet().iterator(); e.hasNext();) {
      ServiceRegistrationImpl sr = (ServiceRegistrationImpl)e.next();
      if (sr.isUsedByBundle(b)) {
        res.add(sr);
      }
    }
    return res;
  }


  static final Util.Comparator sComp = new Util.Comparator() {
      /**
       * Name compare two ServiceRegistrationImpl objects according
       * to the ServiceReference compareTo.
       *
       * @param oa ServiceRegistrationImpl to compare.
       * @param ob ServiceRegistrationImpl to compare.
       * @return Return 0 if equals, negative if first object is less than second
       *         object and postive if first object is larger then second object.
       */
      public int compare(Object oa, Object ob) {
        ServiceRegistrationImpl a = (ServiceRegistrationImpl)oa;
        ServiceRegistrationImpl b = (ServiceRegistrationImpl)ob;
        return a.reference.compareTo(b.reference);
      }
    };

}
