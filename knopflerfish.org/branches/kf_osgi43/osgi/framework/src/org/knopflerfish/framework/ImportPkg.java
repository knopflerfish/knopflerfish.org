/*
 * Copyright (c) 2005-2013, KNOPFLERFISH project
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

import org.knopflerfish.framework.Util.HeaderEntry;

/**
 * Data structure for import package definitions.
 *
 * @author Jan Stein, Gunnar Ekolin
 */
class ImportPkg implements BundleRequirement, Comparable<ImportPkg> {

  /**
   * The value of the resolution directive for dynamically imported packages.
   */
  static final String RESOLUTION_DYNAMIC = "dynamic";

  @SuppressWarnings("deprecation")
  private static final String PACKAGE_SPECIFICATION_VERSION = Constants.PACKAGE_SPECIFICATION_VERSION;

  // To maintain the creation order in the osgi.wiring.package name space.
  static private int importPkgCount = 0;

  final int orderal = ++importPkgCount;

  final String name;
  final BundlePackages bpkgs;
  final String resolution;
  final String bundleSymbolicName;
  final VersionRange packageRange;
  final VersionRange bundleRange;
  final Map<String,Object> attributes;
  final ImportPkg parent;

  // Link to pkg entry
  Pkg pkg = null;

  // Link to exporter
  ExportPkg provider = null;
  // Link to interal exporter ok to use
  ExportPkg internalOk = null;

  // Ordering of dynamic imports
  int dynId = 0;


