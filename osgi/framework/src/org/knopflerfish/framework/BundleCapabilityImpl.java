/*
 * Copyright (c) 2013-2022, KNOPFLERFISH project
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.knopflerfish.framework.Util.HeaderEntry;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.CapabilityRefDTO;

/**
 * Implementation of bundle capability for generic capabilities specified in the
 * Bundle-Capability header.
 */
public class BundleCapabilityImpl extends DTOId implements BundleCapability {
  private final BundleGeneration gen;
  private final BundleGeneration owner;
  private final String namespace;
  Map<String,Object> attributes;
  Map<String,String> directives;
  private Vector<BundleWireImpl> wires = new Vector<BundleWireImpl>(2);

  /**
   * Creates a {@link BundleCapability} from one entry of the Bundle-Capability
   * header.
   *
   * @param gen
   *          the owning bundle revision.
   * @param he
   *          the parsed bundle capability header entry.
   */
  public BundleCapabilityImpl(final BundleGeneration gen, final HeaderEntry he)
  {
    this.gen = gen;
    owner = gen;
    namespace = he.getKey();
    if (gen.bundle.id != 0) {
      for (final String ns : Arrays
             .asList(new String[] { BundleRevision.BUNDLE_NAMESPACE,
                                    BundleRevision.HOST_NAMESPACE,
                                    BundleRevision.PACKAGE_NAMESPACE,
                                    ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE,
                                    IdentityNamespace.IDENTITY_NAMESPACE,
                                    NativeNamespace.NATIVE_NAMESPACE})) {
        if (ns.equals(namespace)) {
          throw new IllegalArgumentException("Capability with name-space '" + ns
                                             + "' must not be provided in the "
                                             + Constants.PROVIDE_CAPABILITY
                                             + " manifest header.");
        }
      }
    }
    attributes = Collections.unmodifiableMap(he.getAttributes());
    directives = Collections.unmodifiableMap(he.getDirectives());
  }

  public BundleCapabilityImpl(BundleCapability bc, BundleGeneration bg) {
    gen = bg;
    owner = ((BundleCapabilityImpl)bc).owner;
    namespace = bc.getNamespace();
    attributes = bc.getAttributes();
    directives = bc.getDirectives();
  }

