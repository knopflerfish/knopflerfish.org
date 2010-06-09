/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
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

import java.util.*;

import org.osgi.service.component.ComponentConstants;


/**
 * This class needs to be a Dictionary and a Map.
 * TBD, check that this class is immutable
 */
class PropertyDictionary extends Hashtable
{

  /**
   *
   */
  PropertyDictionary(Long id,
                     ComponentDescription cd,
                     Dictionary cm,
                     Dictionary instance,
                     boolean service) {
    addDict(cd.getProperties(), service);
    if (cm != null) {
      addDict(cm, service);
    }
    if (instance != null) {
      addDict(instance, service);
    }
    super.put(ComponentConstants.COMPONENT_ID, id);
    super.put(ComponentConstants.COMPONENT_NAME, cd.getName());
  }


  /**
   *
   */
  public Object put(Object key, Object value) { 
    throw new RuntimeException("Operation not supported."); 
  }


  /**
   *
   */
  public Object remove(Object key) { 
    throw new RuntimeException("Operation not supported.");
  }

  //
  // Private methods
  //

  /**
   *
   */
  private void addDict(Dictionary d, boolean service) {
    for (Enumeration e = d.keys(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      if (!service || !key.startsWith(".")) {
        super.put(key, d.get(key));
      }
    }
  }
}
