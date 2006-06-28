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

package org.knopflerfish.util.metatype;

import org.osgi.framework.*;
import org.osgi.service.metatype.*;

import java.util.*;

/**
 * Implementation of the <tt>MetaTypeProvider</tt> interface.
 */
public class MTP implements MetaTypeProvider, PIDProvider {

  Map ocdMap = new HashMap();

  Set services  = new HashSet();
  Set factories = new HashSet();

  String id;

  public MTP(String id) {
    this.id = id;
  }
  
  public void addService(String pid, ObjectClassDefinition ocd) {
    ocdMap.put(pid, ocd);
    services.add(pid);
  }

  public void addFactory(String pid, ObjectClassDefinition ocd) {
    ocdMap.put(pid, ocd);
    factories.add(pid);
  }


  static String[] toStringArray(Set set) {
    String[] sa = new String[set.size()];
    int i = 0;
    for(Iterator it = set.iterator(); it.hasNext();) {
      sa[i++] = it.next().toString();
    }
    return sa;
  }

  public String[] getPids() {
    return toStringArray(services);
  }

  public String[] getFactoryPids() {
    return toStringArray(factories);
  }

  public String[] getLocales() {
    return null;
  }

  public ObjectClassDefinition getObjectClassDefinition(String pid, 
							String locale) {

    ObjectClassDefinition  ocd = (ObjectClassDefinition)ocdMap.get(pid);

    return ocd;
  }

  public String toString() {
    return toString(true);
  }

  public String toString(boolean bFull) {
    StringBuffer sb = new StringBuffer();


    sb.append("MTP[\n");
    for(Iterator it = ocdMap.keySet().iterator(); it.hasNext();) {
      String pid = (String)it.next();
      ObjectClassDefinition ocd = (ObjectClassDefinition)ocdMap.get(pid);
      if(bFull) {
	sb.append(pid + "=");
	sb.append(ocd);
      } else {
	sb.append(pid);
      }
      if(it.hasNext()) {
	sb.append("\n");
      }
    }
    sb.append("\n/MTP]");

    
    return sb.toString();
  }

  public String getId() {
    return id;
  }

  public int compareTo(Object other) {
    return id.compareTo(((MTP)other).id);
  }
  
  public int hashCode() {
    return id.hashCode();
  }
  
  public boolean equals(Object other) {
    if(other == null || !(other instanceof MTP)) {
      return false;
    }

    return id.equals(((MTP)other).id);
  }
}
 
