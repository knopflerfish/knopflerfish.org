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
package org.knopflerfish.util.metatype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * A meta type information object that extracts its information from
 * {@link ManagedService} and {@link ManagedServiceFactory} services
 * registrations which also implements {@link MetaTypeProvider} or
 * {@link MetaTypeInformation}.
 *
 * TODO: The current implementation actually looks at all services which does
 * not seam to be right, but this way it picks up MetaTypeProvider services
 * which the SystemMetatypeProvider does not.
 */
class BundleMetaTypeInformationSnapshot
  implements MetaTypeInformation
{
  private final BundleContext bc;
  private final Bundle bundle;
  final List<String> pids = new ArrayList<>();
  final List<String> factoryPids = new ArrayList<>();
  final Set<String> locales = new TreeSet<>();
  final Map<String, MetaTypeProvider> idToMtp = new HashMap<>();

  public static BundleMetaTypeInformationSnapshot extractMetatypeInformation(BundleContext bc,
                                                                             Bundle b)
  {
    return new BundleMetaTypeInformationSnapshot(bc, b).foundInformation();
  }

  private BundleMetaTypeInformationSnapshot foundInformation()
  {
    return pids.isEmpty() && factoryPids.isEmpty() ? null : this;
  }

  private BundleMetaTypeInformationSnapshot(BundleContext bc, Bundle b)
  {
    this.bc = bc;
    this.bundle = b;
    extractMetaTypeInformation();

  }

  private void extractMetaTypeInformation()
  {
    final ServiceReference<?>[] srs = bundle.getRegisteredServices();
    for (int i = 0; srs != null && i < srs.length; ++i) {
      extractMetaTypeInformation(srs[i]);
    }
  }

  private void extractMetaTypeInformation(ServiceReference<?> sr)
  {
    Object s = null;
    try {
      s = bc.getService(sr);
      if (!(s instanceof MetaTypeProvider)) {
        return;
      }
      final MetaTypeProvider mtp = (MetaTypeProvider) s;

      final List<String> pidProps = new ArrayList<>();
      final List<String> fpidProps = new ArrayList<>();
      if(s instanceof ManagedService){
        pidProps.add(Constants.SERVICE_PID);
        pidProps.add(MetaTypeProvider.METATYPE_PID);
        fpidProps.add(MetaTypeProvider.METATYPE_FACTORY_PID);
      } else if (s instanceof ManagedServiceFactory) {
        pidProps.add(MetaTypeProvider.METATYPE_PID);
        fpidProps.add(Constants.SERVICE_PID);
        fpidProps.add(MetaTypeProvider.METATYPE_FACTORY_PID);
      } else {
        // MetaTypeProvide service
        pidProps.add(MetaTypeProvider.METATYPE_PID);
        fpidProps.add(MetaTypeProvider.METATYPE_FACTORY_PID);
      }
      addSrPropertiesToListAndMapIds(sr, pidProps, pids, mtp);
      addSrPropertiesToListAndMapIds(sr, fpidProps, factoryPids, mtp);

      final String[] ls = mtp.getLocales();
      if (ls != null && ls.length > 0) {
        locales.addAll(Arrays.asList(ls));
      }
    } finally {
      if (s != null) {
        bc.ungetService(sr);
      }
    }
  }

  public ObjectClassDefinition getObjectClassDefinition(String id, String locale)
  {
    final MetaTypeProvider mtp = idToMtp.get(id);

    return mtp != null ? mtp.getObjectClassDefinition(id, locale) : null;
  }

  public String[] getLocales()
  {
    return toStringArray(locales);
  }

  public String[] getPids()
  {
    return toStringArray(pids);
  }

  public String[] getFactoryPids()
  {
    return toStringArray(factoryPids);
  }

  public Bundle getBundle()
  {
    return bundle;
  }

  private static String[] toStringArray(Collection<String> al)
  {
    return al.toArray(new String[0]);
  }

  private void addSrPropertiesToListAndMapIds(ServiceReference<?> sr,
                                              Collection<String> keys,
                                              List<String> al,
                                              MetaTypeProvider mtp)
  {
    for (final String key : keys) {
      final Object o = sr.getProperty(key);
      if (o == null) {
        continue;
      }
      if (o instanceof String) {
        final String s = (String) o;
        al.add(s);
        idToMtp.put(s, mtp);
      } else if (o instanceof String[]) {
        final List<String> c = Arrays.asList((String[]) o);
        al.addAll(c);
        mapAllIds(c, mtp);
      } else if (o instanceof Collection) {
        @SuppressWarnings("unchecked")
        final Collection<? extends String> sc =
          (Collection<? extends String>) o;
        al.addAll(sc);
        mapAllIds(sc, mtp);
      }
    }
  }

  private void mapAllIds(Collection<? extends String> c, MetaTypeProvider mtp)
  {
    for (String s : c) {
      idToMtp.put(s, mtp);
    }
  }

  @Override
  public String toString()
  {
    return
        "Bundle: #" + bundle.getBundleId() + '\n' +
        "PIDs: " + pids + '\n' +
        "factory PIDs: " + factoryPids + '\n' +
        "locales: " + locales + '\n';
  }

}
