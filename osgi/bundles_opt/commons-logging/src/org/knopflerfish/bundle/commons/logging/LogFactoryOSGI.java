/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.commons.logging;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

public final class LogFactoryOSGI extends LogFactory {

  public LogFactoryOSGI() {
    super();
  }
  
  /**
   * The configuration attributes for this {@link LogFactory}.
   */
  private Hashtable<String, Object> attrs = new Hashtable<>();

  private Hashtable<String, Log> logs = new Hashtable<>();
  
  public Object getAttribute(String name) {
    return (attrs.get(name));
  }
  
  public String[] getAttributeNames() {
    Vector<String> names = new Vector<>();
    Enumeration<String> keys = attrs.keys();
    while (keys.hasMoreElements()) {
      names.addElement(keys.nextElement());
    }
    String[] results = new String[names.size()];
    for (int i = 0; i < results.length; i++) {
      results[i] = names.elementAt(i);
    }
    return (results);
  }

  public Log getInstance(@SuppressWarnings("rawtypes") Class clazz)
    throws LogConfigurationException
  {
    final String className = clazz.getName();
    Log log = logs.get(className);

    if( log != null ) {
      return log;
    }
    
    log = new LogOSGI(className);
    logs.put(className, log);
    return log;
  }
  
  
  public Log getInstance(String name)
    throws LogConfigurationException
  {
    Log log = logs.get(name);
    if( log != null ) {
      return log;
    }
    
    log = new LogOSGI( name );
    logs.put( name, log );
    return log;
  }
  
  
  public void release() {
    
    logs.clear();
    
  }


  public void removeAttribute(String name) {
    attrs.remove(name);
  }
  
  
  public void setAttribute(String name, Object value) {
    if (value == null) {
      attrs.remove(name);
    } else {
      attrs.put(name, value);
    }
  }

}
