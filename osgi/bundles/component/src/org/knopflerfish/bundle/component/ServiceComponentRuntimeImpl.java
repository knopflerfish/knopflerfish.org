/*
 * Copyright (c) 2016-2022, KNOPFLERFISH project
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

class ServiceComponentRuntimeImpl implements ServiceComponentRuntime
{

  private final SCR scr;

  ServiceComponentRuntimeImpl(final SCR scr) {
    this.scr = scr;
  }


  /**
   *  {@inheritDoc}
   */
  public Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs(Bundle... bundles) {
    List<ComponentDescriptionDTO> res = new ArrayList<>();
    Bundle [] bs =  (bundles.length == 0) ? scr.bc.getBundles() : bundles;
    for (Bundle b : bs) {
      res.addAll(getComponentDescriptionDTOs(b, null));
    }
    return res;
  }


  /**
   *  {@inheritDoc}
   */
  public ComponentDescriptionDTO getComponentDescriptionDTO(Bundle bundle, String name) {
    if (name == null) {
      throw new NullPointerException("Parameter name can't be null");
    }
    List<ComponentDescriptionDTO> cds = getComponentDescriptionDTOs(bundle, name);
    return cds.isEmpty() ? null : cds.get(0);
  }


  /**
   *  {@inheritDoc}
   */
  public Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description) {
    List<ComponentConfigurationDTO> res = new ArrayList<>();
    Component c = getComponent(description);
    if (c != null) {
      List<SatisfiedReferenceDTO> srd = new ArrayList<>();
      List<UnsatisfiedReferenceDTO> urd = new ArrayList<>();
      Reference [] refs = c.getRawReferences();
      if (refs != null) {
        for (Reference r : refs) {
          if (r.isSatisfied()) {
            srd.add(createSatisfiedReferenceDTO(r));
          } else {
            urd.add(createUnsatisfiedReferenceDTO(r));
          }
        }
      }
      for (ComponentConfiguration cc : c.getComponentConfigurations()) {
        res.add(createComponentConfigurationDTO(cc, description,
                                                srd.toArray(new SatisfiedReferenceDTO[0]),
                                                urd.toArray(new UnsatisfiedReferenceDTO[0])));
      }
    }
    return res;
  }

  /**
   *  {@inheritDoc}
   */
  public boolean isComponentEnabled(ComponentDescriptionDTO description) {
    Component c = getComponent(description);
    return c != null && c.isEnabled();
  }


  /**
   *  {@inheritDoc}
   */
  public Promise<Void> enableComponent(ComponentDescriptionDTO description) {
    Component c = getComponent(description);
    Deferred<Void> d = new Deferred<>();
    if (c != null) {
      c.enable(d);
    } else {
      d.fail(new IllegalArgumentException("Unknown component"));
    }
    return d.getPromise();
  }


  /**
   *  {@inheritDoc}
   */
  public Promise<Void> disableComponent(ComponentDescriptionDTO description) {
    Component c = getComponent(description);
    Deferred<Void> d = new Deferred<>();
    if (c != null) {
      c.disable(d);
    } else {
      d.fail(new IllegalArgumentException("Unknown component"));
    }
    return d.getPromise();
  }


  private ComponentConfigurationDTO createComponentConfigurationDTO(ComponentConfiguration cc, ComponentDescriptionDTO cdd, SatisfiedReferenceDTO [] satisfiedRefs, UnsatisfiedReferenceDTO [] unsatisfiedRefs) {
    ComponentConfigurationDTO res = new ComponentConfigurationDTO();
    res.description = cdd;
    switch (cc.getState()) {
    case ComponentConfiguration.STATE_ACTIVATING:
    case ComponentConfiguration.STATE_DEACTIVATING:
    case ComponentConfiguration.STATE_DEACTIVE:
      res.state = unsatisfiedRefs.length == 0 ? ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION : ComponentConfigurationDTO.UNSATISFIED_REFERENCE;
      break;
    case ComponentConfiguration.STATE_REGISTERED:
      res.state = ComponentConfigurationDTO.SATISFIED;
      break;
    case ComponentConfiguration.STATE_ACTIVE:
      res.state = ComponentConfigurationDTO.ACTIVE;
      break;
    }
    res.id = cc.id;
    res.properties = cc.getPropertiesMap();
    res.satisfiedReferences = satisfiedRefs;
    res.unsatisfiedReferences = unsatisfiedRefs;
    return res;
  }


  private ComponentDescriptionDTO createComponentDescriptionDTO(Component c) {
    ComponentDescriptionDTO res = new ComponentDescriptionDTO();
    res.name = c.getName();
    res.bundle = c.getBundle().adapt(BundleDTO.class);
    res.factory = c.getFactory();
    res.scope = c.getScope();
    res.implementationClass = c.getClassName();
    res.defaultEnabled = c.isDefaultEnabled();
    res.immediate = c.isImmediate();
    res.serviceInterfaces = c.getServices();
    res.properties = c.getPropertiesMap();
    Reference [] refs = c.getRawReferences();
    int len = refs != null ? refs.length : 0;
    res.references = new ReferenceDTO[len];
    for (int i = 0; i < len; i++) {
      res.references[i] = createReferenceDTO(refs[i]);
      String target = res.references[i].target;
      if (target != null) {
        String name = res.references[i].name + ".target";
        if (!res.properties.containsKey(name)) {
          res.properties.put(name, target);
        }
      }
    }
    res.activate = c.getActivate();
    res.deactivate = c.getDeactivate();
    res.modified = c.getModified();
    res.configurationPolicy = c.getConfigurationPolicy();
    res.configurationPid = c.getConfigurationPid();
    return res;
  }


  private ReferenceDTO createReferenceDTO(Reference r) {
    ReferenceDTO res = new ReferenceDTO();
    res.name = r.getName();
    res.interfaceName = r.getServiceName();
    if (r.isMultiple()) {
      res.cardinality = r.isOptional() ? "0..n" : "1..n";
    } else {
      res.cardinality = r.isOptional() ? "0..1" : "1..1";
    }
    res.policy = r.isStatic() ? "static" : "dynamic";
    res.policyOption = r.isGreedy() ? "greedy" : "reluctant";
    res.target = r.getTarget();
    res.bind = r.getBindMethodName();
    res.unbind = r.getUnbindMethodName();
    res.updated = r.getUpdatedMethodName();
    res.field = r.refDesc.field;
    res.fieldOption = r.refDesc.fieldUpdate == null ? null : (r.refDesc.fieldUpdate ? "update" : "replace");
    res.scope = r.getScope();
    return res;
  }


  private SatisfiedReferenceDTO createSatisfiedReferenceDTO(Reference r) {
    SatisfiedReferenceDTO res = new SatisfiedReferenceDTO();
    res.name = r.getName();
    res.target = r.getTarget();
    HashSet<ServiceReference<?>> services = new HashSet<>();
    for (ReferenceListener rl : r.getAllReferenceListeners()) {
      ServiceReference<?>[] bound = rl.getBoundServiceReferences();
      if (bound != null) {
        Collections.addAll(services, bound);
      }
    }
    res.boundServices = getServiceReferenceDTOs(services);
    return res;
  }


  private UnsatisfiedReferenceDTO createUnsatisfiedReferenceDTO(Reference r) {
    UnsatisfiedReferenceDTO res = new UnsatisfiedReferenceDTO();
    res.name = r.getName();
    res.target = r.getTarget();
    HashSet<ServiceReference<?>> services = new HashSet<>();
    for (ReferenceListener rl : r.getAllReferenceListeners()) {
      Collections.addAll(services, rl.getServiceReferences());
    }
    res.targetServices = getServiceReferenceDTOs(services);
    return res;
  }


  private Component getComponent(ComponentDescriptionDTO cdd) {
    if (cdd == null) {
      throw new NullPointerException("ComponentDescriptionDTO can't be null");
    }
    Component [] cs = scr.getComponent(cdd.name);
    if (cs != null) {
      for (Component c : cs) {
        if (c.getBundle().getBundleId() == cdd.bundle.id ||
            c.getBundle().getSymbolicName().equals(cdd.bundle.symbolicName)) {
          return c;
        }
      }
    }
    return null;
  }


  private List<ComponentDescriptionDTO> getComponentDescriptionDTOs(Bundle b, String name) {
    List<ComponentDescriptionDTO> res = new ArrayList<>();
    Component [] cs = scr.getComponents(b);
    if (cs != null) {
      for (Component c : cs) {
        if (name == null || name.equals(c.getName())) {
          res.add(createComponentDescriptionDTO(c));
        }
      }
    }
    return res;
  }


  private ServiceReferenceDTO [] getServiceReferenceDTOs(Set<ServiceReference<?>> services) {
    ServiceReferenceDTO[] res = new ServiceReferenceDTO[services.size()];
    int i = 0;
    for (ServiceReference<?> sr : services) {
      res[i++] = getServiceReferenceDTO(sr);
    }
    return res;
  }


  private ServiceReferenceDTO getServiceReferenceDTO(ServiceReference<?> sr) {
    ServiceReferenceDTO res = new ServiceReferenceDTO();
    res.properties = new HashMap<>();
    for (String key : sr.getPropertyKeys()) {
      Object val = safeDTOObject(sr.getProperty(key));
      res.properties.put(key, val);
    }
    res.id = (Long) sr.getProperty(Constants.SERVICE_ID);
    Bundle [] using = sr.getUsingBundles();
    if (using != null) {
      res.usingBundles = new long [using.length];
      for (int i = 0; i < using.length; i++) {
        res.usingBundles[i] = using[i].getBundleId();
      }
    } else {
      res.usingBundles = new long [0];
    }
    Bundle b = sr.getBundle();
    if (b == null) {
      return null;
    }
    res.bundle = b.getBundleId();
    return res;
  }


  private Object safeDTOObject(Object val) {
    Class<?> c = val.getClass();
    Object res = val;
    if (c.isArray()) {
      if (!validDTOType(c.getComponentType())) {
        Object [] oa = (Object[])val;
        String [] sa = new String [oa.length];
        for (int i = 0; i < sa.length; i++) {
          sa[i] = oa[i].toString();
        }
        res = sa;
      }
    } else if (!validDTOType(c)) {
      res = val.toString();
    }
    return res;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean validDTOType(Class<?> c) {
    return c == String.class || c == Boolean.class ||
        (c.isPrimitive() && c != Boolean.TYPE && c != Character.TYPE) ||
        Number.class.isAssignableFrom(c) ||
        DTO.class.isAssignableFrom(c);
  }

}
