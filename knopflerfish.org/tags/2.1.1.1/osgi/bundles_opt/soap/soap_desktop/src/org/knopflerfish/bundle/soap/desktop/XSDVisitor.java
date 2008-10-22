package org.knopflerfish.bundle.soap.desktop;

public interface XSDVisitor {
  public void visit(XSDObj obj, Object data, int level);
}
