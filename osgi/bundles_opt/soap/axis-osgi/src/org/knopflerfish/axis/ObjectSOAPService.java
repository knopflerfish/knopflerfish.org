/*
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
 
package org.knopflerfish.axis;

import java.io.ByteArrayInputStream;

import org.apache.axis.WSDDEngineConfiguration;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.deployment.wsdd.WSDDDocument;
import org.apache.axis.deployment.wsdd.WSDDService;
import org.apache.axis.server.AxisServer;
import org.apache.axis.utils.XMLUtils;

import org.apache.commons.logging.Log;

import org.knopflerfish.bundle.axis.Activator;

import java.lang.reflect.*;


/** An Axis SOAP service representation of an arbitrary object
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class ObjectSOAPService {
  protected static Log log = LogFactory.getLog(ObjectSOAPService.class.getName());
  private AxisServer axisServer = null;
  private String serviceName = null;
  private Object serviceObject = null;
  
  private String[] classNames;

  String  allowedMethods;

   public ObjectSOAPService(AxisServer server, 
			    String serviceName, 
                            Object serviceObject,
			    String[] classNames,
			    String   allowedMethods) {
      this.serviceObject = serviceObject;
      this.serviceName = serviceName;
      this.axisServer = server;
      this.classNames = classNames;

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
      log.error("Failed to analyze methods in service object:"  + serviceObject.getClass().getName(), e);
    }
    return "*";
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
         log.error(
                "ObjectSOAPService::undeploy() service " + serviceName + 
                " does not exist");
      } else {
         deployWSDD(undeployDoc());
         axisServer.getApplicationSession().remove(serviceName);
      }
   }

   private String deployDoc() {
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
