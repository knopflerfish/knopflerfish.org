/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */


/**
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */

package org.knopflerfish.bundle.connectors.datagram;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.SocketException;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;


public class DatagramConnectionAdapter implements DatagramConnection {
	
    private final DatagramSocket socket;
    private final String address;

    public DatagramConnectionAdapter(String address, boolean timeouts) throws SocketException {
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
    
    public Datagram newDatagram(byte[] buf, int size, String address) throws IOException {
	return new DatagramImpl(buf, size, address);
    }

    public void close() throws IOException {
	socket.close();
    }

}



