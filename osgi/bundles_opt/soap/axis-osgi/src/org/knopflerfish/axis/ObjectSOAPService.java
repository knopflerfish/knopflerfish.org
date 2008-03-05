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

package org.knopflerfish.axis;

import java.io.ByteArrayInputStream;

import org.apache.axis.WSDDEngineConfiguration;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.deployment.wsdd.WSDDDocument;
import org.apache.axis.server.AxisServer;
import org.apache.axis.utils.XMLUtils;

import org.apache.commons.logging.Log;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * An Axis SOAP service representation of an arbitrary object
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class ObjectSOAPService {
  protected static Log log = LogFactory.getLog(ObjectSOAPService.class.getName());
  private AxisServer axisServer = null;
  private String serviceName = null;
  private Object serviceObject = null;

  String  allowedMethods;

  public ObjectSOAPService(AxisServer server,
                           String serviceName,
                           Object serviceObject,
                           String[] classNames,
                           String   allowedMethods)
  {
    this.serviceObject = serviceObject;
    this.serviceName = serviceName;
    this.axisServer = server;

    if(allowedMethods == null) {
      this.allowedMethods = getMethodNames(classNames);
    } else {
      this.allowedMethods = allowedMethods;
    }
  }

  /**
   * Get all method names from a set of classes, except for the
   * methods names in java.lang.Object.
   *
   * @param classNames array of class names. Each class object will
   *                   be created using Class.forName
   */
  String getMethodNames(String[] classNames) {
    StringBuffer sb = new StringBuffer();

    try {
      Class objectClass = Object.class;

      for(int i = 0; i < classNames.length; i++) {
        Class clazz = Class.forName(classNames[i]);
        Method[] methods = clazz.getMethods();

        for(int j = 0; j < methods.length; j++) {
          boolean bInObject = false;
          try {
            objectClass.getMethod(methods[i].getName(),
                                  methods[i].getParameterTypes());
            bInObject = true;
          } catch (Exception ignored) {
            // If not in objectClass methods
          }

          if(!bInObject) {
            if(sb.length() > 0) {
              sb.append(" ");
            }
            sb.append(methods[j].getName());
          }
        }
      }
      return sb.toString();
    } catch (Exception e) {
      log.error("Failed to analyze methods in service object:"
                + serviceObject.getClass().getName(), e);
    }
    return "*";
  }


  /**
   * Get all objects used in the methods of the serviceObject that do not
   * have a standard SOAP mapping.
   *
   * @return a HashSet containing all unmapped object classes
   */
  private HashSet getMethodObjects() {
    HashSet         classset = new HashSet();
    HashSet allowedMethodSet = new HashSet();

    StringTokenizer st = new StringTokenizer(allowedMethods);
    while (st.hasMoreTokens()) allowedMethodSet.add(st.nextToken());

    try {
      Class objectClass = Object.class;
      Class clazz       = serviceObject.getClass();
      Method[] methods  = clazz.getMethods();

      for(int i = 0; i < methods.length; i++) {
        if(allowedMethodSet.contains(methods[i].getName())) {
          Class params[] = methods[i].getParameterTypes();

          for (int j = 0; j < params.length; j++) {
            if (!params[j].isArray()) {
              String paramname = params[j].getName();
              classset.add(paramname);
            }
          }

          if (!methods[i].getReturnType().isArray()) {
            String returnname = methods[i].getReturnType().getName();
            classset.add(returnname);
          }
        }
      }

    } catch (Exception e) {
      log.error("Failed to analyze methods: " + e.toString());
    }
    allowedMethodSet.clear();

    // Remove all standard mappings from the set because they already have
    // a (de)serializer.
    classset.remove("byte");
    classset.remove("boolean");
    classset.remove("double");
    classset.remove("float");
    classset.remove("int");
    classset.remove("long");
    classset.remove("short");
    classset.remove("void");
    classset.remove("java.util.Calendar");
    classset.remove("java.math.BigDecimal");
    classset.remove("java.math.BigInteger");
    classset.remove("javax.xml.namespace.QName");
    classset.remove("java.lang.String");

    return classset;
  }


  public void deploy() {
    Object obj = axisServer.getApplicationSession().get(serviceName);

    if (obj == null) {
      deployWSDD(deployDoc());
      axisServer.getApplicationSession().set(serviceName, serviceObject);
      log.info("deployed object=" + serviceObject.getClass().getName() +
               ", name=" + serviceName + ", allowedMethods=" + allowedMethods);
    } else {
      log.error(
                "ObjectSOAPService::deploy() service " + serviceName +
                " do already exist");
    }
  }

  public void undeploy() {
    Object obj = axisServer.getApplicationSession().get(serviceName);

    if (obj == null) {
      log.error("ObjectSOAPService::undeploy() service " + serviceName +
                " does not exist");
    } else {
      deployWSDD(undeployDoc());
      axisServer.getApplicationSession().remove(serviceName);
      log.info("undeployed object=" + serviceObject.getClass().getName() +
               ", name=" + serviceName);
    }
  }

  private String deployDoc() {
    StringBuffer sb = new StringBuffer();

    Iterator it =  getMethodObjects().iterator();
    while (it.hasNext()) {
      String classname  = (String) it.next();
      int    qnamestart = classname.lastIndexOf('.');
      if (qnamestart < 0) {
        qnamestart = 0;
      } else {
        qnamestart++;
      }

      try {
        Class mybean = Class.forName(classname);
        sb.append("   <beanMapping qname=\"tnsgw:"
                  + classname.substring(qnamestart)
                  + "\" xmlns:tnsgw=\"urn:BeanService\" "
                  + "languageSpecificType=\"java:" + mybean.getName()
                  + "\"/>\n");
      } catch (Exception e) {
        log.error("Failed to get parameter class: "  + classname, e);
      }
    }

    return
      "<deployment" +
      " xmlns=\"http://xml.apache.org/axis/wsdd/\"\n" +
      " xmlns:java=\"http://xml.apache.org/axis/wsdd/providers/java\"\n" +
      ">\n" +
      " <service name     = \"" + serviceName + "\"\n" +
      "          provider = \"java:RPC\">\n" +
      "   <parameter name  = \"allowedMethods\"\n"+
      "              value = \"" + allowedMethods + "\"/>\n" +
      "   <parameter name  = \"className\"\n" +
      "              value=\""+serviceObject.getClass().getName()+"\"/>\n"+
      "   <parameter name=\"scope\"\n" +
      "              value=\"Application\"/>\n" +
      sb.toString() +
      " </service>\n" +
      "</deployment>";
  }

  private void deployWSDD(String sdoc) {
    try {
      WSDDEngineConfiguration config = (WSDDEngineConfiguration) axisServer.getConfig();
      WSDDDeployment deployment = config.getDeployment();
      WSDDDocument doc = new WSDDDocument(XMLUtils.newDocument(new ByteArrayInputStream(sdoc.getBytes())));

      doc.deploy(deployment);
      axisServer.refreshGlobalOptions();
    } catch (Exception e) {
      log.error("ObjectSOAPService::deployWSDD() exception", e);
    }
  }

  private String undeployDoc() {
    return "<undeployment" +
      " xmlns=\"http://xml.apache.org/axis/wsdd/\">" +
      "<service name=\"" + serviceName + "\"/>" + "</undeployment>";
  }

  public Object getServiceObject() {
    return serviceObject;
  }
}
