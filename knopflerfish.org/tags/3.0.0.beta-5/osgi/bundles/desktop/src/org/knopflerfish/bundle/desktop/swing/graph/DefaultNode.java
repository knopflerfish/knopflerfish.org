package org.knopflerfish.bundle.desktop.swing.graph;


import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Color;

public class DefaultNode implements Node, Comparable {
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

  public Collection getOutLinks() {
    return Collections.EMPTY_SET;
  }
  public Collection getInLinks() {
    return Collections.EMPTY_SET;
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

  public int compareTo(Object obj) {
    if(!(obj instanceof DefaultNode)) {
      return 1;
    }
    
    DefaultNode node = (DefaultNode)obj;
    
    int r = z - node.z;
    if(r == 0) {
      r = id.compareTo(node.id);
    }
    return r;
  }
}
