/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

import java.security.AccessControlException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.TopicPermission;

/**
 * A wrapper class for events. Connects an event with ServiceReferences to
 * the EventHandlers it should be delivered to.
 *
 * @author Magnus Klack (refactoring by Björn Andersson)
 */
public class InternalAdminEvent {

  private Event event;

  private ServiceReference[] references;

  /**
   * Standard constructor of the InternalAdminEvent
   * @param event the event to be stored
   * @param references ServiceReference to the EventHandlers this event should be
   *                   delivered to.
   */
  public InternalAdminEvent(Event event, ServiceReference[] references){
    this.event = event;
    this.references = references;
  }

  /**
   * Returns the event
   * @return the event
   */
  protected Event getEvent(){
    return event;
  }

  public ServiceReference[] getReferences() {
    return references;
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
