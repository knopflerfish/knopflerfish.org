package org.knopflerfish.bundle.desktop.swing.graph;


import java.util.*;
import java.awt.geom.Point2D;

public interface Node {
  public Collection getOutLinks();
  public Collection getInLinks();
  public double getSize();
  public int getDepth();
  public void refresh();
  public void setPoint(Point2D p);
  public Point2D getPoint();
  public String getId();

  public double getOutMin();
  public double getOutMax();
  public void setOutMin(double n);
  public void setOutMax(double n);

  public double getInMin();
  public double getInMax();
  public void setInMin(double n);
  public void setInMax(double n);
}
