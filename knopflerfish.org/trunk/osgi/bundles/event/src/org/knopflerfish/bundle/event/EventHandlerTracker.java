/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
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

package org.knopflerfish.bundle.event;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.*;


public class EventHandlerTracker implements ServiceTrackerCustomizer {
  private final BundleContext bc;
  private final ServiceTracker tracker;
  private final Hashtable topicsToHandlers = new Hashtable();
  private final Hashtable wildcardsToHandlers = new Hashtable();

  public EventHandlerTracker(BundleContext bc) {
    this.bc = bc;
    tracker = new ServiceTracker(bc, EventHandler.class.getName(), this);
  }

  public void open() {
    tracker.open();
  }

  public void close() {
    tracker.close();
  }


  public Object addingService(ServiceReference serviceReference) {
    EventHandler eh = (EventHandler) bc.getService(serviceReference);
    if(eh != null) {
      return TrackedEventHandler.create(this, serviceReference, eh);
    } else {
      return null;
    }
  }

  public void modifiedService(ServiceReference serviceReference, Object o) {
    TrackedEventHandler teh = (TrackedEventHandler) o;
    teh.update(serviceReference);  
  }

  public void removedService(ServiceReference serviceReference, Object o) {
    TrackedEventHandler teh = (TrackedEventHandler) o;
    teh.destroy();
    bc.ungetService(serviceReference);
  }

  public void addHandlerForTopic(String topic, TrackedEventHandler teh) {
    addToSetIn(topic, teh, topicsToHandlers);
  }

  public void addHandlerForWildcard(String wildcard, TrackedEventHandler teh) {
    addToSetIn(wildcard, teh, wildcardsToHandlers);
  }

  private static void addToSetIn(String key, TrackedEventHandler teh, Hashtable h) {
    Set s = null;
    synchronized(h) {
      s = (Set)h.get(key);
      if(s == null) {
        s = new HashSet();
        h.put(key, s);
      }
    }
    synchronized(s) {
      s.add(teh);
      teh.referencedIn(s);
    }
  }


  private static void addSetMatching(Set result, Set matching) {
    if (matching != null) {
      synchronized (matching) {
        result.addAll(matching);
      }
    }
  }


  public Set getHandlersMatching(String topic) {
    Set result = new HashSet();
    addSetMatching(result, (Set)topicsToHandlers.get(topic));
    synchronized (wildcardsToHandlers) {
      Iterator wildcards = wildcardsToHandlers.entrySet().iterator(); 
      while (wildcards.hasNext()) {
        Map.Entry we = (Map.Entry)wildcards.next();
        String wildcard = (String)we.getKey();
        if (topic.startsWith(wildcard)) {
          addSetMatching(result, (Set)we.getValue());
        }
      }
    }
    return result;
  }


  public boolean anyHandlersMatching(String topic) {
    Set matching = (Set)topicsToHandlers.get(topic);
    if (matching != null && !matching.isEmpty()) {
      return true;
    }
    synchronized (wildcardsToHandlers) {
      Iterator wildcards = wildcardsToHandlers.entrySet().iterator(); 
      while (wildcards.hasNext()) {
        Map.Entry we = (Map.Entry)wildcards.next();
        String wildcard = (String)we.getKey();
        if (topic.startsWith(wildcard)  && !((Set)we.getValue()).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }
    
}
