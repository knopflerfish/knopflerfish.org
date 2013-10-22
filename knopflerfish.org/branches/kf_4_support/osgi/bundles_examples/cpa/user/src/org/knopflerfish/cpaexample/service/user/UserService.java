package org.knopflerfish.cpaexample.service.user;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.service.log.LogService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

// Separate activator, interface and implementation classes
// should be used but to keep the number of source files
// to a minimum, only one class is used in this example
public class UserService implements BundleActivator {
  private static String fileName = "/tmp/osgiuser";
  
  private BundleContext bc;
  
  public void start(BundleContext bc) throws BundleException
  {
    this.bc = bc;
    bc.registerService(UserService.class.getName(), this, null);
  }

  public void stop(BundleContext context)
  {
  }
    
  public void login(final String name) {
    final File f = new File(fileName);

    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        if (f.exists()) {
          throw new IllegalStateException("User already logged in");
        }

        try {
          OutputStream os = new FileOutputStream(f);
          os.write(name.getBytes("UTF-8"));
          os.close();
          log(LogService.LOG_INFO, "User " + name + " logged in");
        } catch (IOException ioe) {
          log(LogService.LOG_WARNING, "Problem logging user in: " + ioe);
        }
        return null;
      }
    });
  }
  
  public void logout() {
    final File f = new File(fileName);

    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        if (!f.exists()) {
          throw new IllegalStateException("No user logged in");
        }
    
        f.delete();
        log(LogService.LOG_INFO, "User logged out");
        return null;
      }
    });
  }

  private void log(int level, String message)
  {
    ServiceReference sRef = 
      bc.getServiceReference(LogService.class.getName());
    if (sRef != null) {
      LogService log = (LogService) bc.getService(sRef);
      if (log != null) {
        log.log(level, message);
      }
      bc.ungetService(sRef);
    }
  }

}
