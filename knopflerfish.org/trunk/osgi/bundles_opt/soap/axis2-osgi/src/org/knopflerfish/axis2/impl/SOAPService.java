/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
package org.knopflerfish.axis2.impl;

import org.codehaus.jam.JMethod;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.java2wsdl.DefaultSchemaGenerator;
import org.apache.axis2.description.java2wsdl.SchemaGenerator;
import org.apache.axis2.description.java2wsdl.Java2WSDLConstants;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.deployment.util.Utils;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.engine.ObjectSupplier;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.xml.namespace.QName;

import java.util.*;

import org.knopflerfish.service.axis2.Axis2Admin;
import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Filter;
import org.osgi.framework.BundleContext;

public class SOAPService {
  ServiceReference sr;
  Object           serviceObj;
  String           serviceClass;
  String           serviceName;
  Filter           allowedMethodsFilter;

  String targetNamespace;

  DefaultSchemaGenerator schemaGenerator;

  // For each deploy(), an AxisService will be created
  // and stored in serviceMap
  // 
  // AxisConfiguration -> AxisService
  protected Map serviceMap = new HashMap();

  public SOAPService(ServiceReference sr) {
    this.sr = sr;
    serviceName       = (String)   sr.getProperty(Axis2Admin.SOAP_SERVICE_NAME);
    serviceClass      = (String)   sr.getProperty(Axis2Admin.SOAP_SERVICE_CLASS);

    if(serviceClass == null) {
      String[] classes = (String[])sr.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
      serviceClass     = classes[0];
    }


    allowedMethodsFilter = makeFilter(Activator.bc,
                                      (String)sr.getProperty(Axis2Admin.SOAP_SERVICE_METHODS),
                                      "name", 
                                      " ");
    
    Activator.log.info("allowedMethodsFilter=" + allowedMethodsFilter);
    
    if(serviceName == null) {
      throw new NullPointerException("serviceName argument cannot be null");
    }

    if(serviceClass == null) {
      serviceClass = serviceObj.getClass().getName();
    }

    targetNamespace = (String)sr.getProperty(Axis2Admin.SOAP_SERVICE_TARGETNAMESPACE);

    if(targetNamespace == null) {
      StringTokenizer st = new StringTokenizer(serviceClass, ".");
      while (st.hasMoreTokens()) {
        String part = st.nextToken();
        if(targetNamespace == null) {
          targetNamespace = part;
        } else {
          targetNamespace = part + "." + targetNamespace;
        }
      }
      targetNamespace = targetNamespace + "/" + serviceName;
    } else if(Axis2Admin.NAMESPACE_AXIS2AUTO.equals(targetNamespace)) {
      targetNamespace = null;
    }
    // if this is null, WSDL generator
  }
  
  public int getDeployCount() {
    return serviceMap.size();
  }
  
  /**
   * Utility method to create an OR filter from string containing items
   * separated by a specified string.
   *
   * @param bc context to create filter from
   * @param items string containing items
   * @param property name to which items are compares in resulting filter
   * @param sep separator string between items
   */
  static Filter makeFilter(BundleContext bc,
                           String        items, 
                           String        propName, 
                           String        sep) {
    StringBuffer sb = new StringBuffer();
    try {
      if(items != null && (items.startsWith("(") && items.endsWith(")"))) {
        sb.append(items);
      } else {
        if(items == null || "*".equals(items)) {
          sb.append("(" + propName + "=*)");
        } else {
          sb.append("(|");
          StringTokenizer st = new StringTokenizer(items, sep);
          while (st.hasMoreTokens()) {
            String part = st.nextToken().trim();
            sb.append("(" + propName + "=" + part + ")");
          }
          sb.append(")");
        }
      }
      return bc.createFilter(sb.toString());
    } catch (Exception e) {
      Activator.log.error("Bad filter", e);
      return null;
    }
  }