  /**
   * Create an import package entry from manifest parser data.
   *
   * @param name the name of the package to be imported.
   * @param he the parsed import package statement.
   * @param b back link to the bundle revision owning this import declaration.
   * @param dynamic Set to true if this is a dynamic import package declaration.
   */
  ImportPkg(final String name, final HeaderEntry he, final BundlePackages b,
            boolean dynamic)
  {
    this.bpkgs = b;
    this.name = name;
    if (name.startsWith("java.")) {
      throw new IllegalArgumentException("You can not import a java.* package");
    }
    final String res = he.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
    if (dynamic) {
      if (res != null) {
        throw new IllegalArgumentException("Directives not supported for "
                                           + "Dynamic-Import, found "
                                           + Constants.RESOLUTION_DIRECTIVE
                                           + ":=" +res);
      }
      this.resolution = RESOLUTION_DYNAMIC;
    } else {
      if (res != null) {
        if (Constants.RESOLUTION_OPTIONAL.equals(res)) {
          this.resolution = Constants.RESOLUTION_OPTIONAL;
        } else if (Constants.RESOLUTION_MANDATORY.equals(res)) {
          this.resolution = Constants.RESOLUTION_MANDATORY;
        } else {
          throw new IllegalArgumentException("Directive " + Constants.RESOLUTION_DIRECTIVE +
                                             ", unexpected value: " + res);
        }
      } else {
        this.resolution = Constants.RESOLUTION_MANDATORY;
      }
    }
    this.bundleSymbolicName = (String) he.getAttributes()
        .remove(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
    final String versionStr = (String) he.getAttributes()
        .remove(Constants.VERSION_ATTRIBUTE);
    final String specVersionStr = (String) he.getAttributes()
        .remove(PACKAGE_SPECIFICATION_VERSION);
    if (specVersionStr != null) {
      this.packageRange = new VersionRange(specVersionStr);
      if (versionStr != null && !this.packageRange.equals(new VersionRange(versionStr))) {
        throw new IllegalArgumentException("Both " + Constants.VERSION_ATTRIBUTE +
                                           " and " + PACKAGE_SPECIFICATION_VERSION +
                                           " are specified, but differs");
      }
    } else if (versionStr != null) {
      this.packageRange = new VersionRange(versionStr);
    } else {
      this.packageRange = VersionRange.defaultVersionRange;
    }
    final String rangeStr = (String) he.getAttributes()
        .remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
    if (rangeStr != null) {
      this.bundleRange = new VersionRange(rangeStr);
    } else {
      this.bundleRange = VersionRange.defaultVersionRange;
    }
    this.attributes = Collections.unmodifiableMap(he.getAttributes());
    this.parent = null;
  }


  /**
   * Create an import package entry with a new name from an import template.
   */
  ImportPkg(ImportPkg ip, String name) {
    this.name = name;
    this.bpkgs = ip.bpkgs;
    this.resolution = ip.resolution;
    this.bundleSymbolicName = ip.bundleSymbolicName;
    this.packageRange = ip.packageRange;
    this.bundleRange = ip.bundleRange;
    this.attributes = ip.attributes;
    this.parent = ip;
  }


  /**
   * Creates an import package entry with a new host bundle.
   */
  ImportPkg(ImportPkg ip, BundlePackages bpkgs) {
    this.name = ip.name;
    this.bpkgs = bpkgs;
    this.resolution = ip.resolution;
    this.bundleSymbolicName = ip.bundleSymbolicName;
    this.packageRange = ip.packageRange;
    this.bundleRange = ip.bundleRange;
    this.attributes = ip.attributes;
    this.parent = ip.parent;
  }


  /**
   * Create an import package entry.
   */
  ImportPkg(ExportPkg p) {
    this.name = p.name;
    this.bpkgs = p.bpkgs;
    this.resolution = Constants.RESOLUTION_MANDATORY;
    this.bundleSymbolicName = null;
    if (p.version == Version.emptyVersion) {
      this.packageRange = VersionRange.defaultVersionRange;
    } else {
      this.packageRange = new VersionRange(p.version.toString());
    }
    this.bundleRange = VersionRange.defaultVersionRange;
    this.attributes = p.attributes;
    this.parent = null;
  }


  /**
   * Attach this to a Pkg object which indicate that it is a valid importer.
   */
  void attachPkg(Pkg p) {
    pkg = p;
  }


  /**
   * Detach this from a Pkg object which indicate that it is no longer valid.
   */
  void detachPkg() {
    pkg = null;
    provider = null;
  }


  /**
   * Check if version fulfills import package constraints.
   *
   * @param ver Version to compare to.
   * @return Return 0 if equals, negative if this object is less than obj and
   *         positive if this object is larger then obj.
   */
  public boolean okPackageVersion(Version ver) {
    return packageRange.withinRange(ver);
  }


  /**
   * Check that all package attributes match.
   *
   * @param ep Exported package.
   * @return True if okay, otherwise false.
   */
  boolean checkAttributes(ExportPkg ep) {
    /* Mandatory attributes */
    if (!checkMandatory(ep.mandatory)) {
      return false;
    }
    /* Predefined attributes */
    if (!okPackageVersion(ep.version) ||
        (bundleSymbolicName != null &&
         !bundleSymbolicName.equals(ep.bpkgs.bg.symbolicName)) ||
        !bundleRange.withinRange(ep.bpkgs.bg.version)) {
      return false;
    }
    /* Other attributes */
    for (final Entry<String,Object> entry : attributes.entrySet()) {
      final String a = (String) ep.attributes.get(entry.getKey());
      if (a == null || !a.equals(entry.getValue())) {
        return false;
      }
    }
    return true;
  }


  /**
   * Check that we have import permission for exported package.
   *
   * @param ep Exported package.
   * @return True if okay, otherwise false.
   */
  boolean checkPermission(ExportPkg ep) {
    return bpkgs.bg.bundle.fwCtx.perm.hasImportPackagePermission(bpkgs.bg.bundle, ep);
  }


  /**
   * Check that we can do an internal wire to the exported package.
   *
   * @return True if okay, otherwise false.
   */
  boolean mustBeResolved() {
    return resolution == Constants.RESOLUTION_MANDATORY && internalOk == null;
  }


  /**
   * Check that we intersect specified ImportPkg.
   *
   * @param ip ImportPkg to check.
   * @return True if we overlap, otherwise false.
   */
  boolean intersect(ImportPkg ip) {
    if (ip.bundleSymbolicName != null && bundleSymbolicName != null &&
        !ip.bundleSymbolicName.equals(bundleSymbolicName)) {
      return false;
    }

    // Check that no other attributes conflict
    for (final Entry<String,Object> entry : attributes.entrySet()) {
      final String a = (String) ip.attributes.get(entry.getKey());
      if (a != null && !a.equals(entry.getValue())) {
        return false;
      }
    }

    // Resolution doesn't need to be checked.
    // This is handle when resolving package.
    // If one import is mandatory then all must match.

    if (!packageRange.intersectRange(ip.packageRange)) {
      return false;
    }
    return bundleRange.intersectRange(ip.bundleRange);
  }


  /**
   * String describing package name and specification version, if specified.
   *
   * @return String.
   */
  public String pkgString() {
    // NYI! More info?
    if (packageRange.isSpecified()) {
      return name + ";" + Constants.VERSION_ATTRIBUTE + "=" + packageRange;
    } else {
      return name;
    }
  }


  /**
   * String describing this object.
   *
   * @return String.
   */
  @Override
  public String toString() {
    return pkgString() + "(" + bpkgs.bg.bundle + ")";
  }


  boolean isDynamic() {
    return resolution == RESOLUTION_DYNAMIC;
  }


  /**
   * Check that we have all mandatory attributes.
   *
   * @param mandatory Collection of mandatory attribute.
   * @return Return true if we have all mandatory attributes, otherwise false.
   */
  private boolean checkMandatory(final Collection<String> mandatory) {
    if (mandatory != null) {
      for (final String a : mandatory) {
        if (Constants.VERSION_ATTRIBUTE.equals(a)) {
          if (!packageRange.isSpecified()) {
            return false;
          }
        } else if (Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE.equals(a)) {
          if (bundleSymbolicName == null) {
            return false;
          }
        } else if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(a)) {
          if (!bundleRange.isSpecified()) {
            return false;
          }
        } else if (!attributes.containsKey(a)) {
          return false;
        }
      }
    }
    return true;
  }


