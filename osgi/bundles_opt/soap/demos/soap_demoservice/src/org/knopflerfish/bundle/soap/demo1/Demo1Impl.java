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

package org.knopflerfish.bundle.soap.demo1;

import org.knopflerfish.service.soap.demo1.*;

import java.util.*;

/**
 * Implementation of the Demo1 service.
 */
public class Demo1Impl implements Demo1 {

  public int add(int a, int b) {
    return a + b;
  }

  public String getName() {
    return "The soap demo1 service";
  }

  public String[] getNames() {
    return new String[] {
      "soap 1",
      "soap 2",
    };
  }

  public String[] getNamesFromArray(int[] array) {
    String[] sa = new String[array.length+1];

    int n = 0;
    for(int i = 0; i < array.length; i++) {
      sa[i] = Integer.toString(array[i]);
      n += array[i];
    }
    sa[array.length] = Integer.toString(n);

    return sa;
  }

  Hashtable props = new Hashtable() {
      {
	put("key1", "value 1");
	put("key2", "value 2");
      }
    };


  public Dictionary getDictionary() {
    return (Hashtable)props.clone();
  }

  Vector vector = new Vector() {
      {
	addElement("item1");
	addElement("item2");
	addElement("item3");
      }
    };
  public Vector getVector() {
    return (Vector)vector.clone();
  }
}
