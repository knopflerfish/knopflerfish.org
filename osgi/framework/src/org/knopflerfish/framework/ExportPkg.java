/*
 * Copyright (c) 2005, KNOPFLERFISH project
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
 * Data structure for export package definitions.
 *
 * @author Jan Stein
 */
class ExportPkg {
  final String name;
  final BundleImpl bundle;
  final String [] uses;
  final String [] mandatory;
  final String [] include;
  final String [] exclude;
  final Version version;
  final Map attributes;
  boolean zombie = false;

  // Link to pkg entry
  Pkg pkg = null;

  /**
   * Create an export package entry.
   */
  ExportPkg(Map tokens, BundleImpl b) {
    this.bundle = b;
    this.name = (String)tokens.remove("key");
    this.uses = Util.parseEnumeration(Constants.USES_DIRECTIVE,
				      (String)tokens.remove(Constants.USES_DIRECTIVE));
    this.mandatory = Util.parseEnumeration(Constants.MANDATORY_DIRECTIVE,
					   (String)tokens.remove(Constants.MANDATORY_DIRECTIVE));
    this.include = Util.parseEnumeration(Constants.INCLUDE_DIRECTIVE,
					   (String)tokens.remove(Constants.INCLUDE_DIRECTIVE));
    this.exclude = Util.parseEnumeration(Constants.EXCLUDE_DIRECTIVE,
					   (String)tokens.remove(Constants.EXCLUDE_DIRECTIVE));
    String versionStr = (String)tokens.remove(Constants.VERSION_ATTRIBUTE);
    String specVersionStr = (String)tokens.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
    if (specVersionStr != null) {
      this.version = new Version(specVersionStr);
    } else if (versionStr != null) {
      this.version = new Version(versionStr);
    } else {
      this.version = Version.emptyVersion;
    }
    this.attributes = tokens;
  }


  /**
   * Version compare object to another ExportPkg.
   *
   * @param obj Version to compare to.
   * @return Return 0 if equals, negative if this object is less than obj
   *         and positive if this object is larger then obj.
   * @exception ClassCastException if object is not a ExportPkg object.
   */
  public int compareVersion(Object obj) throws ClassCastException {
    ExportPkg o = (ExportPkg)obj;
    return version.compareTo(o.version);
  }


  /**
   * String describing package name and specification version, if specified.
   *
   * @return String.
   */
  public String pkgString() {
    if (version != Version.emptyVersion) {
      return name + ";" + Constants.PACKAGE_SPECIFICATION_VERSION + "=" + version;
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
    return pkgString() + "(" + bundle + ")";
  }


  /**
   * Hash code for this package entry.
   *
   * @return int value.
   */
  public int hashCode() {
    // TBD Do we need this?
    return name.hashCode() + version.hashCode();
  }

}
