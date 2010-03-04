package org.knopflerfish.bundle.desktop.swing.graph;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public interface Link extends Comparable {
  public Node getFrom();
  public Node getTo();
  public int getDepth();
  public void refresh();
  public String getId();
  public int getType();
  public int getDetail();
}
