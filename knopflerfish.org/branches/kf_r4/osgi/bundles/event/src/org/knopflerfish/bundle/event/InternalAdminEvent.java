package org.knopflerfish.bundle.event;

import java.security.AccessControlException;
import java.util.Calendar;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.TopicPermission;

/**
 * A wrapper class for events. Connects an event with a timestamp and a boolean which
 * is used to determine whether an event has been delivered or not.
 *
 * @author Magnus Klack (refactoring by Björn Andersson)
 */
public class InternalAdminEvent {

  private Event event;
  private Calendar timeStamp;
  private boolean isDelivered=false;
  /** variable holding the creator of the object */
  EventAdminService owner;

  private ServiceReference[] references;

  /**
   * Standard constructor of the InternalAdminEvent
   * @param event the event to be stored
   * @param time The timestamp of this event
   * @param creator A handle to the admin service
   * @param references ServiceReference to the EventHandlers this event should be
   *                   delivered to.
   */
  public InternalAdminEvent(Event event,
                            Calendar time,
                            EventAdminService creator,
                            ServiceReference[] references){
    this.event = event;
    timeStamp = time;
    owner = creator;
    this.references = references;
  }

  /**
   * Returns the event
   * @return the event
   */
  protected Event getEvent(){
    return event;
  }

  /**
   * Returns the timestamp
   * @return the time at which the event arrived
   */
  protected Calendar getTimeStamp(){
    return timeStamp;
  }

  public ServiceReference[] getReferences() {
    return references;
  }

  /**
   * Returns the boolean illustrating whether the event has been delivered or not.
   * @return true if the event has been delivered, false otherwise
   */
  protected synchronized boolean isDelivered(){
    return isDelivered;
  }

  public void deliver() {
    SecurityManager securityManager = getSecurityManager();

    // variable indicates if the handler is allowed to publish
    boolean canPublish;

    // variable indicates if handlers are granted access to topic
    boolean canSubscribe;

    // variable indicates if the topic is well formatted
    boolean isWellFormatted;

    // check if security is applied
    if (securityManager != null) {
      // check if there are any security limitation
      canPublish   = checkPermission(event, securityManager, TopicPermission.PUBLISH);
      canSubscribe = checkPermission(event, securityManager, TopicPermission.SUBSCRIBE);
    } else {
      // no security here
      canPublish = true;
      canSubscribe = true;
    }

    // get if the topic is wellformatted
    isWellFormatted = topicIsWellFormatted(event.getTopic());

    if (Activator.log.doDebug()) Activator.log.debug("Checks: " + canPublish + " " + canSubscribe + " " + isWellFormatted);

    if (canPublish && canSubscribe && isWellFormatted) {
      // create an instance of the deliver session to deliver events
      DeliverSession deliverSession;
      deliverSession = new DeliverSession(this);
      // start deliver events
      deliverSession.deliver();

    // this will happen if an error occures in getReferences():
    } else if (!isWellFormatted) {
      Activator.log.error("Topic is not well formatted:"
                          + event.getTopic());
    } else if (canSubscribe) {
      // no publish permission
      Activator.log.error("No permission to publishto topic:"
                          + event.getTopic());
    } else if (canPublish) {
      // no subscribe permission
      Activator.log.error("No permission to granted for subscription to topic:"
                          + event.getTopic());
    } else {
      // no permissions at all are given
      Activator.log.error("No permission to publish and subscribe top topic:"
                          + event.getTopic());
    }
  }

  /**
   * this function checks for invalid topics
   * like null topics and "" topics.
   *
   * @param topic the topic string
   * @return true if well formatted else false if null or "" formatted
   */
  private boolean topicIsWellFormatted(String topic) {
    if(topic!=null){
      // this is the "*" topic
      if(topic.length()==1 && topic.equals("*")){
        return true;
      }

      // this is the "" topic
      if(topic.length()==0){
        return false;
      }

      // this is a topic with length >1
      if(topic.length()> 1){
        return true;
      }
    }
    // this is the null topic
    return false;
  }

  /**
   * checks the permission "permissionName" to this subject. OBS! this one will
   * only se if there are any permissions granted for all objects.
   *
   * @param event the event
   * @param securityManager the system securitymanager
   * @param action The action: subscribe or publish
   * @return true if the object is permitted, false otherwise
   */
  private boolean checkPermission(Event event,
                                  SecurityManager securityManager,
                                  String action) {
    try {
      TopicPermission permission = new TopicPermission(event.getTopic(), action);
      securityManager.checkPermission(permission);
      return true;
    } catch (AccessControlException e) {
      return false;
    }
  }

  /**
   * returns the security manager
   *
   * @return the security manager if any else null
   */
  private SecurityManager getSecurityManager() {
    // return the security manager
    return System.getSecurityManager();
  }

}
