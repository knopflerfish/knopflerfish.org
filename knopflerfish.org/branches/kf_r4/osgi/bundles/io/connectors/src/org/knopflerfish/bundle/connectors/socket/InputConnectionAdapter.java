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
 */
public class InputConnectionAdapter implements InputConnection {

  // private fields

  private Socket socket;


  // constructors

  public InputConnectionAdapter(Socket socket) {
    this.socket = socket;
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
  }

} // InputConnectionAdapter
