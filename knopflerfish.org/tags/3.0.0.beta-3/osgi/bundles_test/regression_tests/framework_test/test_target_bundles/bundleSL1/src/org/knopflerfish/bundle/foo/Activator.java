package org.knopflerfish.bundle.foo;

import org.knopflerfish.service.foo.FooService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator
  implements BundleActivator, ServiceTrackerCustomizer
{
  private ServiceTracker tracker;
  private BundleContext context;
  public boolean serviceAdded   = false;
  public boolean serviceRemoved = false;

  public void start(BundleContext context) throws Exception {
    this.context = context;

    context.registerService(this.getClass().getName(), this, null);

    tracker = new ServiceTracker(context, FooService.class.getName(), this);
    tracker.open();
  }

  public void stop(BundleContext context) throws Exception {
    tracker.close();
  }

  public Object addingService(ServiceReference reference) {
    serviceAdded = true;
    System.out.println("Adding reference = " + reference);

    FooService fooService = (FooService)context.getService(reference);
    fooService.foo();
    return fooService;
  }

  public void modifiedService(ServiceReference reference, Object service) {
  }

  public void removedService(ServiceReference reference, Object service) {
    serviceRemoved = true;
    System.out.println("Removing reference = " + reference);
  }
}
