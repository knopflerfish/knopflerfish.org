/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktopawt;

import org.osgi.framework.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

public class InstallPanel extends Panel {
  TextField tf;
  TextArea msg;
  LF lf = LF.getLF();

  String defText = "Enter bundle URL\nthen select \"Install\"";

  FileDialog fd;

  public InstallPanel() {
    super();
    setLayout(new BorderLayout());

    Container row = new Container();
    row.setLayout(new BorderLayout());

    tf = new TextField();
    tf.setFont(lf.defaultFont);
    tf.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent ev) {
          if(ev.getKeyCode() == KeyEvent.VK_ENTER) {
            installBundle(tf.getText());
          }
        }
      });

    msg = new TextArea(10, 10);
    msg.setText(defText);

    msg.setBackground(lf.bgColor);
    msg.setForeground(lf.textColor);
    msg.setFont(lf.smallFont);
    msg.setEditable(false);
    
    Button installB = new Button("Install");
    installB.setBackground(lf.bgColor);
    
    ImageLabel open = new ImageLabel("/open.gif", 1, lf.bgColor);
    open.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          browseFile();
        }
      });

    installB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          installBundle(tf.getText());
        }
      });
    row.add(new Label("URL"), BorderLayout.WEST);
    row.add(tf, BorderLayout.CENTER);
    row.add(open, BorderLayout.EAST);
    row.add(installB, BorderLayout.SOUTH);
    
    add(row, BorderLayout.NORTH);
    add(msg, BorderLayout.CENTER);

  }

  void browseFile() {
    if( fd == null) {
      fd = new FileDialog(Desktop.frame);
    }
    fd.show();
    File f = new File(fd.getDirectory());
    f = new File(f, fd.getFile());
    tf.setText("file:" + f.getAbsolutePath());
  }

  void installBundle(String url) {
    try {
      url = url.trim();
      if(url.length() == 0) {
        msg.setText(defText);
      } else {
        Bundle b = Activator.bc.installBundle(url.trim());
        msg.setText("Installed " + url + "\n as bundle #" + b.getBundleId());
      }
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      msg.setText(sw.toString());
    }
  }
}
