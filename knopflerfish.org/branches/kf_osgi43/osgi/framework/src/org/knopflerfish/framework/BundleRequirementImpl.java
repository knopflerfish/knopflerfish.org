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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

import org.knopflerfish.framework.Util.HeaderEntry;

public class BundleRequirementImpl
  implements BundleRequirement
{

  private final BundleGeneration gen;
  private final String nameSpace;
  private final Map<String,String> directives;
  private final Filter filter;
  private BundleWireImpl wire = null;


  /**
   * Creates a {@link BundleRequirement} from one entry in the parsed
   * "Require-Capability" manifest header.
   *
   * @param gen
   *          the owning bundle revision.
   * @param he
   *          the parsed entry from the "Require-Capability" manifest header.
   */
  BundleRequirementImpl(final BundleGeneration gen, final HeaderEntry he)
  {
    this.gen = gen;
    nameSpace = he.getKey();
    for (final String ns : Arrays
        .asList(new String[] { BundleRevision.BUNDLE_NAMESPACE,
                               BundleRevision.HOST_NAMESPACE,
                               BundleRevision.PACKAGE_NAMESPACE })) {
      if (ns.equals(nameSpace)) {
        throw new IllegalArgumentException("Capability with name-space '" + ns
                                           + "' must not be required in the "
                                           + Constants.REQUIRE_CAPABILITY
                                           + " manifest header.");
      }
    }

    final String filterStr = he.getDirectives().remove("filter");
    if (null!=filterStr && filterStr.length()>0) {
      try {
        filter = FrameworkUtil.createFilter(filterStr);
        he.getDirectives().put("filter", filter.toString());
      } catch (final InvalidSyntaxException ise) {
        final String msg = "Invalid filter '" + filterStr + "' in "
                           + Constants.REQUIRE_CAPABILITY
                           + " for name-space " + nameSpace + ": " + ise;
        throw (IllegalArgumentException)
          new IllegalArgumentException(msg).initCause(ise);
      }
    } else {
      filter = null;
    }
    directives = Collections.unmodifiableMap(he.getDirectives());
    
    // TODO, warn about defined attributes
  }


  public String getNamespace()
  {
    return nameSpace;
  }


  public Map<String, String> getDirectives()
  {
    return directives;
  }


  @SuppressWarnings("unchecked")
  public Map<String, Object> getAttributes()
  {
    return Collections.EMPTY_MAP;
  }


  public BundleRevision getRevision()
  {
    return gen.bundleRevision;
  }


  public boolean matches(BundleCapability capability)
  {
    if (nameSpace.equals(capability.getNamespace())) {
      return null==filter ? true : filter.matches(capability.getAttributes());
    }
    return false;
  }


  public String toString() {
    final StringBuffer sb = new StringBuffer(40);

    sb.append("[")
    .append(BundleRequirement.class.getName())
    .append(": ")
    .append(nameSpace)
    .append(" directives: ")
    .append(directives.toString())
    .append("]");

    return sb.toString();
  }


  BundleGeneration getBundleGeneration() {
    return gen;
  }


  BundleWireImpl getWire() {
    return wire;
  }


  void resetWire() {
    ((BundleCapabilityImpl)wire.getCapability()).removeWire(this.wire);
    this.wire = null;
  }


  void setWire(BundleWireImpl wire) {
    ((BundleCapabilityImpl)wire.getCapability()).addWire(wire);
    this.wire = wire;
  }


  boolean isOptional() {
    final String resolution = directives.get(Constants.RESOLUTION_DIRECTIVE);
    return Constants.RESOLUTION_OPTIONAL.equals(resolution);
  }


  boolean shouldResolve() {
    final String effective = directives.get(Constants.EFFECTIVE_DIRECTIVE);
    return effective == null || effective.equals(Constants.EFFECTIVE_RESOLVE);
  }


  boolean isWired() {
    return wire != null;
  }
}
