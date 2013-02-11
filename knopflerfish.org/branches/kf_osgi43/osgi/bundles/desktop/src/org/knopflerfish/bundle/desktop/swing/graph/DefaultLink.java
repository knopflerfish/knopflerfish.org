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


import java.awt.Color;

public class DefaultLink implements Link {
  Node from;
  Node to;
  int depth;
  String id;
  String name;
  Color color = Color.blue;
  int dir;
  int z;
  int type;
  int detail;

  public DefaultLink(Node from,
                     Node to,
                     int depth,
                     String id,
                     String name) {
    this.from = from;
    this.to   = to;
    this.depth = depth;
    this.id = id;
    this.name = name;
  }

  public int getDetail() {
    return detail;
  }

  public void setDetail(int d) {
    detail = d;
  }


  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }
  
  public void setZ(int z) {
    this.z = z;
  }

  public void setColor(Color c) {
    this.color = c;
  }

  public Color getColor() {
    return color;
  }

  public void refresh() {
    from.refresh();
    to.refresh();
  }

  public Node getFrom() {
    return from;
  }
  
  public Node getTo() {
    return to;
  }
  
  public int getDepth() {
    return depth;
  }
  
  public int hashCode() {
    return id.hashCode();
  }
  
  public boolean equals(Object obj) {
    if(!(obj instanceof DefaultLink)) {
      return false;
    }
    
    DefaultLink sl = (DefaultLink)obj;
    return id.equals(sl.id);
  }

  public String getId() {
    return id;
  }

  
  public String toString() {
    return name;
  }

  public int compareTo(Link link) {
    int r = 0;
    if (link instanceof DefaultLink) {
      r = z - ((DefaultLink)link).z;
    }
    if(r == 0) {
      r = id.compareTo(link.getId());
    }
    return r;
  }
}
