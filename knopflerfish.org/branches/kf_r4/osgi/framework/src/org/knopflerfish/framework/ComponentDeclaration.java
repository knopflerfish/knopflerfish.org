/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.net.URL;

import org.osgi.framework.Bundle;

/**
 * @author Martin Berg 
 *
 * This class is used to store information of a specific component
 * It has four helper classes that supply additional information
 * these are:
 * ServiceInformation
 * ReferenceInformation
 */
public class ComponentDeclaration {
  /** The name of the component */
  private String componentName;
  /** to use autoenable on the component or not */
  private boolean autoEnable;
  /** Is the component immediate? */
  private boolean immediate;
  /** the factory to use */
  private String factory;
  /** to use a factory with the component or not */
  private boolean serviceFactory;
  /** variable holding the declaring bundle */
  private BundleImpl declaringBundle;
  /** String variable holding the path to the XML **/
  private String xmlFile;

  /**
   * the class that implements the interfaces that is listed in
   * serviceInfo
   */
  private String implementation;
  /** A list containing all services for the component */
  private ArrayList serviceInfo;
  /** A list containing all references for the component */
  private ArrayList referenceInfo;
  /** A list containing all interfaces for the component */
  private ArrayList provideInfo;
  private Dictionary properties;


  ComponentDeclaration(Bundle bundle ){
    serviceInfo = new ArrayList();
    referenceInfo = new ArrayList();
    provideInfo = new ArrayList();
    properties = new Hashtable();
    implementation = null;
    declaringBundle = (BundleImpl)bundle;
  }


  public Object getClone()  {
      ComponentDeclaration clone = new ComponentDeclaration(declaringBundle);
      clone.setAutoEnable(isAutoEnable());
      clone.setImmediate(isImmediate());
      clone.setComponentName(getComponentName());
      clone.setDeclaraingBundle(getDeclaringBundle());
      clone.setFactory(getFactory());
      clone.setImplementation(getImplementation());
      clone.setReferenceInfo(getReferenceInfo());
      clone.setServiceFactory(isServiceFactory());
      clone.setServiceInfo(getServiceInfo());
      clone.setXmlFile(this.xmlFile);

      for (Enumeration e = properties.keys(); e.hasMoreElements();) {
	Object key = e.nextElement();
	clone.addProperty((String)key, properties.get(key));
      }

      return clone;
  }

  public Class loadClass() throws ClassNotFoundException {
    return loadClass(getImplementation());
  }

  public Class loadClass(String name) throws ClassNotFoundException {
    return getDeclaringBundle().getClassLoader().loadClass(name);
  }

  /**
   * sets the bundle which declares the component
   *
   * @param bundle the declaring bundle
   */
  public void setDeclaraingBundle(Bundle bundle){
    declaringBundle = (BundleImpl) bundle;
  }

  /**
   * returns the declaring bundle,i.e,
   * the bundle holding this component.
   *
   * @return bundle if set else null
   */
  public BundleImpl getDeclaringBundle(){
    return declaringBundle;
  }

  /**
   * @return Returns the autoEnable.
   */
  public boolean isAutoEnable() {
    return autoEnable;
  }
  /**
   * @return Returns the immediate.
   */
  public boolean isImmediate() {
    return immediate;
  }
  /**
   * @return Returns the serviceFactory.
   */
  public boolean isServiceFactory() {
    return serviceFactory;
  }
  /**
   * @param serviceFactory The serviceFactory to set.
   */
  public void setServiceFactory(boolean serviceFactory) {
    this.serviceFactory = serviceFactory;
  }
  /**
   * @param autoEnable The autoEnable to set.
   */
  public void setAutoEnable(boolean autoEnable) {
    this.autoEnable = autoEnable;
  }

  /**
   * @param immediate The immediate to set.
   */
  public void setImmediate(boolean immediate) {
    this.immediate = immediate;
  }
  /**
   * @return Returns the componentName.
   */
  public String getComponentName() {
    return componentName;
  }
  /**
   * @param componentName The componentName to set.
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * @return Returns the factory.
   */
  public String getFactory() {
    return factory;
  }
  /**
   * @param factory The factory to set.
   */
  public void setFactory(String factory) {
    this.factory = factory;
  }

