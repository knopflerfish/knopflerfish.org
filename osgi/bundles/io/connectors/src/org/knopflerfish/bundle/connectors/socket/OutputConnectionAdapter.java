/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */

package org.knopflerfish.bundle.connectors.socket;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.microedition.io.OutputConnection;


/**
 * @author Kaspar Weilenmann &lt;kaspar@gatespacetelematics.com&gt;
 * @author Mats-Ola Persson
 */
public class OutputConnectionAdapter implements OutputConnection {

  // private fields

  private Socket socket;
  private SocketConnectionFactory factory;

  // constructors

  public OutputConnectionAdapter(SocketConnectionFactory factory, Socket socket) {
    this.socket = socket;
	this.factory = factory;
	factory.registerConnection(this);
  }


  // implements OutputConnection

  public DataOutputStream openDataOutputStream() throws IOException {
    return new DataOutputStream(socket.getOutputStream());
  }

  public OutputStream openOutputStream() throws IOException {
    return openDataOutputStream();
  }

  public void close() throws IOException {
		socket.close();
  		factory.unregisterConnection(this);
  }

} // StreamConnectionAdapter
