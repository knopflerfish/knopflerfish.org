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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;

public class FrameworkWiringImpl implements FrameworkWiring {

  private final FrameworkContext fwCtx;

  static final String SPEC_VERSION = "1.0";

  FrameworkWiringImpl(FrameworkContext fwCtx) {
    this.fwCtx = fwCtx;
  }

  public Bundle getBundle() {
    return fwCtx.systemBundle;
  }

  public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
    final Bundle[] bs = bundles != null ?
                  bundles.toArray(new Bundle[bundles.size()]) :
                  null;
    fwCtx.packageAdmin.refreshPackages(bs, listeners);
  }

  public boolean resolveBundles(Collection<Bundle> bundles) {
    final Bundle[] bs = bundles != null ?
                  bundles.toArray(new Bundle[bundles.size()]) :
                  null;
    return fwCtx.packageAdmin.resolveBundles(bs);
  }

  public Collection<Bundle> getRemovalPendingBundles() {
    Set<Bundle> res = new HashSet<Bundle>();
    fwCtx.bundles.getRemovalPendingBundles(res);
    return res;
  }

  public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
    final HashSet<Bundle> res = new HashSet<Bundle>();
    for (Bundle b : bundles) {
      fwCtx.checkOurBundle(b);
      res.add(b);
    }
    fwCtx.resolver.closure(res);
    return res;
  }

  @SuppressWarnings("unchecked")
  public Collection<BundleCapability> findProviders(Requirement requirement) {
    final String namespace = requirement.getNamespace();
    final String filterStr = requirement.getDirectives().get("filter");
    Filter filter;
    if (filterStr != null) {
      try {
        filter = FrameworkUtil.createFilter(filterStr);
      } catch (InvalidSyntaxException ise) {
        final String msg = "Invalid filter directive '" + filterStr + "': " + ise;
        throw new IllegalArgumentException(msg, ise);
      }
    } else {
      filter = null;
    }
    HashSet<BundleCapability> res = new HashSet<BundleCapability>();
    for (BundleGeneration bg : fwCtx.bundles.getBundleGenerations(null)) {
      BundleRevisionImpl bri = bg.bundleRevision;
      if (bri != null) {
        for (BundleCapability bc : bri.getDeclaredCapabilities(namespace)) {
          if (null == filter || filter.matches(bc.getAttributes())) {
            res.add(bc);
          }
        }
      }
    }
    return res;
  }

}
