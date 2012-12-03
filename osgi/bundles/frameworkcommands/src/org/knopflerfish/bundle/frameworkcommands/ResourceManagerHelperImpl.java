/*
 * Copyright (c) 2012, KNOPFLERFISH project
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

package org.knopflerfish.bundle.frameworkcommands;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;

import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.knopflerfish.service.resman.ResourceManager;
import org.knopflerfish.service.resman.BundleMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


/**
 * Interface hiding the dependency to the {@link ResourceManager}
 * service and to {@link BundleMonitor} for framework console commands
 * interacting with the optional {@link ResourceManager} service.
 *
 * @author Makewave AB
 */

public class ResourceManagerHelperImpl
  implements ResourceManagerHelper
{
  private final BundleContext bc;
  private final FrameworkCommandGroup fcg;
  private final ResourceManager resman;

  /**
   * Look up the resource management service if available. The service
   * will not change during the lifecycle of the framework instance so
   * no need to track it.
   */
  public ResourceManagerHelperImpl(final BundleContext bc,
                                   final FrameworkCommandGroup fcg) {
    this.bc  = bc;
    this.fcg = fcg;

    ServiceReference sr
      = bc.getServiceReference(ResourceManager.class.getName());

    resman = null==sr ? null : (ResourceManager) bc.getService(sr);
  }



  public int cmdResman(final Dictionary opts,
                       final Reader in,
                       final PrintWriter out,
                       final Session session) {
    out.println("   id  level/state  Name                     Memory(max)   Threads(max) CPU(max)");
    out.println("   -----------------------------------------------------------------------------");

    final Bundle[] bundles =
      fcg.getBundles((String[]) opts.get("bundle"),true,true);
    for (int i = 0; i < bundles.length; i++) {
      final StringBuffer sbuf = new StringBuffer(80);
      sbuf.setLength(80);
      for (int j = 0; j < sbuf.length(); j++) {
        sbuf.setCharAt(j, ' ');
      }
      sbuf.insert(0, Util.showId(bundles[i]));
      sbuf.insert(7, fcg.showState(bundles[i]));
      FrameworkCommandGroup.prettyPrint(sbuf, 20, 16, true, Util.shortName(bundles[i]));

      final BundleMonitor bmon = resman.getMonitor(bundles[i]);
      if (bmon != null) {
        FrameworkCommandGroup.prettyPrint(sbuf, 38, 21, false, FrameworkCommandGroup.formatValueAndMax((int)bmon.getMemory(), (int)bmon.getMemoryLimit()));
        FrameworkCommandGroup.prettyPrint(sbuf, 61, 8, false, FrameworkCommandGroup.formatValueAndMax(bmon.getThreadCount(), bmon.getThreadCountLimit()));
        FrameworkCommandGroup.prettyPrint(sbuf, 71, 8, false, FrameworkCommandGroup.formatValueAndMax(bmon.getCPU(), bmon.getCPULimit()));
      }
      sbuf.setLength(80);
      out.println(sbuf);
    }
    return 0;
  }

}
