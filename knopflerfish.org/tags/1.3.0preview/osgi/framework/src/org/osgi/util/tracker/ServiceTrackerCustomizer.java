/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.osgi.util.tracker;

import org.osgi.framework.ServiceReference;

/**
 * Interface allowing users of <tt>ServiceTracker</tt> to modify the behavior
 * and selection of tracked services.
 *
 * <p><b>Note:</b>
 * <tt>ServiceTracker</tt> itself implements 
 * <tt>ServiceTrackerCustomizer</tt>, and in many cases it might be easier
 * to override the one of the <tt><i>xxxxx</i>Service</tt> methods 
 * directly in <tt>ServiceTracker</tt>
 *
 * <p><b>Note II:</b>
 * If you <b>really</b> need to monitor the lifecycle of one ore 
 * more services (not just use it) it's higly recommended to use 
 * the <tt>ServiceListener</tt> interface
 * directly.
 */
public interface ServiceTrackerCustomizer {

  /**
   * A service is being added to the <tt>ServiceTracker</tt> object.
   *
   * <p>This method is called before a service which matched
   * the search parameters of the <tt>ServiceTracker</tt> object is
   * added to it. This method should return the
   * service object to be tracked for this <tt>ServiceReference</tt> object,
   * typically by calling <tt>context.getServiceReference(reference)</tt>
   * 
   *
   * @param reference Reference to service being added
   * @return The service object to be tracked for the
   *         <tt>ServiceReference</tt> or <tt>null</tt> if the 
   *         <tt>ServiceReference</tt> should not be tracked.
   */
  public Object addingService(ServiceReference reference);
  
  /**
   * A service tracked by the <tt>ServiceTracker</tt> object has been modified.
   *
   * <p>This method is called when a service being tracked
   * by the <tt>ServiceTracker</tt> object has had it properties modified.
   *
   * @param reference Service that has been modified.
   * @param service The service for the modified service.
   */
  public void modifiedService(ServiceReference reference, Object service);
  
  /**
   * A service tracked by the <tt>ServiceTracker</tt> object has been removed.
   *
   * <p>This method is called after a service is no longer being tracked
   * by the <tt>ServiceTracker</tt> object. The action should be the inverse
   * of the action in <tt>addingService</tt>, typically by calling
   * <tt>context.ungetService(reference)</tt>
   *
   * @param reference Service that has been removed.
   * @param service service that has been removed.
   */
  public void removedService(ServiceReference reference, Object service);
}
