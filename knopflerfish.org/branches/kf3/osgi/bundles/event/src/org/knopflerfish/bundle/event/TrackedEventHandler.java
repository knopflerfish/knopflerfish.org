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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class TrackedEventHandler {
  private final EventHandlerTracker tracker;
  private ServiceReference sr = null;
  private final EventHandler tracked;
  private Filter filter = null;
  private boolean destroyed = false;
  private boolean blacklisted = false;
  private HashSet referencingSets = new HashSet();

  private TrackedEventHandler(EventHandlerTracker tracker, EventHandler tracked)
  {
    this.tracker = tracker;
    this.tracked = tracked;
  }

  public boolean handleEventSubjectToFilter(Event event)
  {
    if (destroyed || isBlacklisted()) {
      return false;
    }
    if (filter == null || event.matches(filter)) {
      tracked.handleEvent(event);
      setBlacklist(false);
      return true;
    }
    return false;
  }

  public synchronized boolean isBlacklisted()
  {
    return blacklisted;
  }

  public synchronized void setBlacklist(boolean blacklisted)
  {
    this.blacklisted = blacklisted;
  }

  public static TrackedEventHandler create(EventHandlerTracker eht,
                                           ServiceReference sr,
                                           EventHandler eh)
  {
    TrackedEventHandler teh = new TrackedEventHandler(eht, eh);
    teh.update(sr);
    return teh;
  }

  public void update(ServiceReference sr)
  {
    this.sr = sr;
    // removeAllReferences();
    updateEventFilter();
    updateTopicsAndWildcards();
  }

  public ServiceReference getServiceReference()
  {
    return sr;
  }

  public void destroy()
  {
    destroyed = true;
    removeAllReferences();
  }

  private void updateEventFilter()
  {
    String filterString = (String) sr.getProperty(EventConstants.EVENT_FILTER);
    try {
      if (filterString == null) {
        filter = null;
      } else {
        filter = FrameworkUtil.createFilter(filterString);
      }
    } catch (InvalidSyntaxException e) {
      filter = null;
      setBlacklist(true);
      /*
       * if (Activator.log.doError()) {
       * Activator.log.error("Failure when matching filter '" +filterString
       * +"' in handler with service.id " +sid, handlerSR, err); }
       */
    }
  }

  private void updateTopicsAndWildcards()
  {
    Object o = sr.getProperty(EventConstants.EVENT_TOPIC);
    if (o == null) {
      if (!isBlacklisted()) {
        setBlacklist(true);
        if (Activator.log.doError()) {
          Activator.log.error("EventHandler must have service property '"
                              + EventConstants.EVENT_TOPIC
                              + "' in handler with service.id "
                              + sr.getProperty("service.id"), sr);
        }
      }
      return;
    }

    String[] topics = (o instanceof String) ? new String[] { (String) o }
        : (String[]) o;

    for (int i = 0; i < topics.length; ++i) {
      String t = topics[i];
      if (t.length() == 0) {
        setBlacklist(true);
        return;
      }
      int idx = t.indexOf("*");
      if (idx > -1) {
        if (idx != (t.length() - 1)) {
          setBlacklist(true);
          return;
        }
        tracker.addHandlerForWildcard(t.substring(0, idx), this);
      } else {
        tracker.addHandlerForTopic(t, this);
      }
    }
  }

  void referencedIn(Set s)
  {
    referencingSets.add(s);
  }

  void removeAllReferences()
  {
    Iterator i = referencingSets.iterator();
    while (i.hasNext()) {
      Set s = (Set) i.next();
      synchronized (s) {
        s.remove(this);
      }
    }
  }
}
