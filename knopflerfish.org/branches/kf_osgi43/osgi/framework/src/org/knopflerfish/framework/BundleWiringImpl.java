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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class BundleWiringImpl implements BundleWiring {

  final BundleRevisionImpl bundleRevision;

  BundleWiringImpl(BundleRevisionImpl br) {
    bundleRevision = br;
  }


  public Bundle getBundle() {
    return bundleRevision.getBundle();
  }


  public boolean isCurrent() {
    return bundleRevision == bundleRevision.bundle.bundleRevision;
  }


  public boolean isInUse() {
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    return gen.isCurrentBundleRevision(bundleRevision) && bundleRevision.bundle.usesBundleGeneration(gen);
  }


  public List<BundleCapability> getCapabilities(String namespace) {
    if (!isInUse()) {
      return null;
    }
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    final int ns = BundleRevisionImpl.whichNameSpaces(namespace);
    final ArrayList<BundleCapability> res = new ArrayList<BundleCapability>();
    if (!gen.isFragment()) {
      if ((ns & BundleRevisionImpl.NS_BUNDLE) != 0) {
        final BundleCapability bc = gen.getBundleCapability();
        if (bc != null) {
          res.add(bc);
        }
      }
      if ((ns & BundleRevisionImpl.NS_HOST) != 0) {
        final BundleCapability bc = gen.getHostCapability();
        if (bc != null) {
          res.add(bc);
        }
      }
      if ((ns & BundleRevisionImpl.NS_PACKAGE) != 0) {
        res.addAll(gen.bpkgs.getPackageCapabilities());
      }
      if ((ns & BundleRevisionImpl.NS_OTHER) != 0) {
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
              if (bc.isEffectiveResolve()) {
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
        for (final ImportPkg ip : gen.bpkgs.getPackageRequirements()) {
          if (ip.provider != null) {
            res.add(ip);
          }
        }
      }
      if ((ns & BundleRevisionImpl.NS_OTHER) != 0) {
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
            res.add(new BundleWireImpl(gen.getBundleCapability(), gen, rb, bp.bg));
          }
        }
      }
    }
    if ((ns & BundleRevisionImpl.NS_HOST) != 0) {
      final Vector<BundleGeneration> frags = gen.fragments;
      if (frags != null) {
        synchronized (frags) {
          for (final BundleGeneration fbg : frags) {
            res.add(new BundleWireImpl(gen.getHostCapability(), gen, fbg.fragment, fbg));
          }
        }
      }
    }
    // TODO Manifest order
    if ((ns & BundleRevisionImpl.NS_PACKAGE) != 0) {
      // TODO, do we need to synchronize
      for (final Iterator<ExportPkg> i = gen.bpkgs.getExports(); i.hasNext(); ) {
        final ExportPkg ep = i.next();
        final List<ImportPkg> ips = ep.getPackageImporters();
        if (ips != null) {
          for (final ImportPkg ip : ips) {
            res.add(new BundleWireImpl(ep, gen, ip, ip.bpkgs.bg));
          }
        }
      }
    }
    if ((ns & BundleRevisionImpl.NS_OTHER) != 0) {
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
          res.add(new BundleWireImpl(rb.bpkgs.bg.getBundleCapability(), rb.bpkgs.bg, rb, gen));
        }
      }
    }
    if ((ns & BundleRevisionImpl.NS_HOST) != 0) {
      if (gen.fragment != null) {
        for (final BundleGeneration hbg : gen.fragment.getHosts()) {
          res.add(new BundleWireImpl(hbg.getHostCapability(), hbg, gen.fragment, gen));
        }
      }
    }
    // TODO Manifest order
    if ((ns & BundleRevisionImpl.NS_PACKAGE) != 0) {
      for (final Iterator<ImportPkg> i = gen.bpkgs.getImports(); i.hasNext(); ) {
        final ImportPkg ip = i.next();
        final ExportPkg ep = ip.provider;
        if (ep != null) {
          res.add(new BundleWireImpl(ep, ep.bpkgs.bg, ip, gen));
        }
      }

    }
    if ((ns & BundleRevisionImpl.NS_OTHER) != 0) {
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
    BundleGeneration gen = bundleRevision.getBundleGeneration();
    if (gen.isCurrentBundleRevision(bundleRevision)) {
      return gen.getClassLoader();
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
    // TODO Auto-generated method stub
    return null;
  }

}
