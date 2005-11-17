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

package org.knopflerfish.framework;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.*;

public class FilterImpl implements Filter {

  private String filter = null;
  private LDAPExpr ldap;


  protected FilterImpl(String filter) throws InvalidSyntaxException {
    ldap = new LDAPExpr(filter);
  }


  public boolean match(ServiceReference reference) {
    if(reference instanceof ServiceReferenceImpl) {
      // This is the normal case
      return ldap.evaluate(((ServiceReferenceImpl)reference).getProperties(), false);
    } else {
      // This might happen if we live in a remote framework world
      // Copy the properties the hard way
      Hashtable props = new Hashtable();
      String[] keys = reference.getPropertyKeys();
      for(int i = 0; i < keys.length; i++) {
    	  props.put(keys[i], reference.getProperty(keys[i]));
      }
      return ldap.evaluate(props, false);
    }
  }


  public boolean match(Dictionary dictionary) {
    return ldap.evaluate(new PropertiesDictionary(dictionary), false);
  }
  
  public boolean matchCase(Dictionary dictionary) {
	  return ldap.evaluate(new PropertiesDictionary(dictionary), true);
  }


  public String toString() {
    if (filter == null) {
      filter = ldap.toString();
    }
    return filter;
  }


  public boolean equals(Object obj) {
    return toString().equals(obj.toString());
  }


  public int hashCode() {
    return toString().hashCode();
  }
}
