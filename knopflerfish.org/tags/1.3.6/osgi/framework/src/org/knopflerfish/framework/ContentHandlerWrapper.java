/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.io.*;
import java.net.*;

import java.util.Set;
import java.util.Dictionary;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Vector;

import org.osgi.service.url.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.*;

/**
 * Wrapper which delegates an Mime ContentHandlers
 * OSGi registered ContentHandlers
 *
 * <p>
 * Each instance of ContentHandlerWrapper  tracks ContentHandlers
 * for a named mime type and selects the best from all available services.
 * </p>
 */public class ContentHandlerWrapper
  extends ContentHandler 
{

  Framework              framework;
  String                 mimetype;
  ServiceTracker         tracker;
  String                 filter;

  ContentHandlerWrapper(Framework              framework,
			String                 mimetype) {
    
    this.framework = framework;
    this.mimetype  = mimetype;

    filter = 
      "(&" + 
      "(" + Constants.OBJECTCLASS + "=" + 
      ContentHandler.class.getName() + ")" + 
      "(" + URLConstants.URL_CONTENT_MIMETYPE + "=" + mimetype + 
      ")" + 
      ")";

    try {
      tracker = new ServiceTracker(framework.systemBC, 
				   framework.systemBC.createFilter(filter), 
				   null);
      tracker.open();
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    if(Debug.url) {
      Debug.println("created wrapper for " + mimetype + ", filter=" + filter);
    }
  }

  private ContentHandler getService() {
    ContentHandler obj = 
      (ContentHandler)tracker.getService();

    if(obj == null) {
      throw new IllegalStateException("Lost service for mimetype=" + mimetype);
    }

    return obj;
  }

  public Object getContent(URLConnection urlc) throws IOException {
    return getService().getContent(urlc);
  }

  public Object getContent(URLConnection urlc, Class[] classes) throws IOException {
    return getService().getContent(urlc, classes);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("ContentHandlerWrapper[");

    ServiceReference ref = tracker.getServiceReference();
    sb.append("mimetype=" + mimetype);
    if(ref != null) {
      sb.append(", id=" + ref.getProperty(Constants.SERVICE_ID));
      sb.append(", rank=" + ref.getProperty(Constants.SERVICE_RANKING));
    } else {
      sb.append(" no service tracked");
    }

    sb.append("]");

    return sb.toString();
  }
}

  
