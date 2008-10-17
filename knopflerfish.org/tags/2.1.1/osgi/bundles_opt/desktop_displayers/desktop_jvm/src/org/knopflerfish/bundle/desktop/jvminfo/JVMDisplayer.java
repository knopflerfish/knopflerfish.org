/*
 * Copyright (c) 2004-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.jvminfo;

import org.osgi.framework.*;
import java.util.*;
import org.knopflerfish.service.desktop.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JVMDisplayer extends DefaultSwingBundleDisplayer {

  public JVMDisplayer(BundleContext bc) {
    super(bc, "JVM", "JVM information", true);
    bUseListeners = false;

  }

  public JComponent newJComponent() {
    JVMInfo info = new JVMInfo();
    info.start();
    return info;
  }

  public void  disposeJComponent(JComponent comp) {
    JVMInfo info = (JVMInfo)comp;
    info.stop();

    super.disposeJComponent(comp);
  }

  void closeComponent(JComponent comp) {
    JVMInfo info = (JVMInfo)comp;
    info.stop();    
  }

  public void valueChanged(long bid) {
    super.valueChanged(bid);

    Bundle[] bl = bc.getBundles();
    
    for(int i = 0; i < bl.length; i++) {
      if(bundleSelModel.isSelected(bl[i].getBundleId())) {
	
      }
    }
  }

  public Icon getSmallIcon() {
    return null;
  }

  class JVMInfo extends JPanel implements Runnable {
    JGraph memGraph;
    JGraph threadGraph;

    public JVMInfo() {
      setLayout(new BorderLayout());

      memGraph = new JGraph("Heap memory", 100, 0, 8 * 1024 * 1024, 1024 * 1024) {
	  String getYUnit() {
	    return "Mb";
	  }
	  String getYLabel(long y) {
	    return Long.toString(y / 1024 / 1024);
	  }
	};


      threadGraph = new JGraph("Active threads", 100, 0, 30, 5) {
	  String getYUnit() {
	    return "threads";
	  }
	  String getYLabel(long y) {
	    return Long.toString(y);
	  }
	};
      

      
      JPanel cmdPanel = new JPanel(new FlowLayout());
      cmdPanel.add(new JButton("Run GC") { {
	addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
	      Runtime rt = Runtime.getRuntime();
	      rt.gc();
	    }
	  });
      }});
      
      JPanel graphs = new JPanel(new GridLayout(0, 1, 3, 3));
      
      graphs.add(memGraph);
      graphs.add(threadGraph);
      
      add(graphs, BorderLayout.CENTER);
      add(cmdPanel, BorderLayout.SOUTH);

      setPreferredSize(new Dimension(350, 300));

    }
    
    public void start() {
      if(runner == null) {
	runner = new Thread(this, "JVMInfo thread");
	bRun = true;
	runner.start();
      }
    }
    public void stop() {
      if(runner != null) {
	bRun = false;
	try {
	  runner.join(delay * 10);
	} catch (Exception e) {
	}
	runner = null;
      }
    }
    
    long delay = 500;
    boolean bRun = false;
    Thread runner = null;

    public void run() {
      Runtime rt = Runtime.getRuntime();
      while(bRun) {
	try {
	  long mem     = rt.totalMemory() - rt.freeMemory();
	  int nThreads = countThreads();
	  
	  memGraph.addValue(mem);
	  
	  threadGraph.addValue(nThreads);
	  SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
		memGraph.repaint();
		threadGraph.repaint();
	      }
	    });
	  Thread.sleep(delay);
	} catch (Exception e) {
	}
      }
    }
    
  }
  
  int countThreads() {
    ThreadGroup tg = Thread.currentThread().getThreadGroup();
    
    while(tg.getParent() != null) {
      tg = tg.getParent();
    }
    
    return tg.activeCount();
  }
}
