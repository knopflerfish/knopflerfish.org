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

import org.osgi.service.url.*;
import org.osgi.framework.*;


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
  String                 filter;
  ServiceReference       best;

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

    ServiceListener serviceListener = 
      new ServiceListener() {
        public void serviceChanged(ServiceEvent evt) {
          ServiceReference ref = 
            evt.getServiceReference();
            
          switch (evt.getType()) {
          case ServiceEvent.MODIFIED: {
            // fall through
          } 
          case ServiceEvent.REGISTERED: {
            if (best == null) {
              updateBest();
              return ;
            }
            
            if (compare(best, ref) > 0) {
              best = ref;
            }
            
          }; break;
          case ServiceEvent.UNREGISTERING: {
            if (best.equals(ref)) {
              best = null;
            }
          }
          }
        }
      };
    
    try {
      framework.systemBC.addServiceListener(serviceListener, filter);
      
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not register service listener for content handler: " + e);
    }
    
    if(Debug.url) {
      Debug.println("created wrapper for " + mimetype + ", filter=" + filter);
    }
  }
  
  private int compare(ServiceReference ref1, ServiceReference ref2) {
    Object tmp1 = ref1.getProperty(Constants.SERVICE_RANKING);
    Object tmp2 = ref2.getProperty(Constants.SERVICE_RANKING);
    
    int r1 = (tmp1 instanceof Integer) ? ((Integer)tmp1).intValue() : 0;
    int r2 = (tmp2 instanceof Integer) ? ((Integer)tmp2).intValue() : 0;

    if (r2 == r1) {
      Long i1 = (Long)ref1.getProperty(Constants.SERVICE_ID);      
      Long i2 = (Long)ref2.getProperty(Constants.SERVICE_ID);
      return i1.compareTo(i2);

    } else {
      return r2 -r1;
    }
  }

  private void updateBest() {
    try {
      ServiceReference[] refs =
        framework.systemBC.getServiceReferences(ContentHandler.class.getName(), 
                                                filter);
      if (refs != null) {
        best = refs[0];
      } 

      for (int i = 1; i < refs.length; i++) {
        if (compare(best, refs[i]) > 0) {
          best = refs[i];
        }
      }

    } catch (Exception e) {
      // this should not happen.
      throw new IllegalArgumentException("Could not register url handler: " + e);
    }
  }
  

  private ContentHandler getService() {
    ContentHandler obj;

    try {
      if (best == null) {
        updateBest();
      }

      if (best == null) {
        throw new IllegalStateException("null: Lost service for protocol="+ mimetype);
      }

      obj = (ContentHandler)framework.systemBC.getService(best);

      if (obj == null) {
        throw new IllegalStateException("null: Lost service for protocol=" + mimetype);
      }
      
    } catch (Exception e) {
      throw new IllegalStateException("null: Lost service for protocol=" + mimetype);
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

    ServiceReference ref = best; 
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

  
