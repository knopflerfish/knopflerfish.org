/*
 * Copyright (c) 2010-2013, KNOPFLERFISH project
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

/**
 * Meta Iterator
 *
 * @author Jan Stein
 */
public class IteratorIterator<A> implements Iterator<A>
{

  final private Iterator<Iterator<A>> iter;
  /* The current iterator */
  private Iterator<A> current = null;

  public  IteratorIterator(List<Iterator<A>> ilist) {
    iter = ilist.iterator();
    if (iter.hasNext()) {
      current = iter.next();
    } else {
      // We need an empty iterator as current, reuse iter.
      @SuppressWarnings("unchecked")
      Iterator<A> empty = (Iterator<A>) iter;
      current = empty;
    }
  }


  public boolean hasNext() {
    return getIterator().hasNext();
  }

  public void remove() {
    current.remove();
  }

  public A next() {
    return getIterator().next();
  }


  private Iterator<A> getIterator() {
    while (!current.hasNext() && iter.hasNext()) {
      current = iter.next();
    }
    return current;
  }

}
