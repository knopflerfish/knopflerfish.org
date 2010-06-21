/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class JFloatable extends JPanel {

  JComponent main;
  JComponent buttonFloat;
  String     title;

  JFrame     frame = null;
  JButton    buttonInfo;

  boolean    bAutoClose = true;

  Point      frameLocation = null;
  Dimension  frameSize     = null;


  public JFloatable(JComponent main, String title) {
    super(new BorderLayout());
    this.main   = main;
    this.title  = title;
    buttonFloat = new JLabel(Activator.desktop.floatIcon);
    buttonFloat.setToolTipText("Float window");
    //    buttonFloat.setBorder(null);
    buttonFloat.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent ev) {
	  if(frame == null) {
	    doFloat();
	  } else {
	    doUnfloat();
	  }
	}
      });

    buttonInfo = new JButton("Show window");
    buttonInfo.setToolTipText("Moves floating window to front");
    buttonInfo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  if(frame != null) {
	    frame.toFront();
	  }
	}
      });

    addAncestorListener(new AncestorListener() {
	public void ancestorAdded(AncestorEvent event) {
	}
	public void ancestorMoved(AncestorEvent event) {
	}
	public void ancestorRemoved(AncestorEvent event) {
	  if(bAutoClose) {
	    if(frame != null) {
	      frame.setVisible(false);
	      frame = null;
	    }
	  }
	}
      });

    add(main, BorderLayout.CENTER);
    JPanel top = new JPanel(new BorderLayout());

    top.add(buttonFloat, BorderLayout.EAST);
    add(top, BorderLayout.NORTH);
  }

  void setAutoClose(boolean b) {
    bAutoClose = b;
  }

  synchronized public void doFloat() {
    if(frame == null) {
      frame = new JFrame(title);
      Activator.desktop.setIcon(frame, "/kf_");
      frame.getContentPane().setLayout(new BorderLayout());
      frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    doUnfloat();
	  }
      });
      buttonFloat.setToolTipText("Unfloat window");
    }
    
    remove(main);
    add(buttonInfo, BorderLayout.SOUTH);

    revalidate();
    invalidate();
    repaint();

    frame.getContentPane().add(main, BorderLayout.CENTER);

    Dimension size = main.getPreferredSize();
    size = new Dimension(Math.min(200, size.width),
			 Math.min(200, size.height));
    
    if(frameLocation != null) {
      frame.setLocation(frameLocation);
    }
    if(frameSize != null) {
      main.setPreferredSize(frameSize);
    } else {
      frame.setSize(size);
    }


    //    frame.setSize(size);
    frame.pack();
    frame.setVisible(true);
  }
  
  synchronized public void doUnfloat() {
    if(frame != null) {
      frameLocation = frame.getLocationOnScreen();
      frameSize     = main.getSize();

      frame.getContentPane().remove(main);
      frame.setVisible(false);
      frame = null;
    }
    buttonFloat.setToolTipText("Float window");
    add(main, BorderLayout.CENTER);
    remove(buttonInfo);

    revalidate();
    invalidate();
    doLayout();
    repaint();
  }

  public String toString() {
    return "JFloatable[" + 
      "title=" + title + 
      ", main=" + main + 
      ", frame=" + frame + 
      "]";
  }
}



