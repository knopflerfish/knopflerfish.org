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

package org.knopflerfish.service.desktop;

/**
 * Model describing which bundles are selected.
 *
 * <p>
 * @see <a href="SwingBundleDisplayer.html">SwingBundleDisplayer</a>
 * </p>
 */
public interface BundleSelectionModel {
  /**
   * Clear the entire selection and notify all listeners.
   */
  public void    clearSelection();

  /**
   * Get the number of selected bundles.
   *
   * @return Number of selected bundles.
   */
  public int getSelectionCount();

  /**
   * Get selected bundle. Return the bundle id of one of the selected
   * bundles.
   *
   * @return The bundle ID of one of the selected bundles
   * (arbitrary). If no selection this method returns -1.
   */
  public long getSelected();

  /**
   * Get selection status of a bundle Id.
   *
   * @param bid Bundle Id to check.
   * @return <tt>true</tt> if <tt>bid</tt> is selected by the
   *         model, <tt>false</tt> otherwise.
   */
  public boolean isSelected(long bid);

  /**
   * Set selection status of a bundle id and notify all listeners.
   *
   * @param bid Bundle Id to set selection status.
   * @param bSelected new selection status of the bundle.
   */
  public void    setSelected(long bid, boolean bSelected);

  /**
   * Add a <tt>BundleSelectionListener</tt> to the set of listeners
   * that gets notified at bundle selection changes.
   *
   * @param l listener to add.
   */
  public void    addBundleSelectionListener(BundleSelectionListener l);

  /**
   * Remove a <tt>BundleSelectionListener</tt> from the set of listeners
   * that gets notified at bundle selection changes.
   *
   * @param l listener to remove.
   */
  public void    removeBundleSelectionListener(BundleSelectionListener l);
}
