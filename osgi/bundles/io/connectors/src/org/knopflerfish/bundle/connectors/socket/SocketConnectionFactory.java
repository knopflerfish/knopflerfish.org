/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */

package org.knopflerfish.bundle.connectors.socket;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.microedition.io.Connection;

import org.osgi.service.io.ConnectorService;
import org.knopflerfish.bundle.connectors.BaseConnectionFactory;

/**
 * @author Kaspar Weilenmann &lt;kaspar@gatespacetelematics.com&gt;
 */
public class SocketConnectionFactory extends BaseConnectionFactory {

  // implements ConnectionFactory

  public String[] getSupportedSchemes() {
    return new String[]{"socket"};
  }

  public Connection createConnection(String name, int mode, boolean timeouts) throws IOException {
    try {
	
      //comment by pl: I don't think this works, see Datagram code	
      URI uri = new URI(name);
      Connection retval;
      Socket socket = new Socket(uri.getHost(), uri.getPort());
      if (!timeouts) socket.setSoTimeout(0);
      if (mode == ConnectorService.READ) {
        retval = new InputConnectionAdapter(socket);
      } else if (mode == ConnectorService.WRITE) {
        retval = new OutputConnectionAdapter(socket);
      } else if (mode == ConnectorService.READ_WRITE) {
        retval = new StreamConnectionAdapter(socket);
      } else {
        throw new IllegalArgumentException("Illegal value for mode: " + mode);
      }

      addConnection(retval);
      return retval;
      
    } catch (URISyntaxException urise) {
      throw new IOException("Invalid URL syntax: " + urise.getMessage());
    }

    
  }

} // SocketConnectionFactory
