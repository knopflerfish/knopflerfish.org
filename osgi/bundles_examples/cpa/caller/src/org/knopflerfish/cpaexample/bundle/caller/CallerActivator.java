package org.knopflerfish.cpaexample.bundle.caller;

import org.knopflerfish.cpaexample.service.user.UserService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("unused")
public class CallerActivator implements BundleActivator {
  public void start(BundleContext bc) {
    ServiceReference<UserService> sRef =
        bc.getServiceReference(UserService.class);
    if (sRef != null) {
      UserService us = bc.getService(sRef);
      if (us != null) {
        us.login("joek");
      }
      bc.ungetService(sRef);
    }
  }

  public void stop(BundleContext bc) {
    ServiceReference<UserService> sRef =
      bc.getServiceReference(UserService.class);
    if (sRef != null) {
      UserService us = bc.getService(sRef);
      if (us != null) {
        us.logout();
      }
      bc.ungetService(sRef);
    }
  }

}
