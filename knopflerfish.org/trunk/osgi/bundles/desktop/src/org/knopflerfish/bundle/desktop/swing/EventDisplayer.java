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
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import org.knopflerfish.bundle.desktop.event.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class EventDisplayer extends DefaultSwingBundleDisplayer {

  EventTableModel       eventModel;
  EventReaderDispatcher eventDispatcher;
    
  public EventDisplayer(BundleContext bc) {
    super(bc, "Events", "Show events", true); 

    // We're not interested in bundle events, nor in service events
    bUseListeners = false;
  }

  public void open() {
    super.open();
    eventModel = new EventTableModel();
    eventDispatcher = new EventReaderDispatcher(bc, eventModel);
    eventModel.setDispatcher(eventDispatcher);
    eventDispatcher.open();
    eventDispatcher.getAll();

    System.out.println("****** open: " + eventModel + ", " + eventModel.getDispatcher());
  }

  public void close() {

    eventDispatcher.close();

    super.close();
  }
  
  public JComponent newJComponent() {
    return new JEvent();
  }

  public void valueChanged(long  bid) {
    Bundle[] bl = Activator.desktop.getSelectedBundles();
    
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JEvent comp = (JEvent)it.next();
      comp.valueChanged(bl);
    }
  }

  class JEvent extends JPanel {
    JEventPanel           eventPanel;

    JEventEntryDetail     eventDetail;
    FilterEventTableModel filterEventModel;
    
    JEvent() {
      setLayout(new BorderLayout());


      filterEventModel = new FilterEventTableModel(eventModel);
      filterEventModel.setBundles(null);

      // construct in two steps
      eventDetail = new JEventEntryDetail(null, null);
      eventPanel  = new JEventPanel(filterEventModel, eventDetail, false);

      eventDetail.setParentAndEntry(eventPanel.getJEventTable(), null);
      eventDetail.setModel(filterEventModel);


      
      
      JSplitPane splitPane = 
	new JSplitPane(JSplitPane.VERTICAL_SPLIT,
		       eventPanel,
		       eventDetail);
      
      splitPane.setDividerLocation(120);

      add(splitPane, BorderLayout.CENTER);
    }

    public void valueChanged(Bundle[] bl) {
      /*
      filterEventModel.setBundles(bl);

      JTable table = eventPanel.getJEventTable();

      if(table.getRowCount() > 0) {
	table.setRowSelectionInterval(0, 0);
      }
      */
    }
  }
}

