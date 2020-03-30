/*
 * Copyright (c) 2006-2016, KNOPFLERFISH project
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
import java.util.Map;
import java.util.Map.Entry;

import org.knopflerfish.framework.Util.HeaderEntry;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.VersionRange;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;


class RequireBundle
  extends DTOId
  implements BundleRequirement, Comparable<RequireBundle>
{
  // To maintain the creation order in the osgi.wiring.bundle name space.
  static private int requireBundleCount = 0;
  final int orderal = ++requireBundleCount;

  final BundlePackages requestor;
  final String name;
  final String visibility;
  final String resolution;
  final VersionRange bundleRange;
  BundlePackages bpkgs = null;
  final Map<String,Object> attributes;
  final Map<String,String> directives;


  /**
   * A re-required bundle for fragment hosts.
   * @param parent    The fragment require bundle object to re-require.
   * @param requestor The bundle packages of the fragment host that
   *                  re-requires a required bundle from one of its
   *                  fragments.
   */
  RequireBundle(RequireBundle  parent, BundlePackages requestor)
  {
    this.requestor  = requestor;
    this.name       = parent.name;
    this.visibility = parent.visibility;
    this.resolution = parent.resolution;
    this.bundleRange= parent.bundleRange;
    this.attributes = parent.attributes;
    this.directives = parent.directives;
  }

  /**
   * A require bundle requirement.
   *
   * @param requestor
   *          The bundle packages of the fragment host that requires a bundle.
   * @param he
   *          The parsed require bundle header.
   */
  RequireBundle(final BundlePackages requestor, final HeaderEntry he)
  {
    this.requestor = requestor;
    this.name = he.getKey();

    final Map<String,String> dirs = he.getDirectives();
    final String visibility = dirs.get(Constants.VISIBILITY_DIRECTIVE);
    if (visibility != null) {
      this.visibility = visibility.intern();
      if (this.visibility!=Constants.VISIBILITY_PRIVATE &&
          this.visibility!=Constants.VISIBILITY_REEXPORT ) {
        throw new IllegalArgumentException
          ("Invalid directive : '"
           +Constants.VISIBILITY_DIRECTIVE +":="+this.visibility
           +"' in manifest header '"
           +Constants.REQUIRE_BUNDLE +": " +this.name
           +"' of bundle with id " +this.requestor.bg.bundle.getBundleId()
           +" ("+this.requestor.bg.symbolicName+")"
           +". The value must be either '"
           +Constants.VISIBILITY_PRIVATE  +"' or '"
           +Constants.VISIBILITY_REEXPORT +"'.");
      }
    } else {
      this.visibility = Constants.VISIBILITY_PRIVATE;
    }

    final String resolution = dirs.get(Constants.RESOLUTION_DIRECTIVE);
    if (resolution != null) {
      this.resolution = resolution.intern();
      if (this.resolution!=Constants.RESOLUTION_MANDATORY &&
          this.resolution!=Constants.RESOLUTION_OPTIONAL ) {
        throw new IllegalArgumentException
          ("Invalid directive : '"
           +Constants.RESOLUTION_DIRECTIVE +":="+this.resolution
           +"' in manifest header '"
           +Constants.REQUIRE_BUNDLE +": " +this.name
           +"' of bundle with id " +this.requestor.bg.bundle.getBundleId()
           +" ("+this.requestor.bg.symbolicName+")"
           +". The value must be either '"
           +Constants.RESOLUTION_MANDATORY +"' or '"
           +Constants.RESOLUTION_OPTIONAL  +"'.");
      }
    } else {
      this.resolution = Constants.RESOLUTION_MANDATORY;
    }

    this.attributes = he.getAttributes();
    final String range = (String) attributes.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
    if (range != null) {
      this.bundleRange = new VersionRange(range);
    } else {
      this.bundleRange = null;
    }
    final Filter filter = toFilter();
    if (null!=filter) {
      dirs.put(Constants.FILTER_DIRECTIVE, filter.toString());
    }
    this.directives = Collections.unmodifiableMap(dirs);
  }


  /**
   * Check if this object completely overlap specified RequireBundle.
   *
   * @return True if we overlap, otherwise false.
   */
  boolean overlap(RequireBundle rb) {
    if (visibility.equals(Constants.VISIBILITY_REEXPORT) &&
        !rb.visibility.equals(Constants.VISIBILITY_REEXPORT)) {
      return false;
    }
    if (resolution.equals(Constants.RESOLUTION_MANDATORY) &&
        !rb.resolution.equals(Constants.RESOLUTION_MANDATORY)) {
      return false;
    }
    return  bundleRange == null || rb.bundleRange == null ||
        !bundleRange.intersection(rb.bundleRange).isEmpty();
  }


  // BundleRequirement method
  @Override
  public String getNamespace()
  {
    return BundleRevision.BUNDLE_NAMESPACE;
  }


  // BundleRequirement method
  @Override
  public Map<String, String> getDirectives()
  {
    return directives;
  }


  private Filter toFilter()
  {
    final StringBuilder sb = new StringBuilder(80);
    boolean multipleConditions = false;

    sb.append('(');
    sb.append(BundleRevision.BUNDLE_NAMESPACE);
    sb.append('=');
    sb.append(name);
    sb.append(')');

    if (bundleRange != null) {
      sb.append(bundleRange.toFilterString(Constants.BUNDLE_VERSION_ATTRIBUTE));
      multipleConditions = true;
    }

    for (final Entry<String,Object> entry : attributes.entrySet()) {
      sb.append('(');
      sb.append(entry.getKey());
      sb.append('=');
      sb.append(entry.getValue().toString());
      sb.append(')');
      multipleConditions = true;
    }

    if (multipleConditions) {
      sb.insert(0, "(&");
      sb.append(')');
    }
    try {
      return FrameworkUtil.createFilter(sb.toString());
    } catch (final InvalidSyntaxException _ise) {
      throw new RuntimeException("Internal error, createFilter: '" +sb.toString() +"': " +_ise.getMessage());
    }
  }


  // BundleRequirement method
  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> getAttributes()
  {
    return Collections.EMPTY_MAP;
  }


  // BundleRequirement method
  @Override
  public BundleRevision getRevision()
  {
    return requestor.bg.bundleRevision;
  }


  @Override
  public BundleRevision getResource() {
    return requestor.bg.bundleRevision;
  }


  // BundleRequirement method
  @Override
  public boolean matches(BundleCapability capability)
  {
    if (BundleRevision.BUNDLE_NAMESPACE.equals(capability.getNamespace())) {
      return toFilter().matches(capability.getAttributes());
    }
    return false;
  }


  @Override
  public int compareTo(RequireBundle o)
  {
    return this.orderal - o.orderal;
  }

}
