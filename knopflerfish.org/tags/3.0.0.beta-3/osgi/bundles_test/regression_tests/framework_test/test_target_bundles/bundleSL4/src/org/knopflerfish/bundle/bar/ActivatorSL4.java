package org.knopflerfish.bundle.bar;

import org.knopflerfish.service.foo.FooService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ActivatorSL4
  implements BundleActivator, FooService
{

  public void foo() {
    System.out.println("bundleSL4: Doing foo");
  }

  public void start(BundleContext context)
    throws Exception
  {
    final ServiceRegistration registration
      = context.registerService(FooService.class.getName(), this, null);

    System.out.println("bundleSL4: Registered " + registration);
  }

  public void stop(BundleContext context) throws Exception {
    //unregister will be done automagically
  }
}
