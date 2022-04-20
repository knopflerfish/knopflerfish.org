/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

import java.io.IOException;
import java.net.ContentHandler;
import java.net.URLConnection;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;


/**
 * Wrapper which delegates an MIME ContentHandlers
 * OSGi registered ContentHandlers
 *
 * <p>
 * Each instance of ContentHandlerWrapper  tracks ContentHandlers
 * for a named MIME type and selects the best from all available services.
 * </p>
 */
public class ContentHandlerWrapper
  extends ContentHandler
{

  FrameworkContext       framework;
  String                 mimetype;
  String                 filter;
  ServiceReference<ContentHandler> best;

  ContentHandlerWrapper(FrameworkContext       framework,
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

    final ServiceListener serviceListener =
      new ServiceListener() {
        public void serviceChanged(ServiceEvent evt) {
          @SuppressWarnings("unchecked")
          final
          ServiceReference<ContentHandler> ref =
              (ServiceReference<ContentHandler>) evt.getServiceReference();

          switch (evt.getType()) {
          case ServiceEvent.MODIFIED:
            // fall through
          case ServiceEvent.REGISTERED:
            if (best == null) {
              updateBest();
              return ;
            }

            if (compare(best, ref) > 0) {
              best = ref;
            }
            break;
          case ServiceEvent.MODIFIED_ENDMATCH:
            // fall through
          case ServiceEvent.UNREGISTERING:
            if (best.equals(ref)) {
              best = null;
            }
          }
        }
      };

    try {
      framework.systemBundle.bundleContext.addServiceListener(serviceListener, filter);

    } catch (final Exception e) {
      throw new IllegalArgumentException("Could not register service listener for content handler: " + e);
    }

    if (framework.debug.url) {
      framework.debug.println("created wrapper for " + mimetype + ", filter=" + filter);
    }
  }

  private int compare(ServiceReference<?> ref1, ServiceReference<?> ref2) {
    final Object tmp1 = ref1.getProperty(Constants.SERVICE_RANKING);
    final Object tmp2 = ref2.getProperty(Constants.SERVICE_RANKING);

    final int r1 = (tmp1 instanceof Integer) ? ((Integer)tmp1).intValue() : 0;
    final int r2 = (tmp2 instanceof Integer) ? ((Integer)tmp2).intValue() : 0;

    if (r2 == r1) {
      final Long i1 = (Long)ref1.getProperty(Constants.SERVICE_ID);
      final Long i2 = (Long)ref2.getProperty(Constants.SERVICE_ID);
      return i1.compareTo(i2);

    } else {
      return r2 -r1;
    }
  }

  private void updateBest() {
    try {
      @SuppressWarnings("unchecked")
      final
      ServiceReference<ContentHandler>[] refs
        = (ServiceReference<ContentHandler>[]) framework.systemBundle.bundleContext
          .getServiceReferences(ContentHandler.class.getName(), filter);
      if (refs != null) {
        best = refs[0];
        for (int i = 1; i < refs.length; i++) {
          if (compare(best, refs[i]) > 0) {
            best = refs[i];
          }
        }
      }
    } catch (final Exception e) {
      // TODO, handle differently!? this should not happen.
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

      obj = framework.systemBundle.bundleContext.getService(best);

      if (obj == null) {
        throw new IllegalStateException("null: Lost service for protocol=" + mimetype);
      }

    } catch (final Exception e) {
      throw new IllegalStateException("null: Lost service for protocol=" + mimetype);
    }

    return obj;
  }

  @Override
  public Object getContent(URLConnection urlc) throws IOException {
    return getService().getContent(urlc);
  }

  @Override
  public Object getContent(URLConnection urlc,
                           @SuppressWarnings("rawtypes") Class[] classes)
      throws IOException
  {
    return getService().getContent(urlc, classes);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append("ContentHandlerWrapper[");

    final ServiceReference<ContentHandler> ref = best;
    sb.append("mimetype=").append(mimetype);
    if(ref != null) {
      sb.append(", id=").append(ref.getProperty(Constants.SERVICE_ID));
      sb.append(", rank=").append(ref.getProperty(Constants.SERVICE_RANKING));
    } else {
      sb.append(" no service tracked");
    }

    sb.append("]");

    return sb.toString();
  }
}


