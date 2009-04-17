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

import org.osgi.framework.*;


/**
 * Class representing a package.
 */
class Pkg {

  final String pkg;

  private PkgEntry provider = null;

  private boolean zombie = false;

  ArrayList /* PkgEntry */ exporters = new ArrayList(1);

  ArrayList /* PkgEntry */ importers = new ArrayList();


  /**
   * Create package object.
   */
  Pkg(String pkg) {
    this.pkg = pkg;
  }


  /**
   * Get provider.
   *
   * @return PkgEntry object for provider of this package.
   */
  PkgEntry getProvider() {
    return provider;
  }


  /**
   * Set provider for this package.
   *
   * @param Provider for this package.
   */
  synchronized void setProvider(PkgEntry p) {
    provider = p;
    zombie = false;
  }


  /**
   * Mark package as a zombie.
   *
   */
  synchronized void setZombie() {
    zombie = true;
  }


  /**
   * Check if a package is in zombie state.
   *
   * @return True if this package is a zombie exported.
   */
  synchronized boolean isZombie() {
    return zombie;
  }


  /**
   * Add an exporter entry from this package.
   *
   * @param pe PkgEntry to add.
   */
  synchronized void addExporter(PkgEntry pe) {
    int i = Math.abs(binarySearch(exporters, pe) + 1);
    exporters.add(i, pe);
    pe.setPkg(this);
  }


  /**
   * Remove an exporter entry from this package.
   *
   * @param p PkgEntry to remove.
   */
  synchronized boolean removeExporter(PkgEntry p) {
    if (p == provider) {
      return false;
    }
    for (int i = exporters.size() - 1; i >= 0; i--) {
      if (p == exporters.get(i)) {
	exporters.remove(i);
	p.setPkg(null);
	break;
      }
    }
    return true;
  }


  /**
   * Add an importer entry to this package.
   *
   * @param pe PkgEntry to add.
   */
  synchronized void addImporter(PkgEntry pe) {
    int i = Math.abs(binarySearch(importers, pe) + 1);
    importers.add(i, pe);
    pe.setPkg(this);
  }


  /**
   * Remove an importer entry from this package.
   *
   * @param p PkgEntry to remove.
   */
  synchronized void removeImporter(PkgEntry p) {
    for (int i = importers.size() - 1; i >= 0; i--) {
      if (p == importers.get(i)) {
	importers.remove(i);
	p.setPkg(null);
	break;
      }
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

  //
  // Private methods.
  //

  /**
   * Do binary search for a package entry in the list with the same
   * version number add the specifies package entry.
   *
   * @param pl Sorted list of package entries to search.
   * @param p Package entry to search for.
   * @return index of the found entry. If no entry is found, return
   *         <tt>(-(<i>insertion point</i>) - 1)</tt>.  The insertion
   *         point</i> is defined as the point at which the
   *         key would be inserted into the list.
   */
  int binarySearch(List pl, PkgEntry p) {
    int l = 0;
    int u = pl.size()-1;

    while (l <= u) {
      int m = (l + u)/2;
      int v = ((PkgEntry)pl.get(m)).compareVersion(p);
      if (v > 0) {
	l = m + 1;
      } else if (v < 0) {
	u = m - 1;
      } else {
	return m;
      }
    }
    return -(l + 1);  // key not found.
  }

  public String toString() {
    return toString(2);
  }

  public String toString(int level) {
    StringBuffer sb = new StringBuffer();
    sb.append("Pkg[");

    if(level > 0) {
      sb.append("pkg=" + pkg);
    }
    if(level > 1) {
      sb.append(", provider=" + provider);
    }
    if(level > 2) {
      sb.append(", zombie=" + zombie);
    } 
    if(level > 3) {
      sb.append(", exporters=" + exporters);
    }
    sb.append("]");

    return sb.toString();
  }
}
