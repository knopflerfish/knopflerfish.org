/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.repository.xml;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;


public class CapabilityImpl implements Capability {
  final Data d;
  CapabilityImpl(Data d) {
    this.d = d;
  }

  @Override
  public String getNamespace() {
    return d.namespace;
  }

  @Override
  public Map<String, String> getDirectives() {
    return d.directives;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return d.attributes;
  }

  @Override
  public Resource getResource() {
    return d.resource;
  }
  
  public String toString() {
    return "Capability[\n" + d.toString() +"]\n";
  }
  
  public boolean equals(Object other) {
    if (this == other)
      return true;
    if (other == null)
      return false;
    if (!(other instanceof Capability))
      return false;
    Capability c = (Capability)other;
    return 
        getNamespace().equals(c.getNamespace()) &&
        getDirectives().equals(c.getDirectives()) &&
        getAttributes().equals(c.getAttributes())  &&
        getResource() == c.getResource();
        // getResource().equals(c.getResource());
  }
}
