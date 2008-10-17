/*
 * Copyright (c) 2008, KNOPFLERFISH project
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


package org.knopflerfish.bundle.desktop.prefs;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

public class TreeUtils {

  static public Collection getExpandedPaths(JTree tree) {
    Collection set = new TreeSet(tpLengthComparator);
    TreePath root = new TreePath(tree.getModel().getRoot());
    if(root != null) {
      for(Enumeration e = tree.getExpandedDescendants(root); e != null && e.hasMoreElements(); ) {
        TreePath tp = (TreePath)e.nextElement();
        set.add(tp);
      }
    }
    return set;
  }
  
  static public void expandPaths(JTree tree, Collection paths) {
    if(paths != null) {
      for(Iterator it = paths.iterator(); it.hasNext(); ) {
        TreePath tp = (TreePath)it.next();
        Object[] path = tp.getPath();
        
        expandPath(tree, tp);
      }
    }
  }
  
  static public void expandPath(JTree tree, TreePath tp) {
    Object root = tree.getModel().getRoot();
    expandPath(tree,  
               new TreePath(root),
               tp, 
               0);
  }
  
  static public void expandPath(JTree tree, 
                                TreePath targetPath, 
                                TreePath tp, 
                                int pos) {
    Object[] nodes = tp.getPath();

    Object node   = targetPath.getLastPathComponent();
    Object tpNode = nodes[pos];
    
    if(node.equals(tpNode)) {
      tree.expandPath(targetPath);
    } else {
      return;
    }
    
    TreeModel model = tree.getModel();
    if(pos < nodes.length - 1) {
      int n = model.getChildCount(node);
      for(int i = 0; i < n; i++) {
        Object child = model.getChild(node, i);        
        if(child.equals(nodes[pos+1])) {
          expandPath(tree, targetPath.pathByAddingChild(child), tp, pos+1);
        }
      }
    }
  }


  public static Comparator tpLengthComparator = new Comparator() {
      public int compare(Object o1, Object o2) {
        Object[] a1 = ((TreePath)o1).getPath();
        Object[] a2 = ((TreePath)o2).getPath();
        
        int n1 = a1 != null ? a1.length : 0;
        int n2 = a2 != null ? a2.length : 0;
        
        return n1 - n2;
      }
    };
}
