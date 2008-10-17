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

/**
 * This is a simple class to encapsulate a package declaration for
 * bundle imports and exports for the bundle repository.
**/
public class PackageDeclaration
{
    public static final String PACKAGE_ATTR = "package";
    public static final String VERSION_ATTR = "specification-version";

    private String m_name = null;
    private int[] m_version = null;

    /**
     * Construct a package declaration.
     * @param name the name of the package.
     * @param versionString the package version as a string.
    **/
    public PackageDeclaration(String name, String versionString)
    {
        m_name = name;
        m_version = parseVersionString((versionString == null) ? "" : versionString);
    }

    /**
     * Construct a package declaration.
     * @param name the name of the package.
     * @param version the package version as an integer triplet.
    **/
    public PackageDeclaration(String name, int[] version)
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
        return m_version[0] + "." + m_version[1] + "." + m_version[2];
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
        if (!getName().equals(pkg.getName()))
        {
            throw new IllegalArgumentException(
                "Cannot compare versions on different packages");
        }

        if (m_version[0] > pkg.m_version[0])
            return 1;
        else if (m_version[0] < pkg.m_version[0])
            return -1;
        else if (m_version[1] > pkg.m_version[1])
            return 1;
        else if (m_version[1] < pkg.m_version[1])
            return -1;
        else if (m_version[2] > pkg.m_version[2])
            return 1;
        else if (m_version[2] < pkg.m_version[2])
            return -1;

        return 0;
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
        if (!getName().equals(pkg.getName()))
        {
            return false;
        }

        return (compareVersion(pkg) >= 0) ? true : false;
    }

    /**
     * Gets the string representation of the package declaration.
     * @return the string representation of the package declaration.
    **/
    public String toString()
    {
        return m_name + "; specification-version=" + getVersion();
    }

    private int[] parseVersionString(String versionString)
    {
        StringTokenizer st = new StringTokenizer(versionString, ".");
        int[] version = new int[3];
        version[0] = 0;
        version[1] = 0;
        version[2] = 0;
        if (st.countTokens() > 0)
        {
            try
            {
                version[0] = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens())
                {
                    version[1] = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens())
                    {
                        version[2] = Integer.parseInt(st.nextToken());
                    }
                }
            }
            catch (NumberFormatException ex)
            {
                throw new IllegalArgumentException(
                    "Improper version number: " + versionString);
            }
        }

        return version;
    }
}