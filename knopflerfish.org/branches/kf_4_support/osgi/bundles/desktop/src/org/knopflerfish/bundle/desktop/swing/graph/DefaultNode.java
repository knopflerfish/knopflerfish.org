/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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
package org.knopflerfish.bundle.desktop.swing.graph;


import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;

public class DefaultNode implements Node, Comparable<DefaultNode> {
  int depth;
  String name;
  Point2D p;
  String id;
  double size = 1.0;
  int z;
  int detail = 0;

  double outMin = 0;
  double outMax = 0;
  double inMin = 0;
  double inMax = 0;

  public DefaultNode(String name, int depth, String id) {
    this.depth = depth;
    this.name = name;
    this.id = id;
  }

  public double getOutMin() {
    return outMin;
  }

  public double getOutMax() {
    return outMax;
  }

  public void setOutMin(double n) {
    outMin = n;
  }

  public void setOutMax(double n) {
    outMax = n;
  }

  public double getInMin() {
    return inMin;
  }

  public double getInMax() {
    return inMax;
  }

  public void setInMin(double n) {
    inMin = n;
  }

  public void setInMax(double n) {
    inMax = n;
  }

  public int getDetail() {
    return detail;
  }

  public void setDetail(int d) {
    detail = d;
  }

  public double getSize() {
    return size;
  }

  public void setSize(double d) {
    this.size = d;
  }

  public void setZ(int z) {
    this.z = z;
  }

  public Collection<? extends Link> getOutLinks() {
    return Collections.emptySet();
  }
  public Collection<? extends Link> getInLinks() {
    return Collections.emptySet();
  }

  public String getId() {
    return id;
  }

  public int getDepth() {
    return depth;
  }
  public void refresh() {
  }
  
  public String toString() {
    return name;
  }

  public void setPoint(Point2D p) {
    this.p = p;
  }

  public Point2D getPoint() {
    return p;
  }

 public int hashCode() {
   return id.hashCode();
 }
  
  public boolean equals(Object obj) {
    if(!(obj instanceof DefaultNode)) {
      return false;
    }
    
    DefaultNode node = (DefaultNode)obj;
    return id.equals(node.id);
  }

  public int compareTo(DefaultNode node) {
    int r = z - node.z;
    if(r == 0) {
      r = id.compareTo(node.id);
    }
    return r;
  }
}
