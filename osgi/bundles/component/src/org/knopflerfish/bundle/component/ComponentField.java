/*
 * Copyright (c) 2016-2017, KNOPFLERFISH project
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentServiceObjects;


class ComponentField
{
  final String name;
  final Component comp;
  final Reference ref;
  private Field   field = null;
  private int elem;
  private Class<?>  fieldType;
  private HashMap<ValueKey, Object> oldVals = null;


  /**
   *
   */
  ComponentField(String name, Component comp, Reference ref)
      throws NoSuchFieldException
  {
    this.name = name;
    this.comp = comp;
    this.ref = ref;
    Class<?> impl = comp.getImplementation();
    boolean usePkg = true;
    NoSuchFieldException err = null;
    for (Class<?>clazz = impl; clazz != null; clazz = clazz.getSuperclass()) {
      if (clazz.getClassLoader() != impl.getClassLoader()) {
        usePkg = false;
      }
      try {
        field = clazz.getDeclaredField(name);
        int m = field.getModifiers();
        fieldType = field.getType();
        if (clazz != impl) {
          if (!Modifier.isPublic(m) && !Modifier.isProtected(m)) {
            if (Modifier.isPrivate(m) || !usePkg) {
              err = new NoSuchFieldException("Field is not accesible by component: " + field);
              break;
            }
          }
        }
        if (Modifier.isStatic(m)) {
          err = new NoSuchFieldException("Field has static modifier: " + field);
          break;
        }
        if (!ref.refDesc.isFieldUpdate()) {
          if (Modifier.isFinal(m)) {
            err = new NoSuchFieldException("Field has final modifier: " + field);
            break;
          }
          if (!ref.isStatic()) {
            if (!Modifier.isVolatile(m)) {
              err = new NoSuchFieldException("Dynamic field has no volatile modifier: " + field);
              break;
            }
            if (ref.isMultiple() && fieldType != Collection.class && fieldType != List.class) {
              err = new NoSuchFieldException("Dynamic multi-service field must be of type Collection or List: " + field);
              break;
            }
          }
        }
        if (ref.isMultiple()) {
          elem = ref.refDesc.fieldCollectionType;
        } else {
          if (fieldType.getName().equals(ref.getServiceName())) {
            elem = ReferenceDescription.FIELD_ELEM_SERVICE;
          } else if (ServiceReference.class == fieldType) {
            elem = ReferenceDescription.FIELD_ELEM_REFERENCE;
          } else if (ComponentServiceObjects.class == fieldType) {
            elem = ReferenceDescription.FIELD_ELEM_SERVICE_OBJECTS;
          } else if (Map.Entry.class == fieldType) {
            elem = ReferenceDescription.FIELD_ELEM_TUPLE;
          } else if (Map.class == fieldType) {
            elem = ReferenceDescription.FIELD_ELEM_PROPERTIES;
          } else if (fieldType.isAssignableFrom(comp.bc.getBundle().loadClass(ref.getServiceName()))) {
            elem = ReferenceDescription.FIELD_ELEM_SERVICE;
          } else {
            err = new NoSuchFieldException("Field has no useable type: " + fieldType);
          }
        }
        field.setAccessible(true);
        return;
      } catch (NoSuchFieldException nsfe) {
        if (err == null) {
          err = nsfe;
        }
      } catch (ClassNotFoundException e) {
        if (err == null) {
          err = new NoSuchFieldException("Unable to get reference class: "+ e);
        }
      }
    }
    throw err;
  }


  /**
   *
   */
  ComponentException set(ComponentContextImpl cci,
                         ServiceReference<?> sr,
                         ReferenceListener rl,
                         boolean update) {
    if (isMissing()) {
      return new ComponentException("Missing specified field: " + name);
    }
    try {
      Object instance = cci.getComponentInstance().getInstance();
      if (ref.isMultiple()) {
        Activator.logDebug("Add to collection " + field + " in " + instance.getClass()
        +  " for component " + comp.compDesc.getName() +
        " registered for " + comp.compDesc.bundle);
        if (ref.refDesc.isFieldUpdate()) {
          if (ref.isStatic()) {
            Activator.logError("Attribute field-update in reference-tag must not be "
                + "set to \"update\" for a static reference");      
          } else {
            Object val = getElemValue(cci, sr, rl);
            if (!update || getOldVal(cci, sr, rl) != val) {
              addCollection(instance, val);
              saveOldVal(cci, sr, rl, val);
            }
          }
        } else {
          setValue(instance, getElemValues(cci, rl));
        }
      } else if (ref.refDesc.isFieldUpdate()) {
          Activator.logError("Attribute field-update in reference-tag must not be "
              + "set to \"update\" for a single cardinality reference");      
      } else {
        Activator.logDebug("Set " + field + " in " + instance.getClass()
        +  " for component " + comp.compDesc.getName() +
        " registered for " + comp.compDesc.bundle);
        setValue(instance, getElemValue(cci, sr, rl));
      }
    } catch (final ComponentException ce) {
      return ce;
    }
    return null;
  }


  ComponentException unset(ComponentContextImpl cci,
                           ServiceReference<?> sr,
                           ReferenceListener rl,
                           boolean resetField)
  {
    if (isMissing()) {
      return new ComponentException("Missing specified field: " + name);
    }
    try {
      Object instance = cci.getComponentInstance().getInstance();
      if (ref.isMultiple()) {
        if (ref.refDesc.isFieldUpdate()) {
          removeCollection(instance, getOldVal(cci, sr, rl));
        } else {
          setValue(instance, new ArrayList<Object>());
        }
      } else if (resetField){
        setValue(instance, null);
      }
    } catch (final ComponentException ce) {
      return ce;
    }
    return null;
  }


  /**
   *
   */
  boolean isMissing() {
    if (field == null) {
        Activator.logError(comp.bc, "Didn't find field \"" + name + "\" in " + comp, null);
      return true;
    }
    return false;
  }


  Object getRefService(ComponentContextImpl cci,
                       ServiceReference<?> s,
                       ReferenceListener rl) throws ComponentException {
    try {
      Object res = rl.getService(s, cci);
      if (res == null) {
        throw new ComponentException("Got null service, " + Activator.srInfo(s));
      }
      return res;
    } catch (final Exception e) {
      Activator.logDebug("Got " + e + " when getting service for field " + name +
                         " for component " + comp.compDesc.getName() + " registered for " +
                         comp.compDesc.bundle);
      if (e instanceof ComponentException) {
        throw (ComponentException)e;
      } else {
        throw new ComponentException("Failed to get service, " + Activator.srInfo(s), e);
      }
    }
  }


  private Object getElemValue(ComponentContextImpl cci,
                              ServiceReference<?> sr,
                              ReferenceListener rl) {
    switch (elem) {
    case ReferenceDescription.FIELD_ELEM_SERVICE:
      return getRefService(cci, sr, rl);
    case ReferenceDescription.FIELD_ELEM_REFERENCE:
      return sr;
    case ReferenceDescription.FIELD_ELEM_SERVICE_OBJECTS:
      return cci.getComponentServiceObjects(sr, rl);
    case ReferenceDescription.FIELD_ELEM_PROPERTIES:
      return new PropertyDictionary(sr);
    case ReferenceDescription.FIELD_ELEM_TUPLE:
      return new PropertyTuple(new PropertyDictionary(sr), getRefService(cci, sr, rl));
    default:
      throw new IllegalStateException("Internal error, elem=" +elem);
    }
  }


  private Collection<Object> getElemValues(ComponentContextImpl cci,
                                           ReferenceListener rl) {
    Collection<Object> res = new ArrayList<Object>();
    for (ServiceReference<?> sr : rl.getServiceReferences()) {
      switch (elem) {
      case ReferenceDescription.FIELD_ELEM_SERVICE:
        res.add(getRefService(cci, sr, rl));
        break;
      case ReferenceDescription.FIELD_ELEM_REFERENCE:
        res.add(sr);
        break;
      case ReferenceDescription.FIELD_ELEM_SERVICE_OBJECTS:
        res.add(cci.getComponentServiceObjects(sr, rl));
        break;
      case ReferenceDescription.FIELD_ELEM_PROPERTIES:
        res.add(new PropertyDictionary(sr));
        break;
      case ReferenceDescription.FIELD_ELEM_TUPLE:
        res.add(new PropertyTuple(new PropertyDictionary(sr), getRefService(cci, sr, rl)));
        break;
      default:
        throw new IllegalStateException("Internal error, elem=" +elem);
      }
    }
    return res;
  }


  private Object getOldVal(ComponentContextImpl cci,
                           ServiceReference<?> sr,
                           ReferenceListener rl)
  {
    if (oldVals != null && sr != null) {
      return oldVals.remove(new ValueKey(cci, sr, rl));
    } else {
      return null;
    }
  }


  private void saveOldVal(ComponentContextImpl cci,
                          ServiceReference<?> sr,
                          ReferenceListener rl,
                          Object value)
  {
    if (oldVals == null) {
      oldVals = new HashMap<ValueKey, Object>();
    }
    oldVals.put(new ValueKey(cci, sr, rl), value);
  }


  /**
   *
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void addCollection(Object instance, Object val) {
    try {
      Collection c;
      if (List.class == fieldType || Collection.class == fieldType) {
        c = (Collection) field.get(instance);
        if (c == null) {
          c = new ArrayList();
          setValue(instance, c);
        }
      } else if (Collection.class.isAssignableFrom(fieldType)) {
        c = (Collection) field.get(instance);
        if (c == null) {
          Activator.logError(comp.bc, "No field instanciate! Only update of subclasses, " + fieldType, null);
          return;
        }
      } else {
        Activator.logError(comp.bc, "No field assignment! Unsupported type for components with multiple cardinality, " + fieldType, null);
        return;
      }
      c.add(val);
      if (elem == ReferenceDescription.FIELD_ELEM_PROPERTIES) {
        PropertyDictionary pd = (PropertyDictionary) val;
        for (Object o : c) {
          if (o != pd && o instanceof PropertyDictionary) {
            if (pd.get(Constants.SERVICE_ID).equals(((PropertyDictionary)o).get(Constants.SERVICE_ID))) {
              c.remove(o);
              break;
            }
          }
        }
      } else if (elem == ReferenceDescription.FIELD_ELEM_TUPLE) {
        PropertyTuple pt = (PropertyTuple) val;
        for (Object o : c) {
          if (o != pt && o instanceof PropertyTuple) {
            if (pt.getKey().get(Constants.SERVICE_ID).equals(((PropertyTuple)o).getKey().get(Constants.SERVICE_ID))) {
              c.remove(o);
              break;
            }
          }
        }
      }
    } catch (IllegalArgumentException e) {
      Activator.logError(comp.bc, "Field assignment failed", e);
    } catch (IllegalAccessException e) {
      Activator.logError(comp.bc, "Field assignment failed", e);
    }
  }


  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void removeCollection(Object instance, Object val)
  {
    try {
      if (List.class == fieldType || Collection.class == fieldType) {
        Collection c = (Collection) field.get(instance);
        if (c == null) {
          c = new ArrayList();
          setValue(instance, c);
        }
        c.remove(val);
      } else if (Collection.class.isAssignableFrom(fieldType)) {
        Collection c = (Collection) field.get(instance);
        if (c == null) {
          Activator.logError(comp.bc, "No field instanciate! Only update of subclasses, " + fieldType, null);
          return;
        }
        c.remove(val);
      } else {
        Activator.logError(comp.bc, "No field assignment! Unsupported type for components with multiple cardinality, " + fieldType, null);
      }
    } catch (IllegalArgumentException e) {
      Activator.logError(comp.bc, "Field assignment failed", e);
    } catch (IllegalAccessException e) {
      Activator.logError(comp.bc, "Field assignment failed", e);
    }
  }


  /**
   *
   */
  private void setValue(Object instance,
                        Object val) {
    try {
      field.set(instance, val);
    } catch (IllegalArgumentException iae) {
      Activator.logError(comp.bc, "Field assignment failed", iae);
    } catch (IllegalAccessException iae) {
      Activator.logError(comp.bc, "Field assignment failed", iae);
    }
  }

  static class ValueKey {

    final private ComponentContextImpl cci;
    final private ServiceReference<?> sr;
    final private ReferenceListener rl;

    ValueKey(ComponentContextImpl cci, ServiceReference<?> sr,
                    ReferenceListener rl)
    {
       this.cci = cci;
       this.sr = sr;
       this.rl = rl;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ValueKey other = (ValueKey) obj;
      if (cci == null) {
        if (other.cci != null)
          return false;
      } else if (!cci.equals(other.cci))
        return false;
      if (rl == null) {
        if (other.rl != null)
          return false;
      } else if (!rl.equals(other.rl))
        return false;
      if (sr == null) {
        if (other.sr != null)
          return false;
      } else if (!sr.equals(other.sr))
        return false;
      return true;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((cci == null) ? 0 : cci.hashCode());
      result = prime * result + ((rl == null) ? 0 : rl.hashCode());
      result = prime * result + ((sr == null) ? 0 : sr.hashCode());
      return result;
    }
    
  }
}
