package org.knopflerfish.tutorial.simplebundle.impl; 

import org.osgi.framework.BundleActivator; 
import org.osgi.framework.BundleContext; 

/** 
 * @author Sven Haiges | sven.haiges@vodafone.com 
 */ 
public class Activator implements BundleActivator { 
  public static BundleContext bc = null; 
  
  
  private HelloWorldThread thread = null; 

  public void start(BundleContext bc) throws Exception { 
    System.out.println("SimpleBundle starting..."); 
    Activator.bc = bc; 
    thread = new HelloWorldThread(); 
    thread.start(); 
  } 
  
  public void stop(BundleContext bc) throws Exception { 
    System.out.println("SimpleBundle stopping..."); 
    thread.stopThread(); 
    thread.join(); 
  } 
}
