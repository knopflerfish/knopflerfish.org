package org.knopflerfish.bundle.httpclient_connector;
import java.io.IOException;

import javax.microedition.io.Connection;

import org.osgi.service.io.ConnectionFactory;

public class HttpClientFactory implements ConnectionFactory {

	public Connection createConnection(String name, int mode, boolean timeouts) throws IOException {
		return new HttpClientConnection(name, mode, timeouts);
	}

}
