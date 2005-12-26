/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */

/**
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */

package org.knopflerfish.bundle.connectors.datagram;

import java.io.IOException;
import java.util.ArrayList;

import java.net.DatagramSocket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.microedition.io.Connection; 

import org.osgi.service.io.ConnectorService;
import org.knopflerfish.bundle.connectors.BaseConnectionFactory;

public class DatagramConnectionFactory extends BaseConnectionFactory {

    public String[] getSupportedSchemes() {
		return new String[]{"datagram"};
    }

    public Connection createConnection(String address, int mode, boolean timeouts) throws IOException {
		if (mode != ConnectorService.READ &&
			mode != ConnectorService.WRITE &&
			mode != ConnectorService.READ_WRITE) 
				throw new IOException("Invalid mode");
	
		try {
	    
			Connection con = 
				new DatagramConnectionAdapter(this, address, timeouts);

			return con;

		} catch (Exception e) {
			// if anything goes wrong it *must* throw IOException
			throw new IOException(e.getMessage());
		}
    }
}
