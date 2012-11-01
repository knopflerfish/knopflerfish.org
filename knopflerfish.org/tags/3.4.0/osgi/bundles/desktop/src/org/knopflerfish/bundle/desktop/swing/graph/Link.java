package org.knopflerfish.bundle.desktop.swing.graph;



public interface Link extends Comparable {
  public Node getFrom();
  public Node getTo();
  public int getDepth();
  public void refresh();
  public String getId();
  public int getType();
  public int getDetail();
}
