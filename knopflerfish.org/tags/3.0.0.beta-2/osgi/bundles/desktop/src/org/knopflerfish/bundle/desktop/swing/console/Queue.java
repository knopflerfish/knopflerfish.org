/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing.console;

import java.util.Vector;

//  ********************     Queue     ********************
/**
 ** The <code>Queue</code> class represents a first-in-first-out 
 ** (FIFO) queue of objects. 
 ** @author Per Lundgren
 ** @version $Revision: 1.2 $
 */
public class Queue extends Vector {
/** @serial */
  private int m_nMaxSize = -1;
/** @serial */
  private boolean queueClosed = false;
  
  //  ====================    Queue      ====================
  /**
   ** Constructs an Queue with the specifies maximum size.
   **
   ** @param	size	maximum queue size.
   */
  public Queue(int size)
  {
    m_nMaxSize = size;
  }
 
  //  ====================    insert      ====================
  /**
   ** Inserts an item into the queue. If there are threads blocked on
   ** <code>remove</code>, one of them is unblocked.
   **
   ** @param	item	the item to be inserted.
   ** @exception IndexOutOfBoundsException if maximum queue size is reached.
   */
  public synchronized void insert(Object item) throws IndexOutOfBoundsException
  {
    // Check if queue is full
    if (m_nMaxSize > 0 && size() >= m_nMaxSize) 
      throw new IndexOutOfBoundsException("Queue full");

    addElement(item);
    notify();
  }

  //  ====================    insertFirst      ====================
  /**
   ** Inserts an item first into the queue. If there are threads blocked on
   ** <code>remove</code>, one of them is unblocked.
   **
   ** @param	item	the item to be inserted.
   */
  public synchronized void insertFirst(Object item)
  {
    insertElementAt(item, 0);
    notify();
  }

  //  ====================    remove      ====================
  /**
   ** Removes and returns the first item in the queue.
   ** If the queue is empty, the calling thread will block.
   **
   ** @param    timeout timeout in seconds.
   ** @return The first item in the queue, or <code>null</code> if a
   ** timeout occurred. To distinguish timeouts, <code>null</code>
   ** items should not be inserted in the queue.
   */
  public synchronized Object removeWait(float timeout)
  {
    Object obj = null;

    // If queue is empty wait for object to be inserted
    if (isEmpty() && !queueClosed) {
      try {
	if (timeout > 0) {
	  wait(Math.round(timeout * 1000.0f));
	} else
	  wait();
      } catch (InterruptedException e) {}
    }

    if (queueClosed) {
      return null;
    }    
    
    try {
      obj = firstElement();
      removeElementAt(0);
    } catch (Exception e) {}
 
    return obj;
  }

  //  ====================    remove      ====================
  /**
   ** Removes and returns the first object in the queue.
   ** Same as <code>remove(float timeout)</code> but this function
   ** blocks forever.
   **
   ** @return The first item in the queue.
   */
  public Object remove()
  {
    return removeWait(0);
  }


  //  ====================     close     ====================
  /**
   ** Closes the queue, i.e. wakes up all threads blocking on
   ** a call to remove(). 
   */

  public synchronized void close() {
    queueClosed = true;
    notifyAll();
  }
  
}

