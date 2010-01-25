package org.knopflerfish.bundle.event;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class TrackedEventHandler {
  private final EventHandlerTracker tracker;
  private ServiceReference sr = null;
  private final EventHandler tracked;
  private Filter filter = null;
  private boolean destroyed = false;
  private boolean blacklisted = false;
  private HashSet referencingSets = new HashSet();

  private TrackedEventHandler(EventHandlerTracker tracker, EventHandler tracked) {
    this.tracker = tracker;
    this.tracked = tracked;
  }

  public boolean handleEventSubjectToFilter(Event event) {
    if(destroyed || blacklisted) {
      return false;
    }
    if(filter == null || event.matches(filter) ) {
      tracked.handleEvent(event);
      return true;
    }
    return false;
  }

  public void blacklist() {
    blacklisted = true;
    removeAllReferences();
  }

  public static TrackedEventHandler create(EventHandlerTracker eht, ServiceReference sr, EventHandler eh) {
    TrackedEventHandler teh = new TrackedEventHandler(eht, eh);
    teh.update(sr);
    return teh;
  }

  public void update(ServiceReference sr) {
    this.sr = sr;
    removeAllReferences();
    updateEventFilter();
    updateTopicsAndWildcards();
  }


  public void destroy() {
    destroyed = false;
    removeAllReferences();
  }

  private void updateEventFilter() {
    String filterString = (String) sr.getProperty(EventConstants.EVENT_FILTER);
    try {
      if(filterString == null) {
        filter = null;
      } else {
        filter = FrameworkUtil.createFilter(filterString);
      }
    } catch (InvalidSyntaxException e) {
      filter = null;
      blacklist();
      /*
                if (Activator.log.doError()) {
            Activator.log.error("Failure when matching filter '" +filterString
                                +"' in handler with service.id " +sid,
                                handlerSR,
                                err);
          }
       */
    }
  }

  private void updateTopicsAndWildcards() {
    Object o = sr.getProperty(EventConstants.EVENT_TOPIC);
    if(o == null) {
      blacklist();
      /*
                if (Activator.log.doError()) {
            Activator.log
              .error("Invalid value type for service property with key '"
                     +EventConstants.EVENT_TOPIC
                     +"' should be String[] found '" +topic.getClass()
                     +"' (" +topic +") " +"in handler with service.id " +sid,
                     handlerSR);
          }
       */
      return;
    }

    String[] topics =
      (o instanceof String) ?
        new String[] {(String)o} :
        (String[])o;

    for(int i = 0; i < topics.length; ++i) {
      String t = topics[i];
      if(t.length() == 0) {
        blacklist();
        return;
      }
      int idx = t.indexOf("*");
      if(idx > -1) {
        if(idx != (t.length() - 1)) {
          blacklist();
          return;
        }
        if(idx > 0) idx = idx - 1;
        tracker.addHandlerForWildcard(t.substring(0, idx), this);
      } else {
        tracker.addHandlerForTopic(t, this);
      }
    }
  }

  void referencedIn(Set s) {
    referencingSets.add(s);
  }

  void removeAllReferences() {
    Iterator i = referencingSets.iterator();
    while(i.hasNext()) {
      ((Set)i.next()).remove(this);
    }
  }
}

