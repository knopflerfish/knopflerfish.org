/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.bundle.httpconsole;
	
import org.osgi.framework.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

public class Util {
  static String BUNDLE_IMAGE = Activator.RES_ALIAS + "/bundle.gif";
  static String LIB_IMAGE    = Activator.RES_ALIAS + "/lib.gif";

  static String BUNDLE_ACTIVE_IMAGE = Activator.RES_ALIAS + "/bundle_started.gif";
  static String LIB_ACTIVE_IMAGE    = Activator.RES_ALIAS + "/lib_started.gif";

  static String BUNDLE_ID_PREFIX = "bundle_id_";


  static public String toHTML(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return 
      "<div class=\"error\">" + 
      sw.toString() + 
      "</div>";
  }


  public static long[] getBundleIds(HttpServletRequest request) {
    Vector v = new Vector();
    for(Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      if(key.startsWith(BUNDLE_ID_PREFIX)) {
	try {
	  long bid = Long.parseLong(key.substring(BUNDLE_ID_PREFIX.length()));
	  v.addElement(new Long(bid));
	} catch (Exception ex) {
	  System.out.println("Bad bid=" + key + " ex=" + ex);
	}
      }
    }

    long[] bids = new long[v.size()];
    for(int i = 0; i < v.size(); i++) {
      bids[i] = ((Long)v.elementAt(i)).longValue();
    }

    return bids;
  }

  public static String getName(Bundle b) {
    String s = (String)b.getHeaders().get(Constants.BUNDLE_NAME);
    if(s != null) {
      return s;
    }
    s = b.getLocation();
    if(s != null) {
      int ix = s.lastIndexOf("/");
      if(ix == -1) {
	ix = s.lastIndexOf("\\");
      }
      if(ix != -1) {
	return s.substring(ix + 1);
      }
    }

    return "#" + b.getBundleId();
  }
  
  static String getBundleImage(Bundle b) {
    if(hasActivator(b)) {
      if(b.getState() == Bundle.ACTIVE) {
	return BUNDLE_ACTIVE_IMAGE;
      }
      return BUNDLE_IMAGE;
    }
    if(b.getState() == Bundle.ACTIVE) {
      return LIB_ACTIVE_IMAGE;
    }
    return LIB_IMAGE;
  }

  public static boolean hasActivator(Bundle b) {
    return null != getHeader(b, "Bundle-Activator");
  }

  public static String getDescription(Bundle b) {
    return getHeader(b, Constants.BUNDLE_DESCRIPTION, "");
  }

  public static String getHeader(Bundle b, String name) {
    return getHeader(b, name, null);
  }

  public static String getHeader(Bundle b, String name, String def) {
    String s = 
      b != null
      ? (String)b.getHeaders().get(name)
      : def;

    if(s == null) {
      s = def;
    }

    return s;
  }


  // String replace functions below by Erik Wistrand

  /**
   * Replace all occurances of a substring with another string.
   *
   * <p>
   * The returned string will shrink or grow as necessary, depending on
   * the lengths of <tt>v1</tt> and <tt>v2</tt>.
   * </p>
   *
   * <p>
   * Implementation note: This method avoids using the standard String
   * manipulation methods to increase execution speed. 
   * Using the <tt>replace</tt> method does however
   * include two <tt>new</tt> operations in the case when matches are found.
   * </p>
   *
   *
   * @param s  Source string.
   * @param v1 String to be replaced with <code>v2</code>.
   * @param v2 String replacing <code>v1</code>. 
   * @return Modified string. If any of the input strings are <tt>null</tt>,
   *         the source string <tt>s</tt> will be returned unmodified. 
   *         If <tt>v1.length == 0</tt>, <tt>v1.equals(v2)</tt> or
   *         no occurances of <tt>v1</tt> is found, also 
   *         return <tt>s</tt> unmodified.
   */
  public static String replace(final String s, 
			       final String v1, 
			       final String v2) {
    
    // return quick when nothing to do
    if(s == null 
       || v1 == null 
       || v2 == null 
       || v1.length() == 0 
       || v1.equals(v2)) {
      return s;
    }

    int ix       = 0;
    int v1Len    = v1.length(); 
    int n        = 0;

    // count number of occurances to be able to correctly size
    // the resulting output char array
    while(-1 != (ix = s.indexOf(v1, ix))) {
      n++;
      ix += v1Len;
    }

    // No occurances at all, just return source string
    if(n == 0) {
      return s;
    }

    // Set up an output char array of correct size
    int     start  = 0;
    int     v2Len  = v2.length();
    char[]  r      = new char[s.length() + n * (v2Len - v1Len)];
    int     rPos   = 0;

    // for each occurance, copy v2 where v1 used to be
    while(-1 != (ix = s.indexOf(v1, start))) {
      while(start < ix) r[rPos++] = s.charAt(start++);
      for(int j = 0; j < v2Len; j++) {
	r[rPos++] = v2.charAt(j);
      }
      start += v1Len;
    }

    // ...and add all remaining chars
    ix = s.length(); 
    while(start < ix) r[rPos++] = s.charAt(start++);
    
    // ..ouch. this hurts.
    return new String(r);
  }


