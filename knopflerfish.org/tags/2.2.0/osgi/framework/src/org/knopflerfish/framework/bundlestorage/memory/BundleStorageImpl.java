/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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

package org.knopflerfish.framework.bundlestorage.memory;

import org.knopflerfish.framework.*;
import java.io.*;
import java.util.*;

/**
 * Storage of all bundles jar content.
 *
 * @author Jan Stein
 * @version $Revision: 1.1.1.1 $
 */
public class BundleStorageImpl implements BundleStorage {

  /**
   * Next available bundle id.
   */
  private long nextFreeId = 1;

  /**
   * Bundle id sorted list of all active bundle archives.
   */
  private ArrayList /* BundleArchive */ archives = new ArrayList();

  /**
   * Create a container for all bundle data in this framework.
   * Try to restore all saved bundle archive state.
   *
   */
  public BundleStorageImpl() {
  }

  /**
   * Insert bundle into persistent storage
   *
   * @param location Location of bundle.
   * @param is Inputstrem with bundle content.
   * @return Bundle archive object.
   */
  public BundleArchive insertBundleJar(String location, InputStream is)
    throws Exception
  {
    long id = nextFreeId++;
    BundleArchive ba = new BundleArchiveImpl(this, is, location, id);
    archives.add(ba);
    return ba;
  }


  /**
   * Insert a new jar file into persistent storagedata as an update
   * to an existing bundle archive. To commit this data a call to
   * <code>replaceBundleArchive</code> is needed.
   *
   * @param old BundleArchive to be replaced.
   * @param is Inputstrem with bundle content.
   * @return Bundle archive object.
   */
  public BundleArchive updateBundleArchive(BundleArchive old, InputStream is)
    throws Exception
  {
    return new BundleArchiveImpl((BundleArchiveImpl)old, is);
  }


  /**
   * Replace old bundle archive with a new updated bundle archive, that
   * was created with updateBundleArchive.
   *
   * @param oldBA BundleArchive to be replaced.
   * @param newBA Inputstrem with bundle content.
   * @return New bundle archive object.
   */
  public void replaceBundleArchive(BundleArchive oldBA, BundleArchive newBA)
    throws Exception
  {
    int pos;
    long id = oldBA.getBundleId();
    synchronized (archives) {
      pos = find(id);
      if (pos >= archives.size() || archives.get(pos) != oldBA) {
        throw new Exception("replaceBundleJar: Old bundle archive not found, pos=" + pos);
      }
      archives.set(pos, newBA);
    }
  }


  /**
   * Get all bundle archive objects.
   *
   * @return Private array of all BundleArchives.
   */
  public BundleArchive [] getAllBundleArchives() {
    synchronized (archives) {
      return (BundleArchive [])archives.toArray(new BundleArchive[archives.size()]);
    }
  }


  /**
   * Get all bundles tagged to start at next launch of framework.
   * This list is sorted in increasing bundle id order.
   *
   * @return Private copy of a List with bundle id's.
   */
  public List getStartOnLaunchBundles() {
    ArrayList res = new ArrayList();
    for (Iterator i = archives.iterator(); i.hasNext(); ) {
      BundleArchive ba = (BundleArchive)i.next();
      if (ba.getStartOnLaunchFlag()) {
        res.add(ba.getBundleLocation());
      }
    }
    return res;
  }

  //
  // Package methods
  //

  /**
   * Remove bundle archive from archives list.
   *
   * @param id Bundle archive id to find.
   * @return true if element was removed.
   */
  boolean removeArchive(BundleArchive ba) {
    synchronized (archives) {
      int pos = find(ba.getBundleId());
      if (archives.get(pos) == ba) {
        archives.remove(pos);
        return true;
      } else {
        return false;
      }
    }
  }


  //
  // Private methods
  //

  /**
   * Find posisition for BundleArchive with specified id
   *
   * @param id Bundle archive id to find.
   * @return String to write
   */
  private int find(long id) {
    int lb = 0;
    int ub = archives.size();
    int x = 0;
    while (lb != ub) {
      x = (lb + ub) / 2;
      long xid = ((BundleArchive)archives.get(x)).getBundleId();
      if (id <= xid) {
        ub = x;
      } else {
        lb = x+1;
      }
    }
    return lb;
  }

}
