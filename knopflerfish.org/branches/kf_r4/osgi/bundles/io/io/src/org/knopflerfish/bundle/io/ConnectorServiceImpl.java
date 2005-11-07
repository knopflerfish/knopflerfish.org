
package org.knopflerfish.bundle.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.Connection;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;
import javax.microedition.io.ConnectionNotFoundException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import org.osgi.service.io.*;

import org.osgi.util.tracker.ServiceTracker;


class ConnectorServiceImpl implements ConnectorService {

  

  public ConnectorServiceImpl(BundleContext bc) {
	
  }


  void close() {
	
  }

 
  public Connection open(String uri) throws IOException {
	return null;
  }


  
  public Connection open(String uri, int mode) throws IOException {
	return null;
  }


  
  public Connection open(String uri, int mode, boolean timeouts) throws IOException {

	return null;
  }


  private ConnectionFactory getConnectionFactory(String scheme) {
	
		return null;
  }


  private int getRanking(ServiceReference ref) {
	
	return 0;
  }


  private boolean contains(String[] a, String el) {
	
	return false;
  }

 
  public DataInputStream openDataInputStream(String name) throws IOException {
	
	return null;
  }

  
  public DataOutputStream openDataOutputStream(String name) throws IOException {
	
	return null;
  }

  
  public InputStream openInputStream(String name) throws IOException {
	

	return null;
  }

  
  public OutputStream openOutputStream(String name) throws IOException {
	
	return null;
  }
}
