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

package org.knopflerfish.bundle.commons.logging;

import org.osgi.framework.*;
import org.knopflerfish.service.log.LogRef;

import org.apache.commons.logging.*;

//import java.lang.reflect.Constructor;
//import java.lang.reflect.Method;
import java.util.*;

/**
 */
public final class LogFactoryOSGI extends LogFactory {

  public LogFactoryOSGI() {
    super();
  }
  
  /**
   * The configuration attributes for this {@link LogFactory}.
   */
  private Hashtable attrs = new Hashtable();

  // Sttring -> Log
  private Hashtable logs = new Hashtable();
  
  public Object getAttribute(String name) {
    return (attrs.get(name));
  }
  
  public String[] getAttributeNames() {
    Vector names = new Vector();
    Enumeration keys = attrs.keys();
    while (keys.hasMoreElements()) {
      names.addElement((String) keys.nextElement());
    }
    String results[] = new String[names.size()];
    for (int i = 0; i < results.length; i++) {
      results[i] = (String) names.elementAt(i);
    }
    return (results);
  }

  public Log getInstance(Class clazz)
    throws LogConfigurationException
  {
    Log log = (Log) logs.get(clazz.getName());

    if( log != null ) {
      return log;
    }
    
    log = new LogOSGI(clazz.getName());
    logs.put( clazz, log );
    return log;
  }
  
  
  public Log getInstance(String name)
    throws LogConfigurationException
  {
    Log log = (Log) logs.get(name);
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
