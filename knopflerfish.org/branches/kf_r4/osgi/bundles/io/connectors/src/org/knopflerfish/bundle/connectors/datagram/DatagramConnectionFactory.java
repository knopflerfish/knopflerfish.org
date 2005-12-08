/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */


/**
 * @author Philippe Laporte
 */

//TODO only started, to complete

package org.knopflerfish.bundle.connectors.datagram;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.microedition.io.Connection;

import org.osgi.service.io.ConnectionFactory;
import org.osgi.service.io.ConnectorService;


public class DatagramConnectionFactory implements ConnectionFactory {

  // implements ConnectionFactory

  public Connection createConnection(String name, int mode, boolean timeouts) throws IOException {
   
	try {
      if (mode == ConnectorService.READ) {
    	//this does not work: port not gotten  
    	//URI uri = new URI(name);
        //DatagramSocket socket = new DatagramSocket(uri.getPort());
        
    	//this does :-) TODO: make this clean ie better/investigate
    	URI uri = new URI(name);
    	int port = Integer.parseInt(uri.getAuthority().substring(1));
        DatagramSocket socket = new DatagramSocket(port);  
        if (!timeouts) socket.setSoTimeout(0);  
        return new DatagramConnectionAdapter(socket);
      } 
      //TODOs
      else if (mode == ConnectorService.WRITE) {
    	  throw new UnsupportedOperationException();
      } 
      else if (mode == ConnectorService.READ_WRITE) {
    	  throw new UnsupportedOperationException();
      } 
      else {
        throw new IllegalArgumentException("Illegal value for mode: " + mode);
      }
    } 
    catch (URISyntaxException urise) {
      throw new IOException("Invalid URL syntax: " + urise.getMessage());
    }
  }

} // DatagramConnectionFactory