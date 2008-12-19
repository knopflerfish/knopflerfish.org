/*
 * Copyright (c) 2006, KNOPFLERFISH project
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

package org.knopflerfish.bundle.connectors.http;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.microedition.io.HttpConnection;


/**
 * @author Kaspar Weilenmann &lt;kaspar@gatespacetelematics.com&gt;
 */
class HttpConnectionAdapter implements HttpConnection {

  // private fields

  private HttpURLConnection connection;
  private URL url;
  private HttpConnectionFactory factory;

  // constructors

  public HttpConnectionAdapter(HttpConnectionFactory factory, HttpURLConnection connection) {
    this.connection = connection;
    this.url = connection.getURL();
	this.factory = factory;
	factory.registerConnection(this);
  }

  // implements HttpConnection

  public String getType() {
    return connection.getContentType();
  }

  public long getDate() throws IOException {
    return connection.getDate();
  }

  public long getExpiration() throws IOException {
    return connection.getExpiration();
  }

  public String getFile() {
    return url.getFile();
  }

  public String getHeaderField(int n) {
    return connection.getHeaderField(n);
  }

  public String getHeaderField(String name) throws IOException {
    return connection.getHeaderField(name);
  }

  public long getHeaderFieldDate(String name, long def) throws IOException {
    return connection.getHeaderFieldDate(name, def);
  }

  public int getHeaderFieldInt(String name, int def) throws IOException {
    return connection.getHeaderFieldInt(name, def);
  }

  public String getHeaderFieldKey(int n) throws IOException {
    return connection.getHeaderFieldKey(n);
  }

  public String getHost() {
    return url.getHost();
  }

  public long getLastModified() throws IOException {
    return connection.getLastModified();
  }

  public int getPort() {
    return url.getPort();
  }

  public String getProtocol() {
    return url.getProtocol();
  }

  public String getQuery() {
    return url.getQuery();
  }

  public String getRef() {
    return url.getRef();
  }

  public String getRequestMethod() {
    return connection.getRequestMethod();
  }

  public String getRequestProperty(String key) {
    return connection.getRequestProperty(key);
  }

  public int getResponseCode() throws IOException {
    return connection.getResponseCode();
  }

  public String getResponseMessage() throws IOException {
    return connection.getResponseMessage();
  }

  public String getURL() {
    return url.toString();
  }

  public void setRequestMethod(String method) throws IOException {
    connection.setRequestMethod(method);
  }

  public void setRequestProperty(String key, String value) throws IOException {
    connection.setRequestProperty(key, value);
  }

  public String getEncoding() {
    return connection.getContentEncoding();
  }

  public long getLength() {
    return connection.getContentLength();
  }

  public DataInputStream openDataInputStream() throws IOException {
    return new DataInputStream(connection.getInputStream());
  }

  public InputStream openInputStream() throws IOException {
    return openDataInputStream();
  }

  public DataOutputStream openDataOutputStream() throws IOException {
    return new DataOutputStream(connection.getOutputStream());
  }

  public OutputStream openOutputStream() throws IOException {
    return openDataOutputStream();
  }

  public void close() throws IOException {
    connection.disconnect();
	factory.unregisterConnection(this);
  }

} // HttpConnectionAdapter
