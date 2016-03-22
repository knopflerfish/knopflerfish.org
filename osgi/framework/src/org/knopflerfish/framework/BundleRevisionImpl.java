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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.CapabilityRefDTO;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.resource.dto.RequirementRefDTO;

public class BundleRevisionImpl
  extends BundleReferenceImpl
  implements BundleRevision
{

  static final int NS_BUNDLE =    1;
  static final int NS_HOST =      2;
  static final int NS_IDENTITY =  4;
  static final int NS_PACKAGE =   8;
  static final int NS_OTHER =    16;

  final BundleGeneration gen;
  private BundleWiringImpl bundleWiring = null;


  BundleRevisionImpl(BundleGeneration gen) {
    super(gen.bundle);
    this.gen = gen;
  }


  @Override
  public String getSymbolicName() {
    return gen.symbolicName;
  }


  @Override
  public Version getVersion() {
    return gen.version;
  }


  @Override
  public List<BundleCapability> getDeclaredCapabilities(String namespace) {
    final ArrayList<BundleCapability> res = new ArrayList<BundleCapability>();
    final int ns = whichNameSpaces(namespace);

    if ((ns & NS_IDENTITY) != 0) {
      final BundleCapability bc = gen.identity;
      if (bc!=null) {
        res.add(bc);
      }
    }

    if ((ns & NS_BUNDLE) != 0) {
      final BundleCapability bc = gen.bundleCapability;
      if (bc!=null) {
        res.add(bc);
      }
    }

    if ((ns & NS_HOST) != 0) {
      final BundleCapability bc = gen.hostCapability;
      if (bc!=null) {
        res.add(bc);
      }
    }

    if ((ns & NS_PACKAGE) != 0) {
      res.addAll(gen.bpkgs.getDeclaredPackageCapabilities());
    }

    if ((ns & NS_OTHER) != 0) {
      final Map<String, List<BundleCapabilityImpl>> caps = gen.getDeclaredCapabilities();
      if (null != namespace) {
        final List<BundleCapabilityImpl> lcap = caps.get(namespace);
        if (lcap != null) {
          res.addAll(lcap);
        }
      } else {
        for (final List<BundleCapabilityImpl> lcap : caps.values()) {
          res.addAll(lcap);
        }
      }
    }

    return res;
  }


  @Override
  public List<BundleRequirement> getDeclaredRequirements(String namespace) {
    final ArrayList<BundleRequirement> res = new ArrayList<BundleRequirement>();
    final int ns = whichNameSpaces(namespace);

    if ((ns & NS_BUNDLE) != 0) {
      res.addAll(gen.bpkgs.getDeclaredBundleRequirements());
    }

    if ((ns & NS_HOST) != 0) {
      if (gen.isFragment()) {
        res.add(gen.fragment);
      }
    }

    if ((ns & NS_PACKAGE) != 0) {
      res.addAll(gen.bpkgs.getDeclaredPackageRequirements());
    }

    if ((ns & (NS_IDENTITY|NS_OTHER)) != 0) {
      final Map<String, List<BundleRequirementImpl>> reqs = gen.getDeclaredRequirements();
      if (null != namespace) {
        final List<BundleRequirementImpl> lbr = reqs.get(namespace);
        if (lbr != null) {
          res.addAll(lbr);
        }
      } else {
        for (final List<BundleRequirementImpl> lbr : reqs.values()) {
          res.addAll(lbr);
        }
      }
    }

    return res;
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<Capability> getCapabilities(String namespace) {
    return (List<Capability>)(List<?>)getDeclaredCapabilities(namespace);
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<Requirement> getRequirements(String namespace) {
    return (List<Requirement>)(List<?>)getDeclaredRequirements(namespace);
  }


  @Override
  public int getTypes() {
    return gen.isFragment() ? TYPE_FRAGMENT : 0;
  }


  @Override
  public BundleWiring getWiring() {
    return bundleWiring;
  }


  @Override
  public String toString() {
    return "BundleRevision[" + getSymbolicName() + ":" + getVersion() + "]";
  }


  BundleGeneration getBundleGeneration() {
    return gen;
  }
  

  BundleWiringImpl getWiringImpl() {
    return bundleWiring;
  }


  void setWired() {
    bundleWiring = new BundleWiringImpl(this);
  }


  void clearWiring() {
    bundleWiring = null;
  }


  BundleRevisionDTO getDTO() {
    BundleRevisionDTO res = new BundleRevisionDTO();
    res.symbolicName = gen.symbolicName;
    res.type = getTypes();
    res.version = gen.version.toString();
    res.bundle = gen.bundle.id;
    res.id = dtoId;
    res.capabilities = new ArrayList<CapabilityDTO>();
    for (BundleCapability bc :  getDeclaredCapabilities(null)) {
      res.capabilities.add(BundleCapabilityImpl.getDTO(bc, this));
    }
    res.requirements = new ArrayList<RequirementDTO>();
    for (BundleRequirement br :  getDeclaredRequirements(null)) {
      res.requirements.add(BundleRequirementImpl.getDTO(br, this));
    }
    return res;
  }


  List<CapabilityRefDTO> getCapabilityRefDTOs() {
    List<CapabilityRefDTO> res = new ArrayList<CapabilityRefDTO>();
    for (BundleCapability bc :  getDeclaredCapabilities(null)) {
      res.add(BundleCapabilityImpl.getRefDTO(bc, this));
    }
    return res;
  }


  List<RequirementRefDTO> getRequirementRefDTOs() {
    List<RequirementRefDTO> res = new ArrayList<RequirementRefDTO>();
    for (BundleRequirement br :  getDeclaredRequirements(null)) {
      res.add(BundleRequirementImpl.getRefDTO(br, this));
    }
    return res;
  }


  static int whichNameSpaces(String namespace) {
    int ns;
    if (namespace == null) {
      ns = NS_BUNDLE|NS_HOST|NS_IDENTITY|NS_PACKAGE|NS_OTHER;
    } else if (BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
      ns = NS_BUNDLE;
    } else if (BundleRevision.HOST_NAMESPACE.equals(namespace)) {
      ns = NS_HOST;
    } else if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace)) {
      ns = NS_IDENTITY;
    } else if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
      ns = NS_PACKAGE;
    } else {
      ns = NS_OTHER;
    }
    return ns;
  }

}
