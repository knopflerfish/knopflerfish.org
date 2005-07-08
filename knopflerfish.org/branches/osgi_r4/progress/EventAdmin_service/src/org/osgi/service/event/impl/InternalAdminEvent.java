/*
 * Created on Jul 7, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.osgi.service.event.impl;

import java.util.Calendar;


public class InternalAdminEvent {
    /** the Object from external source */
    private Object element;
    /** the timeStamp */
    private Calendar timeStamp;
    /** the variable indicates if delivered or not **/
    private boolean isDelivered=false;
    /** variable holding the creator of the object */
    EventAdminService owner;
    
    public InternalAdminEvent (Object object,Calendar time,EventAdminService creator){
        /* assign the element */
        element=object;
        /* assign the time */
        timeStamp=time;
        /* assign the owner */
         owner = creator;
    }
    
    public Object getElement(){
       return element;
    }
    
    public Calendar getTimeStamp(){
        return timeStamp;
    }
    
    public synchronized boolean isDelivered(){
        return isDelivered;
    }
    
    public void setAsDelivered(){
        synchronized(owner){
            owner.notify();
        }
        
    
    }
}
