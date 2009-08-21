/*
 * Copyright (c) 2007-2009 KNOPFLERFISH project
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

package org.knopflerfish.bundle.framework_test;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;
import org.knopflerfish.service.framework_test.*;

import org.osgi.service.packageadmin.*;
import org.osgi.service.permissionadmin.*;

import junit.framework.*;

public class ServiceListenerTestSuite
  extends TestSuite
  implements FrameworkTest
{
  BundleContext bc;
  Bundle bu;

  // A bundle that publishes one service, used to trigger the service
  // listener events.
  Bundle buA;
  Bundle buA2;

  PrintStream out = System.out;
  PrintStream err = System.err;

  final static String TRUE   = "true";
  final static String FALSE  = "false";

  // If set to true, then during the UNREGISTERING event the Listener
  // can use the ServiceReference to receive an instance of the service.
  final static boolean UNREGISTERSERVICE_VALID_DURING_UNREGISTERING
    = TRUE.equals(System.getProperty
           ("org.knopflerfish.servicereference.valid.during.unregistering",
            TRUE));


  public ServiceListenerTestSuite (BundleContext bc)
  {
    super("ServiceListenerTestSuite");
    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new FrameSL05a());
    addTest(new FrameSL10a());
    addTest(new Cleanup());
  }


  class FWTestCase
    extends TestCase
  {
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

  class Setup
    extends FWTestCase
  {
    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "bundleA_test-1.0.0.jar");
      assertNotNull(buA);
      buA2 = Util.installBundle(bc, "bundleA2_test-1.0.0.jar");
      assertNotNull(buA2);
      out.println("org.knopflerfish.servicereference.valid.during.unregistering"
                  +" is " +UNREGISTERSERVICE_VALID_DURING_UNREGISTERING);
      out.println("### ServiceListenerTestSuite :SETUP:PASS");
    }
  }

  class Cleanup
    extends FWTestCase
  {
    public void runTest() throws Throwable {
      Bundle[] bundles = new Bundle[] {
        buA,
        buA2,
      };
      for(int i = 0; i < bundles.length; i++) {
        try {  bundles[i].uninstall();  }
        catch (Exception ignored) { }
      }

      buA = null;
      buA2 = null;
    }
  }

  public final static String [] HELP_FRAMESL05A =  {
    "Checks that the correct service events",
    "are sent to a registered service listener.",
    "Case where the bundle does not unregisters its",
    "service in the stop()-method."
  };

  class FrameSL05a
    extends FWTestCase
  {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      int cnt = 1;

      teststatus = runStartStopTest( "FRAMEsl05A", cnt, buA,
                                     new int[]{
                                       ServiceEvent.REGISTERED,
                                       ServiceEvent.UNREGISTERING,
                                     },
                                     new int[]{
                                       ServiceEvent.REGISTERED,
                                       ServiceEvent.UNREGISTERING,
                                     } );

      if (teststatus == true) {
        out.println("### ServiceListenerTestsuite :FRAMEsl05A:PASS");
      }
      else {
        fail("### ServiceListenerTestsuite :FRAMEsl05A:FAIL");
      }
    }
  }


  public final static String [] HELP_FRAMESL10A =  {
    "Checks that the correct service events",
    "are sent to a registered service listener.",
    "Case where the bundle unregisters its service",
    "in the stop()-method."
  };


  class FrameSL10a
    extends FWTestCase
  {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      int cnt = 1;

      teststatus = runStartStopTest( "FRAMEsl10A", cnt, buA2,
                                     new int[]{
                                       ServiceEvent.REGISTERED,
                                       ServiceEvent.UNREGISTERING,
                                     },
                                     new int[]{
                                       ServiceEvent.REGISTERED,
                                       ServiceEvent.UNREGISTERING,
                                     });


      if (teststatus == true) {
        out.println("### ServiceListenerTestsuite :FRAMEsl10A:PASS");
      }
      else {
        fail("### ServiceListenerTestsuite :FRAMEsl10A:FAIL");
      }
    }
  }



  boolean runStartStopTest( String tcName,
                            int cnt,
                            Bundle targetBundle,
                            int[] eventsA,
                            int[] events)
  {
    boolean teststatus = true;

    for (int i = 0; i < cnt && teststatus; i++) {
      ServiceListener sListen = new ServiceListener();
      try {
        bc.addServiceListener(sListen);
      } catch (IllegalStateException ise) {
        teststatus  = false;
        err.println("service listener registration failed "+ ise
                    + " :" +tcName +":FAIL");
      }
      AllServiceListener asListen = new AllServiceListener();
      try {
        bc.addServiceListener(asListen);
      } catch (IllegalStateException ise) {
        teststatus  = false;
        err.println("all service listener registration failed "+ ise
                    + " :" +tcName +":FAIL");
      }

      // Start the test target to get a service published.
      try {
        out.println("Starting targetBundle: "+targetBundle);
        targetBundle.start();
      } catch (BundleException bex) {
        teststatus  = false;
        err.println("Failed to start bundle, got exception: "
                    + bex.getNestedException()
                    + " in " +tcName +":FAIL");
      } catch (Exception e) {
        teststatus  = false;
        e.printStackTrace(err);
        err.println("Failed to start bundle, got exception "
                    + e + " + in " +tcName +":FAIL");
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception ex) {
        out.println("### framework test bundle :" +tcName +" exception");
        ex.printStackTrace(out);
      }

      // Stop the test target to get a service unpublished.
      try {
        targetBundle.stop();
      } catch (BundleException bex) {
        teststatus  = false;
        err.println("Failed to stop bundle, got exception: "
                    + bex.getNestedException()
                    + " in " +tcName +":FAIL");
      } catch (Exception e) {
        teststatus  = false;
        e.printStackTrace(err);
        err.println("Failed to stop bundle, got exception "
                    + e + " + in " +tcName +":FAIL");
      }

      if ( teststatus && !sListen.checkEvents(events)) {
        teststatus  = false;
        err.println("Service listener event notification error :"
                    +tcName +":FAIL");
      }

      if ( teststatus && !asListen.checkEvents(eventsA)) {
        teststatus  = false;
        err.println("All service listener event notification error :"
                    +tcName +":FAIL");
      }

      try {
        bc.removeServiceListener(sListen);
        teststatus &= sListen.teststatus;
        sListen.clearEvents();
      } catch (IllegalStateException ise) {
        teststatus  = false;
        err.println("service listener removal failed "+ ise
                    + " :" +tcName +":FAIL");
      }
      try {
        bc.removeServiceListener(asListen);
        teststatus &= asListen.teststatus;
        asListen.clearEvents();
      } catch (IllegalStateException ise) {
        teststatus  = false;
        err.println("all service listener removal failed "+ ise
                    + " :" +tcName +":FAIL");
      }
    }
    return teststatus;
  }


  class ServiceListener
    implements org.osgi.framework.ServiceListener
  {
    ArrayList events = new ArrayList(10);
    boolean teststatus = true;

    public void serviceChanged(ServiceEvent evt) {
      events.add(evt);
      out.println("ServiceEvent: " +toString(evt) );
      if (ServiceEvent.UNREGISTERING==evt.getType()) {
        ServiceReference sr = evt.getServiceReference();

        // Validate that no bundle is marked as using the service
        Bundle[] usingBundles = sr.getUsingBundles();
        if (null!=usingBundles) {
          teststatus = false;
          printUsingBundles(sr,"Using bundles (unreg) should be null but is: ");
        }

        // Check if the service can be fetched
        Object service = bc.getService(sr);
        usingBundles = sr.getUsingBundles();
        if (UNREGISTERSERVICE_VALID_DURING_UNREGISTERING) {
          // In this mode the service shall be obtainable during
          // unregistration.
          if (null==service) {
            teststatus = false;
            out.print("Service should be available to ServiceListener "
                      +"while handling unregistering event.");
          }
          out.println("Service (unreg): " +service);
          if (usingBundles.length!=1) {
            teststatus = false;
            printUsingBundles(sr,
                              "One using bundle expected "
                              +"(unreg, after getService), found: ");
          } else {
            printUsingBundles(sr, "Using bundles (unreg, after getService): ");
          }
        } else {
          // In this mode the service shall NOT be obtainable during
          // unregistration.
          if (null!=service) {
            teststatus = false;
            out.print("Service should not be available to ServiceListener "
                      +"while handling unregistering event.");
          }
          if (null!=usingBundles) {
            teststatus = false;
            printUsingBundles(sr,
                              "Using bundles (unreg, after getService), "
                              +"should be null but is: ");
          } else {
            printUsingBundles(sr,
                              "Using bundles (unreg, after getService): null");
          }
        }
        bc.ungetService(sr);

        // Check that the UNREGISTERING service can not be looked up
        // using the service registry.
        try {
          Long sid = (Long)sr.getProperty(Constants.SERVICE_ID);
          String sidFilter = "(" +Constants.SERVICE_ID +"=" +sid +")";
          ServiceReference[] srs = bc.getServiceReferences(null, sidFilter);
          if (null==srs || 0==srs.length) {
            out.println("ServiceReference for UNREGISTERING service is not"
                        +" found in the service registry; ok.");
          } else {
            teststatus = false;
            out.println("*** ServiceReference for UNREGISTERING"
                        +" service, "
                        + sr
                        +", not found in the service registry; fail.");
            out.print("Found the following Service references: ");
            for (int i=0; null!=srs && i<srs.length; i++) {
              if (i>0) out.print(", ");
              out.print(srs[i]);
            }
            out.println();
          }
        } catch (Throwable t) {
          teststatus = false;
          out.println("*** Unexpected excpetion when trying to lookup a"
                      +" service while it is in state UNREGISTERING; "
                      +t);
          t.printStackTrace(out);
        }
      }
    }

    void clearEvents()
    {
      events.clear();
    }

    boolean checkEvents(int[] eventTypes)
    {
      if (events.size() != eventTypes.length) {
        dumpEvents(eventTypes);
        return false;
      }

      for (int i=0; i<eventTypes.length; i++) {
        ServiceEvent evt = (ServiceEvent) events.get(i);
        if (eventTypes[i] != evt.getType() ) {
          dumpEvents(eventTypes);
          return false;
        }
      }
      return true;
    }

    private void printUsingBundles(ServiceReference sr, String caption)
    {
      Bundle[] usingBundles = sr.getUsingBundles();

      out.print(caption!=null ? caption : "Using bundles: ");
      for (int i=0; usingBundles!=null && i<usingBundles.length; i++) {
        if (i>0) out.print(", ");
        out.print(usingBundles[i]);
      }
      out.println();
    }

    String toStringEventType(int eventType)
    {
      String res = String.valueOf(eventType);

      switch (eventType) {
      case ServiceEvent.REGISTERED:    res +=" (REGISTERED)    "; break;
      case ServiceEvent.MODIFIED:      res +=" (MODIFIED)      "; break;
      case ServiceEvent.UNREGISTERING: res +=" (UNREGISTERING) "; break;
      default:                         res +=" (?)             ";
      }
      return res;
    }

    String toString(ServiceEvent evt)
    {
      if (null==evt) return " - null - ";

      ServiceReference sr = evt.getServiceReference();
      String[] objectClasses = (String[]) sr.getProperty(Constants.OBJECTCLASS);
      String res = toStringEventType(evt.getType());

      for (int i=0; i<objectClasses.length; i++){
        if (i>0) res += ", ";
        res += objectClasses[i];
      }
      return res;
    }

    // Print expected and actual service events.
    void dumpEvents(int[] eventTypes)
    {
      int max = events.size()> eventTypes.length
        ? events.size() : eventTypes.length;
      out.println("Expected event type --  Actual event");
      for (int i=0; i<max; i++) {
        ServiceEvent evt
          = i<events.size() ? (ServiceEvent) events.get(i) : null;
        out.println(" "
                    +(i<eventTypes.length
                      ? toStringEventType(eventTypes[i]) : "- NONE - ")
                    +" -- " +toString(evt) );
      }
    }

  } // end of class ServiceListener

  // A servicelistener that will be notified about all Services, not
  // only assignable ones.
  class AllServiceListener
    extends ServiceListener
    implements org.osgi.framework.AllServiceListener
  {
  }
}
