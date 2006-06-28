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
import java.util.Set;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;


/**
 * Here we handle all the services that are registered in framework.
 *
 * @author Jan Stein
 * @author Philippe Laporte
 */
class Services {

  /**
   * All registered services in the current framework.
   * Mapping of registered service to class names under which service is registerd.
   */
  private HashMap /* serviceRegistration -> Array of Class Names */ services = new HashMap();

  /**
   * Mapping of classname to registered service.
   */
  private HashMap /* String->ServiceRegistration */ classServices = new HashMap();
  
  /**
   * Handle to secure call class.
   */
  private PermissionOps secure;


  Services(PermissionOps perm) {
    secure = perm;
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
    ServiceRegistration res;
    synchronized (this) {
      // Check if service implements claimed classes and that they exist.
      Class sc = service.getClass();
      ClassLoader scl = sc.getClassLoader();
      for (int i = 0; i < classes.length; i++) {
        String cls = classes[i];
	if (cls == null) {
	  throw new IllegalArgumentException("Can't register as null class");
	}
	secure.checkRegisterServicePerm(cls);
	if (bundle.id != 0) {
          if (cls.equals(PackageAdmin.class.getName())) {
            throw new IllegalArgumentException("Registeration of a PackageAdmin service is not allowed");
          }
          if (cls.equals(PermissionAdmin.class.getName())) {
            throw new IllegalArgumentException("Registeration of a PermissionAdmin service is not allowed");
          }
        }
	if (!(service instanceof ServiceFactory)) {
          ClassLoader cl = sc.getClassLoader();
          Class c = null;
          boolean ok = false;
          try {
            if (cl != null) {
              c = cl.loadClass(cls);
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
          if (!ok) {
            throw new IllegalArgumentException("Service object is not an instance of " + cls);
          }
        }
      }

      res = new ServiceRegistrationImpl(bundle, service,
					new PropertiesDictionary(properties, classes, null));
      services.put(res, classes);
      for (int i = 0; i < classes.length; i++) {
	ArrayList s = (ArrayList) classServices.get(classes[i]);
	if (s == null) {
	  s = new ArrayList(1);
	  classServices.put(classes[i], s);
	}
	s.add(res);
      }
    }
    bundle.framework.listeners.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, res.getReference()));
    return res;
  }


  /**
   * Get a service implementing a certain class.
   *
   * @param bundle bundle requesting reference
   * @param clazz The class name of requested service.
   * @return A {@link ServiceReference} object.
   */
  synchronized ServiceReference get(BundleImpl bundle, String clazz) {
	//TODO spec omits to say when to do the isAssignableTo test
    ArrayList v = (ArrayList) classServices.get(clazz);
    if (v != null) {
      ServiceReference lowestId = ((ServiceRegistration)v.get(0)).getReference();
      ServiceReference res = lowestId;
      int size = v.size();
      if (size > 1) {
    	  int rank_res = ranking(res);
    	  for (int i = 1; i < size; i++) {
    		  ServiceReference s = ((ServiceRegistration)v.get(i)).getReference();
    		  int rank_s = ranking(s);
    		  if (rank_s > rank_res && s.isAssignableTo(bundle, clazz)) {
    			  res = s;
    			  rank_res = rank_s;
    		  }
    	  }
      }
      if(res == lowestId){
    	  if(res.isAssignableTo(bundle, clazz)){
    		  return res;
    	  }
    	  else{
    		  return null;
    	  }
      }
      else{
    	  return res;
      }
    } 
    else {
      return null;
    }
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
  synchronized ServiceReference[] get(String clazz, String filter, BundleImpl bundle,
		                              boolean doAssignableToTest)
    throws InvalidSyntaxException {
    Iterator s;
    if (clazz == null) {
      s = services.keySet().iterator();
      if (s == null) { //TODO can this ever happen?
	return null;
      }
    } else {
      ArrayList v = (ArrayList) classServices.get(clazz);
      if (v != null) {
	s = v.iterator();
      } else {
	return null;
      }
    }
    ArrayList res = new ArrayList();
    while (s.hasNext()) {
      ServiceRegistrationImpl sr = (ServiceRegistrationImpl)s.next();
      String[] classes = (String[]) services.get(sr);
      //should never happen?
      if (classes == null) {
        return null;
      }
      if (!secure.okGetServicePerms(classes)) {
        continue; //sr not part of returned set
      }
      if (filter == null || LDAPExpr.query(filter, sr.properties)) {
        if (doAssignableToTest) {
          int i;
          int length = classes.length;
          for (i = 0; i < length; i++) {
            if(!sr.getReference().isAssignableTo(bundle, classes[i])){
              break;
            }
          }
          if (i == length) {
            res.add(sr.getReference());
          }
        } else {
          res.add(sr.getReference());
        }
      }
    }
    if (res.isEmpty()) {
      return null;
    } else {
      ServiceReference[] a = new ServiceReference[res.size()];
      res.toArray((Object[])a);
      return a;
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

  //
  // Private methods
  //
    
  /**
   * Get service ranking from a service reference.
   *
   * @param s The service reference
   * @return Ranking value of service, default value is zero
   */
  private int ranking(ServiceReference s) {
    Object v = s.getProperty(Constants.SERVICE_RANKING);
    if (v != null && v instanceof Integer) {
      return ((Integer)v).intValue();
    } else {
      return 0;
    }
  }

}
