/*
 * Copyright (c) 2006-2010, KNOPFLERFISH project
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

import org.osgi.framework.*;


class RequireBundle {

  final BundlePackages requestor;
  final String name;
  final String visibility;
  final String resolution;
  final VersionRange bundleRange;
  BundlePackages bpkgs = null;

  /**
   * A re-required bundle for fragment hosts.
   * @param parent    The fragment require bundle object to re-require.
   * @param requestor The bundle packages of the fragment host that
   *                  re-requires a required bundle from one of its
   *                  fragments.
   */
  RequireBundle(RequireBundle  parent, BundlePackages requestor)
  {
    this.requestor  = requestor;
    this.name       = parent.name;
    this.visibility = parent.visibility;
    this.resolution = parent.resolution;
    this.bundleRange= parent.bundleRange;
  }

  RequireBundle(BundlePackages requestor,
                String name,
                String visibility,
                String resolution,
                String range)
  {
    this.requestor = requestor;
    this.name = name;
    if (visibility != null) {
      this.visibility = visibility.intern();
      if (this.visibility!=Constants.VISIBILITY_PRIVATE &&
          this.visibility!=Constants.VISIBILITY_REEXPORT ) {
        throw new IllegalArgumentException
          ("Invalid directive : '"
           +Constants.VISIBILITY_DIRECTIVE +":="+this.visibility
           +"' in manifest header '"
           +Constants.REQUIRE_BUNDLE +": " +this.name
           +"' of bundle with id " +this.requestor.bg.bundle.getBundleId()
           +" ("+this.requestor.bg.symbolicName+")"
           +". The value must be either '"
           +Constants.VISIBILITY_PRIVATE  +"' or '"
           +Constants.VISIBILITY_REEXPORT +"'.");
      }
    } else {
      this.visibility = Constants.VISIBILITY_PRIVATE;
    }
    if (resolution != null) {
      this.resolution = resolution.intern();
      if (this.resolution!=Constants.RESOLUTION_MANDATORY &&
          this.resolution!=Constants.RESOLUTION_OPTIONAL ) {
        throw new IllegalArgumentException
          ("Invalid directive : '"
           +Constants.RESOLUTION_DIRECTIVE +":="+this.resolution
           +"' in manifest header '"
           +Constants.REQUIRE_BUNDLE +": " +this.name
           +"' of bundle with id " +this.requestor.bg.bundle.getBundleId()
           +" ("+this.requestor.bg.symbolicName+")"
           +". The value must be either '"
           +Constants.RESOLUTION_MANDATORY +"' or '"
           +Constants.RESOLUTION_OPTIONAL  +"'.");
      }
    } else {
      this.resolution = Constants.RESOLUTION_MANDATORY;
    }
    if (range != null) {
      this.bundleRange = new VersionRange(range);
    } else {
      this.bundleRange = VersionRange.defaultVersionRange;
    }
  }


  /**
   * Check if this object completly overlap specified RequireBundle.
   *
   * @return True if we overlap, otherwise false.
   */
  boolean overlap(RequireBundle rb) {
    if (visibility.equals(Constants.VISIBILITY_REEXPORT) &&
        !rb.visibility.equals(Constants.VISIBILITY_REEXPORT)) {
      return false;
    }
    if (resolution.equals(Constants.RESOLUTION_MANDATORY) &&
        !rb.resolution.equals(Constants.RESOLUTION_MANDATORY)) {
      return false;
    }
    return bundleRange.withinRange(rb.bundleRange);
  }

}
