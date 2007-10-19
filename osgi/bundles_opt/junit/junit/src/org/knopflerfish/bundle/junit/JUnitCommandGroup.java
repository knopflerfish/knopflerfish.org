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

package org.knopflerfish.bundle.junit;

import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import org.knopflerfish.service.console.*;
import org.knopflerfish.service.junit.*;
import junit.framework.*;

public class JUnitCommandGroup extends CommandGroupAdapter
{
  private   BundleContext           bc    = null;
  protected ServiceRegistration     reg   = null;

  protected ServiceTracker junitTracker;

  public JUnitCommandGroup(BundleContext bc) {
    super("junit", "JUnit test commands");
    this.bc  = bc;

    junitTracker = new ServiceTracker(Activator.bc, 
				      JUnitService.class.getName(),
				      null);
    junitTracker.open();

  }


  
  JUnitService getJUnitService() {
    JUnitService ju = (JUnitService)junitTracker.getService();
    if(ju == null) {
      throw new RuntimeException("No JUnitService available");
    }
    return ju;
  }
  
  public void register() {
    if(reg == null) {
      Hashtable props = new Hashtable();
      props.put("groupName", getGroupName());
      reg = bc.registerService(CommandGroup.class.getName(),
			       this,
			       props);
    }
  }

  void unregister() {
    if(reg != null) {
      reg.unregister();
      reg = null;
    }
  }

  public final static String USAGE_LIST = "";
  public final static String [] HELP_LIST = new String [] {
    "List available tests",
  };
  
  public int cmdList(Dictionary opts, Reader in, PrintWriter out, Session session) {
    try {
      String filter =  
	"(|" + 
	"(objectclass=" + Test.class.getName() + ")" + 
	"(objectclass=" + TestSuite.class.getName() + ")" + 
	"(objectclass=" + TestCase.class.getName() + ")" + 
	")";
      
      ServiceReference[] srl = 
	Activator.bc.getServiceReferences(null, filter);
      
      if(srl == null || srl.length == 0) {
	out.println("No Test services found");
      } else {
        out.println("Found " + srl.length + " tests");
      }
      for(int i = 0; srl != null && i < srl.length; i++) {
	Object obj = Activator.bc.getService(srl[i]);
	if(obj instanceof Test) {
	  String id   = (String)srl[i].getProperty("service.pid");
	  String desc = (String)srl[i].getProperty("service.description");
	  
	  out.print(" " + (i + 1) + ": " + id);
	  if(desc != null && !"".equals(desc)) {
	    out.print(" - " + desc);
	  }
	  out.println("");
	}
	Activator.bc.ungetService(srl[i]);
      }
    } catch (Exception e) {
      e.printStackTrace(out);
    }

    return 0;
  }
  

  public final static String USAGE_RUN = "[-out #file#] <id>";
  public final static String [] HELP_RUN = new String [] {
    "Run a test and dump XML results to a file or console.",
    " id            -   service.pid of registered test",
    " -out #file#   -   optional file name of destionation file.",
    "                   If not set, print to console output.",
  };
  
  public int cmdRun(Dictionary opts, Reader in, PrintWriter out, Session session) {
    String id      = (String)opts.get("id");
    String subid   = (String)opts.get("-subid");
    String outName = (String)opts.get("-out");
    
    PrintWriter pw = out;

    if(outName != null) {
      try {
	File file = new File(outName);
	pw = new PrintWriter(new FileOutputStream(file));
      } catch (Exception e) {
	e.printStackTrace(out);
      } 
    }

    try {
      TestSuite suite = getJUnitService().getTestSuite(id, subid); 
      getJUnitService().runTest(pw, suite);
    } catch (Exception e) {
      e.printStackTrace(out);
    }

    if(pw != out) {
      try { pw.close(); }  catch (Exception ignored) { }
    }
    return 0;
  }
  
  /**
   * Wrap a PrintWriter into a PrintStream by overriding all methods.
   */
  public static class PrintWriterStream extends PrintStream {
    PrintWriter pw;
    boolean     bClose = false;
    
    /**
     * @param pw underlying writer to which all data is send to
     * @param bClose if <tt>true</tt> close the underlying writer
     *               when <tt>PrintWriterStream.close()</tt> is called.
     */
    public PrintWriterStream(PrintWriter pw, boolean bClose) {
      super(new ByteArrayOutputStream()); // This is really a dummy stream
      this.pw     = pw;
      this.bClose = bClose;
    }
    
    /**
     * Same as <tt>PrintWriterStream(pw, false)</tt>
     */
    public PrintWriterStream(PrintWriter pw) {
      this(pw, false);
    }
    
    /**
     * Only closes the underlying stream if
     * constructued with the close flag.
     */
    public void close() {
      super.close();
      if(bClose) {
	pw.close();
      }
    }
    
    /**
     * Write using the trivial, but possibly not always correct translation:
     * <pre>
     *  write((int) byte)
     * </pre>
     */
    public void write(byte[] buf, int off, int len) { 
      for(int i = off; i < off + len; i++) {
	write((int)buf[i]);
      }
    } 
    
    public void write(int b) { 
      pw.write(b);
    } 
    
    public boolean checkError() {
      return pw.checkError();
    }
    
    public void flush() {
      pw.flush();
    }
    
    public void print(boolean b) {
      pw.print(b);
    }
    
    public void print(char c) {
      pw.print(c);
    }
    
    public void print(char[] s) {
      pw.print(s);
    }
    
    public void print(double d) {
      pw.print(d);
    }

    public void print(float f) {
      pw.print(f);
    }
    
    public void print(int i) {
      pw.print(i);
    }
    
    public void print(long l) {
      pw.print(l);
    }
    
    public void print(Object obj) {
      pw.print(obj);
    }
    
    public void print(String s) {
      pw.print(s);
    }
    
    public void println() {
      pw.println();
    }
    
    public void println(boolean x) {
      pw.println(x);
    }
    
    public void println(char x) {
      pw.println(x);
    }
    
    public void println(char[] x) { 
      pw.println(x);
    }
    
    public void println(double x) { 
      pw.println(x);
    } 
    
    public void println(float x) { 
      pw.println(x);
    }
    
    public void println(int x) { 
      pw.println(x);
    }
    
    public void println(long x) { 
      pw.println(x);
    }
    
    public void println(Object x) { 
      pw.println(x);
    }
    
    public void println(String x) { 
      pw.println(x);
    } 
  }

}



