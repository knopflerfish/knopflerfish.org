package org.knopflerfish.bundle.event;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

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
    Set s = (Set)h.get(key);
    if(s == null) {
      s = new HashSet();
      h.put(key, s);
    }
    s.add(teh);
    teh.referencedIn(s);
  }

  public Set getHandlersMatching(String topic) {
    Set result = new HashSet();

    Set matching = (Set)topicsToHandlers.get(topic);

    if(matching != null) {
      result.addAll(matching);
    }

    Enumeration wildcards = wildcardsToHandlers.keys();
    while(wildcards.hasMoreElements()) {
      String wildcard = (String)wildcards.nextElement();
      if(topic.startsWith(wildcard)) {
        result.addAll((Set)wildcardsToHandlers.get(wildcard));
      }
    }

    return result;
  }

  public boolean anyHandlersMatching(String topic) {
    boolean result = topicsToHandlers.contains(topic);
    if(result) return true;

    Enumeration wildcards = wildcardsToHandlers.keys();
    while(wildcards.hasMoreElements()) {
      String wildcard = (String)wildcards.nextElement();
      if(topic.startsWith(wildcard)) {
        return true;
      }
    }
    return false;
  }
}
