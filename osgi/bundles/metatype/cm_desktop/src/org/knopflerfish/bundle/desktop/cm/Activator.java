/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.cm;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.metatype.SystemMetatypeProvider;

public class Activator
  implements BundleActivator
{
  static public LogRef log;
  static public BundleContext bc;

  static ServiceTracker<MetaTypeService, MetaTypeService> mtsTracker;

  static CMDisplayer disp;

  @Override
  public void start(BundleContext _bc)
  {
    Activator.bc = _bc;
    Activator.log = new LogRef(bc);

    mtsTracker =
        new ServiceTracker<>(
            bc,
            MetaTypeService.class,
            null);
    mtsTracker.open();

    // bundle displayers
    disp = new CMDisplayer(bc);
    disp.open();
    disp.register();
  }

  static MetaTypeInformation getMTI(Bundle b)
  {
    final MetaTypeService mts = mtsTracker.getService();
    if (mts != null) {
      return mts.getMetaTypeInformation(b);
    } else {
      log.warn("No MetaTypeService available");
    }
    return null;
  }

  static MetaTypeInformation getMTP(Bundle b)
  {
    final MetaTypeService mts = mtsTracker.getService();
    if (mts instanceof SystemMetatypeProvider) {
      final SystemMetatypeProvider smtp = (SystemMetatypeProvider) mts;
      return smtp.getMTP(b);
    }
    return null;
  }

  @Override
  public void stop(BundleContext bc)
  {
    try {
      disp.unregister();
      disp.close();
      disp = null;

      mtsTracker.close();
      mtsTracker = null;

      Activator.bc = null;
    } catch (final Exception e) {
      log.error("stop failed: " + e.getMessage(), e);
    }

    if (log != null) {
      log.close();
      log = null;
    }
  }

}
