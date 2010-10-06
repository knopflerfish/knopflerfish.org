/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.io.*;

import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.*;

/**
 * Implementation of the BundleContext object.
 *
 * @see org.osgi.framework.BundleContext
 * @author Jan Stein
 * @author Philippe Laporte
 */
public class BundleContextImpl
  implements BundleContext
{

  /**
   * Reference to bundleImpl for this context.
   */
  final BundleImpl bundle;


  /**
   * Is bundle context valid.
   */
  private boolean valid = true;


  /**
   * Create a BundleContext for specified bundle.
   */
  public BundleContextImpl(BundleImpl bundle) {
    this.bundle = bundle;
  }

  //
  // BundleContext interface
  //

  /**
   * Retrieve the value of the named environment property.
   *
   * @see org.osgi.framework.BundleContext#getProperty
   */
  public String getProperty(String key) {
    checkValid();
    return bundle.fwCtx.props.getProperty(key);
  }


  /**
   * Install a bundle from location.
   *
   * @see org.osgi.framework.BundleContext#installBundle
   */
  public Bundle installBundle(String location) throws BundleException {
    checkValid();
    return bundle.fwCtx.bundles.install(location, null);
  }


  /**
   * Install a bundle from an InputStream.
   *
   * @see org.osgi.framework.BundleContext#installBundle
   */
  public Bundle installBundle(String location, InputStream in)
    throws BundleException
  {
    try {
      checkValid();
      return bundle.fwCtx.bundles.install(location, in);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignore) {}
      }
    }
  }


  /**
   * Retrieve the Bundle object for the calling bundle.
   *
   * @see org.osgi.framework.BundleContext#getBundle
   */
  public Bundle getBundle() {
    checkValid();
    return bundle;
  }


  /**
   * Retrieve the bundle that has the given unique identifier.
   *
   * @see org.osgi.framework.BundleContext#getBundle
   */
  public Bundle getBundle(long id) {
    return bundle.fwCtx.bundles.getBundle(id);
  }


  /**
   * Retrieve a list of all installed bundles.
   *
   * @see org.osgi.framework.BundleContext#getBundles
   */
  public Bundle[] getBundles() {
    List bl = bundle.fwCtx.bundles.getBundles();
    return (Bundle[])bl.toArray(new Bundle [bl.size()]);
  }


  /**
   * Add a service listener with a filter.
   *
   * @see org.osgi.framework.BundleContext#addServiceListener
   */
  public void addServiceListener(ServiceListener listener, String filter)
    throws InvalidSyntaxException {
    checkValid();
    bundle.fwCtx.listeners.addServiceListener(this, listener, filter);
  }


  /**
   * Add a service listener.
   *
   * @see org.osgi.framework.BundleContext#addServiceListener
   */
  public void addServiceListener(ServiceListener listener) {
    checkValid();
    try {
      bundle.fwCtx.listeners.addServiceListener(this, listener, null);
    } catch (InvalidSyntaxException neverHappens) { }
  }


  /**
   * Remove a service listener.
   *
   * @see org.osgi.framework.BundleContext#removeServiceListener
   */
  public void removeServiceListener(ServiceListener listener) {
    checkValid();
    bundle.fwCtx.listeners.removeServiceListener(this, listener);
  }


  /**
   * Add a bundle listener.
   *
   * @see org.osgi.framework.BundleContext#addBundleListener
   */
  public void addBundleListener(BundleListener listener) {
    checkValid();
    bundle.fwCtx.listeners.addBundleListener(this, listener);
  }


  /**
   * Remove a bundle listener.
   *
   * @see org.osgi.framework.BundleContext#removeBundleListener
   */
  public void removeBundleListener(BundleListener listener) {
    checkValid();
    bundle.fwCtx.listeners.removeBundleListener(this, listener);
  }


  /**
   * Add a framework listener.
   *
   * @see org.osgi.framework.BundleContext#addFrameworkListener
   */
  public void addFrameworkListener(FrameworkListener listener) {
    checkValid();
    bundle.fwCtx.listeners.addFrameworkListener(this, listener);
  }


  /**
   * Remove a framework listener.
   *
   * @see org.osgi.framework.BundleContext#removeFrameworkListener
   */
  public void removeFrameworkListener(FrameworkListener listener) {
    checkValid();
    bundle.fwCtx.listeners.removeFrameworkListener(this, listener);
  }


  /**
   * Register a service with multiple names.
   *
   * @see org.osgi.framework.BundleContext#registerService
   */
  public ServiceRegistration registerService(String[] clazzes,
                                             Object service,
                                             Dictionary properties) {
    checkValid();
    String [] classes = (String[]) clazzes.clone();
    return bundle.fwCtx.services.register(bundle, classes, service, properties);
  }


  /**
   * Register a service with a single name.
   *
   * @see org.osgi.framework.BundleContext#registerService
   */
  public ServiceRegistration registerService(String clazz,
                                             Object service,
                                             Dictionary properties) {
    checkValid();
    String [] classes =  new String [] { clazz };
    return bundle.fwCtx.services.register(bundle, classes, service, properties);
  }


  /**
   * Get a list of service references.
   *
   * @see org.osgi.framework.BundleContext#getServiceReferences
   */
  public ServiceReference[] getServiceReferences(String clazz, String filter)
    throws InvalidSyntaxException {
    checkValid();
    return bundle.fwCtx.services.get(clazz, filter, bundle);
  }

  /**
   * Get a list of service references.
   *
   * @see org.osgi.framework.BundleContext#getAllServiceReferences
   */
  public ServiceReference[] getAllServiceReferences(String clazz, String filter)
  throws InvalidSyntaxException {
    checkValid();
    return bundle.fwCtx.services.get(clazz, filter, null);
  }


  /**
   * Get a service reference.
   *
   * @see org.osgi.framework.BundleContext#getServiceReference
   */
  public ServiceReference getServiceReference(String clazz) {
    checkValid();
    return bundle.fwCtx.services.get(bundle, clazz);
  }


  /**
   * Get the service object.
   *
   * @see org.osgi.framework.BundleContext#getService
   */
  public Object getService(ServiceReference reference) {
    checkValid();

    if(reference == null) {
      // Throw an NPE with a message to be really clear we do it
      // intentionally.
      // A better solution would be to throw IllegalArgumentException,
      // but the OSGi ref impl throws NPE, and we want to keep as
      // close as possible
      throw new NullPointerException("null ServiceReference is not valid input to getService()");
    }

    return ((ServiceReferenceImpl)reference).getService(bundle);
  }


  /**
   * Unget the service object.
   *
   * @see org.osgi.framework.BundleContext#ungetService
   */
  public boolean ungetService(ServiceReference reference) {
    checkValid();

    if(reference == null) {
      // Throw an NPE with a message to be really clear we do it
      // intentionally.
      // A better solution would be to throw IllegalArgumentException,
      // but the OSGi ref impl throws NPE, and we want to keep as
      // close as possible
      throw new NullPointerException("null ServiceReference is not valid input to ungetService()");
    }

    return ((ServiceReferenceImpl)reference).ungetService(bundle, true);
  }


  /**
   * Creates a File object for a file in the persistent storage
   * area provided for the bundle.
   *
   * @see org.osgi.framework.BundleContext#getDataFile
   */
  public File getDataFile(String filename) {
    checkValid();
    File dataRoot = bundle.getDataRoot();
    if (dataRoot != null) {
      if (!dataRoot.exists()) {
        dataRoot.mkdirs();
      }
      return new File(dataRoot, filename);
    } else {
      return null;
    }
  }


  /**
   * Constructs a Filter object. This filter object may be used
   * to match a {@link ServiceReference} or a Dictionary.
   *
   * @param filter the filter string.
   * @return the Filter object encapsulating the filter string.
   * @exception InvalidSyntaxException If the filter parameter contains
   * an invalid filter string which cannot be parsed.
   *
   * @since 1.1
   */
  public Filter createFilter(String filter) throws InvalidSyntaxException {
    checkValid();
    return FrameworkUtil.createFilter(filter);
  }

  //
  // Package methods
  //

  /**
   * Invalidate this BundleContext.
   */
  void invalidate() {
    valid = false;
  }


  /**
   * Is bundle still valid.
   *
   * @return true if valid.
   * @exception IllegalStateException, if bundle isn't active.
   */
  boolean isValid() {
    return valid;
  }


  //
  // Private methods
  //

  /**
   * Check that the bundle is still valid.
   *
   * @return true if valid.
   * @exception IllegalStateException, if bundle isn't active.
   */
  private void checkValid() {
    if (!valid) {
      throw new IllegalStateException("This bundle context is no longer valid");
    }
  }

}
