/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.util.cm;

/**
 ** This interface provides all element and attribute names used in
 ** the cm_data DTD as constants.
 **
 ** @author Gatespace
 ** @version $Id: CMDataNames.java,v 1.1.1.1 2004/03/05 20:34:49 wistrand Exp $
 **/
interface CMDataNames {

  // DTD stuff
  /** The Public ID of the cm_data DTD.*/
  static final String CM_DATA_0_1_ID = "-//Gatespace//DTD cm_data 0.1//EN";
  /** The file name of the cm_data DTD.*/
  static final String CM_DATA_0_1_URI = "cm_data.dtd";

  
  // Element names
  /** Name of the root node in cm_data*/
  static final String CM_DATA_ROOT_NAME = "cm_data";
  /** Name of a 'configuration' node. */
  static final String CM_DATA_CONFIGRUATION_NAME = "configuration";
  /** Name of a 'factoryconfiguration' node. */
  static final String CM_DATA_FACTORYCONFIGRUATION_NAME
    = "factoryconfiguration";
  /** Name of a 'filter' node. */
  static final String CM_DATA_FILTER_NAME = "filter";
  /** Name of a 'include' node. */
  static final String CM_DATA_INCLUDE_NAME = "include";
  /** Name of a 'property' element. */
  static final String CM_DATA_PROPERTY_NAME = "property";
  /** Name of a 'value' element. */
  static final String CM_DATA_VALUE_NAME = "value";
  /** Name of a 'primitveValue' element. */
  static final String CM_DATA_PRIMITIVEVALUE_NAME = "primitiveValue";
  /** Name of a 'array' element. */
  static final String CM_DATA_ARRAY_NAME = "array";
  /** Name of a 'value' element. */
  static final String CM_DATA_VECTOR_NAME = "vector";

  // Attribute names
  /** Name of 'version' attribute. */
  static final String CM_DATA_VERSION_ANAME = "version";

  /** This should really be in <code>org.osgi.framework.Constants</code> 
   ** but it is not.*/
  static final String FACTORY_PID = "service.factoryPid";
  /** This should really be in <code>org.osgi.framework.Constants</code> 
   ** but it is not.*/
  static final String BUNDLE_LOCATION = "service.bundleLocation";


}// of class CMDataNames
