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
import org.osgi.service.repository.ContentNamespace;

public class BasicRequirement implements Requirement {

  /**
   * MIME type to be stored in the extra field of a {@code ZipEntry} object
   * for an installable bundle file.
   * 
   * @see org.osgi.service.provisioning.ProvisingService#MIME_BUNDLE
   */
  public final static String  MIME_BUNDLE         = "application/vnd.osgi.bundle";

  /**
   * Alternative MIME type to be stored in the extra field of a
   * {@code ZipEntry} object for an installable bundle file.
   * 
   * @see org.osgi.service.provisioning.ProvisingService#MIME_BUNDLE_ALT
   */
  public final static String  MIME_BUNDLE_ALT       = "application/x-osgi-bundle";

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

  public void addBundleIdentityFilter() {
    String bf = eq(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE);
    String ff = eq(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_FRAGMENT);
    multiOpFilter('&', multiOp('|', bf, ff));
  }

  public void addBundleContentFilter() {
    String bf = eq(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, MIME_BUNDLE);
    String ff = eq(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, MIME_BUNDLE_ALT);
    multiOpFilter('&', multiOp('|', bf, ff));
  }

  public void addVersionRangeFilter(VersionRange versionRange) {
    multiOpFilter('&', versionRange.toFilterString("version"));
  }

  public void multiOpFilter(char op, String ... andFilter) {
    if (andFilter.length == 0) {
      throw new IllegalArgumentException("Expected at least one argument");
    }
    String [] f;
    String filter = directives.get("filter");
    if (filter != null) {
      f = new String[andFilter.length + 1];
      f[0] = filter;
      System.arraycopy(andFilter, 0, f, 1, andFilter.length);
    } else {
      f = andFilter;
    }
    addDirective("filter", multiOp(op, f));
  }

  public String multiOp(char op, String ... args) {
    if (args.length == 1) {
      return args[0];
    } else if (args.length > 1) {
      StringBuffer f = new StringBuffer("(");
      f.append(op);
      for (String a : args) {
        f.append(a);
      }
      return f.append(')').toString();
    } else {
      throw new IllegalArgumentException("Expected at least one argument");
    }
  }

  public String op(char op, String l, String r) {
    return "(" + l + op + r + ")";
  }

  public String eq(String l, String r) {
    return op('=', l, r);
  }

  @Override
  public String toString() {
    return "BasicRequirement [namespace=" + namespace + ", attributes=" + attributes
           + ", directives=" + directives + "]";
  }

}
