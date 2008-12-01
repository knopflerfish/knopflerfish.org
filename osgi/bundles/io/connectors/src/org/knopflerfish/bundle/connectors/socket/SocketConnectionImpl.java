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
package org.knopflerfish.bundle.connectors.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.microedition.io.SocketConnection;

/**
 * @author perg@makewave.com
 */
class SocketConnectionImpl implements SocketConnection {

  // private fields

  private Socket socket;
  private SocketConnectionFactory factory;


  // constructors

  public SocketConnectionImpl(SocketConnectionFactory factory, Socket socket) {
    this.socket = socket;
    this.factory = factory;
    factory.registerConnection(this);
  }


  // implements StreamConnection

  public DataInputStream openDataInputStream() throws IOException {
    return new DataInputStream(socket.getInputStream());
  }

  public InputStream openInputStream() throws IOException {
    return openDataInputStream();
  }

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
  
  public void setSocketOption(byte option, int value)
  throws IllegalArgumentException, IOException {
		switch (option) {
		case DELAY:
			socket.setTcpNoDelay(value != 0);
			break;
		case LINGER:
			socket.setSoLinger(value != 0, value);
			break;
		case KEEPALIVE:
			socket.setKeepAlive(value != 0);
			break;
		case RCVBUF:
			socket.setReceiveBufferSize(value);
			break;
		case SNDBUF:
			socket.setSendBufferSize(value);
			break;
		default:
			throw new IllegalArgumentException("Illegal option (" + option
					+ ") for SocketConnection.setOption(byte, int)");
		}
			  
  }

	public int getSocketOption(byte option) throws IllegalArgumentException,
			IOException {
		switch (option) {
		case DELAY:
			return socket.getTcpNoDelay() ? 1 : 0;
		case LINGER:
			return socket.getSoLinger();
		case KEEPALIVE:
			return socket.getKeepAlive() ? 1 : 0;
		case RCVBUF:
			return socket.getReceiveBufferSize();
		case SNDBUF:
			return socket.getSendBufferSize();
		default:
			throw new IllegalArgumentException("Illegal option (" + option
					+ ") for SocketConnection.getOption(byte)");
		}
	}

	public String getLocalAddress() throws IOException {
		return socket.getLocalAddress().getHostAddress();

	}

	public int getLocalPort() throws IOException {
		return socket.getLocalPort();

	}

	public String getAddress() throws IOException {
		return socket.getInetAddress().getHostAddress();

	}

	public int getPort() throws IOException {
		return socket.getPort();

	}

}
