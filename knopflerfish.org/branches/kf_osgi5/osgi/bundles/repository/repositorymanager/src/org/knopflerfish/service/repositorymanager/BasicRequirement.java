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
package org.knopflerfish.service.repositorymanager;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class BasicRequirement implements Requirement {

  final private String namespace;
  final private Map<String, Object> attributes = new HashMap<String, Object>();
  final private Map<String, String> directives = new HashMap<String, String>();

  public BasicRequirement(final String ns) {
    namespace = ns;
  }

  public BasicRequirement(final String ns, final String nsFilter) {
    namespace = ns;
    addDirective("filter", "(" + ns + "=" + nsFilter + ")");
  }

  public void addAttribute(final String key, final Object val) {
    attributes.put(key, val);
  }

  public void addDirective(final String key, final String val) {
    directives.put(key, val);
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Map<String, String> getDirectives() {
    return directives;
  }

  @Override
  public Resource getResource() {
    return null;
  }

  public void addBundleTypeFilter() {
    String bf = "(" + IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + "=" + IdentityNamespace.TYPE_BUNDLE + ")";
    String ff = "(" + IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + "=" + IdentityNamespace.TYPE_FRAGMENT + ")";
    opFilter('&', bf, ff);
  }

  public void addVersionRangeFilter(VersionRange versionRange) {
    opFilter('&', versionRange.toFilterString("version"));
  }

  public void opFilter(char op, String ... andFilter) {
    String filter = directives.get("filter");
    if (andFilter.length > 0) {
      if (filter  != null || andFilter.length > 1) {
        StringBuffer f = new StringBuffer("(");
        f.append(op);
        if (filter != null) {
          f.append(filter);
        }
        for (String af : andFilter) {
          f.append(af);
        }
        addDirective("filter", f.append(')').toString());
      } else {
        addDirective("filter", andFilter[0]);
      }
    } else {
      throw new IllegalArgumentException("Expected at least one argument");
    }
  }

}
