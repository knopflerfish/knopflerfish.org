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

package org.knopflerfish.ksoap;

import java.lang.reflect.Method;

import java.io.ByteArrayInputStream;

import org.knopflerfish.bundle.ksoap.Activator;

/** A SOAP service representation of an arbitrary object
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class ObjectSOAPService {
  private String serviceName = null;
  private Object serviceObject = null;

  private String[] classNames;

  String  allowedMethods;

  public ObjectSOAPService(Object dummy,
                           String serviceName,
                           Object serviceObject,
                           String[] classNames,
                           String   allowedMethods) {
    this.serviceObject = serviceObject;
    this.serviceName = serviceName;
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
      Activator.log.error("Failed to analyze methods in service object:"  + serviceObject.getClass().getName(), e);
    }
    return "*";
  }

  public Object getServiceObject() {
    return serviceObject;
  }
}
