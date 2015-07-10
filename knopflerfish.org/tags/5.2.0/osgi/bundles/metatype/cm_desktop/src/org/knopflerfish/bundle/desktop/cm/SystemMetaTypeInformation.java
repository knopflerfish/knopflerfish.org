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

package org.knopflerfish.bundle.desktop.cm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * This class holds a snapshot of all meta type information objects available at
 * the time of creation.
 *
 * @author ekolin
 *
 */
class SystemMetaTypeInformation
  implements MetaTypeInformation
{

  private final Map<Bundle,MetaTypeInformation> bundle2mti = new HashMap<Bundle, MetaTypeInformation>();

  public SystemMetaTypeInformation()
  {
    for (final Bundle bundle : DefaultSwingBundleDisplayer.getAllBundlesSortedByName()) {
      final MetaTypeInformation mti = Activator.getMTI(bundle);
      if (mti != null) {
        bundle2mti.put(bundle, mti);
      }
    }
  }

  @Override
  public ObjectClassDefinition getObjectClassDefinition(String pid, String locale)
  {
    for (final MetaTypeInformation mti : bundle2mti.values()) {
      if (Arrays.asList(mti.getPids()).contains(pid)) {
        return mti.getObjectClassDefinition(pid, locale);
      }
      if (Arrays.asList(mti.getFactoryPids()).contains(pid)) {
        return mti.getObjectClassDefinition(pid, locale);
      }
    }
    return null;
  }

  @Override
  public String[] getLocales()
  {
    final Set<String> res = new TreeSet<String>();

    for (final MetaTypeInformation mti : bundle2mti.values()) {
      res.addAll(Arrays.asList(mti.getLocales()));
    }
    return res.toArray(new String[res.size()]);
  }

  @Override
  public String[] getPids()
  {
    final Set<String> res = new TreeSet<String>();

    for (final MetaTypeInformation mti : bundle2mti.values()) {
      res.addAll(Arrays.asList(mti.getPids()));
    }
    return res.toArray(new String[res.size()]);
  }

  @Override
  public String[] getFactoryPids()
  {
    final Set<String> res = new TreeSet<String>();

    for (final MetaTypeInformation mti : bundle2mti.values()) {
      res.addAll(Arrays.asList(mti.getFactoryPids()));
    }
    return res.toArray(new String[res.size()]);
  }

  @Override
  public Bundle getBundle()
  {
    throw new UnsupportedOperationException("use getBudnle(PID) to get the bundle owning PID.");
  }

  public Bundle getBundle(String pid)
  {
    for (final MetaTypeInformation mti : bundle2mti.values()) {
      if (Arrays.asList(mti.getPids()).contains(pid)) {
        return mti.getBundle();
      }
      if (Arrays.asList(mti.getFactoryPids()).contains(pid)) {
        return mti.getBundle();
      }
    }
    return null;
  }

}
