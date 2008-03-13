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
package org.knopflerfish.service.axis2;

public interface Axis2Admin {

  /**
   * Service property for services wishing to be exported as
   * SOAP services.
   * 
   * <p>
   * Value of the property is <tt>SOAP.service.name</tt>
   * </p>
   */
  public static final String SOAP_SERVICE_NAME    = "SOAP.service.name";

  /**
   * Optional class/interface name of service object. If unset, 
   * use the (first) class name of the published service object.
   */
  public static final String SOAP_SERVICE_CLASS    = "SOAP.service.class";

  /**
   * Service property limiting which methods are exposed.
   * <p>
   * If <tt>null</tt>, expose all methods in the registered interfaces.
   * </p>
   * 
   * <p>
   * Set to <tt>"*"</tt> to expose <b>all</b> methods in the registered
   * class.
   * </p>
   * <p>
   * To restrict to a more specific set of method names, list the methods
   * separated by space.
   * </p>
   *
   * <p>
   * Value of the property is <tt>SOAP.service.methods</tt>
   * </p>
   */
  public static final String SOAP_SERVICE_METHODS = "SOAP.service.methods";

  /**
   * Optional target namespace för generated WSDL.
   *
   * <p>
   * If unset, use a namespace based on the sevice class name + service name
   * </p>
   *
   * <p>
   * Value is <tt>SOAP.service.targetnamespace</tt>
   * </p>
   */
  public static final String SOAP_SERVICE_TARGETNAMESPACE = "SOAP.service.targetnamespace";

  /**
   * If SOAP_SERVICE_TARGETNAMESPACE is set to NAMESPACE_AXIS2AUTO, use Axis2 automatic value
   * for WSDL namespace. (this is generally not a good idea, since such namespaces tend
   * to look like valid URLs event though they're most likely not).
   *
   * <p>
   * Value is <tt>[axis2auto]</tt>
   * </p>
   */
  public static final String NAMESPACE_AXIS2AUTO = "[axis2auto]";

  /**
   * The default Axis2 soap message namespace. Same as Java2WSDLConstants.AXIS2_XSD
   */
  public static final String AXIS2_XSD = "http://ws.apache.org/axis2/xsd";

  public String[] getPublishedServiceNames();
}
