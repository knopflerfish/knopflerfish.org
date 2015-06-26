/*
 * Copyright (c) 2015-2015, KNOPFLERFISH project
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

package org.knopflerfish.framework.bundlestorage.dex;

import org.knopflerfish.framework.FWProps;
import org.knopflerfish.framework.FileTree;
import org.knopflerfish.framework.FrameworkContext;
import org.knopflerfish.framework.bundlestorage.file.Archive;
import org.knopflerfish.framework.bundlestorage.file.BundleArchiveImpl;
import org.osgi.framework.Constants;


/**
 * Storage of all bundles jar content.
 *
 */
public class BundleStorageImpl extends org.knopflerfish.framework.bundlestorage.file.BundleStorageImpl {

  private final static String ALWAYS_UNPACK_PROP =
    "org.knopflerfish.framework.bundlestorage.dex.always_unpack";
  private final static String REFERENCE_PROP =
    "org.knopflerfish.framework.bundlestorage.dex.reference";
  private final static String TRUSTED_PROP =
    "org.knopflerfish.framework.bundlestorage.dex.trusted";

  public BundleStorageImpl(FrameworkContext framework) {
    super(framework);
  }

  protected Archive createArchive(BundleArchiveImpl ba, FileTree dir, int rev) {
    return new DexArchive(ba, dir, rev);
  }

  /**
   * Initialize values for properties.
   *
   */
  protected void initProps(FWProps props) {
    props.setPropertyDefault(ALWAYS_UNPACK_PROP, FWProps.FALSE);
    props.setPropertyDefault(REFERENCE_PROP, FWProps.FALSE);
    props.setPropertyDefault(TRUSTED_PROP, FWProps.TRUE);
    alwaysUnpack = props.getBooleanProperty(ALWAYS_UNPACK_PROP);
    fileReference = props.getBooleanProperty(REFERENCE_PROP);
    trustedStorage = props.getBooleanProperty(TRUSTED_PROP);
    unpack = true;
    execPermCmd = props.getProperty(Constants.FRAMEWORK_EXECPERMISSION).trim();
    checkSigned = props.getBooleanProperty(FWProps.BUNDLESTORAGE_CHECKSIGNED_PROP);
    isWindows = false;
    jarVerifierBug = false;
    readOnly = props.getBooleanProperty(FWProps.READ_ONLY_PROP);
   }

}
