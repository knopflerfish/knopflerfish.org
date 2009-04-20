package org.knopflerfish.bundle.desktop.swing.graph;


import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import org.osgi.framework.*;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.knopflerfish.bundle.desktop.swing.Activator;

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

  public int compareTo(Object obj) {
    DefaultLink link = (DefaultLink)obj;
    int r = z - link.z;
    if(r == 0) {
      r = id.compareTo(link.id);
    }
    return r;
  }
}
