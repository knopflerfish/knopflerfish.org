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

package org.knopflerfish.util.cm;

import java.util.Properties;
import java.util.StringTokenizer;

// XML packages (provided by the jaxp bundle)
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;

public class XMLErrors {

  public static final String MSGFILE = "/Messages_en.properties";
  
  // Utility method
  public static String getErrorString(SAXException se, String currentSource) {
    StringBuffer sb = new StringBuffer();
    String lineInfo = "";
    if (se instanceof SAXParseException) {
      SAXParseException spe = (SAXParseException)se;
      lineInfo = "line " + spe.getLineNumber()
	+", column "     + spe.getColumnNumber()
	+", public ID "  + spe.getPublicId()
	+", system ID "  + spe.getSystemId();
    }
    String msg = se.getMessage();
    StringTokenizer st = new StringTokenizer(msg.substring(msg.indexOf('/')+1));
    String code = st.nextToken();
    
    try {
      Properties props = new Properties();
      props.load(CMDataManager.class.getResourceAsStream(MSGFILE));
      String emsg = (String)props.get(code);
      if (emsg == null) {
	sb.append("Cannot findError reading XML error message database");	
      } else {
	for (int i = 0; st.hasMoreTokens(); i++) {
	  emsg = replace(emsg, "{"+i+"}", st.nextToken());
	}
	sb.append("While parsing " + currentSource + ":");
	sb.append(lineInfo + ": " + emsg);
      }
    } catch (IOException e) {
      sb.append("Error reading XML error message database " + MSGFILE + ", " + e);
    }    
    return sb.toString();

  }

  public static String replace(String s, String v1, String v2) {
    int           ix  = 0;

    // Resulting string will hopefully be somewhere near this size
    StringBuffer  r   = new StringBuffer(s.length());

    int start = 0;
    ix        = s.indexOf(v1, start);
    
    while(ix != -1) {
      r.append(s.substring(start, ix));
      r.append(v2);
      start = ix + v1.length();
      ix    = s.indexOf(v1, start);
    }
    if(start < s.length()) {
      r.append(s.substring(start));
    }
    return r.toString();
  }
}

