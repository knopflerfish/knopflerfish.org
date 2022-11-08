/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of BundleSelectionModel.
 *
 */
public class DefaultBundleSelectionModel implements BundleSelectionModel {

  private final Set<Long> selection = new HashSet<>();
  private final Set<BundleSelectionListener> listeners = new HashSet<>();

  public DefaultBundleSelectionModel() {

  }

  public void clearSelection() {
    selection.clear();
    fireChange(-1);
  }

  public int getSelectionCount()
  {
    return selection.size();
  }

  public long getSelected()
  {
    return selection.size()>0
      ? selection.iterator().next()
      : -1;
  }

  public boolean isSelected(long bid) {
    return selection.contains(bid);
  }

  public void    setSelected(long bid, boolean bSelected) {
    if(bSelected) {
      selection.add(bid);
    } else {
      selection.remove(bid);
    }
    fireChange(bid);
  }

  public void setSelected(List<Long> bids, boolean bSelected)
  {
    if (null==bids || 0==bids.size()) {
      // Nothing to do!
      return;
    }

    if(bSelected) {
      selection.addAll(bids);
    } else {
      selection.removeAll(bids);
    }
    fireChange(bids.iterator().next());
  }


  public void    addBundleSelectionListener(BundleSelectionListener l) {
    synchronized(listeners) {
      listeners.add(l);
    }
  }

  public void    removeBundleSelectionListener(BundleSelectionListener l) {
    synchronized(listeners) {
      listeners.remove(l);
    }
  }

  /**
   * Send a value changed event to all <tt>BundleSelectionListener</tt>
   */
  boolean bInFireChange = false;
  public void fireChange(long bid) {
    synchronized(listeners) {
      try {
        if(!bInFireChange) {
          bInFireChange = true;
          for (BundleSelectionListener l : listeners) {
            l.valueChanged(bid);
          }
        }
      } finally {
        bInFireChange = false;
      }
    }
  }
}
