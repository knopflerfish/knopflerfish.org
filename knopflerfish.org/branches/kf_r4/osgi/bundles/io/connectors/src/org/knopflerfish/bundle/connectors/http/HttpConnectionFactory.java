/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */

package org.knopflerfish.bundle.connectors.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.microedition.io.Connection;

import org.osgi.service.io.ConnectorService;

import org.knopflerfish.bundle.connectors.BaseConnectionFactory;

/**
 * @author Kaspar Weilenmann &lt;kaspar@gatespacetelematics.com&gt;
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */
public class HttpConnectionFactory extends BaseConnectionFactory {
	
  static{
	  //comply with javax.microedition.io.HttpConnection
	  HttpURLConnection.setFollowRedirects(false);
  }

  public Connection createConnection(String name, int mode, boolean timeouts) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(name).openConnection();
    
    if (mode == ConnectorService.READ_WRITE) {
      connection.setDoInput(true);
      connection.setDoOutput(true);
    } else {
      connection.setDoInput(mode == ConnectorService.READ);
      connection.setDoOutput(mode == ConnectorService.WRITE);
    }
    
    Connection con = new HttpConnectionAdapter(connection);
    addConnection(con);
    return con;
  }

  public String[] getSupportedSchemes() {
    return new String[]{"http"};
  }

} // HttpConnectionFactory
