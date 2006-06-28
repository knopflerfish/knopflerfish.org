/*
 * Copyright (c) 2005-2006, KNOPFLERFISH project
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

import java.util.*;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;


/**
 * Data structure for import package definitions.
 *
 * @author Jan Stein
 */
class ImportPkg {
  final String name;
  final BundlePackages bpkgs;
  final String resolution;
  final String bundleSymbolicName;
  final VersionRange packageRange;
  final VersionRange bundleRange;
  final Map attributes;

  // Link to pkg entry
  Pkg pkg = null;

  // Link to exporter
  ExportPkg provider = null;

  /**
   * Create an import package entry.
   */
  ImportPkg(String name, Map tokens, BundlePackages b) {
    this.bpkgs = b;
    this.name = name;
    if (name.startsWith("java.")) {
      throw new IllegalArgumentException("You can not import a java.* package");
    }
    String res = (String)tokens.remove(Constants.RESOLUTION_DIRECTIVE);
    if (res != null) {
      if (Constants.RESOLUTION_OPTIONAL.equals(res)) {
	this.resolution = Constants.RESOLUTION_OPTIONAL;
      }  else if (Constants.RESOLUTION_MANDATORY.equals(res)) {
	this.resolution = Constants.RESOLUTION_MANDATORY;
      } else {
	throw new IllegalArgumentException("Directive " + Constants.RESOLUTION_DIRECTIVE +
					   ", unexpected value: " + res);
      }
    } else {
      this.resolution = Constants.RESOLUTION_MANDATORY;
    }
    this.bundleSymbolicName = (String)tokens.remove(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
    String versionStr = (String)tokens.remove(Constants.VERSION_ATTRIBUTE);
    String specVersionStr = (String)tokens.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
    if (specVersionStr != null) {
      this.packageRange = new VersionRange(specVersionStr);
      if (versionStr != null && !this.packageRange.equals(new VersionRange(versionStr))) {
	throw new IllegalArgumentException("Both " + Constants.VERSION_ATTRIBUTE + 
                                           " and " + Constants.PACKAGE_SPECIFICATION_VERSION +
					   "are specified, but differs");
      }
    } else if (versionStr != null) {
      this.packageRange = new VersionRange(versionStr);
    } else {
      this.packageRange = VersionRange.defaultVersionRange;
    }
    String rangeStr = (String)tokens.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
    if (rangeStr != null) {
      this.bundleRange = new VersionRange(rangeStr);
    } else {
      this.bundleRange = VersionRange.defaultVersionRange;
    }
    this.attributes = tokens;
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
  }

  /**
   * Create an import package entry.
   */
  ImportPkg(ExportPkg p) {
    this.name = p.name;
    this.bpkgs = p.bpkgs;
    this.resolution = Constants.RESOLUTION_MANDATORY;;
    this.bundleSymbolicName = null;
    if (p.version == Version.emptyVersion) {
      this.packageRange = VersionRange.defaultVersionRange;
    } else {
      this.packageRange = new VersionRange(p.version.toString());
    }
    this.bundleRange = VersionRange.defaultVersionRange;
    this.attributes = p.attributes;
  }


  /**
   * Attach this to a Pkg object which indicate that it is a valid importer.
   */
  synchronized void attachPkg(Pkg p) {
    pkg = p;
  }


  /**
   * Detach this from a Pkg object which indicate that it is no longer valid.
   */
  synchronized void detachPkg() {
    pkg = null;
    provider = null;
  }


  /**
   * Check if version fullfills import package constraints.
   *
   * @param ver Version to compare to.
   * @return Return 0 if equals, negative if this object is less than obj
   *         and positive if this object is larger then obj.
   */
  public boolean okPackageVersion(Version ver) {
    return packageRange.withinRange(ver);
  }


  /**
   * Check that we have all mandatory attributes.
   *
   * @param mandatory List of mandatory attribute.
   * @return Return true if we have all mandatory attributes, otherwise false.
   */
  boolean checkMandatory(List mandatory) {
    if (mandatory != null) {
      for (Iterator i = mandatory.iterator(); i.hasNext(); ) {
        String a = (String)i.next();
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


  /**
   * Check that we completly overlap specifed ImportPkg.
   *
   * @param ip ImportPkg to check.
   * @return True if we overlap, otherwise false.
   */
  boolean overlap(ImportPkg ip) {
    if (ip.bundleSymbolicName == null ? bundleSymbolicName != null :
        !ip.bundleSymbolicName.equals(bundleSymbolicName)) {
      return false;
    }

    // Check that all other attributes match
    for (Iterator i = attributes.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry e = (Map.Entry)i.next();
      String a = (String)ip.attributes.get(e.getKey());
      if (a == null || !a.equals(e.getValue())) {
        return false;
      }
    }

    if (resolution.equals(Constants.RESOLUTION_MANDATORY) &&
        !ip.resolution.equals(Constants.RESOLUTION_MANDATORY)) {
      return false;
    }
    if (!packageRange.withinRange(ip.packageRange)) {
      return false;
    }
    return bundleRange.withinRange(ip.bundleRange);
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
  public String toString() {
    return pkgString() + "(" + bpkgs.bundle + ")";
  }

}
