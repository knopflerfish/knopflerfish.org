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

import java.security.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;

/**
 * Framework service which allows bundle programmers to inspect the packages
 * exported in the framework and eagerly update or uninstall bundles.
 *
 * If present, there will only be a single instance of this service
 * registered in the framework.
 * 
 * <p> The term <i>exported package</i> (and the corresponding interface
 * {@link ExportedPackage}) refers to a package that has actually been
 * exported (as opposed to one that is available for export).
 *
 * <p> Note that the information about exported packages returned by this
 * service is valid only until the next time {@link #refreshPackages} is
 * called.
 * If an ExportedPackage becomes stale, (that is, the package it references
 * has been updated or removed as a result of calling 
 * PackageAdmin.refreshPackages()),
 * its getName() and getSpecificationVersion() continue to return their
 * old values, isRemovalPending() returns true, and getExportingBundle()
 * and getImportingBundles() return null.
 *
 * @see org.osgi.service.packageadmin.PackageAdmin
 * @author Jan Stein
 * @author Erik Wistrand
 * @author Robert Shelley
 */
public class PackageAdminImpl implements PackageAdmin {

  final static String SPEC_VERSION = "1.1";

  private Framework framework;

  PackageAdminImpl(Framework fw) {
    framework = fw;
  }

  /**
   * Gets the packages exported by the specified bundle.
   *
   * @param bundle The bundle whose exported packages are to be returned,
   *               or <tt>null</tt> if all the packages currently
   *               exported in the framework are to be returned.  If the
   *               specified bundle is the system bundle (that is, the
   *               bundle with id 0), this method returns all the packages
   *               on the system classpath whose name does not start with
   *               "java.".  In an environment where the exhaustive list
   *               of packages on the system classpath is not known in
   *               advance, this method will return all currently known
   *               packages on the system classpath, that is, all packages
   *               on the system classpath that contains one or more classes
   *               that have been loaded.
   *
   * @return The array of packages exported by the specified bundle,
   * or <tt>null</tt> if the specified bundle has not exported any packages.
   */
  public ExportedPackage[] getExportedPackages(Bundle bundle) {
    Collection pkgs = framework.packages.getPackagesProvidedBy(bundle);
    int size = pkgs.size();
    if (size > 0) {
      ExportedPackage[] res = new ExportedPackage[size];
      Iterator i = pkgs.iterator();
      for (int pos = 0; pos < size;) {
	res[pos++] = new ExportedPackageImpl((PkgEntry)i.next());
      }
      return res;
    } else {
      return null;
    }
  }

  
  /**
   * Gets the ExportedPackage with the specified package name.  All exported 
   * packages 
   * will be checked for the specified name.  In an environment where the
   * exhaustive list of packages on the system classpath is not known in
   * advance, this method attempts to see if the named package is on the
   * system classpath.
   * This
   * means that this method may discover an ExportedPackage that was
   * not present in the list returned by <tt>getExportedPackages()</tt>.
   *
   * @param name The name of the exported package to be returned.
   *
   * @return The exported package with the specified name, or <tt>null</tt>
   *         if no expored package with that name exists.
   */
  public ExportedPackage getExportedPackage(String name) {
    PkgEntry provider = framework.packages.getProvider(name);
    if (provider != null) {
      return new ExportedPackageImpl(provider);
    } else {
      return null;
    }
  }
  

