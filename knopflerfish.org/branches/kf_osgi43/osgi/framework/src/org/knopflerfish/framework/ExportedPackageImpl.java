/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.util.HashSet;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;


/**
 * An exported package.
 *
 * Instances implementing this interface are created by the
 * {@link PackageAdmin} service.
 * <p> Note that the information about an exported package provided by
 * this class is valid only until the next time
 * <tt>PackageAdmin.refreshPackages()</tt> is
 * called.
 * If an ExportedPackage becomes stale (that is, the package it references
 * has been updated or removed as a result of calling
 * PackageAdmin.refreshPackages()),
 * its getName() and getSpecificationVersion() continue to return their
 * old values, isRemovalPending() returns true, and getExportingBundle()
 * and getImportingBundles() return null.
 */
@SuppressWarnings("deprecation")
public class ExportedPackageImpl implements ExportedPackage {

  final private ExportPkg pkg;

  ExportedPackageImpl(ExportPkg pkg) {
    this.pkg = pkg;
  }


  /**
   * Returns the name of this <tt>ExportedPackage</tt>.
   *
   * @return The name of this <tt>ExportedPackage</tt>.
   */
  public String getName() {
    return pkg.name;
  }


  /**
   * Returns the bundle that is exporting this <tt>ExportedPackage</tt>.
   *
   * @return The exporting bundle, or null if this <tt>ExportedPackage</tt>
   *         has become stale.
   */
  public Bundle getExportingBundle() {
    if (pkg.pkg != null) {
      return pkg.bpkgs.bg.bundle;
    } else {
      return null;
    }
  }


  /**
   * Returns the resolved bundles that are currently importing this
   * <tt>ExportedPackage</tt>.
   *
   * @return The array of resolved bundles currently importing this
   * <tt>ExportedPackage</tt>, or null if this <tt>ExportedPackage</tt>
   * has become stale.
   */
  public Bundle[] getImportingBundles() {
    final List<ImportPkg> imps = pkg.getPackageImporters();
    if (imps != null) {
      final HashSet<Bundle> bs = new HashSet<Bundle>();
      for (final ImportPkg ip : imps) {
        bs.add(ip.bpkgs.bg.bundle);
      }
      final List<BundlePackages> rl = pkg.bpkgs.getRequiredBy();
      for (final BundlePackages bp : rl) {
        bs.add(bp.bg.bundle);
      }
      return bs.toArray(new Bundle[bs.size()]);
    } else {
      return null;
    }
  }


  /**
   * Returns the specification version of this <tt>ExportedPackage</tt>, as
   * specified in the exporting bundle's manifest file.
   *
   * @return The specification version of this <tt>ExportedPackage</tt>, or
   *         <tt>null</tt> if no version information is available.
   */
  public String getSpecificationVersion() {
    return pkg.version.toString();
  }


  /**
   * Returns <tt>true</tt> if this <tt>ExportedPackage</tt> has been
   * exported by a bundle that has been updated or uninstalled.
   *
   * @return <tt>true</tt> if this <tt>ExportedPackage</tt> is being
   * exported by a bundle that has been updated or uninstalled;
   * <tt>false</tt> otherwise.
   */
  public boolean isRemovalPending() {
    if (pkg.isProvider()) {
      return pkg.zombie;
    } else {
      return false;
    }
  }


  public Version getVersion() {
    return pkg.version;
  }

  @Override
  public String toString() {
    return pkg.name +"(" +pkg.version +")";
  }

}