  /**
   * IdentityNamespace capability
   */
  public BundleCapabilityImpl(BundleGeneration bg) {
    gen = bg;
    owner = gen;
    namespace = IdentityNamespace.IDENTITY_NAMESPACE;
    Map<String,Object> attrs = new HashMap<String, Object>();
    attrs.put(IdentityNamespace.IDENTITY_NAMESPACE, gen.symbolicName);
    attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
              gen.fragment != null ? IdentityNamespace.TYPE_FRAGMENT : IdentityNamespace.TYPE_BUNDLE);
    attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, gen.version);
    if (gen.archive != null) {
      String a = gen.archive.getAttribute(Constants.BUNDLE_COPYRIGHT);
      if (a != null) {
        attrs.put(IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE, a);
      }
      a = gen.archive.getAttribute(Constants.BUNDLE_DESCRIPTION);
      if (a != null) {
        attrs.put(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE, a);
      }
      a = gen.archive.getAttribute(Constants.BUNDLE_DOCURL);
      if (a != null) {
        attrs.put(IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE, a);
      }
      a = gen.archive.getAttribute("Bundle-License");
      if (a != null) {
        StringBuilder sb = new StringBuilder();
        try {
          List<HeaderEntry> lic = Util.parseManifestHeader("Bundle-License", a, true, true, false);
          for (HeaderEntry he : lic) {
            if (sb.length() > 0) {
              sb.append(", ");
            }
            sb.append(he.getKey());
          }
        } catch (IllegalArgumentException iae) {
          gen.bundle.fwCtx.frameworkInfo(gen.bundle, iae);
          sb.append(a);
        }
        attrs.put(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE, sb.toString());
      }
    }
    attributes = Collections.unmodifiableMap(attrs);
    Map<String,String> dirs = new HashMap<String, String>();
    dirs.put(Constants.SINGLETON_DIRECTIVE, gen.singleton ? "true" : "false");
    // TODO, should we try to find classfiers?
    directives = Collections.unmodifiableMap(dirs);
  }

  /**
   * NativeNamespace capability
   */
  BundleCapabilityImpl(final BundleGeneration gen, final FWProps fwProps)
  {
    this.gen = gen;
    owner = gen;
    namespace = NativeNamespace.NATIVE_NAMESPACE;
    attributes = fwProps.getFWProperties();
    for (Iterator<Entry<String,Object>> i = attributes.entrySet().iterator(); i.hasNext(); ) {
      if (i.next().getKey().startsWith(NativeNamespace.NATIVE_NAMESPACE + ".")) {
        i.remove();
      }
    }
    final ArrayList<String> proc = new ArrayList<String>(3);
    final String procP = fwProps.getProperty(Constants.FRAMEWORK_PROCESSOR);
    proc.add(procP);
    final String procS = System.getProperty("os.arch");
    if (!procP.equalsIgnoreCase(procS)) {
      proc.add(procS);
    }
    // Handle deprecated value "arm"
    if (procP.startsWith("arm_")) {
      proc.add("arm");
    }
    for (int i = 0; i < Alias.processorAliases.length; i++) {
      if (procP.equalsIgnoreCase(Alias.processorAliases[i][0])) {
        for (int j = 1; j < Alias.processorAliases[i].length; j++) {
          if (!procS.equalsIgnoreCase(Alias.processorAliases[i][j])) {
            proc.add(Alias.processorAliases[i][j]);
          }
        }
        break;
      }
    }
    attributes.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, proc);
    final ArrayList<String> os = new ArrayList<String>();
    final String osP = fwProps.getProperty(Constants.FRAMEWORK_OS_NAME);
    os.add(osP);
    final String osS = System.getProperty("os.name");
    if (!osS.equalsIgnoreCase(osP)) {
      os.add(osS);
    }
    for (int i = 0; i < Alias.osNameAliases.length; i++) {
      if (osP.equalsIgnoreCase(Alias.osNameAliases[i][0])) {
        for (int j = 1; j < Alias.osNameAliases[i].length; j++) {
          if (!osS.equalsIgnoreCase(Alias.osNameAliases[i][j])) {
            os.add(Alias.osNameAliases[i][j]);
          }
        }
        break;
      }
    }
    attributes.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, os);
    attributes.put(NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE,
                   new Version(fwProps.getProperty(Constants.FRAMEWORK_OS_VERSION)));
    attributes.put(NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE,
                   fwProps.getProperty(Constants.FRAMEWORK_LANGUAGE));
    attributes = Collections.unmodifiableMap(attributes);
    directives = Collections.EMPTY_MAP;
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public Map<String, String> getDirectives() {
    return directives;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public BundleRevision getRevision() {
    return owner.bundleRevision;
  }

  @Override
  public BundleRevision getResource() {
	return owner.bundleRevision;
  }

  @Override
  public String toString() {
    return "BundleCapability[namespace=" + namespace + ", attributes=" + attributes +
        ", directives=" + directives + ", revision=" + getRevision() + "]";
  }

  BundleGeneration getBundleGeneration() {
    return gen;
  }

  boolean isEffectiveResolve() {
    final String effective = directives.get(Constants.EFFECTIVE_DIRECTIVE);
    return effective == null || effective.equals(Constants.EFFECTIVE_RESOLVE);
  }

  boolean isZombie() {
    return !gen.isCurrent();
  }

  void addWire(BundleWireImpl bw) {
    wires.add(bw);
  }

  void getWires(List<BundleWireImpl> res) {
    synchronized (wires) {
      res.addAll(wires);
    }
  }

  void removeWire(BundleWireImpl wire) {
    wires.remove(wire);
  }

  void removeWires() {
    wires.clear();
  }

  boolean isWired() {
    return !wires.isEmpty();
  }

  /**
   * Check if we have provide permissions.
   *
   * @return true if we have provide permission
   */
  boolean checkPermission() {
    return gen.bundle.fwCtx.perm.hasProvidePermission(this);
  }

  Set<String> getUses() {
    try {
      return Util.parseEnumeration(Constants.USES_DIRECTIVE,
                                   directives.get(Constants.USES_DIRECTIVE));
    } catch (IllegalArgumentException iae) {
      final BundleImpl b = gen.bundle;
      b.fwCtx.frameworkError(b, iae);
    }
    return null;
  }

  static CapabilityDTO getDTO(BundleCapability bc, BundleRevisionImpl bri) {
    CapabilityDTO res = new CapabilityDTO();
    res.id = ((DTOId)bc).dtoId;
    res.namespace = bc.getNamespace();
    res.directives = new HashMap<String, String>(bc.getDirectives());
    res.attributes = Util.safeDTOMap(bc.getAttributes());
    res.resource = bri.dtoId;
    return res;
  }

  static CapabilityRefDTO getRefDTO(BundleCapability bc, BundleRevisionImpl bri) {
    CapabilityRefDTO res = new CapabilityRefDTO();
    res.capability = ((DTOId)bc).dtoId;
    res.resource = bri.dtoId;
    return res;
  }

}
