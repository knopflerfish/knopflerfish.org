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
 */
public class OutputConnectionAdapter implements OutputConnection {

  // private fields

  private Socket socket;


  // constructors

  public OutputConnectionAdapter(Socket socket) {
    this.socket = socket;
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
  }

} // StreamConnectionAdapter
