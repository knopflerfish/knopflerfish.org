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

import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.SwingUtilities;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.*;


/**
 * Class that listens for all events on some topic and dispatches them
 * to an EventTableModel.
 */
public class EventReaderDispatcher implements EventHandler
{

  final BundleContext bc;
  final EventTableModel model;
  ServiceRegistration reg = null;
  String topic  = "*";
  String filter = "";

  public EventReaderDispatcher(BundleContext bc,
                             EventTableModel model) {
    this.bc    = bc;
    this.model = model;
  }


  public void open() {
    if(reg == null) {
      Hashtable props = new Hashtable();
      props.put(EventConstants.EVENT_TOPIC,  new String[] { topic });
      if(filter != null && !"".equals(filter)) {
        props.put(EventConstants.EVENT_FILTER, filter);
      }
      reg = bc.registerService(EventHandler.class.getName(), this, props);
    }
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String t) {
    if(t.equals(topic)) {
      return;
    }
    topic = t;
    close();
    open();
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String t) {
    if(t.equals(filter)) {
      return;
    }
    filter = t;
    close();
    open();
  }


  public void clearAll() {
    model.clear();
  }

  public void getAll() {
    // NI
  }

  public void close() {
    if(reg != null) {
      reg.unregister();
      reg = null;
    }
  }

  static long idCount = 0;

  /**
   */
  public void handleEvent(final Event ev) {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          model.logged(ev);
        }
      });
  }
}