  public void deploy(AxisConfiguration axisConfig) throws Exception {
    synchronized(serviceMap) {
      if(serviceMap.containsKey(axisConfig)) {
        throw new IllegalStateException(this + " already deployed in " + axisConfig);
      }
      
      if(serviceObj == null) {
        serviceObj     = Activator.bc.getService(sr);
        if(serviceObj == null) {
          throw new NullPointerException("serviceObj argument cannot be null");
        }        
      }
      AxisService as = createService(axisConfig);
      
      axisConfig.addService(as);
      
      Activator.log.info("deployed " + this + " in " + axisConfig);
      serviceMap.put(axisConfig, as);
    }
  }

  

  public void undeploy(AxisConfiguration axisConfig) throws Exception {
    synchronized(serviceMap) {
      AxisService as = (AxisService)serviceMap.get(axisConfig);
      if(as == null) {
        throw new IllegalStateException(this + " not deployed in " + axisConfig);
      }

      axisConfig.removeService(as.getName());
      
      Activator.log.info("undeployed " + this + " from " + axisConfig);
      serviceMap.remove(axisConfig);

      if(getDeployCount() == 0) {
        if(serviceObj != null) {
          Activator.bc.ungetService(sr);
          serviceObj = null;
        }
      }
    }
  }

  AxisService createService(AxisConfiguration axisConfig) throws Exception {
    
    return createService(this, 
                         axisConfig);
  }

