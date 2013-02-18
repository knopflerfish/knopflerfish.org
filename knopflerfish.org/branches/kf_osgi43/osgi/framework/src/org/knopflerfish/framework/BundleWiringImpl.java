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
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class BundleWiringImpl implements BundleWiring {

  private static final int NS_BUNDLE =  1;
  private static final int NS_HOST =    2;
  private static final int NS_PACKAGE = 4;
  private static final int NS_OTHER =   8;

  final BundleGeneration gen;
  
  BundleWiringImpl(BundleGeneration bundleGeneration) {
    gen = bundleGeneration;
  }

  public Bundle getBundle() {
    return gen.bundle;
  }

  public boolean isCurrent() {
    return gen == gen.bundle.current();
  }

  public boolean isInUse() {
    return !gen.isUninstalled() && gen.bundle.usesBundleGeneration(gen);
  }

  public List<BundleCapability> getCapabilities(String namespace) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<BundleRequirement> getRequirements(String namespace) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<BundleWire> getProvidedWires(String namespace) {
    int ns = whichNameSpaces(namespace);
    ArrayList<BundleWire> res = new ArrayList<BundleWire>();
    // TODO Manifest order
    if ((ns & NS_PACKAGE) != 0) {
    }
    return res;
  }

  public List<BundleWire> getRequiredWires(String namespace) {
    int ns = whichNameSpaces(namespace);
    ArrayList<BundleWire> res = new ArrayList<BundleWire>();
    // TODO Manifest order
    if ((ns & NS_PACKAGE) != 0) {
      
    }
    return res;
  }

  public BundleRevision getRevision() {
    return gen.getRevision();
  }

  public ClassLoader getClassLoader() {
    return gen.getClassLoader();
  }

  public List<URL> findEntries(String path, String filePattern, int options) {
    return Collections.unmodifiableList(gen.bundle.secure.callFindEntries(gen, path, filePattern, (options & FINDENTRIES_RECURSE) != 0));
  }

  public Collection<String> listResources(String path, String filePattern, int options) {
    // TODO Auto-generated method stub
    return null;
  }

  private int whichNameSpaces(String namespace) {
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
