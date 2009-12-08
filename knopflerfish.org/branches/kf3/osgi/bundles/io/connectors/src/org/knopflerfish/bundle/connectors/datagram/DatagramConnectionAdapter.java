/*
 * Copyright (c) 2006-2009, KNOPFLERFISH project
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

/**
 * @author Philippe Laporte
 */

package org.knopflerfish.bundle.connectors.datagram;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.SocketException;

import javax.microedition.io.Datagram;
import javax.microedition.io.UDPDatagramConnection;

class DatagramConnectionAdapter implements UDPDatagramConnection {

  private final DatagramSocket socket;
  private final String address;
  private final DatagramConnectionFactory factory;

  public DatagramConnectionAdapter(DatagramConnectionFactory factory,
                                   String address, boolean timeouts)
    throws SocketException
  {
    this.factory = factory;
    this.address = address;

    if (address == null || !address.startsWith("datagram://")) {
      throw new IllegalArgumentException("Excepted datagram://-address");
    }

    int portSep = address.lastIndexOf(':');

    if (portSep < 10) // the portSep needs to lie past "datagram://"
      throw new IllegalArgumentException("Missing port");

    String strPort = null;
    strPort = address.substring(portSep + 1, address.length());
    int port = 0;

    try {
      port = Integer.parseInt(strPort);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid port");
    }

    String host = address.substring(11, portSep); // 11 = "datagram://"

    if (host.equals("")) {
      // A server: Listen on a special port.
      socket = new DatagramSocket(port);
    } else {
      // A client: Any port will do
      socket = new DatagramSocket();
    }

    if (!timeouts)
      socket.setSoTimeout(0);

    factory.registerConnection(this);
  }

  public int getMaximumLength() throws IOException {
    // TODO: maybe?
    return Math.max(socket.getReceiveBufferSize(),
                    socket.getSendBufferSize());
  }

  public int getNominalLength() throws IOException {
    // TODO: is this correct?
    return Math.min(socket.getReceiveBufferSize(),
                    socket.getSendBufferSize());
  }

  public void send(Datagram datagram) throws IOException {
    if (!(datagram instanceof DatagramImpl)) {
      throw new IllegalArgumentException("Datagram is of wrong type");
    }

    socket.send(((DatagramImpl)datagram).getDatagramPacket());
  }

  public void receive(Datagram datagram) throws IOException {
    if(!(datagram instanceof DatagramImpl)){
      throw new IllegalArgumentException("Datagram is of wrong type");
    }
    socket.receive(((DatagramImpl)datagram).getDatagramPacket());
  }

  public Datagram newDatagram(int size) throws IOException {
    return newDatagram(size, address);
  }

  public Datagram newDatagram(int size, String address) throws IOException {
    return newDatagram(new byte[size], size, address);
  }

  public Datagram newDatagram(byte[] buf, int size) throws IOException {
    return newDatagram(buf, size, address);
  }

  public Datagram newDatagram(byte[] buf, int size, String address)
    throws IOException
  {
    return new DatagramImpl(buf, size, address);
  }

  public void close() throws IOException {
    factory.unregisterConnection(this);
    socket.close();
  }

  public String getLocalAddress()
    throws IOException {
    return socket.getLocalAddress().getHostAddress();
  }

  public int getLocalPort()
    throws IOException {
    return socket.getLocalPort();
  }
}
