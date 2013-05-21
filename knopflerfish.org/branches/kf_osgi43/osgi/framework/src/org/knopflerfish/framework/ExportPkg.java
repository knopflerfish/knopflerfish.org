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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knopflerfish.framework.Util.HeaderEntry;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;


/**
 * Data structure for export package definitions.
 *
 * @author Jan Stein, Gunnar Ekolin
 */
class ExportPkg
  implements BundleCapability, Comparable<ExportPkg>
{
  // To maintain the creation order in the osgi.wiring.package name space.
  static private int exportPkgCount = 0;
  final int orderal = ++exportPkgCount;

  final String name;
  final BundlePackages bpkgs;
  final Set<String> uses;
  final Set<String> mandatory;
  final Set <String> include;
  final Set<String> exclude;
  final Version version;
  final Map<String,Object> attributes;
  boolean zombie = false;

  // Link to pkg entry
  Pkg pkg = null;

  /**
   * Create an export package entry.
   */
  ExportPkg(final String name, final HeaderEntry he, final BundlePackages b)
  {
    this.bpkgs = b;
    this.name = name;
    if (name.startsWith("java.")) {
      throw new IllegalArgumentException("You can not export a java.* package");
    }
    this.uses = Util.parseEnumeration(Constants.USES_DIRECTIVE, he
        .getDirectives().get(Constants.USES_DIRECTIVE));
    this.mandatory = Util.parseEnumeration(Constants.MANDATORY_DIRECTIVE, he
        .getDirectives().get(Constants.MANDATORY_DIRECTIVE));
    this.include = Util.parseEnumeration(Constants.INCLUDE_DIRECTIVE, he
        .getDirectives().get(Constants.INCLUDE_DIRECTIVE));
    this.exclude = Util.parseEnumeration(Constants.EXCLUDE_DIRECTIVE, he
        .getDirectives().get(Constants.EXCLUDE_DIRECTIVE));
    final String versionStr = (String) he.getAttributes()
        .remove(Constants.VERSION_ATTRIBUTE);
    final String specVersionStr = (String) he.getAttributes()
        .remove(Constants.VERSION_ATTRIBUTE);
    if (specVersionStr != null) {
      this.version = new Version(specVersionStr);
      if (versionStr != null && !this.version.equals(new Version(versionStr))) {
        @SuppressWarnings("deprecation")
        final String SPEC_VERSION = Constants.PACKAGE_SPECIFICATION_VERSION;
        throw new IllegalArgumentException("Both " + Constants.VERSION_ATTRIBUTE +
                                           " and " + SPEC_VERSION  +
                                           " are specified, and differs");
      }
    } else if (versionStr != null) {
      this.version = new Version(versionStr);
    } else {
      this.version = Version.emptyVersion;
    }
    if (he.getAttributes().containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE)) {
      throw new IllegalArgumentException("Export definition illegally contains attribute, " +
                                         Constants.BUNDLE_VERSION_ATTRIBUTE);
    }
    if (he.getAttributes().containsKey(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE)) {
      throw new IllegalArgumentException("Export definition illegally contains attribute, " +
                                         Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
    }
    this.attributes = Collections.unmodifiableMap(he.getAttributes());
  }


  /**
   * Create an export package entry with a new name from an export template.
   *
   * @param ep The export template to create an export package entry for.
   * @param name The name of the export package entry created from the template.
   */
  ExportPkg(ExportPkg ep, String name) {
    this.name = name;
    this.bpkgs = ep.bpkgs;
    this.uses = ep.uses;
    this.mandatory = ep.mandatory;
    this.include = ep.include;
    this.exclude = ep.exclude;
    this.version = ep.version;
    this.attributes = ep.attributes;
  }


  /**
   * Create a re-export package entry with a new bundle owner from an
   * existing export.
   * @param ep The ExportPkg to create a re-export package entry for.
   * @param b  The BundlePackages that owns this re-exprot entry.
   */
  ExportPkg(ExportPkg ep, BundlePackages b) {
    this.name = ep.name;
    this.bpkgs = b;
    this.uses = ep.uses;
    this.mandatory = ep.mandatory;
    this.include = ep.include;
    this.exclude = ep.exclude;
    this.version = ep.version;
    this.attributes = ep.attributes;
  }


  /**
   * Attach this to a Pkg object which indicate that it is exported.
   */
  void attachPkg(Pkg p) {
    pkg = p;
  }


  /**
   * Detach this from a Pkg object which indicate that it is no longer exported.
   */
  void detachPkg() {
    pkg = null;
    zombie = false;
  }


  /**
   * Checks if we are allowed to export this class according to
   * the filter rules.
   */
  boolean checkFilter(String fullClassName) {
    String clazz = null;
    boolean ok = true;
    if (fullClassName != null) {
      if (include != null) {
        // assert fullClassName.startsWith(name)
        clazz = fullClassName.substring(name.length() + 1);
        for (final Iterator<String> i = include.iterator(); i.hasNext(); ) {
          if (Util.filterMatch(i.next(), clazz)) {
            break;
          }
          if (!i.hasNext()) {
            ok = false;
          }
        }
      }
      if (ok && exclude != null) {
        if (clazz == null) {
          // assert fullClassName.startsWith(name)
          clazz = fullClassName.substring(name.length() + 1);
        }
        for (final String string : exclude) {
          if (Util.filterMatch(string, clazz)) {
            ok = false;
            break;
          }
        }
      }
    }
    return ok;
  }


  /**
   * Check if ExportPkg is provider of a package.
   *
   * @return True if pkg exports the package.
   */
  boolean isProvider() {
    final Pkg p = pkg;
    if (p != null) {
      synchronized (p) {
        return p.providers.contains(this) || bpkgs.isRequired();
      }
    }
    return false;
  }


  /**
   * Check if ExportPkg is exported from its bundle. A package is deemed to
   * be exported if its bundle is resolved and hasn't been replaced by a
   * conflicting import (see resolving process chapter in core spec.).
   * Bundle must also have export permission.
   *
   * @return True if pkg exports the package.
   */
  boolean isExported() {
    final BundlePackages bp = bpkgs;
    if (checkPermission() && pkg != null &&
        (bp.bg.bundle.isResolved() || zombie)) {
      final BundlePackages pbp = bp.getProviderBundlePackages(name);
      return pbp == null || pbp.bg.bundle == bpkgs.bg.bundle;
    }
    return false;
  }


  /**
   * Get active importers of a package.
   *
   * @param pkg Package.
   * @return List of bundles importing, null export is not active.
   */
  List<ImportPkg> getPackageImporters() {
    final Pkg p = pkg;
    if (p != null) {
      final List<ImportPkg> res = new ArrayList<ImportPkg>();
      synchronized (p) {
        for (final ImportPkg ip : p.importers) {
          if (ip.provider == this && ip.bpkgs != bpkgs) {
            res.add(ip);
          }
        }
      }
      return res;
    }
    return null;
  }


  /**
   * Check if we have export permissions.
   *
   * @return true if we have export permission
   */
  boolean checkPermission() {
    return bpkgs.bg.bundle.fwCtx.perm.hasExportPackagePermission(this);
  }


  /**
   * Check if the name, version, attributes and directives are equal.
   *
   * @return true if all package information is equal, otherwise false.
   */
  boolean pkgEquals(Object o) {
    if (this == o) {
      return true;
    }
    if (null == o) {
      return false;
    }
    final ExportPkg ep = (ExportPkg)o;
    return name.equals(ep.name) &&
      version.equals(ep.version) &&
      (uses == null ? ep.uses == null : uses.equals(ep.uses)) &&
      (mandatory == null ? ep.mandatory == null : mandatory.equals(ep.mandatory)) &&
      (include == null ? ep.include == null : include.equals(ep.include)) &&
      (exclude == null ? ep.exclude == null : exclude.equals(ep.exclude)) &&
      attributes.equals(ep.attributes);
  }


  /**
   * String describing package name and specification version, if specified.
   *
   * @return String.
   */
  public String pkgString() {
    if (version != Version.emptyVersion) {
      return name + ";" + Constants.VERSION_ATTRIBUTE + "=" + version;
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
    final StringBuffer sb = new StringBuffer(pkgString());
    sb.append(' ');
    if (zombie) {
      sb.append("Zombie");
    }
    sb.append("Bundle");
    sb.append(bpkgs.bundleGenInfo());
    return sb.toString();
  }


  public String getNamespace() {
    return BundleRevision.PACKAGE_NAMESPACE;
  }


  public Map<String, String> getDirectives() {
    final Map<String,String> res = new HashMap<String, String>(1);

    if (uses!=null) {
      final StringBuffer sb = new StringBuffer(uses.size()*30);
      for (final String pkg : uses) {
        if (sb.length()>0) sb.append(',');
        sb.append(pkg);
      }
      res.put(Constants.USES_DIRECTIVE, sb.toString());
    }

    return res;
  }


  public Map<String, Object> getAttributes() {
    final Map<String,Object> res
      = new HashMap<String, Object>(4+attributes.size());

    res.put(BundleRevision.PACKAGE_NAMESPACE, name);
    res.put(Constants.VERSION_ATTRIBUTE, version);

    res.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, bpkgs.bg.symbolicName);
    res.put(Constants.BUNDLE_VERSION_ATTRIBUTE, bpkgs.bg.version);

    res.putAll(attributes);

    return Collections.unmodifiableMap(res);
  }


  public BundleRevision getRevision() {
    return bpkgs.bg.bundleRevision;
  }


  /**
   * The default ordering is the order in which the {@code ExportPkg}-objects
   * has been created. I.e., the order they appeared in the {@code Export-Package}
   * header.
   *
   * @param o other object to compare with.
   * @return Less than zero, zero or greater than zero of this object is smaller
   *  than, equals to or greater than {@code o}.
   */
  public int compareTo(ExportPkg o)
  {
    return this.orderal - o.orderal;
  }

}
