/*
 * Copyright (c) 2012-2012, KNOPFLERFISH project
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
 * Meta Iterator that takes sorted list and returns
 * a sorted result.
 *
 * @author Jan Stein
 */
public class IteratorIteratorSorted implements Iterator
{

  final private Iterator [] iter;
  final private Object [] top;
  final private Util.Comparator comp;
  int size;

  public  IteratorIteratorSorted(List ilist, Util.Comparator comp) {
    this.comp = comp;
    size = ilist.size();
    iter = new Iterator [size + 1];
    top = new Object [size + 1];
    int pos = 1;
    for (Iterator i = ilist.iterator(); i.hasNext(); ) {
      Iterator si = (Iterator)i.next();
      if (si.hasNext()) {
        top[pos] = si.next();
        iter[pos++] = si;
      } else {
        size--;
      }
    }
    for (pos = size / 2; pos > 0; pos--) {
      balance(pos);
    }
  }


  public boolean hasNext() {
    return size > 0;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  public Object next() {
    if (hasNext()) {
      Object res = top[1];
      if (iter[1].hasNext()) {
        top[1] = iter[1].next();
      } else {
        top[1] = top[size];
        iter[1] = iter[size--];
      }
      balance(1);
      return res;
    }
    throw new NoSuchElementException();
  }

  /**
   * Balance heap.
   */
  private void balance(int current) {
    Object tmp = top[current];
    Iterator itmp = iter[current];

    int child;
    while (current * 2 <= size) {
      child = current * 2;
      if (child != size && comp.compare(top[child + 1], top[child]) > 0) {
        child++;
      }
      if (comp.compare(top[child], tmp) > 0) {
        top[current] = top[child];
        iter[current] = iter[child];
      } else {
        break;
      }
      current = child;
    }
    top[current] = tmp;
    iter[current] = itmp;
  }

}
