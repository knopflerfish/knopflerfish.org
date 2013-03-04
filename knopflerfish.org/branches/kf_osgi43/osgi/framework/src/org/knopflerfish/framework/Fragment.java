/*
 * Copyright (c) 2010-2013, KNOPFLERFISH project
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

import org.knopflerfish.framework.Util.HeaderEntry;

/**
 * Fragment information
 */
class Fragment
  implements BundleRequirement
{
  final BundleGeneration gen;
  final String hostName;
  final String extension;
  final VersionRange versionRange;
  final Map<String,Object> attributes;

  private final Vector<BundleGeneration> hosts = new Vector<BundleGeneration>(2);


  /**
   * @param gen
   *          The bundle generation of this fragment.
   * @param headerEntry
   *          the fragment-host manifest header describing this fragment.
   */
  Fragment(final BundleGeneration gen, final HeaderEntry headerEntry) {
    this.gen = gen;
    this.hostName = headerEntry.getKey();

    if (gen.archive.getAttribute(Constants.BUNDLE_ACTIVATOR) != null) {
      throw new IllegalArgumentException("A fragment bundle can not have a Bundle-Activator.");
    }

    final String extension = headerEntry.getDirectives()
        .remove(Constants.EXTENSION_DIRECTIVE);
    if (Constants.EXTENSION_FRAMEWORK.equals(extension)
        || Constants.EXTENSION_BOOTCLASSPATH.equals(extension)) {
      // an extension bundle must target the system bundle.
      if (!Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(hostName)
          && !BundleGeneration.KNOPFLERFISH_SYMBOLICNAME.equals(hostName)) {
        throw new IllegalArgumentException("An extension bundle must target "
            + "the system bundle(" + Constants.SYSTEM_BUNDLE_SYMBOLICNAME + " or "
            + BundleGeneration.KNOPFLERFISH_SYMBOLICNAME + ")");
      }

      if (gen.archive.getAttribute(Constants.IMPORT_PACKAGE) != null
          || gen.archive.getAttribute(Constants.REQUIRE_BUNDLE) != null
          || gen.archive.getAttribute(Constants.BUNDLE_NATIVECODE) != null
          || gen.archive.getAttribute(Constants.DYNAMICIMPORT_PACKAGE) != null
          || gen.archive.getAttribute(Constants.BUNDLE_ACTIVATOR) != null) {
        throw new IllegalArgumentException("An extension bundle cannot specify: "
            + Constants.IMPORT_PACKAGE + ", " + Constants.REQUIRE_BUNDLE + ", "
            + Constants.BUNDLE_NATIVECODE + ", " + Constants.DYNAMICIMPORT_PACKAGE + " or "
            + Constants.BUNDLE_ACTIVATOR);
      }

      if (!gen.bundle.fwCtx.props.getBooleanProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION)
          && Constants.EXTENSION_FRAMEWORK.equals(extension)) {
        throw new UnsupportedOperationException(
            "Framework extension bundles are not supported "
                + "by this framework. Consult the documentation");
      }
      if (!gen.bundle.fwCtx.props.getBooleanProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION)
          && Constants.EXTENSION_BOOTCLASSPATH.equals(extension)) {
        throw new UnsupportedOperationException(
            "Bootclasspath extension bundles are not supported "
                + "by this framework. Consult the documentation");
      }
    } else {
      if (extension != null) {
        throw new IllegalArgumentException("Did not recognize directive "
            + Constants.EXTENSION_DIRECTIVE + ":=" + extension + ".");
      }
    }
    this.extension = extension;

    final String range = (String) headerEntry.getAttributes()
        .remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
    this.versionRange = range == null ? VersionRange.defaultVersionRange
        : new VersionRange(range);

    this.attributes = headerEntry.getAttributes();
  }


  void addHost(BundleGeneration host) {
    hosts.add(host);
  }


  void removeHost(BundleGeneration host) {
    if (host == null) {
      hosts.clear();
    } else {
      hosts.remove(host);
    }
  }


  boolean isHost(BundleGeneration host) {
    return hosts.contains(host);
  }


  @SuppressWarnings("unchecked")
  Vector<BundleGeneration> getHosts() {
    return hosts.isEmpty() ? null : (Vector<BundleGeneration>)hosts.clone();
  }


  boolean hasHosts()
  {
    return !hosts.isEmpty();
  }

  boolean isTarget(BundleImpl b)
  {
    return hostName.equals(b.getSymbolicName())
           && versionRange.withinRange(b.getVersion());
  }

  List<BundleImpl> targets(final FrameworkContext fwCtx)
  {
    final List<BundleImpl> bundles = fwCtx.bundles.getBundles(hostName,
                                                              versionRange);
    for (final Iterator<BundleImpl> iter = bundles.iterator(); iter.hasNext();) {
      final BundleImpl t = iter.next();

      if (t.current().attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
        iter.remove();
      }
    }

    if (bundles.isEmpty()) {
      return null;
    }
    return bundles;
  }

  public String getNamespace() {
    return BundleRevision.HOST_NAMESPACE;
  }


  public Map<String, String> getDirectives() {
    final Map<String,String> res = new HashMap<String, String>(4);

    if (extension!=null) {
      res.put(Constants.EXTENSION_DIRECTIVE, extension);
    }

    // For HOST_NAMESPACE effective defaults to resolve and no other value
    // is allowed so leave it out.
    // res.put(Constants.EFFECTIVE_DIRECTIVE, Constants.EFFECTIVE_RESOLVE);

    final Filter filter = toFilter();
    if (null!=filter) {
      res.put(Constants.FILTER_DIRECTIVE, filter.toString());
    }
    return res;
  }


  private Filter toFilter()
  {
    final StringBuffer sb = new StringBuffer(80);
    boolean multipleConditions = false;

    sb.append('(');
    sb.append(BundleRevision.HOST_NAMESPACE);
    sb.append('=');
    if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(hostName)) {
      sb.append(BundleGeneration.KNOPFLERFISH_SYMBOLICNAME);
    } else {
      sb.append(hostName);
    }
    sb.append(')');

    if (versionRange != null) {
      multipleConditions |= versionRange
          .appendFilterString(sb, Constants.BUNDLE_VERSION_ATTRIBUTE);
    }

    for (final Entry<String,Object> entry : attributes.entrySet()) {
      sb.append('(');
      sb.append(entry.getKey());
      sb.append('=');
      sb.append(entry.getValue().toString());
      sb.append(')');
      multipleConditions |= true;
    }

    if (multipleConditions) {
      sb.insert(0, "(&");
      sb.append(')');
    }
    try {
      return FrameworkUtil.createFilter(sb.toString());
    } catch (final InvalidSyntaxException _ise) {
      // Should not happen...
      System.err.println("createFilter: '" +sb.toString() +"': " +_ise.getMessage());
      return null;
    }
  }


  public Map<String, Object> getAttributes() {
    @SuppressWarnings("unchecked")
    final
    Map<String, Object> res = Collections.EMPTY_MAP;
    return res;
  }


  public BundleRevision getRevision() {
    return gen.getRevision();
  }


  public boolean matches(BundleCapability capability) {
    if (BundleRevision.HOST_NAMESPACE.equals(capability.getNamespace())) {
      return toFilter().matches(capability.getAttributes());
    }
    return false;
  }

}
