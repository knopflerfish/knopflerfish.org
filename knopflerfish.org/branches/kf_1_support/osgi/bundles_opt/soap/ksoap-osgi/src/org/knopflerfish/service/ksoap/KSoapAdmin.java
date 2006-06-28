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

package org.knopflerfish.service.ksoap;

public interface KSoapAdmin {

  /**
   * Service property for services wishing to be exported as
   * SOAP services.
   *
   * <p>
   * Value of the property is <tt>SOAP.service.name</tt>
   * </p>
   */
  public static final String SOAP_SERVICE_NAME = "SOAP.service.name";

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
   * Get the currently published service names.
   */
  public String[] getPublishedServiceNames();
}
