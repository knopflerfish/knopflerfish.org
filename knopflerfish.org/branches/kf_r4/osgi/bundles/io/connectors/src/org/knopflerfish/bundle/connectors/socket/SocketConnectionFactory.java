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
        retval = new InputConnectionAdapter(this, socket);
      } else if (mode == ConnectorService.WRITE) {
        retval = new OutputConnectionAdapter(this, socket);
      } else if (mode == ConnectorService.READ_WRITE) {
        retval = new StreamConnectionAdapter(this, socket);
      } else {
        throw new IllegalArgumentException("Illegal value for mode: " + mode);
      }

      return retval;
      
    } catch (URISyntaxException urise) {
      throw new IOException("Invalid URL syntax: " + urise.getMessage());
    }

    
  }

} // SocketConnectionFactory
