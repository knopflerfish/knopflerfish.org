/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */


/**
 * @author Philippe Laporte
 */


//TODO only started, to complete

package org.knopflerfish.bundle.connectors.datagram;

import java.io.IOException;

import java.net.DatagramSocket;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;


public class DatagramConnectionAdapter implements DatagramConnection {
	
	private final DatagramSocket socket;

	public DatagramConnectionAdapter(DatagramSocket socket) {
		this.socket = socket;	
	}

	public int getMaximumLength() throws IOException {
		throw new UnsupportedOperationException();
	}

	public int getNominalLength() throws IOException {
		throw new UnsupportedOperationException();
	}

	public void send(Datagram arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void receive(Datagram datagram) throws IOException {
		if(!(datagram instanceof DatagramImpl)){
			throw new IllegalArgumentException("Datagram is of wrong type");
		}
		socket.receive(((DatagramImpl)datagram).getDatagramPacket());
	}

	public Datagram newDatagram(int size) throws IOException {
		return new DatagramImpl(size);
	}

	public Datagram newDatagram(int size, String address) throws IOException {
		throw new UnsupportedOperationException();
		//return new DatagramImpl(size, address);
	}

	public Datagram newDatagram(byte[] buf, int size) throws IOException {
		return new DatagramImpl(buf, size);
	}

	public Datagram newDatagram(byte[] buf, int size, String address)
			throws IOException {
		throw new UnsupportedOperationException();
		//return new DatagramImpl(buf, size, address);
	}

	public void close() throws IOException {
		socket.close();
	}

}



