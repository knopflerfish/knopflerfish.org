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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Implementation of bundle capability for generic capabilities specified in the
 * Bundle-Capability header.
 */
public class BundleCapabilityImpl implements BundleCapability {
  private final BundleGeneration gen;
  private final String nameSpace;
  private final Map<String,Object> attributes;
  private final Map<String,String> directives = new HashMap<String, String>();

  /**
   * Creates a {@link BundleCapability} from the output of the
   * {@link FrameworkUtil#parseEntries() } applied to one entry in the
   * Bundle-Capability header.
   *
   * @param gen the owning bundle revision.
   * @param tokens the parsed data for this requirement.
   */
  public BundleCapabilityImpl(BundleGeneration gen, Map<String, Object> tokens)
  {
    this.gen = gen;
    nameSpace = (String) tokens.remove("$key");
    for (final String ns : Arrays
        .asList(new String[] { BundleRevision.BUNDLE_NAMESPACE,
                               BundleRevision.HOST_NAMESPACE,
                               BundleRevision.PACKAGE_NAMESPACE })) {
      if (ns.equals(nameSpace)) {
        throw new IllegalArgumentException("Capability with name-space '" + ns
                                           + "' must not be provided in the "
                                           + Constants.PROVIDE_CAPABILITY
                                           + " manifest header.");
      }
    }

    // Move directives to directives map
    @SuppressWarnings("unchecked")
    final Set<String> directiveNames = (Set<String>) tokens.remove("$directives");
    for (final String directiveName : directiveNames) {
      directives.put(directiveName, (String) tokens.remove(directiveName));
    }

    attributes = Collections.unmodifiableMap(tokens);
  }

  public String getNamespace() {
    return nameSpace;
  }

  public Map<String, String> getDirectives() {
    return Collections.unmodifiableMap(directives);
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public BundleRevision getRevision() {
    return gen.getRevision();
  }

}