  // This code is more or less copied from AxisService.createService()
  // ...but why is it so complicated???
  static AxisService createService(final SOAPService       soapService,
                                   AxisConfiguration axisConfiguration) throws Exception {
    AxisService     axisService          = new OSGiAxisService(soapService);
    
    HashMap messageReceiverClassMap = new HashMap();

    MessageReceiver messageReceiver = new org.apache.axis2.rpc.receivers.RPCInOnlyMessageReceiver() {
      protected Object makeNewServiceObject(MessageContext msgContext) throws AxisFault {
        return soapService.serviceObj;
      }
    };
    
    messageReceiverClassMap.put(WSDL2Constants.MEP_URI_IN_ONLY,        messageReceiver);

    MessageReceiver inOutmessageReceiver = new org.apache.axis2.rpc.receivers.RPCMessageReceiver() {
      protected Object makeNewServiceObject(MessageContext msgContext) throws AxisFault{
        return soapService.serviceObj;
      }
    };

    messageReceiverClassMap.put(WSDL2Constants.MEP_URI_IN_OUT,         inOutmessageReceiver);
    messageReceiverClassMap.put(WSDL2Constants.MEP_URI_ROBUST_IN_ONLY, inOutmessageReceiver);

    axisService.setParent(axisConfiguration);
    axisService.setName(soapService.serviceName);

    soapService.schemaGenerator = 
      new DefaultSchemaGenerator(soapService.serviceObj.getClass().getClassLoader(),
                                 soapService.serviceClass, 
                                 soapService.targetNamespace,
                                 Java2WSDLConstants.SCHEMA_NAMESPACE_PRFIX, axisService) {
        // override to use "allowed method" semantics
        protected JMethod[] processMethods(JMethod[] declaredMethods) throws Exception {
          ArrayList al = new ArrayList();

          Hashtable props = new Hashtable();
          for(int i = 0; i < declaredMethods.length; i++) {
            String name = declaredMethods[i].getSimpleName();
            props.put("name", name);
            if(soapService.allowedMethodsFilter == null ||
               soapService.allowedMethodsFilter.match(props)) {
              Activator.log.info("allow method " + name);
              al.add(declaredMethods[i]);
            } else {
              Activator.log.info("skip method  " + name);
            }
          }
          
          JMethod[] r = new JMethod[al.size()];
          al.toArray(r);

          JMethod[] methods = super.processMethods(r);
          return methods;
        }
      };
    
    soapService.schemaGenerator.setElementFormDefault(Java2WSDLConstants.FORM_DEFAULT_UNQUALIFIED);

    // Utils.addExcludeMethods(soapService.excludeMethods);
    // soapService.schemaGenerator.setExcludeMethods(soapService.excludeMethods);

    Parameter parameter = new Parameter(Constants.SERVICE_CLASS, soapService.serviceClass);
    OMElement paraElement = Utils.getParameter(Constants.SERVICE_CLASS, soapService.serviceClass, false);
    parameter.setParameterElement(paraElement);
    axisService.setUseDefaultChains(false);
    axisService.addParameter(parameter);
    axisService.setName(soapService.serviceName);

    axisService.setClassLoader(soapService.serviceObj.getClass().getClassLoader());

    NamespaceMap map = new NamespaceMap();
    map.put(Java2WSDLConstants.AXIS2_NAMESPACE_PREFIX,          Java2WSDLConstants.AXIS2_XSD);
    map.put(Java2WSDLConstants.DEFAULT_SCHEMA_NAMESPACE_PREFIX, Java2WSDLConstants.URI_2001_SCHEMA_XSD);
    axisService.setNameSpacesMap(map);
    axisService.setElementFormDefault(false);
    axisService.addSchema(soapService.schemaGenerator.generateSchema());
    axisService.setSchemaTargetNamespace(soapService.schemaGenerator.getSchemaTargetNameSpace());
    axisService.setTypeTable(soapService.schemaGenerator.getTypeTable());

    if (soapService.targetNamespace == null) {
      soapService.targetNamespace = soapService.schemaGenerator.getSchemaTargetNameSpace();
    }
    if (soapService.targetNamespace != null && !"".equals(soapService.targetNamespace)) {
      axisService.setTargetNamespace(soapService.targetNamespace);
    }

    JMethod[] method = soapService.schemaGenerator.getMethods();
    PhasesInfo pinfo = axisConfiguration.getPhasesInfo();
    for (int i = 0; i < method.length; i++) {
      JMethod         jmethod   = method[i];
      AxisOperation   operation = axisService.getOperation(new QName(jmethod.getSimpleName()));
      String          mep       = operation.getMessageExchangePattern();
      MessageReceiver mr;

      if (messageReceiverClassMap != null) {        
        if (messageReceiverClassMap.get(mep) != null) {
          Object obj = messageReceiverClassMap.get(mep);
          if (obj instanceof MessageReceiver) {
            mr = (MessageReceiver) obj;
            operation.setMessageReceiver(mr);
          } else {
            Activator.log.error("Object is not an instance of MessageReceiver," + 
                                " thus, default MessageReceiver has been set");
            mr = axisConfiguration
              .getMessageReceiver(operation.getMessageExchangePattern());
            operation.setMessageReceiver(mr);
          }
        } else {
          Activator.log.error("Required MessageReceiver couldn't be found," + 
                              " thus, default MessageReceiver has been used");
          mr = axisConfiguration
            .getMessageReceiver(operation.getMessageExchangePattern());
          operation.setMessageReceiver(mr);
        }
      } else {
        Activator.log.error("MessageReceiverClassMap couldn't be found," +
                            " thus, default MessageReceiver has been used");
        mr = axisConfiguration.getMessageReceiver(operation.getMessageExchangePattern());
        operation.setMessageReceiver(mr);
      }
      pinfo.setOperationPhases(operation);
      axisService.addOperation(operation);
    }
    return axisService;
  }


  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("SOAPService[" + 
              "sid=#" + sr.getProperty(org.osgi.framework.Constants.SERVICE_ID) +
              ", serviceObj=" + serviceObj + 
              ", serviceClass=" + serviceClass + 
              ", serviceName=" + serviceName);
    sb.append("]");
    return sb.toString();
  }


  /**
   * SOAPService equality is based on ServiceReference member
   */
  public int hashCode() {
    return sr.hashCode();
  }

  /**
   * SOAPService equality is based on ServiceReference member
   */
  public boolean equals(Object obj) {
    if(obj == null || !(obj instanceof SOAPService)) {
      return false;
    }
    SOAPService ss = (SOAPService)obj;

    return sr.equals(ss);
  }
}
