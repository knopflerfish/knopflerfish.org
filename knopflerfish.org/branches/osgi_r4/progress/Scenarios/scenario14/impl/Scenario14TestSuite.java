package com.gstm.test.eventadmin.scenarios.scenario14.impl;

import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This class will stresstest the eventAdmin service
 * and assert that it works during intensive
 * conditions and periods.
 * 
 * @author Magnus Klack
 *
 */
public class Scenario14TestSuite extends TestSuite {
	/** the bundle context */
	private BundleContext bundleContext;
	
	/** the display name */
	private String name;
	
	private Object semaphore= new Object(); 
	
	/**
	 * Constructor
	 * 
	 * @param context the bundle context of the instance creator
	 */
	public Scenario14TestSuite(BundleContext context){
		super("Scenario14 - Stress");
		
		bundleContext=context;
		addTest(new Setup());
		
		String[] topic={"com/acme/*"};
		addTest(new EventConsumer(bundleContext,topic ,"EventConsumer", 1));
		
		String[] topic2={"com/acme/timer/*"};
		addTest(new EventConsumer(bundleContext,topic ,"EventConsumer", 2));
		
		
		String[] topic3={"COM/acme/LOCAL"};
		addTest(new EventConsumer(bundleContext,topic ,"EventConsumer", 3));
		
		String[] topic4={"*"};
		addTest(new EventConsumer(bundleContext,topic ,"EventConsumer", 4));
		
		
		String[] topic5={"com/Roland/timer"};
		addTest(new EventConsumer(bundleContext,topic ,"EventConsumer", 5));
		
		String[] topic6={"/com/acme/timer"};
		addTest(new EventConsumer(bundleContext,topic ,"EventConsumer", 6));
		
		String[] topic7={"*"};
		addTest(new EventConsumer(bundleContext,topic ,"EventConsumer", 7));
		
		
		
		
		
	}
	

	
	
	
 /**
	 * Sets up neccessary environment
	 * 
	 * @author Magnus Klack
	 */
    class Setup extends TestCase {

    
    	
        public Setup(){
        
          
        }
    	public void runTest() throws Throwable {
    	 EventPublisher p1 = new EventPublisher(bundleContext,
					"com/acme/timer", "EventPublisher", 1);
    	 
			EventPublisher p2 = new EventPublisher(bundleContext,
					"com/acme/timer", "EventPublisher", 2);
			
			EventPublisher p3 = new EventPublisher(bundleContext,
					"com/acme/timer", "EventPublisher", 3);
			
			EventPublisher p4 = new EventPublisher(bundleContext,
					"com/Roland/timer", "EventPublisher", 4);
	
    		
			
			p1.start();
    		p2.start();
    		p3.start();
    		p4.start();
    	}
        public String getName() {
            String name = getClass().getName();
            int ix = name.lastIndexOf("$");
            if(ix == -1) {
              ix = name.lastIndexOf(".");
            }
            if(ix != -1) {
              name = name.substring(ix + 1);
            }
            return name;
          }
    }
  /**
     * Clean up the test suite
     * 
     * @author Magnus Klack
     */
    class Cleanup extends TestCase {
        public void runTest() throws Throwable {
            
        }
        public String getName() {
            String name = getClass().getName();
            int ix = name.lastIndexOf("$");
            if(ix == -1) {
              ix = name.lastIndexOf(".");
            }
            if(ix != -1) {
              name = name.substring(ix + 1);
            }
            return name;
          }
    }
	
	
	
	private class EventPublisher extends Thread  {
		/** A reference to a service */
        private ServiceReference serviceReference;

        /** The admin which delivers the events */
        private EventAdmin eventAdmin;
        
		/** the private bundle context */
		private BundleContext bundleContext;
		
		private String topicToPublish;
		
		/** the running variable */
		private boolean running=true;
		
		/** the id of the publisher */
		private int publisherId;
		
		
		public EventPublisher(BundleContext context,String topic,String name,int id){
			/* call super class */
			super(name + ":" + id);
	        /* assign bundleContext */
	        bundleContext = context;
	        /* assign the topic to publish */
	        topicToPublish = topic;
	        /* assign the publisherID */
	        publisherId = id;
	        
	        /* Claims the reference of the EventAdmin Service */
            serviceReference = bundleContext
                    .getServiceReference(EventAdmin.class.getName());

       
            /* get the service  */
            eventAdmin = (EventAdmin) bundleContext
                    .getService(serviceReference);
            
        
		}
		

		 public void run() {

			int i = 0;
			while (i<2000) {
				/* a Hash table to store message in */
				Hashtable message = new Hashtable();
				/* put some properties into the messages */
				message.put("message", new Integer(i));
				/* put the sender */
				message.put("FROM", this);

				
					if (publisherId <= 2) {
						/* send the message */
						eventAdmin
								.sendEvent(new Event(topicToPublish, message));
					} else if (publisherId > 2) {
						/* send the message */
						eventAdmin
								.postEvent(new Event(topicToPublish, message));

					}
				
				i++;

			}// end while..
		}
	    
		 
		
	}
	
	 
	
    
    /**
	 * Class consumes events
	 * 
	 * @author magnus
	 */
    class EventConsumer extends TestCase implements EventHandler {
        /** class variable for service registration */
        private ServiceRegistration serviceRegistration;

        /** class variable indicating the topics */
        private String[] topicsToConsume;
        
        /** class variable keeping number of asynchronus message */
        private int numOfasynchMessages=0;
        
        /** class variable keeping number of asynchronus message */
        private int numOfsynchMessages=0;
        
        /** class variable holding the old syncronus message nummber */
        private int synchMessageExpectedNumber=0;
        
        /** class variable holding the old asyncronus message nummber */
        private int asynchMessageExpectedNumber=0;
        
        /**
         * Constructor creates a consumer service
         * 
         * @param bundleContext
         * @param topics
         */
        public EventConsumer(BundleContext bundleContext, String[] topics,
                String name, int id) {
            /* call super class */
            super(name + ":" + id);
            /* assign the consume topics */
            topicsToConsume = topics;
             
        }
       
        /**
         * run the test
         */
        public void runTest() throws Throwable {
            /* create the hashtable to put properties in */
            Hashtable props = new Hashtable();
            /* put service.pid property in hashtable */
            props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
            /* register the service */
            serviceRegistration = bundleContext.registerService(
                    EventHandler.class.getName(), this, props);   
            
            
            assertNotNull(getName()
                    + " service registration should not be null",
                    serviceRegistration);

            if (serviceRegistration == null) {
                fail("Could not get Service Registration ");
            }

        }
        
       
        /**
         * This method takes events from the event admin service.
         */
        public void handleEvent(Event event) {
        	//System.out.println("****************************  RECEVIED ***********************************");
        	
        	//String from = (String)event.getProperty("FROM");
        	System.err.println(getName() + " recived an event  with topic:"+ event.getTopic());
           
           
        }

    }
	

}
