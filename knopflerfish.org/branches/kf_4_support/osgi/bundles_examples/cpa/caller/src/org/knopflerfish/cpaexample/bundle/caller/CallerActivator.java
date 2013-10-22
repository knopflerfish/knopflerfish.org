package org.knopflerfish.cpaexample.bundle.caller;

import java.io.IOException;

import org.knopflerfish.cpaexample.service.user.UserService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


public class CallerActivator implements BundleActivator {
  public void start(BundleContext bc) throws IOException
  {
    ServiceReference sRef =
      bc.getServiceReference(UserService.class.getName());
    if (sRef != null) {
      UserService us = (UserService) bc.getService(sRef);
      if (us != null) {
        us.login("joek");
      }
      bc.ungetService(sRef);
    }
  }

  public void stop(BundleContext bc) throws IOException
  {
    ServiceReference sRef =
      bc.getServiceReference(UserService.class.getName());
    if (sRef != null) {
      UserService us = (UserService) bc.getService(sRef);
      if (us != null) {
        us.logout();
      }
      bc.ungetService(sRef);
    }
  }

}
