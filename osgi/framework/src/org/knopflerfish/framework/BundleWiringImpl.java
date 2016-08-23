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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.framework.wiring.dto.BundleWireDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO.NodeDTO;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;
import org.osgi.resource.dto.WireDTO;


public class BundleWiringImpl
  extends DTOId
  implements BundleWiring
{
  final BundleRevisionImpl bundleRevision;


  BundleWiringImpl(BundleRevisionImpl br) {
    bundleRevision = br;
  }


  public Bundle getBundle() {
    return bundleRevision.getBundle();
  }


  public boolean isCurrent() {
    return this == bundleRevision.getWiring() &&
           bundleRevision.bundle.current() == bundleRevision.gen;
  }


  public boolean isInUse() {
    return this == bundleRevision.getWiring();
  }


  public List<BundleCapability> getCapabilities(String namespace) {
    if (!isInUse()) {
      return null;
    }
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    final int ns = BundleRevisionImpl.whichNameSpaces(namespace);
    final ArrayList<BundleCapability> res = new ArrayList<BundleCapability>();
    if ((ns & BundleRevisionImpl.NS_IDENTITY) != 0) {
      if (gen.identity != null) {
        res.add(gen.identity);
      }
    }
    if (!gen.isFragment()) {
      if ((ns & BundleRevisionImpl.NS_BUNDLE) != 0) {
        if (gen.bundleCapability != null) {
          res.add(gen.bundleCapability);
        }
      }
      if ((ns & BundleRevisionImpl.NS_HOST) != 0) {
        if (gen.hostCapability != null) {
          res.add(gen.hostCapability);
        }
      }
      if ((ns & BundleRevisionImpl.NS_PACKAGE) != 0) {
        for (ExportPkg ep : gen.bpkgs.getPackageCapabilities()) {
          if (ep.checkPermission()) {
            res.add(ep);
          }
        }
      }
      if ((ns & (BundleRevisionImpl.NS_NATIVE|BundleRevisionImpl.NS_OTHER)) != 0) {
        final Map<String, List<BundleCapabilityImpl>> caps = gen.bpkgs.getOtherCapabilities();
        Collection<List<BundleCapabilityImpl>> clbc = null;
        if (null != namespace) {
          final List<BundleCapabilityImpl> lbc = caps.get(namespace);
          if (lbc != null) {
            clbc  = Collections.singleton(lbc);
          }
        } else {
          clbc = caps.values();
        }
        if (null != clbc) {
          for (final List<BundleCapabilityImpl> lbc : clbc) {
            for (final BundleCapabilityImpl bc : lbc) {
              if (bc.isEffectiveResolve() && bc.checkPermission()) {
                res.add(bc);
              }
            }
          }
        }
      }
    }
    return res;
  }


  public List<BundleRequirement> getRequirements(String namespace) {
    if (!isInUse()) {
      return null;
    }
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    final int ns = BundleRevisionImpl.whichNameSpaces(namespace);
    final ArrayList<BundleRequirement> res = new ArrayList<BundleRequirement>();
    if (gen.isFragment()) {
      if ((ns & BundleRevisionImpl.NS_HOST) != 0) {
        res.add(gen.fragment);
      }
    } else {
      if ((ns & BundleRevisionImpl.NS_BUNDLE) != 0) {
        for (final Iterator<RequireBundle> irb = gen.bpkgs.getRequire(); irb.hasNext(); ) {
          final RequireBundle rb = irb.next();
          if (null != rb.bpkgs && rb.bpkgs.isRequiredBy(gen.bpkgs)) {
            res.add(rb);
          }
        }
      }
      if ((ns & BundleRevisionImpl.NS_PACKAGE) != 0) {
        res.addAll(gen.bpkgs.getPackageRequirements());
      }
      if ((ns & BundleRevisionImpl.NS_NATIVE) != 0 && gen.nativeRequirement != null) {
        res.add(gen.nativeRequirement);
      }
      if ((ns & (BundleRevisionImpl.NS_IDENTITY|BundleRevisionImpl.NS_OTHER)) != 0) {
        final Map<String, List<BundleRequirementImpl>> reqs = gen.getOtherRequirements();
        Collection<List<BundleRequirementImpl>> clbr = null;
        if (null != namespace) {
          final List<BundleRequirementImpl> lbr = reqs.get(namespace);
          if (lbr != null) {
            clbr = Collections.singleton(lbr);
          }
        } else {
          clbr = reqs.values();
        }
        if (null != clbr) {
          for (final List<BundleRequirementImpl> lbr : clbr) {
            for (final BundleRequirementImpl br : lbr) {
              if (br.isWired()) {
                res.add(br);
              }
            }
          }
        }
      }
    }
    return res;
  }

  public List<BundleWire> getProvidedWires(String namespace) {
    if (!isInUse()) {
      return null;
    }
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    final int ns = BundleRevisionImpl.whichNameSpaces(namespace);
    final ArrayList<BundleWire> res = new ArrayList<BundleWire>();
    if ((ns & BundleRevisionImpl.NS_BUNDLE) != 0) {
      final List<BundlePackages> reqBys = gen.bpkgs.getRequiredBy();
      for (final BundlePackages bp : reqBys) {
        for (final Iterator<RequireBundle> irb = bp.getRequire(); irb.hasNext(); ) {
          final RequireBundle rb = irb.next();
          if (rb.bpkgs == gen.bpkgs) {
            res.add(new BundleWireImpl(gen.bundleCapability, gen, rb, bp.bg));
          }
        }
      }
    }
    if ((ns & BundleRevisionImpl.NS_HOST) != 0) {
      if (gen.isFragmentHost()) {
        @SuppressWarnings("unchecked")
        final Vector<BundleGeneration> fix = (Vector<BundleGeneration>)gen.fragments.clone();
        for (final BundleGeneration fbg : fix) {
          res.add(new BundleWireImpl(gen.hostCapability, gen, fbg.fragment, fbg));
        }
      }
    }
    if ((ns & BundleRevisionImpl.NS_PACKAGE) != 0) {
      for (final BundleCapability bc : gen.bpkgs.getPackageCapabilities()) {
        final ExportPkg ep = (ExportPkg)bc;
        final List<ImportPkg> ips = ep.getPackageImporters();
        if (ips != null) {
          for (final ImportPkg ip : ips) {
            // Fetch the original for dynamic and fragment imports
            ImportPkg oip;
            if (ip.parent != null && ip.parent.bpkgs != ip.bpkgs) {
              oip = ip.parent;
            } else {
              oip = ip;
            }
            res.add(new BundleWireImpl(ep, gen, oip, ip.bpkgs.bg));
          }
        }
      }
    }
    if ((ns & BundleRevisionImpl.NS_NATIVE) != 0) {
      if (bundleRevision.gen.bundle.id == 0) {
        final FrameworkContext fc = bundleRevision.gen.bundle.fwCtx;
        final SystemBundle sb = fc.systemBundle;
        for (BundleImpl bi : fc.bundles.getBundles()) {
          for (BundleGeneration bg : bi.generations) {
            ClassLoader cl = bg.getClassLoader();
            if (cl != null && cl instanceof BundleClassLoader) {
              Set<BundleGeneration> nbgs = ((BundleClassLoader)cl).hasNativeRequirements();
              if (nbgs != null) {
                for (BundleGeneration nbg : nbgs) {
                  res.add(new BundleWireImpl(sb.getNativeCapability(), sb.current(), nbg.nativeRequirement, nbg));
                }
              }
            }
          }
        }
      }
    }
    if ((ns & (BundleRevisionImpl.NS_IDENTITY|BundleRevisionImpl.NS_OTHER)) != 0) {
      List<BundleWireImpl> other = gen.getCapabilityWires();
      if (other != null) {
        for (BundleWireImpl bw : other) {
          if (namespace == null || namespace.equals(bw.getCapability().getNamespace())) {
            res.add(bw);
          }
        }
      }
    }
    return res;
  }

  public List<BundleWire> getRequiredWires(String namespace) {
    if (!isInUse()) {
      return null;
    }
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    final int ns = BundleRevisionImpl.whichNameSpaces(namespace);
    final ArrayList<BundleWire> res = new ArrayList<BundleWire>();
    if ((ns & BundleRevisionImpl.NS_BUNDLE) != 0) {
      for (final Iterator<RequireBundle> irb = gen.bpkgs.getRequire(); irb.hasNext(); ) {
        final RequireBundle rb = irb.next();
        if (null != rb.bpkgs && rb.bpkgs.isRequiredBy(gen.bpkgs)) {
          res.add(new BundleWireImpl(rb.bpkgs.bg.bundleCapability, rb.bpkgs.bg, rb, gen));
        }
      }
    }
    if ((ns & BundleRevisionImpl.NS_HOST) != 0) {
      if (gen.isFragment()) {
        for (final BundleGeneration hbg : gen.getHosts()) {
          res.add(new BundleWireImpl(hbg.hostCapability, hbg, gen.fragment, gen));
        }
      }
    }
    if ((ns & BundleRevisionImpl.NS_PACKAGE) != 0) {
      TreeSet<ImportPkg> dynamic = new TreeSet<ImportPkg>(
          new Comparator<ImportPkg>() {
            @Override
            public int compare(ImportPkg o1, ImportPkg o2) {
              return o1.dynId - o2.dynId;
            }
          });
     for (final ImportPkg ip : gen.bpkgs.getPackageRequirements()) {
        ExportPkg ep = ip.provider;
        if (ep != null) {
          res.add(new BundleWireImpl(ep, ep.bpkgs.bg, ip, gen));
        } else {
          // Must be dynamic import or fragment
          for (ImportPkg cip : gen.bpkgs.getActiveChildImports(ip)) {
            // Add dynamic imports in bind order
            if (ip.isDynamic()) {
              dynamic.add(cip);
            } else {
              res.add(new BundleWireImpl(cip.provider, cip.provider.bpkgs.bg, ip, gen));
            }
          }
        }
      }
      for (ImportPkg cip : dynamic) {
        res.add(new BundleWireImpl(cip.provider, cip.provider.bpkgs.bg, cip.parent, gen));
      }
    }
    if ((ns & BundleRevisionImpl.NS_NATIVE) != 0) {
      ClassLoader cl = gen.getClassLoader();
      if (cl != null && cl instanceof BundleClassLoader) {
        Set<BundleGeneration> nbgs = ((BundleClassLoader)cl).hasNativeRequirements();
        if (nbgs != null) {
          final SystemBundle sb = gen.bundle.fwCtx.systemBundle;
          for (BundleGeneration nbg : nbgs) {
            res.add(new BundleWireImpl(sb.getNativeCapability(), sb.current(), nbg.nativeRequirement, nbg));
          }
        }
      }
    }
    if ((ns & (BundleRevisionImpl.NS_IDENTITY|BundleRevisionImpl.NS_NATIVE|BundleRevisionImpl.NS_OTHER)) != 0) {
      List<BundleWireImpl> other = gen.getRequirementWires();
      if (other != null) {
        for (BundleWireImpl bw : other) {
          if (namespace == null || namespace.equals(bw.getRequirement().getNamespace())) {
            res.add(bw);
          }
        }
      }
    }
    return res;
  }

  public BundleRevision getRevision() {
    return bundleRevision;
  }

  public ClassLoader getClassLoader() {
    if (isInUse()) {
      return bundleRevision.getBundleGeneration().getClassLoader();
    }
    return null;
  }

  public List<URL> findEntries(String path, String filePattern, int options) {
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    if (isInUse()) {
      return Collections.unmodifiableList(gen.bundle.secure.callFindEntries(gen, path, filePattern, (options & FINDENTRIES_RECURSE) != 0));
    }
    return null;
  }

  public Collection<String> listResources(String path, String filePattern, int options) {
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    if (!isInUse()) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Collection<String> res = Collections.EMPTY_SET;
    ClassLoader cl = gen.getClassLoader();
    if (cl != null && cl instanceof BundleClassLoader) {
      BundleClassLoader bcl = (BundleClassLoader) gen.getClassLoader();
      res = bcl.listResources(path, filePattern, options);
    }
    return res;
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<Capability> getResourceCapabilities(String namespace) {
    return (List<Capability>)(List<?>)getCapabilities(namespace);
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<Requirement> getResourceRequirements(String namespace) {
    return (List<Requirement>)(List<?>)getRequirements(namespace);
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<Wire> getProvidedResourceWires(String namespace) {
    return (List<Wire>)(List<?>)getProvidedWires(namespace);
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<Wire> getRequiredResourceWires(String namespace) {
    return (List<Wire>)(List<?>)getRequiredWires(namespace);
  }


  @Override
  public BundleRevision getResource() {
    return bundleRevision;
  }


  BundleWiringDTO getDTO() {
    BundleWiringDTO res = new BundleWiringDTO();
    res.bundle = bundleRevision.gen.bundle.id;
    res.root = dtoId;
    Map<BundleWiringImpl, NodeDTO> nodes = new HashMap<BundleWiringImpl, NodeDTO>();
    walkWires(nodes, null);
    res.nodes = new HashSet<NodeDTO>(nodes.values());
    res.resources = new HashSet<BundleRevisionDTO>();
    for (BundleWiringImpl bwi : nodes.keySet()) {
      res.resources.add(bwi.bundleRevision.getDTO());
    }
    return res;
  }

  void walkWires(Map<BundleWiringImpl, NodeDTO> nodes, BundleWireDTO wireDTO) {
    NodeDTO dto = nodes.get(this);
    if (dto == null) {
      boolean isRoot = nodes.isEmpty();
      dto = new NodeDTO();
      dto.inUse = isInUse();
      dto.current = isCurrent();
      dto.id = dtoId;
      dto.capabilities = bundleRevision.getCapabilityRefDTOs();
      dto.requirements = bundleRevision.getRequirementRefDTOs();
      dto.providedWires = new ArrayList<WireDTO>();
      dto.requiredWires = new ArrayList<WireDTO>();
      dto.resource = bundleRevision.dtoId;
      nodes.put(this, dto);
      for (BundleWire bw : getProvidedWires(null)) {
        BundleWireDTO bwdto = ((BundleWireImpl)bw).getDTO();
        dto.providedWires.add(bwdto);
        ((BundleRevisionImpl)bw.getRequirer()).getWiringImpl().walkWires(nodes, bwdto);
      }
      if (isRoot) {
        for (BundleWire bw : getRequiredWires(null)) {
          ((BundleRevisionImpl)bw.getProvider()).getWiringImpl().walkWires(nodes, null);
        }
      }
    }
    if (wireDTO != null) {
      dto.requiredWires.add(wireDTO);
    }
  }

}
