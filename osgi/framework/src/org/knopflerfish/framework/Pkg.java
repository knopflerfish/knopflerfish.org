/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

import org.osgi.framework.Version;


/**
 * Class representing a package.
 *
 * @author Jan Stein
 */
class Pkg {

  final String pkg;

  final ArrayList<ExportPkg> exporters = new ArrayList<ExportPkg>(1);

  final ArrayList<ImportPkg> importers = new ArrayList<ImportPkg>();

  final ArrayList<ExportPkg> providers = new ArrayList<ExportPkg>(1);


  /**
   * Create package entry.
   */
  Pkg(String pkg) {
    this.pkg = pkg;
  }


  /**
   * Add an exporter entry from this package.
   *
   * @param pe ExportPkg to add.
   */
  synchronized void addExporter(ExportPkg ep) {
    final int i = Math.abs(Util.binarySearch(exporters, epComp, ep) + 1);
    exporters.add(i, ep);
    ep.attachPkg(this);
  }


  /**
   * Remove an exporter entry from this package.
   *
   * @param p ExportPkg to remove.
   * @return false if package is provider otherwise true.
   */
  synchronized boolean removeExporter(ExportPkg p) {
    providers.remove(p);
    exporters.remove(p);
    p.detachPkg();
    return true;
  }


  /**
   * Add an importer entry to this package.
   *
   * @param pe ImportPkg to add.
   */
  synchronized void addImporter(ImportPkg ip) {
    final int i = Math.abs(Util.binarySearch(importers, ipComp, ip) + 1);
    importers.add(i, ip);
    ip.attachPkg(this);
  }


  /**
   * Remove an importer entry from this package.
   *
   * @param p ImportPkg to remove.
   */
  synchronized void removeImporter(ImportPkg ip) {
    importers.remove(ip);
    ip.detachPkg();
  }


  /**
   * Add an exporter entry as a provider for this package.
   * If exporter already is provider don't add duplicate.
   *
   * @param pe ExportPkg to add.
   */
  synchronized void addProvider(ExportPkg ep) {
    final int i = Util.binarySearch(providers, epComp, ep);
    if (i < 0) {
      providers.add(-i - 1, ep);
    }
  }


  /**
   * Get best provider. Best provider is provider
   * with highest version number.
   *
   * @return Provider ExportPkg or null if none..
   */
  synchronized ExportPkg getBestProvider() {
    if (!providers.isEmpty()) {
      return providers.get(0);
    } else {
      // See if there are any resolved exporters.
      for (final ExportPkg exportPkg : exporters) {
        final ExportPkg ep = exportPkg;
        if (ep.bpkgs.isActive()) {
          return ep;
        }
      }
      return null;
    }
  }


  /**
   * Check if this package has any exporters or importers.
   *
   * @return true if no exporters or importers, otherwise false.
   */
  synchronized boolean isEmpty() {
    return exporters.size() == 0 && importers.size() == 0;
  }


  @Override
  public String toString() {
    return toString(2);
  }


  public String toString(int level) {
    final StringBuilder sb = new StringBuilder();
    sb.append("Pkg[");

    if(level > 0) {
      sb.append("pkg=").append(pkg);
    }
    if(level > 1) {
      sb.append(", providers=").append(providers);
    }
    if(level > 2) {
      sb.append(", exporters=").append(exporters);
    }
    sb.append("]");

    return sb.toString();
  }


  static final Util.Comparator<ExportPkg, ExportPkg> epComp = new Util.Comparator<ExportPkg, ExportPkg>() {
    /**
     * Version compare two ExportPkg objects. If same version, order according
     * to bundle id, lowest first.
     *
     * @param a
     *          ExportPkg to compare.
     * @param b
     *          ExportPkg to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     */
    public int compare(ExportPkg a, ExportPkg b)
    {
      int d = a.version.compareTo(b.version);
      if (d == 0) {
        final long ld = b.bpkgs.bg.bundle.id - a.bpkgs.bg.bundle.id;
        if (ld < 0)
          d = -1;
        else if (ld > 0)
          d = 1;
      }
      return d;
    }
  };

  static final Util.Comparator<ImportPkg, ImportPkg> ipComp = new Util.Comparator<ImportPkg, ImportPkg>() {
    /**
     * Version compare two ImportPkg objects. If same version, order according
     * to bundle id, lowest first.
     *
     * @param a
     *          ImportPkg to compare.
     * @param b
     *          ImportPkg to compare.
     * @return Return 0 if equals, negative if first object is less than second
     *         object and positive if first object is larger then second object.
     * @exception ClassCastException
     *              if object is not a ImportPkg object.
     */
    public int compare(ImportPkg a, ImportPkg b)
    {
      int d;
      if (a.packageRange == null) {
        d = b.packageRange == null ? 0 : Version.emptyVersion.compareTo(b.packageRange.getLeft());
      } else if (b.packageRange == null) {
        d = a.packageRange.getLeft().compareTo(Version.emptyVersion);        
      } else {
        d = a.packageRange.getLeft().compareTo(b.packageRange.getLeft());
      }
      if (d == 0) {
        final long ld = b.bpkgs.bg.bundle.id - a.bpkgs.bg.bundle.id;
        if (ld < 0)
          d = -1;
        else if (ld > 0)
          d = 1;
      }
      return d;
    }
  };

}
