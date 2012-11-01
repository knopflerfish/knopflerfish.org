package org.knopflerfish.tutorial.simplebundle.impl; 
/** 
 * @author Sven Haiges | sven.haiges@vodafone.com
 * @author Erik Wistrand
 */ 
public class HelloWorldThread extends Thread { 
  private boolean running = true; 

  public HelloWorldThread() { 
    super("Hello World thread");
  } 

  public void run() { 
    while (running) { 
      System.out.println("Hello World!"); 
      try { 
        Thread.sleep(5000); 
      } catch (InterruptedException e) { 
        System.out.println("HelloWorldThread ERROR: " + e); 
      } 
    } 
    System.out.println("thread stopped"); 
  }
 
  public void stopThread() { 
    System.out.println("stopping thread"); 
    this.running = false; 
  } 
} 
