package org.knopflerfish.bundle.event;

import java.util.Calendar;

/**
 * A wrapper class for events. Connects an event with a timestamp and a boolean which'
 *  is used to determine whether an event has been delivered or not.
 *
 * @author Magnus Klack
 */
public class InternalAdminEvent {
    /** the Object from external source */
    private Object element;
    /** the timeStamp */
    private Calendar timeStamp;
    /** the variable indicates if delivered or not **/
    private boolean isDelivered=false;
    /** variable holding the creator of the object */
    EventAdminService owner;
    /**
     * Standard constructor of the InternalAdminEvent
     * @param object the event to be stored, most likely either a BundleEvent, LogEntry, ServiceEvent
     * or FrameworkEvent
     * @param time The timestamp of this event
     * @param creator A handle to the admin service
     */
    public InternalAdminEvent (Object object,Calendar time,EventAdminService creator){
        /* assign the element */
        element=object;
        /* assign the time */
        timeStamp=time;
        /* assign the owner */
         owner = creator;
    }

    /**
     * Returns the event
     * @return the event
     */
    protected Object getElement(){
       return element;
    }

    /**
     * Returns the timestamp
     * @return the time at which the event arrived
     */
    protected Calendar getTimeStamp(){
        return timeStamp;
    }

    /**
     * Returns the boolean illustrating whether the event has been delivered or not.
     * @return true if the event has been delivered, false otherwise
     */
    protected synchronized boolean isDelivered(){
        return isDelivered;
    }

    /**
     * Notifies the owner that the even has been delivered.
     */
    protected void setAsDelivered(){
        synchronized(owner){
            owner.notify();
        }
    }
}
