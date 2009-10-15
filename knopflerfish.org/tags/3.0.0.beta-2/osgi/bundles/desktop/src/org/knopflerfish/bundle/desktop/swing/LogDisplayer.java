/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import org.knopflerfish.bundle.log.window.impl.FilterLogTableModel;
import org.knopflerfish.bundle.log.window.impl.JLogEntryDetail;
import org.knopflerfish.bundle.log.window.impl.JLogPanel;
import org.knopflerfish.bundle.log.window.impl.LogReaderDispatcher;
import org.knopflerfish.bundle.log.window.impl.LogTableModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class LogDisplayer extends DefaultSwingBundleDisplayer {

  LogTableModel       logModel;
  LogReaderDispatcher logDispatcher;

  public LogDisplayer(BundleContext bc) {
    super(bc, "Log", "Shows framework log", true);

    // We're not interested in bundle events, nor in service events
    bUseListeners = false;
  }

  public void open() {
    super.open();
    logModel = new LogTableModel(bc);
    logDispatcher = new LogReaderDispatcher(bc, logModel);

    logDispatcher.open();
    logDispatcher.getAll();
  }

  public void close() {

    logDispatcher.close();

    super.close();
  }

  public JComponent newJComponent() {
    return new JLog();
  }

  public void valueChanged(long  bid) {
    Bundle[] bl = Activator.desktop.getSelectedBundles();

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JLog comp = (JLog)it.next();
      comp.valueChanged(bl);
    }
  }

  class JLog extends JPanel {
    JLogPanel           logPanel;

    JLogEntryDetail     logDetail;
    FilterLogTableModel filterLogModel;

    JLog() {
      setLayout(new BorderLayout());


      filterLogModel = new FilterLogTableModel(bc, logModel);
      filterLogModel.setBundles(null);

      // construct in two steps
      logDetail = new JLogEntryDetail(null, null);
      logPanel  = new JLogPanel(filterLogModel, logDetail, false);

      logDetail.setParentAndEntry(logPanel.getJLogTable(), null);
      logDetail.setModel(filterLogModel);




      JSplitPane splitPane =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                       logPanel,
                       logDetail);

      splitPane.setDividerLocation(120);

      add(splitPane, BorderLayout.CENTER);
    }

    public void valueChanged(Bundle[] bl) {
      filterLogModel.setBundles(bl);

      JTable table = logPanel.getJLogTable();

      if(table.getRowCount() > 0) {
        table.setRowSelectionInterval(0, 0);
      }
    }
  }
}
