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
package org.knopflerfish.framework;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

public class BundleNameVersionCapability implements BundleCapability {

  private final BundleGeneration gen;
  private final String namespace;

  BundleNameVersionCapability(BundleGeneration bundleGeneration, String namespace) {
    gen = bundleGeneration;
    this.namespace = namespace;
  }

  public String getNamespace() {
    return namespace;
  }

  public Map<String, String> getDirectives() {
    return Collections.unmodifiableMap(gen.symbolicNameParameters.getDirectives());
  }

  public Map<String, Object> getAttributes() {
    final Map<String,Object> res = new HashMap<String, Object>(gen.symbolicNameParameters.getAttributes());

    res.put(namespace, gen.symbolicName);
    res.put(Constants.BUNDLE_VERSION_ATTRIBUTE, gen.version);
    return Collections.unmodifiableMap(res);
  }

  public BundleRevision getRevision() {
    return gen.getBundleRevision();
  }


  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((gen == null) ? 0 : gen.hashCode());
    result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
    return result;
  }

  // TODO, should we be equal when gen's are different
  // but the directives and attributes are the same.
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    BundleNameVersionCapability other = (BundleNameVersionCapability) obj;
    if (!gen.equals(other.gen))
      return false;
    if (!namespace.equals(other.namespace))
      return false;
    return true;
  }

  public String toString() {
    return "BundleNameVersionCapability[nameSpace=" + namespace + ", attributes=" +
        getAttributes() + ", directives=" + getDirectives() + ", revision=" +
        getRevision() + "]";
  }

}