  public interface Comparator {
    public int compare(Object a, Object b);
  }

  public static Comparator bundleNameComparator = new Comparator() {
      public int compare(Object a, Object b) {
	Bundle b1 = (Bundle)a;
	Bundle b2 = (Bundle)b;
	
	String s1 = getName(b1).toLowerCase();
	String s2 = getName(b2).toLowerCase();
	
	//	System.out.println("compare " + s1 + ", " + s2);
	return s1.compareTo(s2);
      }
    };

  public static Comparator stringComparator = new Comparator() {
      public int compare(Object a, Object b) {
	String s1 = ((String)a).toLowerCase();
	String s2 = ((String)b).toLowerCase();
	
	return s1.compareTo(s2);
      }
    };


  /**
   * Sort a vector with objects compareble using a comparison function.
   *
   * @param a Vector to sort
   * @param cf comparison function
   */
  static public void sort(Vector a, Comparator cf, boolean bReverse) {
    sort(a, 0, a.size() - 1, cf, bReverse ? -1 : 1);
  }
  
  /**
   * Vector QSort implementation.
   */
  static void sort(Vector a, int lo0, int hi0, Comparator cf, int k) {
    int lo = lo0;
    int hi = hi0;
    Object mid;
    
    if ( hi0 > lo0) {
      
      mid = a.elementAt( ( lo0 + hi0 ) / 2 );
      
      while( lo <= hi ) {
	while( ( lo < hi0 ) && ( k * cf.compare(a.elementAt(lo), mid) < 0 )) {
	  ++lo;
	}
	
	while( ( hi > lo0 ) && ( k * cf.compare(a.elementAt(hi), mid ) > 0 )) {
	  --hi;
	}
	
	if( lo <= hi ) {
	  swap(a, lo, hi);
	  ++lo;
	  --hi;
	}
      }
      
      if( lo0 < hi ) {
	sort( a, lo0, hi, cf, k );
      }
      
      if( lo < hi0 ) {
	sort( a, lo, hi0, cf, k );
      }
    }
  }
  
  private static void swap(Vector a, int i, int j) {
    Object tmp  = a.elementAt(i); 
    a.setElementAt(a.elementAt(j), i);
    a.setElementAt(tmp,            j);
  }


  static void formStart(PrintWriter out, boolean bMultipart) throws IOException {
    out.println("<form " + 
		" action=\"" + Activator.SERVLET_ALIAS + "\"");
    if(bMultipart) {
      out.print(" enctype=\"multipart/form-data\"");
    }
    out.print(" method=\"POST\"" + 
	      ">");
  }

  static void formStop(PrintWriter out) throws IOException {
    out.println("</form>");
  }


  private static int BUF_SIZE = 1024 * 10;

