/*
 * Copyright (c) 2011-2022, KNOPFLERFISH project
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