  // BundleRequirement method
  public String getNamespace() {
    return BundleRevision.PACKAGE_NAMESPACE;
  }


  // BundleRequirement method
  public Map<String, String> getDirectives() {
    final Map<String,String> res = new HashMap<String, String>(4);

    res.put(Constants.RESOLUTION_DIRECTIVE, resolution);

    // For PACKAGE_NAMESPACE effective defaults to resolve and no other value
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
    sb.append(BundleRevision.PACKAGE_NAMESPACE);
    sb.append('=');
    sb.append(name);
    if (name.length()==0 || name.endsWith(".")) {
      // Dynamic import with wild-card.
      sb.append('*');
    }
    sb.append(')');

    if (packageRange != null) {
      multipleConditions |= packageRange
          .appendFilterString(sb, Constants.VERSION_ATTRIBUTE);
    }

    if (bundleSymbolicName != null) {
      sb.append('(');
      sb.append(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
      sb.append('=');
      sb.append(bundleSymbolicName);
      sb.append(')');
      multipleConditions |= true;
    }

    if (bundleRange != null) {
      multipleConditions |= bundleRange
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

  // BundleRequirement method
  public Map<String, Object> getAttributes() {
    @SuppressWarnings("unchecked")
    final
    Map<String, Object> res = Collections.EMPTY_MAP;
    return res;
  }


  // BundleRequirement method
  public BundleRevision getRevision() {
    return bpkgs.bg.bundleRevision;
  }


  // BundleRequirement method
  public boolean matches(BundleCapability capability) {
    if (BundleRevision.PACKAGE_NAMESPACE.equals(capability.getNamespace())) {
      return toFilter().matches(capability.getAttributes());
    }
    return false;
  }


  /**
   * The default ordering is the order in which the {@code ImportPkg}-objects
   * has been created. I.e., the order they appeared in the {@code Import-Package}
   * header.
   *
   * @param o other object to compare with.
   * @return Less than zero, zero or greater than zero of this object is smaller
   *  than, equals to or greater than {@code o}.
   */
  public int compareTo(ImportPkg o)
  {
    return this.orderal - o.orderal;
  }

}
