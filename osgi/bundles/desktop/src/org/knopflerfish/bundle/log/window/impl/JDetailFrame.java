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

package org.knopflerfish.bundle.log.window.impl;

import java.util.*;
import java.text.*;

import java.net.URL;

import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.Text;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

public class JDetailFrame extends JFrame {
  int w = 500;
  int h = 300;

  JLogEntryDetail logEntryDetail;

  public JDetailFrame(JLogTable parent, ExtLogEntry entry) {
    super(new Date(entry.getTime()).toString() + ": " + entry.getMessage());

    getContentPane().setLayout(new BorderLayout());

    logEntryDetail = new JLogEntryDetail(parent, entry);

    Container contentPane = getContentPane();
    
    contentPane.add(logEntryDetail, BorderLayout.CENTER);
    
    pack();
    invalidate();
    setSize(new Dimension(w, h));

    setVisible(true);
  }

  public void setEntry( ExtLogEntry entry) {
    logEntryDetail.setEntry(entry);
  }
}