  public static byte[] loadFormData(HttpServletRequest request,
				    String target,
				    StringBuffer fileName)
    throws ServletException,IOException
  {
    String      contentType = request.getHeader("content-type");
    
    // Only accept valid types
    if(!contentType.startsWith("multipart/form-data")) {
      throw new IOException("Bad content type for form data: " + contentType);
    }
    
    String boundary = "--" + extractParam(contentType,"boundary");
    if (boundary == null) {
      throw new IOException("No boundary string - can't parse uploaded data");
    }
    
    // Parse the data
    
    int       numRead;
    String    filename         = "";
    byte[]    buf              = new byte[BUF_SIZE];
    boolean   bHeader          = false;
    String    param            = null;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ServletInputStream    sis  = request.getInputStream();
    
    boolean bDone = false;
    
    while(!bDone && (numRead = sis.readLine(buf, 0, BUF_SIZE)) != -1) {
      //	Activator.log.info("read " + numRead + " bytes");
      if (bHeader) {
	// Read header
	String line = new String(buf, 0, numRead);
	if (line.equals("\r\n")) {
	  bHeader = false;
	} else if (line.startsWith("Content-Disposition:")) {
	  // Get parameters
	  //	  System.out.println("disp:" + line);
	  param = extractParam(line, "name");
	  if ("file".equals(param)) {
	    // Remove path from file name
	    filename = extractParam(line, "filename");
	    if (filename != null) {
	      StringTokenizer st = new StringTokenizer(filename, "\\/");
	      while (st.hasMoreTokens()) {
		Activator.log.info("got filename");
		filename = st.nextToken();
	      }
	    }
	  }
	} else if (line.startsWith("Content-Type:")) {
	  // Get parameters
	  //	  Activator.log.info("Got Content-Type: "+extractParam(line, "Content-Type"));
	}
      } else {
	// Check for boundary or read data
	if ((numRead == boundary.length()+2 ||
	     numRead == boundary.length()+4) &&
	    (new String(buf, 0, numRead)).startsWith(boundary))
	  {
	    //	    System.out.println("boundary, param=" + param + ", filename=" + filename);
	    if ((target + "_file").equals(param) && filename != null
		&& baos != null && baos.size() > 2)
	      {
		// Remove all the last "\r\n" characters
		byte [] outBytes = new byte[baos.size()-2];
		System.arraycopy(baos.toByteArray(),0,outBytes, 0,baos.size()-2);
		//		System.out.println("done, bytes=" + outBytes.length);
		//		System.out.println("result='" + new String(outBytes) + "'");
		fileName.append(filename);
		return outBytes;
		
	      } else if ("mime".equals(param) &&
			 baos != null && baos.size() > 2)
		{
		  String mime = new String(baos.toByteArray(),0,baos.size()-2);
		  Activator.log.info("got mime: " + mime);
		}
	    bHeader = true;
	  } else {
	    //	    Activator.log.info("write data, numRead=" + numRead);
	    baos.write(buf, 0, numRead);
	  }
      }
    }
    return null;
  }

  private static String extractParam(String line, String param) {
    if (line == null || param == null) return null;
    StringTokenizer st = new StringTokenizer(line, ";");
    String value = null;
    while (st.hasMoreTokens()) {
      String token = st.nextToken().trim();
      if (token.startsWith(param+"=") || token.startsWith(param+":")) {
	value = token.substring(param.length()+1).replace('\"', ' ').trim();
	break;
      }
    }
    return value;
  }

  public static String infoLink(Bundle b) {
    return ("<a href=\"" + 
	    Activator.SERVLET_ALIAS + 
	    "?" + Util.BUNDLE_ID_PREFIX + b.getBundleId() +
	    "=on" + 
	    "&cmd_info.x=1&cmd_info.y=1\">");
  };
  
  static public Bundle[] getSortedBundles(BundleContext bc) {
    Bundle[] bundles = bc.getBundles();

    Vector v = new Vector();
    for(int i = 0; i < bundles.length; i++) {
      v.addElement(bundles[i]);
    }

    Util.sort(v, Util.bundleNameComparator, false);
    v.copyInto(bundles);

    return bundles;
  }

  static void printObject(PrintWriter out, Object val) throws IOException {
    if(val == null) {
      out.println("null");
    } else if(val.getClass().isArray()) {
      printArray(out, (Object[])val);
    } else if(val instanceof Vector) {
      printVector(out, (Vector)val);
    } else if(val instanceof Dictionary) {
      printDictionary(out, (Dictionary)val);
    } else {
      out.print(val);
      //      out.print(" (" + val.getClass().getName() + ")");
    }
  }

  static void printDictionary(PrintWriter out, Dictionary d) throws IOException {

    out.println("<table>");
    for(Enumeration e = d.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      Object val = d.get(key);
      out.println("<tr>");

      out.println("<td>");
      printObject(out, key);
      out.println("</td>");

      out.println("<td>");
      printObject(out, val);
      out.println("</td>");

      out.println("</tr>");
    }
    out.println("</table>");
  }

  static void printArray(PrintWriter out, Object[] a) throws IOException {
    for(int i = 0; i < a.length; i++) {
      printObject(out, a[i]);
      if(i < a.length - 1) {
	out.println("<br>");
      }
    }
  }

  static void printVector(PrintWriter out, Vector a) throws IOException {
    for(int i = 0; i < a.size(); i++) {
      printObject(out, a.elementAt(i));
      if(i < a.size() - 1) {
	out.println("<br>");
      }
    }
  }

  static public String getStateString(int bundleState) {
    switch(bundleState) {
    case Bundle.ACTIVE:      return "active";
    case Bundle.INSTALLED:   return "installed";
    case Bundle.UNINSTALLED: return "uninstalled";
    case Bundle.RESOLVED:    return "resolved";
    case Bundle.STOPPING:    return "stopping";
    case Bundle.STARTING:    return "starting";
    default: return ("unknown state " + bundleState);
    }
  }

  
}
