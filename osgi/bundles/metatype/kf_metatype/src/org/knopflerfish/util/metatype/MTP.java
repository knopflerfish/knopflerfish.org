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

/**
 * @author Erik Wistrand
 * @author Philippe Laporte
 */

package org.knopflerfish.util.metatype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Implementation of the {@link MetaTypeInformation} interface.
 */
public class MTP
  implements MetaTypeInformation, Comparable<MTP>
{
  // Mapping from pid/fpid to OCD
  Map<String, ObjectClassDefinition> ocdMap =
    new HashMap<String, ObjectClassDefinition>();

  Set<String> pids = new HashSet<String>();
  Set<String> factoryPids = new HashSet<String>();

  String id;

  // when system bundle, not set
  private Bundle bundle;

  public MTP(String id)
  {
    this.id = id;
  }

  public void addService(String pid, ObjectClassDefinition ocd)
  {
    ocdMap.put(pid, ocd);
    pids.add(pid);
  }

  public void addFactory(String pid, ObjectClassDefinition ocd)
  {
    ocdMap.put(pid, ocd);
    factoryPids.add(pid);
  }

  static String[] toStringArray(Set<String> set)
  {
    return set.toArray(new String[set.size()]);
  }

  void setBundle(Bundle bundle)
  {
    this.bundle = bundle;
  }

  public Bundle getBundle()
  {
    return bundle;
  }

  public String[] getPids()
  {
    return toStringArray(pids);
  }

  public String[] getFactoryPids()
  {
    return toStringArray(factoryPids);
  }

  public String[] getLocales()
  {
    return null;
  }

  public ObjectClassDefinition getObjectClassDefinition(String pid,
                                                        String locale)
  {

    final ObjectClassDefinition ocd = ocdMap.get(pid);

    return ocd;
  }

  @Override
  public String toString()
  {
    return toString(true);
  }

  public String toString(boolean bFull)
  {
    final StringBuilder sb = new StringBuilder();

    sb.append("MTP[\n");
    for (final Iterator<String> it = ocdMap.keySet().iterator(); it.hasNext();) {
      final String pid = it.next();
      final ObjectClassDefinition ocd = ocdMap.get(pid);
      if (bFull) {
        sb.append(pid).append("=");
        sb.append(ocd);
      } else {
        sb.append(pid);
      }
      if (it.hasNext()) {
        sb.append("\n");
      }
    }
    sb.append("\n/MTP]");

    return sb.toString();
  }

  public String getId()
  {
    return id;
  }

  public int compareTo(MTP o)
  {
    return id.compareTo(o.id);
  }

  @Override
  public int hashCode()
  {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object other)
  {
    if (other == null || !(other instanceof MTP)) {
      return false;
    }

    return id.equals(((MTP) other).id);
  }

}
