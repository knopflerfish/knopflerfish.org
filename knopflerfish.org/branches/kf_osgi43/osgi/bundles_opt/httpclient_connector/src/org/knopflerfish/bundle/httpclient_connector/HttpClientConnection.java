/*
 * Copyright (c) 2006-2012, KNOPFLERFISH project
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
package org.knopflerfish.bundle.httpclient_connector;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Date;

import javax.microedition.io.HttpConnection;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;

import org.osgi.framework.BundleContext;

/**
 * TODO:
 * - is uri.getEscapedURIReference() correct?
 * - shouldn't the getHeaderField(int nth) throw an IOException?
 */
class HttpClientConnection
  implements HttpConnection
{
  public final static String TIMEOUT
    = "org.knopflerfish.httpclient_connector.so_timeout";

  private final static String DELETE_METHOD = "DELETE";
  private final static String PUT_METHOD = "PUT";
  
  private final static int STATE_SETUP     = 0;
  private final static int STATE_CONNECTED = 1;
  private final static int STATE_CLOSED    = 2;

  private int state = STATE_SETUP;
  private boolean requestSent = false;

  private String method = HttpConnection.GET;

  private HttpClient client = new HttpClient();
  private Object lock = new Object(); // lock for client.

  private URI uri;
  private HttpMethod resCache = null;

  // All inputs that are associated with this connection
  private ArrayList iss = new ArrayList();
  private OutputWrapper out = null;

  private final BundleContext bc;

  HttpClientConnection(final BundleContext bc,
                       final String url,
                       final int mode,
                       final boolean timeouts)
    throws URIException
  {
    this.bc = bc;
    uri = new URI(url, false); // assume not escaped URIs
    ProxySelector.configureProxy(bc, client, url);

    final String timeoutString = bc.getProperty(TIMEOUT);
    if (timeoutString != null) {
      try {
        client.getParams().setSoTimeout(Integer.parseInt(timeoutString));
      } catch (NumberFormatException e) {
        throw new RuntimeException("Invalid timeout " + timeoutString);
      }
    }
  }

  public long getDate() throws IOException {
    final HttpMethod res = getResult(true);
    final Header head = res.getResponseHeader("Date");

    if (head == null) {
      return 0;
    }

    try {
      return DateUtil.parseDate(head.getValue()).getTime();
    } catch (DateParseException e) {
      return 0;
    }
  }

  public long getExpiration() throws IOException {
    HttpMethod res = getResult(true);
    Header head = res.getResponseHeader("Expires");

    if (head == null) {
      return 0;
    }

    try {
      return DateUtil.parseDate(head.getValue()).getTime();
    } catch (DateParseException e) {
      return 0;
    }
  }

  public String getFile() {
    return uri.getEscapedPath();
  }

  public String getHeaderField(int nth)
  // throws IOException TODO: doesn't this one throw exceptions??
  {
    try {
      HttpMethod res = getResult(true);
      Header[] hs = res.getResponseHeaders();

      if (hs.length > nth && nth >= 0) {
        return hs[nth].getValue();
      } else {
        return null;
      }
    } catch (IOException e) {
      return null;
    }
  }

  public String getHeaderField(String key) throws IOException {
    HttpMethod res = getResult(true);
    Header head = res.getResponseHeader(key);

    if (head == null) {
      return null;
    }

    return head.getValue();
  }

  public long getHeaderFieldDate(String key, long def) throws IOException {
    HttpMethod res = getResult(true);
    Header head = res.getResponseHeader(key);

    if (head == null) {
      return def;
    }

    try {
      Date date = DateUtil.parseDate(head.getValue());
      return date.getTime();

    } catch (DateParseException e) {
      return def;
    }
  }

  public int getHeaderFieldInt(String key, int def) throws IOException {
    HttpMethod res = getResult(true);
    Header head = res.getResponseHeader(key);

    if (head == null) {
      return def;
    }

    try {
      return Integer.parseInt(head.getValue());
    } catch (NumberFormatException e) {
      return def;
    }
  }

  public String getHeaderFieldKey(int nth) throws IOException {
    HttpMethod res = getResult(true);
    Header[] hs = res.getResponseHeaders();

    if (hs.length > nth && nth >= 0) {
      return hs[nth].getName();
    } else {
      return null;
    }
  }

  public String getHost() {
    try {
      return uri.getHost();
    } catch (URIException e) {
      return null;
    }
  }

  public long getLastModified() throws IOException {
    HttpMethod res = getResult(true);
    Header head = res.getResponseHeader("Last-Modified");

    if (head != null) {
      try {
        return DateUtil.parseDate(head.getValue()).getTime();
      } catch (DateParseException e) {
        return 0;
      }
    }

    return 0;
  }

  public int getPort() {
    return uri.getPort();
  }

  public String getProtocol() {
    return uri.getScheme();
  }

  public String getQuery() {
    return uri.getEscapedQuery();
  }

  public String getRef() {
    // TODO: this returns the URL?!
    return uri.getEscapedURIReference();
  }

  public String getRequestMethod() {
    return method;
  }

  public String getRequestProperty(String key) {
    try {
      HttpMethod res = getResult();
      Header h = res.getRequestHeader(key);
      if (h != null) {
        return h.getValue();
      }

      return null;
    } catch (IOException e) {
      throw new RuntimeException("This is a bug.");
    }
  }

  public int getResponseCode() throws IOException {
    HttpMethod res = getResult(true);
    return res.getStatusCode();
  }

  public String getResponseMessage() throws IOException {
    HttpMethod res = getResult(true);
    return res.getStatusText();
  }

  public String getURL() {
    return uri.getEscapedURI();
  }

  public void setRequestMethod(String method) throws IOException {
    if (!HttpConnection.GET.equals(method) &&
        !HttpConnection.HEAD.equals(method) &&
        !HttpConnection.POST.equals(method) &&
        !PUT_METHOD.equals(method) &&
        !DELETE_METHOD.equals(method)) {
      throw new IllegalArgumentException("method should be one of " +
                                         HttpConnection.GET + ", " +
                                         HttpConnection.HEAD + ", " +
                                         HttpConnection.POST + ", " +
                                         PUT_METHOD + " and " +
                                         DELETE_METHOD);
    }

    if (state == STATE_CLOSED) {
      init();
    }

    if (state != STATE_SETUP) {
      throw new ProtocolException("Can't reset method: already connected");
    }

    if (out != null && !(HttpConnection.POST.equals(method) || PUT_METHOD.equals(method))) {
      // When an outputstream has been created these calls are ignored.
      return ;
    }

    if (this.method != method && resCache != null) {
      // TODO: here we should convert an existing resCache
      throw new RuntimeException
        ("Not yet implemented, as a work around you can always set"
         +" the request method the first thing you do..");
    }

    this.method = method;
  }

  public void setRequestProperty(String key, String value) throws IOException {

    if (state == STATE_CLOSED) {
      init();
    }

    if (state != STATE_SETUP) {
      throw new IllegalStateException("Already connected");
    }

    if (out != null && !(HttpConnection.POST.equals(method) || PUT_METHOD.equals(method))) {
      // When an outputstream has been created these calls are ignored.
      return ;
    }

    HttpMethod res = getResult();
    res.setRequestHeader(key, value);
  }

  public String getEncoding() { // throws IOException
    try {
      HttpMethod res = getResult(true);
      Header head = res.getResponseHeader("Content-Encoding");

      if (head != null) {
        return head.getValue();
      }

      return null;
    } catch (IOException e) {
      return null;
    }
  }

  public long getLength() {
    try {
      HttpMethod res = getResult(true);
      Header head =  res.getResponseHeader("Content-Length");

      if (head == null) {
        return -1;
      }

      return Long.parseLong(head.getValue());
    } catch (IOException e) {
      return -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  public String getType() {
    try {
      HttpMethod res = getResult(true);
      Header head = res.getResponseHeader("Content-Type") ;

      if (head == null) {
        return null;
      }

      return head.getValue();
    } catch (IOException e) {
      return null;
    }
  }

  public DataInputStream openDataInputStream() throws IOException {
    return new DataInputStream(openInputStream());
  }

  public InputStream openInputStream() throws IOException {
    HttpMethod res = getResult(true);
    InputStream is = res.getResponseBodyAsStream();

    if (is == null) {
      return null;
    }

    InputWrapper iw = new InputWrapper(is);

    synchronized(iss) {
      iss.add(iw);
    }

    return iw;
  }

  public void close() throws IOException {

    synchronized(iss) {
      for (int i = 0, n = iss.size(); i < n; i++) {
        InputWrapper iw = (InputWrapper)iss.get(i);
        iw.closeStream();
      }
      iss.clear();
    }

    if (out != null) {
      out.close();
      out = null;
    }

    state = STATE_CLOSED;
  }

  public DataOutputStream openDataOutputStream() throws IOException {
    return new DataOutputStream(openOutputStream());
  }

  public OutputStream openOutputStream() throws IOException {
    if (requestSent) {
      throw new ProtocolException("The request has already been sent");
    }

    if (out == null) {
      out = new OutputWrapper();
    }

    if(!(HttpConnection.POST.equals(method) || PUT_METHOD.equals(method))) {
    	setRequestMethod(HttpConnection.POST);
    }

    return out;
  }

  private HttpMethod getResult() throws IOException {
    return getResult(false);
  }

  private HttpMethod getResult(boolean forceSend) throws IOException {
    if (resCache != null) {
      if (forceSend && !requestSent) {
        // if the method has not been send yet (if we have been
        // working with POST)
        sendRequest();
      }

      return resCache;
    }

    if (method.equals(HttpConnection.POST)) {
      resCache = new PostMethod(uri.getEscapedURI());
    } else if (method.equals(PUT_METHOD)) {
      resCache = new PutMethod(uri.getEscapedURI());
    }else if (method.equals(HttpConnection.HEAD)) {
      resCache = new HeadMethod(uri.getEscapedURI());
    } else if (method.equals(HttpConnection.GET)) {
      resCache = new GetMethod(uri.getEscapedURI());      
    } else if (method.equals(DELETE_METHOD)) {
        resCache = new DeleteMethod(uri.getEscapedURI());
    } else {
      // hopefully this is unreachable code
      throw new IllegalStateException("Not a valid method " + method);
    }

    resCache.setFollowRedirects(false);

    if (forceSend) {
      sendRequest();
      state = STATE_CONNECTED;
    }

    return resCache;
  }

  private void sendRequest() throws IOException {
    synchronized(lock) {
      if (out != null) {
        if (!(resCache instanceof EntityEnclosingMethod)) {
          System.err.println("Warning: data written to request's body, "
                             +"but not supported");
        } else {
          EntityEnclosingMethod m = (EntityEnclosingMethod) resCache;
          m.setRequestEntity(new ByteArrayRequestEntity(out.getBytes()));
        }
      }

      client.executeMethod(resCache);
      requestSent = true;
    }
  }

  private void init() {
    state = STATE_SETUP;
    requestSent = false;
    method = HttpConnection.GET;
    resCache = null;
    //params = new DefaultHttpParams();
    out = null;
  }

  //      private void convert(HttpMethod target) throws IOException {
  //              HttpMethod res = getResult();
  //              target.setFollowRedirects(res.getFollowRedirects());
  //              target.setParams(res.getParams());
  //              target.setPath(res.getPath());
  //              target.setQueryString(res.getQueryString());
  //              Header[] headers = res.getRequestHeaders();
  //
  //              if (headers != null) {
  //                      for (int i = 0; i < headers.length; i++) {
  //                              target.setRequestHeader(headers[i]);
  //                      }
  //              }
  //
  //              target.setURI(res.getURI());
  //      }

  private class OutputWrapper extends OutputStream {
    private ByteArrayOutputStream bout = new ByteArrayOutputStream();

    public void write(int b) throws IOException {
      bout.write(b);
    }

    public void close() throws IOException {
      state = STATE_CONNECTED;
    }

    public void flush() throws IOException {
      getResult(true);
    }

    byte[] getBytes() {
      return bout.toByteArray();
    }

  }

  private void callClose() throws IOException {
    close();
  }

  private class InputWrapper extends InputStream {

    private InputStream is;

    InputWrapper(InputStream is) {
      this.is = is;
    }

    public int read() throws IOException {
      try {
        return is.read();
      } catch (IOException t) {
        callClose();
        throw t;
      }
    }

    public void close() throws IOException {
      synchronized(iss) {
        iss.remove(this);
      }

      is.close();
    }

    public void closeStream() throws IOException {
      is.close();
    }
  }

}