  /**
   * @return Returns the implementation.
   */
  public String getImplementation() {
    return implementation;
  }
  /**
   * @param implementation The implementation to set.
   */
  public void setImplementation(String implementation) {
    this.implementation = implementation;
  }
    
  /**
   * @return Returns the referenceInfo.
   */
  public ArrayList getReferenceInfo() {
    return referenceInfo;
  }
  /**
   * @param referenceInfo The referenceInfo to set.
   */
  public void setReferenceInfo(ArrayList referenceInfo) {
    this.referenceInfo = referenceInfo;
  }
  /**
   * @return Returns the serviceInfo.
   */
  public ArrayList getServiceInfo() {
    return serviceInfo;
  }
  /**
   * @param serviceInfo The serviceInfo to set.
   */
  public void setServiceInfo(ArrayList serviceInfo) {
    this.serviceInfo = serviceInfo;
  }

  /**
   * @return Returns an instance of PropertiesInformation.
   */
  public ComponentPropertiesInfo getNewPropertiesIntance(){
    ComponentPropertiesInfo props = new ComponentPropertiesInfo();
    return props;
  }

  /**
   * @return Returns an instance of PropertyInformation.
   */
  public ComponentPropertyInfo getNewPropertyIntance(){
    ComponentPropertyInfo prop = new ComponentPropertyInfo();
    return prop;
  }

  /**
   * @return Returns an instance of ServiceInformation.
   * MO: are these used?
   */
  public ComponentServiceInfo getNewServiceIntance(){
    ComponentServiceInfo serv = new ComponentServiceInfo();
    return serv;
  }
    /**
   * @return Returns an instance of ReferenceInformation.
   */
  public ComponentReferenceInfo getNewReferenceIntance(){
    ComponentReferenceInfo ref = new ComponentReferenceInfo();
    return ref;
  }

  /**
   * @param ComponentPropertiesInfo The ComponentPropertiesInfo to add.
   * MO: are these used?
   */
  public void addPropertiesInfo(ComponentPropertiesInfo propertyEntry) throws IOException {
    Properties dict = new Properties();
    
    String propertyFile = propertyEntry.getEntry();
    
    ComponentActivator.debug("Reading property file:" + propertyFile);
    String bundleLocation = declaringBundle.getLocation();
    
    JarInputStream jis = 
      new JarInputStream(new URL(bundleLocation).openStream());
    ZipEntry zipEntry;
    
    while ((zipEntry = jis.getNextEntry()) != null && 
	   !zipEntry.getName().equals(propertyFile)) 
      /* skip */ ; 
    
    if (zipEntry == null) {
      ComponentActivator.error("Could not find propertyFile entry. Aborting.");
      throw new IOException("Did not find requested entry " + propertyFile);
    }
    
    dict.load(jis);
    
    for (Enumeration e = dict.keys(); e.hasMoreElements(); ) {
      Object key = e.nextElement();
      addProperty((String)key, dict.get(key));
    }
  }

  /**
   * @param ComponentPropertyInfo The ComponentPropertyInfo to add.
   */
  public void addPropertyInfo(ComponentPropertyInfo propInfo){
    addProperty(propInfo.getName(), propInfo.getValue());
  }

  public void addProperty(String key, Object val) { // make a bit nicer.
    properties.put(key, val);
  }
  
  /**
   * @param ComponentServiceInfo The ComponentServiceInfo to add.
   */
  public void addServiceInfo(ComponentServiceInfo servInfo){
    serviceInfo.add(servInfo);
  }

  /**
   * @param ComponentReferenceInfo The ComponentReferenceInfo to add.
   */
  public void addReferenceInfo(ComponentReferenceInfo refInfo){
    referenceInfo.add(refInfo);
  }

  /**
   * this method converts properties located in a component
   * declaration and returns a dictionary. Those properties
   * are declared within the property element in the
   * xml file.
   *
   * @return  Dictionary with properties
   */
  public Dictionary getDeclaredProperties() {
    // we make a copy of it, since the component initiation may add/alter these
    Dictionary ret = new Hashtable();
    
    for (Enumeration e = properties.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      ret.put(key, properties.get(key));
    }
    
    return ret;
  }

  /**
   * @return Returns the xmlFile.
   */
  public String getXmlFile() {
    return xmlFile;
  }

  /**
   * @param xmlFile The xmlFile to set.
   */
  public void setXmlFile(String xml) {
    this.xmlFile = xml;
  }
}
