/*
 * Copyright (c) 2012-2022, KNOPFLERFISH project
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
package org.knopflerfish.bundle.desktop.swing;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.bundle.desktop.swing.graph.EmptyNode;
import org.knopflerfish.bundle.desktop.swing.graph.Node;
import org.knopflerfish.bundle.desktop.swing.graph.PackageNode;
import org.knopflerfish.service.desktop.BundleSelectionModel;

public class JPackageView extends JSoftGraphBundle {

  private static final long serialVersionUID = 1L;
  ServiceTracker<PackageAdmin,PackageAdmin> pkgTracker;

  public JPackageView(GraphDisplayer.JMainBundles jmb,
                      BundleContext bc,
                      Bundle b,
                      BundleSelectionModel bundleSelModel) {
    super(jmb, bc, b, bundleSelModel);

    pkgTracker = new ServiceTracker<>(bc, PackageAdmin.class, null);
    pkgTracker.open();

    setMaxDepth(8);
    currentNode = makeRootNode();
    setLabel(Strings.get("str_packages"));
  }

  @Override
  public Node makeRootNode() {
    if(Activator.desktop != null && Activator.desktop.alive) {
      return new PackageNode(Activator.desktop.getPackageManager(), b, 0, "#" + b.getBundleId());
    } else {
      return new EmptyNode("", 0, "");
    }
  }

  @Override
  void bundleChanged() {
  }

  @Override
  public void close() {
    super.close();
    pkgTracker.close();
  }
}
