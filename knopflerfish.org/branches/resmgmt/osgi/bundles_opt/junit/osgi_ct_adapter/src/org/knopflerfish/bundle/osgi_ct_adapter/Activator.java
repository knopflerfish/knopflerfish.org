/*
 * Created on Fri Jul 17 13:58:27 CEST 2009
 */
package org.knopflerfish.bundle.osgi_ct_adapter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class Activator implements BundleActivator, BundleTrackerCustomizer {
  BundleTracker bt;
  ServiceRegistration testListenerRegistration = null;

  public void start(final BundleContext bc) throws Exception {
    bt = new BundleTracker(bc, Bundle.ACTIVE, this);
    bt.open();

    TestListener tl = new TestListener() {
      public void addError(Test test, Throwable t) {
      }

      public void addFailure(Test test, AssertionFailedError t) {
      }

      public void endTest(Test test) {
      }

      public void startTest(Test test) {
        try {
          ServiceReference[] srs = bc.getServiceReferences(null, "(test.name=" + test.getClass().getName() + ")");
          if(srs == null || srs.length == 0) {
            return;
          }
          BundleContext targetContext = srs[0].getBundle().getBundleContext();
          java.lang.reflect.Method m = test.getClass().getMethod(
              "setBundleContext", new Class[] { BundleContext.class });
          m.invoke(test, new Object[] { targetContext });
        } catch (NoSuchMethodException e) {
          // Skip if no method is found
        } catch (Exception e) {
          System.err.println("Exception while setting bundle context for: " + test.getClass().getName());
          System.err.flush();
          e.printStackTrace();
        }
      }
    };

    testListenerRegistration = bc.registerService(TestListener.class.getName(),
        tl, null);
  }

  public void stop(BundleContext context) throws Exception {
    if (testListenerRegistration != null) {
      testListenerRegistration.unregister();
    }
    if (bt != null) {
      bt.close();
    }
  }

  public Object addingBundle(Bundle bundle, BundleEvent event) {
    return registerAllFoundTestCases(bundle);
  }

  public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
  }

  public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
    ((ServiceRegistration) object).unregister();
  }

  ServiceRegistration registerAllFoundTestCases(Bundle b) {
    try {
      List testNames = new ArrayList();
      String testCases = (String) b.getHeaders().get("Test-Cases");
      if (testCases != null) {
        int s = 0;
        int e;
        while ((e = testCases.indexOf(",", s)) >= 0) {
          testNames.add(testCases.substring(s, e).trim());
          s = e + 1;
        }
        testNames.add(testCases.substring(s).trim());
      }
      if (testNames.isEmpty()) {
        return null;
      }

      TestSuite ts = new TestSuite(b.getSymbolicName());
      for (Iterator i = testNames.iterator(); i.hasNext();) {
        String fqn = (String) i.next();
        int n = fqn.indexOf(':');
        if (n > 0) {
          String method = fqn.substring(n + 1);
          fqn = fqn.substring(0, n);
          Class clazz = b.loadClass(fqn);
          ts.addTest(TestSuite.createTest(clazz, method));
        } else {
          Class clazz = b.loadClass(fqn);
          ts.addTestSuite(clazz);
        }
      }
      if (ts == null) {
        return null;
      }
      Hashtable h = new Hashtable();
      h.put("service.pid", ts.getName());
      h.put("test.name", testNames);
      return b.getBundleContext().registerService(TestSuite.class.getName(),
          ts, h);
    } catch (Exception ignored) {
    }
    return null;
  }
}
