/*
 * Oscar Bundle Repository
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
 **/
package org.ungoverned.osgi.service.bundlerepository;

import java.util.StringTokenizer;
import org.osgi.framework.Version;
import org.knopflerfish.osgi.bundle.bundlerepository.VersionRange;


/**
 * This is a simple class to encapsulate a package declaration for
 * bundle imports and exports for the bundle repository.
 **/
public class PackageDeclaration
{
  public static final String PACKAGE_ATTR = "package";
  public static final String VERSION_ATTR = "version";
  public static final String SPEC_VERSION_ATTR = "specification-version";

  private String m_name = null;
  // Exported packages uses a version
  private Version m_version = null;
  // Imported packages uses a version range
  private VersionRange m_versionRange = null;

  /**
   * Construct a package declaration.
   * @param name the name of the package.
   * @param versionString the package version as a string.
   * @param range if true then the version string is a version range.
   **/
  public PackageDeclaration(String name, String versionString, boolean range)
  {
    m_name = name;
    if (range) {
      m_versionRange = (null==versionString || 0==versionString.length())
        ? VersionRange.defaultVersionRange
        : new VersionRange(versionString);
    } else {
      m_version = Version.parseVersion(versionString);
    }
  }

  /**
   * Construct a package declaration.
   * @param name the name of the package.
   * @param version the package version as an integer triplet.
   **/
  public PackageDeclaration(String name, Version version)
  {
    m_name = name;
    m_version = version;
  }

  /**
   * Construct a copy of a package declaration.
   * @param pkg the package declaration to copy.
   **/
  public PackageDeclaration(PackageDeclaration pkg)
  {
    m_name = pkg.m_name;
    m_version = pkg.m_version;
  }

  /**
   * Gets the name of the package.
   * @return the package name.
   **/
  public String getName()
  {
    return m_name;
  }

  /**
   * Gets the version of the package represented as a string.
   * @return the string representation of the package version.
   **/
  public String getVersion()
  {
    return m_version!=null? m_version.toString() : m_versionRange.toString();
  }

  /**
   * Compares two package declarations.
   * @param pkg the package declaration used for comparison.
   * @return greater than <tt>0</tt> if the supplied package version
   *         is less, less than <tt>0</tt> if the supplied package is
   *         is greater, and <tt>0</tt> if the two versions are equal.
   * @throws IllegalArgumentException if the package declarations are
   *         not for the same package.
   **/
  public int compareVersion(PackageDeclaration pkg)
  {
    if (!getName().equals(pkg.getName())) {
      throw new IllegalArgumentException
        ("Cannot compare versions on different packages");
    }
    return m_version.compareTo(pkg.m_version);
  }

  /**
   * Determines if the current package declaration satisfies
   * the supplied package declaration.
   * @param pkg the package to be checked.
   * @return <tt>true</tt> if the current package satisfies the
   *         supplied package, <tt>false</tt> otherwise.
   **/
  public boolean doesSatisfy(PackageDeclaration pkg)
  {
    if (!getName().equals(pkg.getName())) {
      return false;
    }

    if (null!=m_versionRange && null==pkg.m_versionRange) {
      return m_versionRange.withinRange(pkg.m_version);
    } else if (null!=m_version && null!=pkg.m_versionRange) {
      return pkg.m_versionRange.withinRange(m_version);
    }
    return false;
  }

  /**
   * Gets the string representation of the package declaration.
   * @return the string representation of the package declaration.
   **/
  public String toString()
  {
    return m_name + "; specification-version=" + getVersion();
  }

}