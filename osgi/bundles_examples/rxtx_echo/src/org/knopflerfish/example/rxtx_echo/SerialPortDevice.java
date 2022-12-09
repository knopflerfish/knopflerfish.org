/*
 * Copyright (c) 2012-2022, KNOPFLERFISH project
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

package org.knopflerfish.example.rxtx_echo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

public class SerialPortDevice implements SerialPortEventListener {

  final static int CR = '\r';

  final static int MAX_MSG_SIZE = 256;
  
  private InputStream in;
  private OutputStream out;
  private SerialPort serialPort;
  private byte[] msg = null;
  private int msgSize;
  private String dev;
  private boolean localEcho;
  final private Activator activator;

  public SerialPortDevice(Activator a, boolean localEcho) {
    activator = a;
    this.localEcho = localEcho;
  }

  public void open(String port) {
    CommPortIdentifier portId;
    Enumeration<?> portIds = CommPortIdentifier.getPortIdentifiers();
    while (portIds.hasMoreElements()) {
      portId = (CommPortIdentifier) portIds.nextElement();
      Activator.logDebug("Found port: " + portId.getName());
      if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
        if (portId.getName().endsWith(port)) {
          dev = portId.getName();
          Activator.logInfo("Open port '" + dev);
          try {
            serialPort = (SerialPort)portId.open("CommPort", 2000);
          } catch (PortInUseException e) {
            Activator.logError("Port in use", e);
          }
          try {
            in = serialPort.getInputStream();
          } catch (IOException e) {
            Activator.logError("Failed to get input stream", e);
          }
          try {
            out = serialPort.getOutputStream();
          } catch (IOException e) {
            Activator.logError("Failed to get output stream", e);
          }
          try {
            serialPort.addEventListener(this);
          } catch (TooManyListenersException e) {
            Activator.logError("Failed to add listener", e);
          }
          serialPort.notifyOnDataAvailable(true);
          serialPort.notifyOnCTS(true);
          try {
            serialPort.setSerialPortParams(9600,
                                           SerialPort.DATABITS_8,
                                           SerialPort.STOPBITS_1,
                                           SerialPort.PARITY_NONE);
            activator.gotDev(serialPort.isCTS(), dev); 
          } catch (UnsupportedCommOperationException e) {
            Activator.logError("Failed to set comm params", e);
          }
        }
      }
    }
  }

  public void close() {
    if (serialPort != null) {
      try {
        serialPort.close();
      } catch (Exception e) {
        Activator.logError("Failed to close port", e);
      }
      serialPort = null;
    }
  }

  public void writeString(String str) {
    if (serialPort != null) {
      try {
        out.write(str.getBytes());
      } catch (Exception e) {
        Activator.logError("Failed to write to port", e);
      }
    }
  }

  public void serialEvent(SerialPortEvent event) {
    if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
      Activator.logDebug("DataAvailable!");
      try {
        while (in.available() > 0) {
          int c = in.read();
          if (msg == null) {
            msgSize = 0;
            msg = new byte[MAX_MSG_SIZE];
          }
          if (c == CR) {
            activator.gotMsg(new String(msg, 0, msgSize));
            msg = null;
          } else {
            if (msgSize < msg.length) {
              msg[msgSize++] = (byte)c;
            } else {
              Activator.logWarning("Packet to large, dropped! >" + msgSize);
              msg = null;
            }
            if (localEcho) {
              out.write(c);
            }
          }
        }
      } catch (Exception e) {
        Activator.logError("Failed to read from port", e);
      }
    } else if (event.getEventType() == SerialPortEvent.CTS) {
      Activator.logDebug("ClearToSend: " + event.getNewValue());
      activator.gotDev(event.getNewValue(), dev);
    } else {
      Activator.logDebug("Unexpected SerialEvent: " + event.getEventType());
    }
  }

}
