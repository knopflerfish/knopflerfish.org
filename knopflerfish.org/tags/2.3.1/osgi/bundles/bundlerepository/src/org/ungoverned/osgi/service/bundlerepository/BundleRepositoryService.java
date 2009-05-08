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

import java.io.PrintStream;

/**
 * This interface defines a simple bundle repository service
 * for Oscar.
**/
public interface BundleRepositoryService
{
    /**
     * Get URL list of repositories.
     * @return a space separated list of URLs to use or <tt>null</tt>
     *         to refresh the cached list of bundles.
    **/
    public String[] getRepositoryURLs();

    /**
     * Set URL list of repositories.
     * @param urls a space separated list of URLs to use or <tt>null</tt>
     *        to refresh the cached list of bundles.
    **/
    public void setRepositoryURLs(String[] urls);

    /**
     * Get the number of bundles available in the repository.
     * @return the number of available bundles.
    **/
    public int getBundleRecordCount();

    /**
     * Get the specified bundle record from the repository.
     * @param i the bundle record index to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public BundleRecord getBundleRecord(int i);

    /**
     * Get bundle record for the bundle with the specified name
     * and version from the repository.
     * @param name the bundle record name to retrieve.
     * @param version three-interger array of the version associated with
     *        the name to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public BundleRecord getBundleRecord(String name, int[] version);

    /**
     * Get all versions of bundle records for the specified name
     * from the repository.
     * @param name the bundle record name to retrieve.
     * @return an array of all versions of bundle records having the
     *         specified name or <tt>null</tt>.
    **/
    public BundleRecord[] getBundleRecords(String name);

    /**
     * Deploys the bundle in the repository that corresponds to
     * the specified update location. The action taken depends on
     * whether the specified bundle is already installed in the local
     * framework. If the bundle is already installed, then this
     * method will attempt to update it. If the bundle is not already
     * installed, then this method will attempt to install it.
     * @param out the stream to use for informational messages.
     * @param err the stream to use for error messages.
     * @param updateLocation the update location of the bundle to deploy.
     * @param isResolve a flag to indicates whether dependencies should
     *        should be resolved.
     * @param isStart a flag to indicate whether installed bundles should
     *        be started.
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise.
    **/
    public boolean deployBundle(
        PrintStream out, PrintStream err, String updateLocation,
        boolean isResolve, boolean isStart);

    /**
     * Returns an array containing all bundle records in the
     * repository that resolve the transitive closure of the
     * passed in array of package declarations.
     * @param pkgs an array of package declarations to resolve.
     * @return an array containing all bundle records in the
     *         repository that resolve the transitive closure of
     *         the passed in array of package declarations.
     * @throws ResolveException if any packages in the transitive
     *         closure of packages cannot be resolved.
    **/
    public BundleRecord[] resolvePackages(PackageDeclaration[] pkgs)
        throws ResolveException;
}