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

package org.knopflerfish.bundle.desktop.event;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Date;

import javax.swing.JFrame;
import org.osgi.service.event.*;

public class JDetailFrame extends JFrame {
  int w = 500;
  int h = 300;

  JEventEntryDetail eventEntryDetail;

  public JDetailFrame(JEventTable parent, Event entry) {
    super(new Date(Util.getTime(entry)).toString() + ": " + Util.getMessage(entry));

    getContentPane().setLayout(new BorderLayout());

    eventEntryDetail = new JEventEntryDetail(parent, entry);

    Container contentPane = getContentPane();
    
    contentPane.add(eventEntryDetail, BorderLayout.CENTER);
    
    pack();
    invalidate();
    setSize(new Dimension(w, h));

    setVisible(true);
  }

  public void setEntry(Event entry) {
    eventEntryDetail.setEntry(entry);
  }
}

