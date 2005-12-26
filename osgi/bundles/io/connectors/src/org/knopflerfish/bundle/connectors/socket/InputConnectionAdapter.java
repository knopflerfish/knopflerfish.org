/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */

package org.knopflerfish.bundle.connectors.socket;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.microedition.io.InputConnection;


/**
 * @author Kaspar Weilenmann &lt;kaspar@gatespacetelematics.com&gt;
 * @author Mats-Ola Persson
 */
public class InputConnectionAdapter implements InputConnection {

  // private fields

  private Socket socket;
  private SocketConnectionFactory factory;

  // constructors

  public InputConnectionAdapter(SocketConnectionFactory factory, Socket socket) {
    this.socket = socket;
	this.factory = factory;
	factory.registerConnection(this);
  }


  // implements InputConnection

  public DataInputStream openDataInputStream() throws IOException {
    return new DataInputStream(socket.getInputStream());
  }

  public InputStream openInputStream() throws IOException {
    return openDataInputStream();
  }

  public void close() throws IOException {
    socket.close();
  	factory.unregisterConnection(this);
  }

} // InputConnectionAdapter
