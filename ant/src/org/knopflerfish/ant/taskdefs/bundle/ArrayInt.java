/*
 * Copyright (c) 2003, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer. 
 * 
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution. 
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

package org.knopflerfish.ant.taskdefs.bundle;

import java.io.*;
import java.util.*;

public class ArrayInt extends Number implements Comparable {
  int[] ia = new int[0];
  
  public static String UNDEF = "";
  
  public ArrayInt() {
    this("0");
  }
  
  public ArrayInt(String s) {
    parseDotString(s);
  }
  
  public void parseDotString(String s) {
    if(UNDEF.equals(s)) {
      ia = new int[0];
      return;
    }
    
    String[] sa = Util.splitwords(s, ".", '\"');
    
    ia = new int[sa.length];
    
    for(int i = 0; i < sa.length; i++) {
      ia[i] = Integer.parseInt(sa[i]);
    }
  }
  
  public int compareTo(Object o) {
    ArrayInt other = (ArrayInt)o;
    
    if(other.ia.length == 0) {
      return 1;
    }
    int i = 0;
    while(i < ia.length && i < other.ia.length) {
      int diff = ia[i] - other.ia[i];
      if(diff != 0) {
	return diff;
      }
      i++;
    }
    
    return 0;
  }
  
  public double doubleValue() {
    return (int)doubleValue();
  }
  
  public float floatValue() {
    return (float)longValue();
  }
  
  public int intValue() {
    return (int)longValue();
  }
  
  
  public long longValue() {
    long v = 0;
    for(int i = 0; i < ia.length; i++) {
      v = (v << 8) + ia[i];
    }
    
    return v;
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer();
    if(ia.length == 0) {
      sb.append(UNDEF);
    } else {
      for(int i = 0; i < ia.length; i++) {
	sb.append(Integer.toString(ia[i]));
	if(i < ia.length - 1) {
	  sb.append(".");
	}
      }
    }
    return sb.toString();
  }
}

