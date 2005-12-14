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
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;

/**
 * @author Martin Berg
 *
 * This class is used to store information of a specific component
 * It has four helper classes that supply additional information
 * these are:
 * PropertiesInformation
 * PropertyInformation
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
  /** A list containing all properties for the component */
  private ArrayList propertiesInfo;
  /** A list containing all property for the component */
  private ArrayList propertyInfo;
  /** A list containing all services for the component */
  private ArrayList serviceInfo;
  /** A list containing all references for the component */
  private ArrayList referenceInfo;
  /** A list containing all interfaces for the component */
  private ArrayList provideInfo;


  ComponentDeclaration(){
    propertiesInfo = new ArrayList();
    propertyInfo = new ArrayList();
    serviceInfo = new ArrayList();
    referenceInfo = new ArrayList();
    provideInfo = new ArrayList();
  }


  public Object getClone()  {
      ComponentDeclaration clone = new ComponentDeclaration();
      clone.setAutoEnable(isAutoEnable());
      clone.setImmediate(isImmediate());
      clone.setComponentName(getComponentName());
      clone.setDeclaraingBundle(getDeclaringBundle());
      clone.setFactory(getFactory());
      clone.setImplementation(getImplementation());
      clone.setPropertiesInfo(getPropertiesInfo());
      clone.setReferenceInfo(getReferenceInfo());
      clone.setServiceFactory(isServiceFactory());
      clone.setServiceInfo(getServiceInfo());
      clone.setXmlFile(this.xmlFile);
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
   * @return Returns the propertiesInfo.
   */
  public ArrayList getPropertiesInfo() {
    return propertiesInfo;
  }
  /**
   * @param propertiesInfo The propertiesInfo to set.
   */
  public void setPropertiesInfo(ArrayList propertiesInfo) {
    this.propertiesInfo = propertiesInfo;
  }
  /**
   * @return Returns the propertyInfo.
   */
  public ArrayList getPropertyInfo() {
    return propertyInfo;
  }
  /**
   * @param propertyInfo The propertyInfo to set.
   */
  public void setPropertyInfo(ArrayList propertyInfo) {
    this.propertyInfo = propertyInfo;
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
   */
  public void addPropertiesInfo(ComponentPropertiesInfo propsInfo){
    propertiesInfo.add(propsInfo);
  }

  /**
   * @param ComponentPropertyInfo The ComponentPropertyInfo to add.
   */
  public void addPropertyInfo(ComponentPropertyInfo propInfo){
    propertyInfo.add(propInfo);
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
    Dictionary properties = new Hashtable();

    Iterator propsIterator = propertyInfo.iterator();

    while(propsIterator.hasNext()){
      ComponentPropertyInfo propertyInfo
        = (ComponentPropertyInfo) propsIterator.next();
      properties.put(propertyInfo.getName(), propertyInfo.getValue());
    }
    return mergeProperties(readPropertyFile(), properties);
  }

  /**
   * this method overides values in one dictionary with values from
   * another dictionary.
   *
   * @param overideTable the dictionary which overides other simular values
   * @param mergeTable the dictionary to be modified
   * @return Dictionary a modified table
   */
  public Dictionary mergeProperties(Dictionary overideTable, Dictionary mergeTable){
    if (overideTable != null) {
      Enumeration enumeration = overideTable.keys();
      while(enumeration.hasMoreElements()){
        String key = (String)enumeration.nextElement();
        mergeTable.put(key, overideTable.get(key));
      }
    }
    return mergeTable;
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

  private Dictionary readPropertyFile() {
    Iterator propsIterator = propertiesInfo.iterator();

    Dictionary returnDictionary = new Hashtable();
    while(propsIterator.hasNext()){
      try {
        /* get the information class */
        ComponentPropertiesInfo propertyEntry = (ComponentPropertiesInfo)propsIterator.next();
        /* get the string representing the file */
        String propertyFile = propertyEntry.getEntry();

        ComponentActivator.debug("Reading property file:" + propertyFile);

        String bundleLocation = declaringBundle.getLocation();
        String formattedLocation = bundleLocation.substring(5, bundleLocation.length());

        /* get the jar file use the formatted location */
        JarFile jarFile = new JarFile(formattedLocation);
        ZipEntry zipEntry = jarFile.getEntry(propertyFile);

        Properties properties = new Properties();
        properties.load(jarFile.getInputStream(zipEntry));

        Enumeration enumeration = properties.keys();

        while(enumeration.hasMoreElements()){
          String key = (String)enumeration.nextElement();
          returnDictionary.put(key,properties.get(key));
        }

      } catch(Exception e){
        ComponentActivator.error("error when reading property file", e);
      }
    }
    return returnDictionary;
  }
}
