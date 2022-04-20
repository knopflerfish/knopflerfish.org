/*
 * Copyright (c) 2013-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.repository.xml;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

public class ResourceImpl implements Resource, RepositoryContent {
  List<Capability> cs = new ArrayList<Capability>();
  List<Requirement> rs = new ArrayList<Requirement>();

  @Override
  public List<Capability> getCapabilities(String namespace) {
    List<Capability> result = cs;
    if (namespace != null) {
      result = new ArrayList<Capability>();
      for (Capability c : cs) {
        if (namespace.equalsIgnoreCase(c.getNamespace())) {
          result.add(c);
        }
      }
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public List<Requirement> getRequirements(String namespace) {
    List<Requirement> result = rs;
    if (namespace != null) {
      result = new ArrayList<Requirement>();
      for (Requirement r : rs) {
        if (namespace.equalsIgnoreCase(r.getNamespace())) {
          result.add(r);
        }
      }
    }
    return Collections.unmodifiableList(result);
  }

  void addReq(RequirementImpl req) {
    rs.add(req);

  }

  void addCap(CapabilityImpl cap) {
    cs.add(cap);
  }

  boolean hasContent() {
    return !getCapabilities(ContentNamespace.CONTENT_NAMESPACE).isEmpty();
  }

  @Override
  public InputStream getContent() {
    try {
      Capability c = getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
      return new URL((String) c.getAttributes().get(
          ContentNamespace.CAPABILITY_URL_ATTRIBUTE)).openStream();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Resource[\n");
    for (Capability c : cs) {
      sb.append(c.toString());
    }
    for (Requirement r : rs) {
      sb.append(r.toString());
    }
    sb.append("]\n");
    return sb.toString();
  }

  public boolean equals(Object other) {
    if (this == other)
      return true;
    if (other == null)
      return false;
    if (!(other instanceof Resource))
      return false;
    Resource r = (Resource) other;
    return getCapabilities(null).equals(r.getCapabilities(null))
        && getRequirements(null).equals(r.getRequirements(null));
  }
}
