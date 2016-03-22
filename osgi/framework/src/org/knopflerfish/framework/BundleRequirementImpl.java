/*
 * Copyright (c) 2013-2016, KNOPFLERFISH project
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

import org.knopflerfish.framework.Util.HeaderEntry;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.resource.dto.RequirementRefDTO;


public class BundleRequirementImpl
  extends DTOId
  implements BundleRequirement
{

  private final BundleGeneration gen;
  private final String namespace;
  private final Map<String, Object> attributes;
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
    namespace = he.getKey();
    for (final String ns : Arrays
        .asList(new String[] { BundleRevision.BUNDLE_NAMESPACE,
                               BundleRevision.HOST_NAMESPACE,
                               BundleRevision.PACKAGE_NAMESPACE })) {
      if (ns.equals(namespace)) {
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
                           + " for name-space " + namespace + ": " + ise;
        throw (IllegalArgumentException)
          new IllegalArgumentException(msg).initCause(ise);
      }
    } else {
      filter = null;
    }
    directives = Collections.unmodifiableMap(he.getDirectives());
    attributes = Collections.unmodifiableMap(he.getAttributes());
  }

  /**
   * Creates a {@link BundleRequirement} from a
   * "Bundle-RequiredExecutionEnvironment" manifest header.
   *
   * @param gen
   *          the owning bundle revision.
   * @param ee
   *          the entry from the "Bundle-RequiredExecutionEnvironment" manifest header.
   */
  @SuppressWarnings("unchecked")
  BundleRequirementImpl(final BundleGeneration gen, final String ee)
  {
    this.gen = gen;
    namespace = ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE;

    final StringBuffer filterStrB = new StringBuffer();
    final String[] l = Util.splitwords(ee, ",");
    if (l.length > 1) {
      filterStrB.append("(|");
    }
    for (final String e : l) {
      final String[] es = Util.splitwords(e, "-");
      try {
        if (es.length == 2) {
          final int si = es[1].indexOf('/');
          new Version(si == -1 ? es[1] : es[1].substring(0, si));
          filterStrB.append("(&(").append(namespace).append('=');
          if (es[0].equalsIgnoreCase("J2SE")) {
            es[0]="JavaSE";
          }
          filterStrB.append(es[0]);
          if (si != -1) {
            filterStrB.append(es[1].substring(si));
          }
          filterStrB.append(")(version=").append(es[1]).append("))");
          continue;
        } else if (es.length > 2) {
          final StringBuffer esStrB = new StringBuffer(es[0]);
          Version v = null;
          for (int i = 1; i < es.length; i++) {
            if (Character.isDigit(es[i].charAt(0))) {
              if (v == null) {
                final int si = es[i].indexOf('/');
                v = new Version(si == -1 ? es[i] : es[i].substring(0, si));
                if (si != -1) {
                  esStrB.append(es[1].substring(si));
                } else if (i != es.length - 1) {
                  throw new IllegalArgumentException("Version not at end");                  
                }
              } else {
                if (v.equals(new Version(es[i])) && i == es.length - 1) {
                  break;
                }
                throw new IllegalArgumentException("Version mismatch");
              }
            } else {
              esStrB.append('-').append(es[i]);
            }
          }
          if (v != null) {
            filterStrB.append("(&(").append(namespace).append('=');
            filterStrB.append(esStrB).append(")(version=");
            filterStrB.append(v).append("))");
            continue;
          }
        }
      } catch (IllegalArgumentException _ignore) { }
      filterStrB.append('(').append(namespace).append('=');
      filterStrB.append(e).append(')');
    }
    if (l.length > 1) {
      filterStrB.append(')');
    }
    
    try {
      filter = FrameworkUtil.createFilter(filterStrB.toString());
    } catch (final InvalidSyntaxException ise) {
      throw new RuntimeException("Internal error");
    }
    directives = Collections.singletonMap("filter", filter.toString());
    attributes = Collections.EMPTY_MAP;
  }

  @Override
  public String getNamespace()
  {
    return namespace;
  }

  @Override
  public Map<String, String> getDirectives()
  {
    return directives;
  }

  @Override
  public Map<String, Object> getAttributes()
  {
    return attributes;
  }

  @Override
  public BundleRevision getRevision()
  {
    return gen.bundleRevision;
  }


  @Override
  public BundleRevision getResource() {
	return gen.bundleRevision;
  }


  @Override
  public boolean matches(BundleCapability capability) {
    if (namespace.equals(capability.getNamespace())) {
      return null==filter ? true : filter.matches(capability.getAttributes());
    }
    return false;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer(40);

    sb.append("[")
    .append(BundleRequirement.class.getName())
    .append(": ")
    .append(namespace)
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
    if (wire != null) {
      ((BundleCapabilityImpl)wire.getCapability()).removeWire(this.wire);
      this.wire = null;
    }
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


  static RequirementDTO getDTO(BundleRequirement br, BundleRevisionImpl bri) {
    RequirementDTO res = new RequirementDTO();
    res.id = ((DTOId)br).dtoId;
    res.namespace = br.getNamespace();
    res.directives = new HashMap<String, String>(br.getDirectives());
    res.attributes = Util.safeDTOMap(br.getAttributes());
    res.resource = bri.dtoId;
    return res;
  }


  static RequirementRefDTO getRefDTO(BundleRequirement br, BundleRevisionImpl bri) {
    RequirementRefDTO res = new RequirementRefDTO();
    res.requirement = ((DTOId)br).dtoId;
    res.resource = bri.dtoId;
    return res;
  }

}
