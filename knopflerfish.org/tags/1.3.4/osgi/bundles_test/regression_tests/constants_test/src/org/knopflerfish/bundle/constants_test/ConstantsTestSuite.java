/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.constants_test;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;

import junit.framework.*;

/**
 * Check that framework constants are present and have correct values.
 *
 * Tests are done using reflection on static class members to avoid
 * compiler optimizations. (plain "==" comparisons will in most cases
 * lead to a compile time "true")
 */
public class ConstantsTestSuite extends TestSuite {
  BundleContext bc;

  PrintStream out = System.out;

  public ConstantsTestSuite(BundleContext bc) {
    super ("ConstantsTestSuite");

    this.bc = bc;

    addTest(new Setup());
    addTest(new Frame0300a());
    addTest(new Frame0310a());
    addTest(new Frame0320a());
    addTest(new Frame0330a());
    addTest(new Cleanup());
  }
 

  // Also install all possible listeners
  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
    }
  }

  // Test Framework.class constants
  class Frame0300a extends ReflectTestCase {
    public void runTest() throws Throwable {
      assertMemberEquals("Bundle-Activator", 
			 Constants.class, "BUNDLE_ACTIVATOR");
      assertMemberEquals("Bundle-Category",  
			 Constants.class, "BUNDLE_CATEGORY");
      assertMemberEquals("Bundle-ClassPath",
			 Constants.class, "BUNDLE_CLASSPATH");
      assertMemberEquals("Bundle-ContactAddress", 
			 Constants.class, "BUNDLE_CONTACTADDRESS");
      assertMemberEquals("Bundle-Copyright", 
			 Constants.class, "BUNDLE_COPYRIGHT");
      assertMemberEquals("Bundle-Description",
			 Constants.class, "BUNDLE_DESCRIPTION");
      assertMemberEquals("Bundle-DocURL",  
			 Constants.class, "BUNDLE_DOCURL");
      assertMemberEquals("Bundle-Name",  
			 Constants.class, "BUNDLE_NAME");
      assertMemberEquals("Bundle-NativeCode",
			 Constants.class, "BUNDLE_NATIVECODE");
      assertMemberEquals("language", 
			 Constants.class, "BUNDLE_NATIVECODE_LANGUAGE");
      assertMemberEquals("osname", 
			 Constants.class, "BUNDLE_NATIVECODE_OSNAME");
      assertMemberEquals("osversion", 
			 Constants.class, "BUNDLE_NATIVECODE_OSVERSION");
      assertMemberEquals("processor", 
			 Constants.class, "BUNDLE_NATIVECODE_PROCESSOR");
      assertMemberEquals("Bundle-UpdateLocation",
			 Constants.class, "BUNDLE_UPDATELOCATION");
      assertMemberEquals("Bundle-Vendor", 
			 Constants.class, "BUNDLE_VENDOR");
      assertMemberEquals("Bundle-Version", 
			 Constants.class, "BUNDLE_VERSION");
      assertMemberEquals("Export-Package", 
			 Constants.class, "EXPORT_PACKAGE");
      assertMemberEquals("Export-Service", 
			 Constants.class, "EXPORT_SERVICE");
      assertMemberEquals("org.osgi.framework.language", 
			 Constants.class, "FRAMEWORK_LANGUAGE");
      assertMemberEquals("org.osgi.framework.os.name", 
			 Constants.class, "FRAMEWORK_OS_NAME");
      assertMemberEquals("org.osgi.framework.os.version", 
			 Constants.class, "FRAMEWORK_OS_VERSION");
      assertMemberEquals("org.osgi.framework.processor", 
			 Constants.class, "FRAMEWORK_PROCESSOR");
      assertMemberEquals("org.osgi.framework.vendor", 
			 Constants.class, 
			 "FRAMEWORK_VENDOR");
      assertMemberEquals("org.osgi.framework.version", 
			 Constants.class, 
			 "FRAMEWORK_VERSION");
      assertMemberEquals("Import-Package", 
			 Constants.class, "IMPORT_PACKAGE");
      assertMemberEquals("Import-Service", 
			 Constants.class, "IMPORT_SERVICE");
      assertMemberEquals("objectClass", 
			 Constants.class, "OBJECTCLASS");
      assertMemberEquals("specification-version",
			 Constants.class, "PACKAGE_SPECIFICATION_VERSION");
      assertMemberEquals("service.description", 
			 Constants.class, "SERVICE_DESCRIPTION");
      assertMemberEquals("service.id", 
			 Constants.class, "SERVICE_ID");
      assertMemberEquals("service.pid", 
			 Constants.class, "SERVICE_PID");
      assertMemberEquals("service.ranking", 
			 Constants.class, "SERVICE_RANKING");
      assertMemberEquals("service.vendor", 
			 Constants.class, "SERVICE_VENDOR");
      assertMemberEquals("System Bundle", 
			 Constants.class, "SYSTEM_BUNDLE_LOCATION");
    }
  }
    
  // Test Bundle.class constants
  class Frame0310a extends ReflectTestCase {
    public void runTest() throws Throwable {
      assertMemberEquals(0x00000001, Bundle.class, "UNINSTALLED");
      assertMemberEquals(0x00000002, Bundle.class, "INSTALLED");
      assertMemberEquals(0x00000004, Bundle.class, "RESOLVED");
      assertMemberEquals(0x00000008, Bundle.class, "STARTING");
      assertMemberEquals(0x00000010, Bundle.class, "STOPPING");
      assertMemberEquals(0x00000020, Bundle.class, "ACTIVE");
    } 
  }

  // Test  BundleEvent.class constants
  class Frame0320a extends ReflectTestCase {
    public void runTest() throws Throwable {
      assertMemberEquals(0x00000001, BundleEvent.class, "INSTALLED");
      assertMemberEquals(0x00000002, BundleEvent.class, "STARTED");
      assertMemberEquals(0x00000004, BundleEvent.class, "STOPPED");
      assertMemberEquals(0x00000010, BundleEvent.class, "UNINSTALLED");
      assertMemberEquals(0x00000008, BundleEvent.class, "UPDATED");
    }
  }
    
  // Test  BundleEvent.class constants
  class Frame0330a extends ReflectTestCase {
    public void runTest() throws Throwable {
      assertMemberEquals(0x00000001, FrameworkEvent.class, "STARTED");
      assertMemberEquals(0x00000002, FrameworkEvent.class, "ERROR");
      assertMemberEquals(0x00000004, FrameworkEvent.class, "PACKAGES_REFRESHED");

    }
  }
  
  // Test PackagePermission.class constants
  class Frame0340a extends ReflectTestCase {
    public void runTest() throws Throwable {
      assertMemberEquals("export", PackagePermission.class, "EXPORT");
      assertMemberEquals("import", PackagePermission.class, "IMPORT");
    }
  }

  class ReflectTestCase extends FWTestCase {

    /**
     * Assert that a static int member variable in a class is
     * public, static final and has a given value.
     */
    void assertMemberEquals(int val, Class clazz, String member) throws Throwable {
      Field field = null;
      try {
	field = clazz.getDeclaredField(member);
      } catch (NoSuchFieldException e) {
	fail("No member '" + member + "' in " + clazz.getName());
      }

      int mods = field.getModifiers();
      
      assertTrue(clazz.getName() + "." + member + " must be public",
		 Modifier.isPublic(mods));
      
      assertTrue(clazz.getName() + "." + member + " must be static",
		 Modifier.isStatic(mods));
      
      assertTrue(clazz.getName() + "." + member + " must be final",
		 Modifier.isFinal(mods));
      
      assertTrue(clazz.getName() + "." + member + " must be int, is " + field.getType(), 
		 field.getType() == Integer.TYPE);
      
      int actual = field.getInt(null);
      assertEquals(clazz.getName() + "." + member + " must have value " + val + ", has " + actual,
		   val, actual);
      
    }
    
    /**
     * Assert that a static string member variable in a class is
     * public, static final and has a given value.
     */
    void assertMemberEquals(String val, Class clazz, String member) throws Throwable {
      
      Field field = null;
      try {
	field = clazz.getDeclaredField(member);
      } catch (NoSuchFieldException e) {
	fail("No member '" + member + "' in " + clazz.getName());
      }

      int mods = field.getModifiers();
      
      assertTrue(clazz.getName() + "." + member + " must be public",
		 Modifier.isPublic(mods));
      
      assertTrue(clazz.getName() + "." + member + " must be static",
		 Modifier.isStatic(mods));
      
      assertTrue(clazz.getName() + "." + member + " must be final",
		 Modifier.isFinal(mods));
      
      assertTrue(clazz.getName() + "." + member + " must be int, is " + field.getType(), 
		 field.getType() == String.class);
      
      String actual = (String)field.get(null);
      assertEquals(clazz.getName() + "." + member + " must have value " + val + ", has " + actual,
		   val, actual);
      
    }
  }
}
