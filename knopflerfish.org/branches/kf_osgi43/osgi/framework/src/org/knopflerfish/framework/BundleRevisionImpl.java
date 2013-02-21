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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

public class BundleRevisionImpl
  extends BundleReferenceImpl
  implements BundleRevision
{

  static final int NS_BUNDLE =  1;
  static final int NS_HOST =    2;
  static final int NS_PACKAGE = 4;
  static final int NS_OTHER =   8;

  final BundleGeneration gen;

  BundleRevisionImpl(BundleGeneration gen)
  {
    super(gen.bundle);
    this.gen = gen;
  }


  public String getSymbolicName()
  {
    return gen.symbolicName;
  }


  public Version getVersion()
  {
    return gen.version;
  }


  public List<BundleCapability> getDeclaredCapabilities(String namespace)
  {
    final ArrayList<BundleCapability> res = new ArrayList<BundleCapability>();
    final int ns = whichNameSpaces(namespace);

    if ((ns & NS_BUNDLE) != 0) {
      final BundleCapability bc = gen.getBundleCapability();
      if (bc!=null) {
        res.add(bc);
      }
    }
    if ((ns & NS_HOST) != 0) {
      final BundleCapability bc = gen.getHostCapability();
      if (bc!=null) {
        res.add(bc);
      }
    }

    if ((ns & NS_PACKAGE) != 0) {
      res.addAll(gen.bpkgs.getDeclaredPackageCapabilities());
    }

    if ((ns & NS_OTHER) != 0) {
      final Map<String, List<BundleCapability>> caps = gen.getDeclaredCapabilities();
      if (null != namespace) {
        final List<BundleCapability> lcap = caps.get(namespace);
        res.addAll(lcap);
      } else {
        for (final List<BundleCapability> lcap : caps.values()) {
          res.addAll(lcap);
        }
      }
    }

    return res;
  }


  public List<BundleRequirement> getDeclaredRequirements(String namespace)
  {
    final ArrayList<BundleRequirement> res = new ArrayList<BundleRequirement>();
    final int ns = whichNameSpaces(namespace);

    if ((ns & NS_BUNDLE) != 0) {
      final List<BundleRequirement> bundleReqs = gen.bpkgs.getDeclaredBundleRequirements();
      res.addAll(bundleReqs);
    }

    if ((ns & NS_HOST) != 0) {
      if (gen.isFragment()) {
        res.add(gen.fragment);
      }
    }

    if ((ns & NS_PACKAGE) != 0) {
      final List<BundleRequirement> packageReqs = gen.bpkgs.getDeclaredPackageRequirements();
      res.addAll(packageReqs);
    }

    if ((ns & NS_OTHER) != 0) {
      final Map<String, List<BundleRequirement>> reqs = gen.getDeclaredRequirements();
      if (null != namespace) {
        final List<BundleRequirement> lbr = reqs.get(namespace);
        res.addAll(lbr);
      } else {
        for (final List<BundleRequirement> lbr : reqs.values()) {
          res.addAll(lbr);
        }
      }
    }

    return res;
  }


  public int getTypes()
  {
    return gen.isFragment() ? TYPE_FRAGMENT : 0;
  }

  public BundleWiring getWiring()
  {
    return gen.getBundleWiring();
  }

  @Override
  public String toString() {
    return "BundleRevision[" + getSymbolicName() + ":" + getVersion() + "]";
  }

  static int whichNameSpaces(String namespace) {
    int ns;
    if (namespace == null) {
      ns = NS_BUNDLE|NS_HOST|NS_PACKAGE|NS_OTHER;
    } else if (BundleRevision.BUNDLE_NAMESPACE.equals(namespace)) {
      ns = NS_BUNDLE;
    } else if (BundleRevision.HOST_NAMESPACE.equals(namespace)) {
      ns = NS_HOST;
    } else if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
      ns = NS_PACKAGE;
    } else {
      ns = NS_OTHER;
    }
    return ns;
  }


}
