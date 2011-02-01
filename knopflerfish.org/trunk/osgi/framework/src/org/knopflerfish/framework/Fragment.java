/*
 * Copyright (c) 2010-2011, KNOPFLERFISH project
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

import java.util.*;

import org.osgi.framework.*;

/**
 * Fragment information
 */
class Fragment {
  final String hostName;
  final String extension;
  final VersionRange versionRange;
  private Vector /* BundleGeneration */hosts = new Vector(2);


  Fragment(String hostName, String extension, String range) {
    this.hostName = hostName;
    this.extension = extension;
    this.versionRange = range == null ? VersionRange.defaultVersionRange
        : new VersionRange(range);
  }


  void addHost(BundleGeneration host) {
    hosts.add(host);
  }


  void removeHost(BundleGeneration host) {
    if (host == null) {
      hosts.clear();
    } else {
      hosts.remove(host);
    }
  }


  boolean isHost(BundleGeneration host) {
    return hosts.contains(host);
  }


  Vector getHosts() {
    return hosts.isEmpty() ? null : (Vector)hosts.clone();
  }


  boolean hasHosts() {
    return !hosts.isEmpty();
  }


  boolean isTarget(BundleImpl b) {
    return hostName.equals(b.gen.symbolicName) && versionRange.withinRange(b.gen.version);
  }


  List /* BundleImpl */targets(final FrameworkContext fwCtx) {
    List bundles = fwCtx.bundles.getBundles(hostName, versionRange);
    for (Iterator iter = bundles.iterator(); iter.hasNext();) {
      BundleImpl t = (BundleImpl)iter.next();

      if (t.gen.attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
        iter.remove();
      }
    }

    if (bundles.isEmpty()) {
      return null;
    }
    return bundles;
  }

}
