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

package org.knopflerfish.framework;

import java.io.InputStream;
import java.util.List;

/**
 * Interface for managing all bundles jar content.
 *
 * @author Jan Stein
 */
public interface BundleStorage {

  /**
   * Insert bundle into persistent storagedata.
   *
   * @param key Name of attribute to get.
   * @return 
   */
  BundleArchive insertBundleJar(String location, InputStream is)
    throws Exception;

  /**
   * Insert a new jar file into persistent storagedata as an update
   * to an existing bundle archive. To commit this data a call to
   * <code>replaceBundleArchive</code> is needed.
   *
   * @param old BundleArchive to be replaced.
   * @param is Inputstrem with bundle content.
   * @return Bundle archive object.
   */
  BundleArchive updateBundleArchive(BundleArchive old, InputStream is)
    throws Exception;

  /**
   * Replace old bundle archive with a new updated bundle archive, that
   * was created with updateBundleArchive.
   *
   * @param oldBA BundleArchive to be replaced.
   * @param newBA Inputstrem with bundle content.
   * @return New bundle archive object.
   */
  void replaceBundleArchive(BundleArchive oldBA, BundleArchive newBA)
    throws Exception;

  /**
   * Get all bundle archive objects.
   *
   * @return Private copy of a List with bundle id's.
   */
  BundleArchive [] getAllBundleArchives();

  /**
   * Get all bundles tagged to start at next launch of framework.
   * This list is sorted in suggest start order.
   *
   * @return Private copy of a List with bundle id's.
   */
  List getStartOnLaunchBundles();
}
