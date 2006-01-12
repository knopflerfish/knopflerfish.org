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


import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

public class Config {

  private String name;
  private String implementation;
  private String factory;

  boolean enabled;
  boolean serviceFactory;
  boolean immediate;

  private Hashtable properties;
  private ArrayList references;
  private ArrayList services;
  private ArrayList components;

  private Bundle bundle;

  public Config(Bundle bundle) {
    this.bundle = bundle;

    properties = new Hashtable();
    references = new ArrayList();
    services   = new ArrayList();
  }

  public void enable() {
    for (Iterator iter = references.iterator(); iter.hasNext();) {
      ((Reference) iter.next()).open();
    }
    //TODO
  }

  public void disable() {
    for (Iterator iter = references.iterator(); iter.hasNext();) {
      ((Reference) iter.next()).close();
    }
    //TODO
  }

  /*
  public Component createComponent() {
    ...

    components.add(retval);
  }
  */
  

  public boolean isSatisfied() {

    for (int i = 0; i < references.size(); i++) {
      Reference ref = (Reference)references.get(i);

      if (!ref.isSatisfied())
        return false;
    }

    return true;
  }

  public void bindReferences(Object instance) {
    for (int i = 0; i < references.size(); i++) {
      //((Reference)references.get(i)).bind(context, instance);
    }
  }

  public void unbindReferences(Object instance) {
    for (int i = references.size(); i > 0; --i) {
      //((Reference)references.get(i)).unbind(context, instance);
    }
  }

  public String[] getServices() {
    String[] ret = new String[services.size()];
    services.toArray(ret);
    return ret;
  }

  public Dictionary getProperties() {

    // we make a copy of the all the properties
    // since different components might have overridden
    // different entries.
    Hashtable copy = new Hashtable();

    for (Enumeration e = properties.keys(); e.hasMoreElements(); ) {

      Object key = e.nextElement();
      copy.put(key, properties.get(key));

    }

    return copy;
  }

  public Bundle getBundle() {
    return bundle;
  }

  public String getName() {
    return name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isAutoEnabled() {
    //TODO?
    return enabled;
  }

  public String getImplementation() {
    return implementation;
  }

  public boolean isImmediate() {
    return immediate;
  }

  public boolean isServiceFactory() {
    return serviceFactory;
  }

  public String getFactory() {
    return factory;
  }

  /* this are only for the parser. */
  public void setProperty(String key, Object value) {
    properties.put(key, value);
  }

  public void addReference(Reference ref) {
    references.add(ref);
  }

  public void addService(String interfaceName) {
    services.add(interfaceName);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setImplementation(String impl) {
    implementation = impl;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setServiceFactory(boolean isServiceFactory) {
    serviceFactory = isServiceFactory;
  }

  public void setFactory(String factory) {
    this.factory = factory;
  }

  public void setImmediate(boolean isImmediate) {
    immediate = isImmediate;
  }
}
