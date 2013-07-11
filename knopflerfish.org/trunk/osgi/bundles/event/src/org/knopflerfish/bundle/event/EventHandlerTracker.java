/*
 * Copyright (c) 2010-2013, KNOPFLERFISH project
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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class EventHandlerTracker
  implements ServiceTrackerCustomizer<EventHandler, TrackedEventHandler>
{
  private final BundleContext bc;
  private final ServiceTracker<EventHandler, TrackedEventHandler> tracker;
  private final Hashtable<String, Set<TrackedEventHandler>> topicsToHandlers
    = new Hashtable<String, Set<TrackedEventHandler>>();
  private final Hashtable<String, Set<TrackedEventHandler>> wildcardsToHandlers
    = new Hashtable<String, Set<TrackedEventHandler>>();

  public EventHandlerTracker(BundleContext bc)
  {
    this.bc = bc;
    tracker = new ServiceTracker<EventHandler, TrackedEventHandler>(
                                                                    bc,
                                                                    EventHandler.class,
                                                                    this);
  }

  public void open() {
    tracker.open();
  }

  public void close() {
    tracker.close();
    topicsToHandlers.clear();
    wildcardsToHandlers.clear();
  }


  public TrackedEventHandler addingService(ServiceReference<EventHandler> serviceReference)
  {
    EventHandler eh = bc.getService(serviceReference);
    if (eh != null) {
      return TrackedEventHandler.create(this, serviceReference, eh);
    } else {
      return null;
    }
  }

  public void modifiedService(ServiceReference<EventHandler> serviceReference,
                              TrackedEventHandler teh)
  {
    teh.update(serviceReference);
  }

  public void removedService(ServiceReference<EventHandler> serviceReference,
                             TrackedEventHandler teh)
  {
    teh.destroy();
    bc.ungetService(serviceReference);
  }

  public void addHandlerForTopic(String topic, TrackedEventHandler teh) {
    addToSetIn(topic, teh, topicsToHandlers);
  }

  public void addHandlerForWildcard(String wildcard, TrackedEventHandler teh) {
    addToSetIn(wildcard, teh, wildcardsToHandlers);
  }

  private static void addToSetIn(String key,
                                 TrackedEventHandler teh,
                                 Hashtable<String, Set<TrackedEventHandler>> h)
  {
    Set<TrackedEventHandler> s = null;
    synchronized (h) {
      s = h.get(key);
      if (s == null) {
        s = new HashSet<TrackedEventHandler>();
        h.put(key, s);
      }
    }
    synchronized (s) {
      s.add(teh);
      teh.referencedIn(s);
    }
  }

  private static <A> void addSetMatching(Set<A> result, Set<A> matching) {
    if (matching != null) {
      synchronized (matching) {
        result.addAll(matching);
      }
    }
  }


  public Set<TrackedEventHandler> getHandlersMatching(String topic)
  {
    Set<TrackedEventHandler> result = new HashSet<TrackedEventHandler>();
    addSetMatching(result, topicsToHandlers.get(topic));
    synchronized (wildcardsToHandlers) {
      for (Entry<String, Set<TrackedEventHandler>> entry : wildcardsToHandlers
          .entrySet()) {
        String wildcard = entry.getKey();
        if (topic.startsWith(wildcard)) {
          addSetMatching(result, entry.getValue());
        }
      }
    }
    return result;
  }

  public boolean anyHandlersMatching(String topic)
  {
    Set<TrackedEventHandler> matching = topicsToHandlers.get(topic);
    if (matching != null && !matching.isEmpty()) {
      return true;
    }
    synchronized (wildcardsToHandlers) {
      for (Entry<String, Set<TrackedEventHandler>> entry : wildcardsToHandlers
          .entrySet()) {
        String wildcard = entry.getKey();
        if (topic.startsWith(wildcard) && !entry.getValue().isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

}
