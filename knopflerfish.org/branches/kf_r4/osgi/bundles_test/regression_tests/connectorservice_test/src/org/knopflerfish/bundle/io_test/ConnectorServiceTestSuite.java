/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

package org.knopflerfish.bundle.io_test;

import java.io.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.io.*;

import org.knopflerfish.bundle.io_test.*;

import junit.framework.*;
import javax.microedition.io.*;

public class ConnectorServiceTestSuite extends TestSuite 
{
  private ConnectorService cs;
  private ServiceReference ioref;
  private BundleContext bc;
    
  public ConnectorServiceTestSuite(BundleContext bc) {
    super("ConnectorServiceTestSuite");
    this.bc = bc;
	
    try {
      Setup setup = new Setup();
		
      addTest(setup);
      if (!setup.giveUp) {
	addTest(new SimpleTest1());
	addTest(new SimpleTest2());
	addTest(new SimpleTest3());
	addTest(new SimpleTest4());
	addTest(new SimpleTest5());
      } 
	    
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
    
  /* from http_test */
  class FWTestCase extends TestCase {
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
    
  private class Setup extends FWTestCase {
    public boolean giveUp = false;
    public void runTest() {
      ioref = bc.getServiceReference(ConnectorService.class.getName());
      giveUp = (ioref == null);
      assertNotNull("Setup: no IO reference. Skipping rest of the tests", ioref);
      cs = (ConnectorService)bc.getService(ioref);
      giveUp = (cs == null);
      assertNotNull("Setup: no service object available. Skipping rest of the tests", cs);
    }
  }

  /* tries to open an existing resource.. */
  private class SimpleTest1 extends FWTestCase {
    public void runTest() {
      /* Ignore this one for now. Need to make sure that http 
	 and connector service are running 
	 try {
		
	 Connection test = cs.open("http://localhost:8080/");
	 assertNotNull("SimpleTest1: got null connection", test); 
	 } catch (Exception e) {
	 fail("SimpleTest1: got exception: " + e); 
	 } */
    }
  }

  /* tries exceptions  */
  private class SimpleTest2 extends FWTestCase {
    public void runTest() {
      try {
	Connection test1 = cs.open("annonexistingscheme://foobar/");
	fail("SimpleTest2: was able to open an nonexisting scheme");

      } catch (ConnectionNotFoundException e) { 
	/* this is excepted */ 
      } catch (Exception e) { 
	fail("SimpleTest2.0: got Exception:" + e);
      } 

      try {
	Connection test1 = cs.open("illegalscheme");
	fail("SimpleTest2: did not get IllegalArgumentException");

      } catch (IllegalArgumentException e) { 
	/* this is excepted */ 
      } catch (Exception e) { 
	fail("SimpleTest2.0: got Exception:" + e);
      } 

	    
    }
  }
    
       
  /* register a bunch of objects then remove them check that all are gone */
  private class SimpleTest3 extends FWTestCase {
    static final int TEST_SIZE = 10;
    static final String TEST_SCHEME = "connectorservicetestsuite";
    public void runTest() {
      ServiceRegistration srs[] = new ServiceRegistration[TEST_SIZE];
	    
      // add a number of services with the same scheme...
      for (int i = 0; i < TEST_SIZE; i++) { 
	ConnectionFactory fac = 
	  new PhonyConnectionFactory();
	Hashtable props = new Hashtable();
	props.put(ConnectionFactory.IO_SCHEME,
		  new String[] { TEST_SCHEME });
		
	srs[i] = bc.registerService(ConnectionFactory.class.getName(),
				    fac, props);
      } 
      // get one 
      try {
	cs.open(TEST_SCHEME + ":/foobar/"); 
      } catch(Exception e) {
	fail("SimpleTest3: got an exception " + e);
      }

      // clean up..
      for (int i = 0; i < TEST_SIZE; i++) {
	srs[i].unregister();
      }
	    
      // try get one again
      try {
	cs.open(TEST_SCHEME + ":/foobar/");
	fail("SimpleTest3: did not throw an exception");
      } catch (ConnectionNotFoundException e) {
	/* this is ok */ 
      } catch (Exception e) {
	fail("SimpleTest3: got exception " + e);
      }	
    }
  }

  /* hack: A useful help class that acts a connection and a factory */
  private class PhonyConnectionFactory 
    implements ConnectionFactory, Connection {
	
    public long id;
    public int rank;
	
    public void close() {}
	
    public Connection 
      createConnection(String uri, int mode, boolean timeouts) {
      return this;
    }
  }

  /* check that we choose the "right" ConnectionFactory */
  private class SimpleTest4 extends FWTestCase {
    private final int TEST_SIZE = 10;
    private final String TEST_SCHEME = "connectorservicetestsuite2";
	
    public void runTest() {

      Random random  = new Random();
      ServiceRegistration[] srs = new ServiceRegistration[TEST_SIZE];
	    
      PhonyConnectionFactory best = null;
	    

      // create a number of services all with the same scheme and randomly generated ranks.
      for (int i = 0; i < TEST_SIZE; i++) { 
		
	PhonyConnectionFactory fac = new PhonyConnectionFactory();
	Hashtable props = new Hashtable();
		
	fac.rank = random.nextInt(TEST_SIZE / 3);
		
	props.put(ConnectionFactory.IO_SCHEME, new String[] { TEST_SCHEME });
	props.put(Constants.SERVICE_RANKING, new Integer((int)fac.rank));
	srs[i] = bc.registerService(ConnectionFactory.class.getName(), fac, props);

	fac.id = ((Long)srs[i].getReference().getProperty(Constants.SERVICE_ID)).longValue();

	if (best == null) { 
	  best = fac;
	} else if (fac.rank > best.rank || 
		   (fac.rank == best.rank && fac.id < best.rank)) {
	  best = fac;
	}
      }
	    
      // try to get the service
      try {
	PhonyConnectionFactory fac = (PhonyConnectionFactory)cs.open(TEST_SCHEME + ":/foobar/");
	assertTrue("SimpleTest4: Did not get the best factory (rank="+ fac.rank +", id="+fac.id+ ")", 
		   fac == best);
      } catch (Exception e) {
	fail("SimpleTest4: got exception:" + e);
      }

      // clean up
      for (int i = 0; i < TEST_SIZE; i++) {
	srs[i].unregister();
      }
    }
  }


  /* tests case-insensitive schemes */
  private class SimpleTest5 extends FWTestCase {
    private final String TEST_SCHEME = "connectorservicetestsuite3";
	
    public void runTest() {
      PhonyConnectionFactory fac = new PhonyConnectionFactory();
      Hashtable props = new Hashtable();
      props.put(ConnectionFactory.IO_SCHEME, new String[]{ TEST_SCHEME });

      ServiceRegistration reg = 
	bc.registerService(ConnectionFactory.class.getName(), fac, props);
	    
      try {
		
	assertSame("SimpleTest5: Did not recieve same service.", 
		   cs.open(TEST_SCHEME + ":/foobar/"), 
		   cs.open(TEST_SCHEME.toUpperCase() + ":/foobar/"));
		
      } catch (Exception e) {
	fail("SimpleTest5: got exception " + e);
      }
		       
      reg.unregister();
    }
  }
}
