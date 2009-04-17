/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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


/**
 * Data structure for saving package info. Contains package name,
 * current provider, list of possible exports and all bundles
 * importing this package.
 */
class PkgEntry {
  final String name;
  final BundleImpl bundle;
  final VersionNumber version;

  // Link to pkg entry
  private Pkg pkg = null;


  /**
   * Create package entry.
   */
  PkgEntry(String p, String v, BundleImpl b) {
    this.name = p;
    this.version = new VersionNumber(v);
    this.bundle = b;
  }


  /**
   * Create package entry.
   */
  PkgEntry(String p, VersionNumber v, BundleImpl b) {
    this.name = p;
    this.version = v;
    this.bundle = b;
  }


  /**
   * Create package entry.
   */
  PkgEntry(PkgEntry pe) {
    this.name = pe.name;
    this.version = pe.version;
    this.bundle = pe.bundle;
  }


  /**
   * Get Pkg object.
   *
   * @return Pkg object associated with this PkgEntry.
   */
  Pkg getPkg() {
    return pkg;
  }


  /**
   * Set Pkg object.
   *
   * @param Pkg to set for this PkgEntry.
   */
  synchronized void setPkg(Pkg p) {
    pkg = p;
  }


  /**
   * Package name equal.
   *
   * @param other Package entry to compare to.
   * @return true if equal, otherwise false.
   */
  boolean packageEqual(PkgEntry other) {
    return name.equals(other.name);
  }


  /**
   * Version compare object to another PkgEntry.
   *
   * @param obj Version to compare to.
   * @return Return 0 if equals, negative if this object is less than obj
   *         and positive if this object is larger then obj.
   * @exception ClassCastException if object is not a PkgEntry object.
   */
  public int compareVersion(Object obj) throws ClassCastException {
    PkgEntry o = (PkgEntry)obj;
    return version.compareTo(o.version);
  }


  /**
   * String describing package name and specification version, if specified.
   *
   * @return String.
   */
  public String pkgString() {
    if (version.isSpecified()) {
      return name + ";" + Constants.PACKAGE_SPECIFICATION_VERSION + "=" + version;
    } else {
      return name;
    }
  }


  /**
   * Check if PkgEntry is provider of a package.
   *
   * @return True if this entry exports the package.
   */
  synchronized boolean isProvider() {
    return pkg != null && pkg.getProvider() == this;
  }


  /**
   * Check if a package is in zombie state.
   *
   * @return True if this package is a zombie exported.
   */
  synchronized boolean isZombiePackage() {
    return pkg != null && pkg.isZombie();
  }


  /**
   * String describing this object.
   *
   * @return String.
   */
  public String toString() {
    return pkgString() + "(" + bundle + ")";
  }


  /**
   * Check if object is equal to this object.
   *
   * @param obj Package entry to compare to.
   * @return true if equal, otherwise false.
   */
  public boolean equals(Object obj) throws ClassCastException {
    PkgEntry o = (PkgEntry)obj;
    return name.equals(o.name) && bundle == o.bundle && version.equals(o.version);
  }


  /**
   * Hash code for this package entry.
   *
   * @return int value.
   */
  public int hashCode() {
    return name.hashCode() + bundle.hashCode() + version.hashCode();
  }

}