  /**
   * Forces the update (replacement) or removal of packages exported by
   * the specified bundles.
   *
   * <p> If no bundles are specified, this method will update or remove any
   * packages exported by any bundles that were previously updated or
   * uninstalled. The technique by which this is accomplished
   * may vary among different framework implementations. One permissible
   * implementation is to stop and restart the Framework.
   *
   * <p> This method returns to the caller immediately and then performs the
   * following steps in its own thread:
   *
   * <ol>
   * <p>
   * <li> Compute a graph of bundles starting with the specified ones. If no
   * bundles are specified, compute a graph of bundles starting with
   * previously updated or uninstalled ones.
   * Any bundle that imports a package that is currently exported
   * by a bundle in the graph is added to the graph. The graph is fully
   * constructed when there is no bundle outside the graph that imports a
   * package from a bundle in the graph. The graph may contain
   * <tt>UNINSTALLED</tt> bundles that are currently still
   * exporting packages.
   *
   * <p>
   * <li> Each bundle in the graph will be stopped as described in the 
   * <tt>Bundle.stop</tt> method.
   *
   * <p>
   * <li> Each bundle in the graph that is in the
   * <tt>RESOLVED</tt> state is moved
   * to the <tt>INSTALLED</tt> state.
   * The effect of this step is that bundles in the graph are no longer
   * <tt>RESOLVED</tt>.
   *
   * <p>
   * <li> Each bundle in the graph that is in the UNINSTALLED state is
   * removed from the graph and is now completely removed from the framework.
   *
   * <p>
   * <li> Each bundle in the graph that was in the
   * <tt>ACTIVE</tt> state prior to Step 2 is started as
   * described in the <tt>Bundle.start</tt> method, causing all
   * bundles required for the restart to be resolved.
   * It is possible that, as a
   * result of the previous steps, packages that were
   * previously exported no longer are. Therefore, some bundles
   * may be unresolvable until another bundle
   * offering a compatible package for export has been installed in the
   * framework.
   * </ol>
   *
   * <p> For any exceptions that are thrown during any of these steps, a
   * <tt>FrameworkEvent</tt> of type <tt>ERROR</tt> is
   * broadcast, containing the exception. 
   *
   * @param bundles the bundles whose exported packages are to be updated or 
   * removed,
   * or <tt>null</tt> for all previously updated or uninstalled bundles.
   *
   * @exception SecurityException if the caller does not have the
   * <tt>AdminPermission</tt> and the Java runtime environment supports
   * permissions.
   */
  public void refreshPackages(final Bundle[] bundles) {
    framework.checkAdminPermission();
    Thread t = new Thread() {
	public void run() {
	  synchronized (framework.packages) {
	    final BundleImpl bi[] = (BundleImpl[])framework.packages
	      .getZombieAffected(bundles).toArray(new BundleImpl[0]);
	    AccessController.doPrivileged(new PrivilegedAction() {
		public Object run() {
		ArrayList startList = new ArrayList();

		  // Stop affected bundles and remove their classloaders
		  // in reverse start order
		  for (int bx = bi.length; bx-- > 0; ) {
		    synchronized (bi[bx]) {
		      if ((bi[bx].state & (Bundle.STARTING|Bundle.ACTIVE)) != 0) {
			try {
			  int ix = 0;
			  if(Framework.R3_TESTCOMPLIANT) {
			    // Make sure start list is in original bundle
			    // start order by using insertion sort
			    Iterator it = startList.iterator();
			    while(it.hasNext()) {
			      BundleImpl sbi = (BundleImpl)it.next();
			      if(bi[bx].getBundleId() < sbi.getBundleId()) {
				break;
			      }
			      ix++;
			    }
			  }
			  startList.add(ix,bi[bx]);
			  bi[bx].stop();
			} catch(BundleException be) {
			  framework.listeners.frameworkError(bi[bx], be);
			}
		      }
		    }
		  }

		  // Update the affected bundle states in normal start order
		  for (int bx = 0; bx < bi.length; bx++) {
		    synchronized (bi[bx]) {
		      switch (bi[bx].state) {
		      case Bundle.STARTING:
		      case Bundle.ACTIVE:
			try {
			  bi[bx].stop();
			} catch(BundleException be) {
			  framework.listeners.frameworkError(bi[bx], be);
			}
		      case Bundle.STOPPING:
		      case Bundle.RESOLVED:
			bi[bx].setStateInstalled();
		      case Bundle.INSTALLED:
		      case Bundle.UNINSTALLED:
			break;
		      }
		      bi[bx].purge();
		    }
		  }

		  // Restart previously active bundles in normal start order
		  framework.bundles.startBundles(startList);
		  framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, this));
		  return null;
		}
	      }
	      );
	  }
	}
      };
    t.setDaemon(false);
    t.start();
  }

  Util.Comparator BSComparator = new Util.Comparator() {
      public int compare(Object o1, Object o2) {
	BundleImpl b1 = (BundleImpl)o1;
	BundleImpl b2 = (BundleImpl)o2;
	
	return (int)(b1.getBundleId() - b2.getBundleId());
      }
    };
}
