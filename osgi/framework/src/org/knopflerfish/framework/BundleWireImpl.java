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

import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.dto.BundleWireDTO;


public class BundleWireImpl implements BundleWire {
  
  private final BundleCapability capability;
  private final BundleRequirement requirement;
  private final BundleGeneration providerGen;
  private final BundleGeneration requirerGen;

  BundleWireImpl(BundleCapability capability, BundleGeneration provider,
                 BundleRequirement requirement, BundleGeneration requirer) {
    this.capability = capability;
    this.providerGen = provider;
    this.requirement = requirement;
    this.requirerGen = requirer;
  }


  @Override
  public BundleCapability getCapability() {
    return capability;
  }


  @Override
  public BundleRequirement getRequirement() {
    return requirement;
  }


  @Override
  public BundleWiring getProviderWiring() {
    return providerGen.bundleRevision.getWiring();
  }


  @Override
  public BundleWiring getRequirerWiring() {
    return requirerGen.bundleRevision.getWiring();
  }


  @Override
  public BundleRevision getProvider() {
    // TODO What should we return if getWiring() is null
    return providerGen.bundleRevision;
  }


  @Override
  public BundleRevision getRequirer() {
    // TODO What should we return if getWiring() is null
    return requirerGen.bundleRevision;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((capability == null) ? 0 : capability.hashCode());
    result = prime * result + ((providerGen == null) ? 0 : providerGen.hashCode());
    result = prime * result + ((requirement == null) ? 0 : requirement.hashCode());
    result = prime * result + ((requirerGen == null) ? 0 : requirerGen.hashCode());
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof BundleWireImpl)) {
      return false;
    }
    BundleWireImpl other = (BundleWireImpl) obj;
    if (capability == null) {
      if (other.capability != null) {
        return false;
      }
    } else if (!capability.equals(other.capability)) {
      return false;
    }
    if (providerGen == null) {
      if (other.providerGen != null) {
        return false;
      }
    } else if (!providerGen.equals(other.providerGen)) {
      return false;
    }
    if (requirement == null) {
      if (other.requirement != null) {
        return false;
      }
    } else if (!requirement.equals(other.requirement)) {
      return false;
    }
    if (requirerGen == null) {
      if (other.requirerGen != null) {
        return false;
      }
    } else if (!requirerGen.equals(other.requirerGen)) {
      return false;
    }
    return true;
  }

 
  BundleGeneration getProviderGeneration() {
    return providerGen;
  }


  BundleGeneration getRequirerGeneration() {
    return requirerGen;
  }


  BundleWireDTO getDTO() {
    BundleWireDTO res = new BundleWireDTO();
    res.providerWiring = providerGen.bundleRevision.getWiringImpl().dtoId;
    res.requirerWiring = requirerGen.bundleRevision.getWiringImpl().dtoId;
    res.capability = BundleCapabilityImpl.getRefDTO(capability, providerGen.bundleRevision);
    res.requirement = BundleRequirementImpl.getRefDTO(requirement, requirerGen.bundleRevision);
    res.provider = providerGen.bundleRevision.dtoId;
    res.requirer = requirerGen.bundleRevision.dtoId;
    return res;
  }

}
