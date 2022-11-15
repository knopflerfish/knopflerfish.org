package org.knopflerfish.cpaexample.service.user;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.knopflerfish.service.log.LogRef;

// Separate activator, interface and implementation classes
// should be used but to keep the number of source files
// to a minimum, only one class is used in this example
public class UserService implements BundleActivator {
  private static String fileName = "/tmp/osgiuser";
  
  private LogRef log;
  
  public void start(BundleContext bc) {
    log = new LogRef(bc);
    bc.registerService(UserService.class.getName(), this, null);
  }

  public void stop(BundleContext context) {
  }
    
  public void login(final String name) {
    final File f = new File(fileName);

    AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
      if (f.exists()) {
        throw new IllegalStateException("User already logged in");
      }

      try {
        OutputStream os = new FileOutputStream(f);
        os.write(name.getBytes(StandardCharsets.UTF_8));
        os.close();
        log.info("User " + name + " logged in");
      } catch (IOException ioe) {
        log.warn("Problem logging user in: " + ioe);
      }
      return null;
    });
  }
  
  public void logout() {
    final File f = new File(fileName);

    AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
      if (!f.exists()) {
        throw new IllegalStateException("No user logged in");
      }

      if (f.delete()) {
        log.info("User logged out");
      } else {
        log.warn("Problem logging user out");
      }
      return null;
    });
  }

}
