/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */


/**
 * @author Philippe Laporte
 */


//TODO only started, to complete

package org.knopflerfish.bundle.connectors.datagram;

import java.io.IOException;

import java.net.DatagramPacket;

import javax.microedition.io.Datagram;


/* package */ class DatagramImpl implements Datagram {
	
	private final DatagramPacket datagram;
	private byte[] buffer;

	public DatagramImpl(int length) {
		createBuffer(length);
		datagram = new DatagramPacket(buffer, length);
	}
	/*
	public DatagramImpl(int length, String address) {
		createBuffer(length);
		//TODO
		datagram = new DatagramPacket(buffer, length);
	}
	*/
	public DatagramImpl(byte[] buffer, int length) {
		this.buffer = buffer;
		datagram = new DatagramPacket(buffer, length);
	}
	/*
	public DatagramImpl(byte[] buffer, int length, String address) {
		this.buffer = buffer;
		//TODO
		datagram = new DatagramPacket(buffer, length);
	}
	*/
	private void createBuffer(int size){
		buffer = new byte[size];
	}
	
	/* package */ DatagramPacket getDatagramPacket(){
		return datagram;
	}

	public String getAddress() {
		throw new UnsupportedOperationException();
	}

	public byte[] getData() {
		return datagram.getData();
	}

	public int getLength() {
		return datagram.getLength();
	}

	public int getOffset() {
		return datagram.getOffset();
	}

	public void setAddress(String arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void setAddress(Datagram arg0) {
		throw new UnsupportedOperationException();
	}

	public void setLength(int length) {
		datagram.setLength(length);
	}

	public void setData(byte[] buffer, int offset, int length) {
		datagram.setData(buffer, offset, length);
	}

	public void reset() {
		throw new UnsupportedOperationException();
	}

	public void readFully(byte[] arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void readFully(byte[] arg0, int arg1, int arg2) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public int skipBytes(int arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public boolean readBoolean() throws IOException {
		throw new UnsupportedOperationException();
	}

	public byte readByte() throws IOException {
		throw new UnsupportedOperationException();
	}

	public int readUnsignedByte() throws IOException {
		throw new UnsupportedOperationException();
	}

	public short readShort() throws IOException {
		throw new UnsupportedOperationException();
	}

	public int readUnsignedShort() throws IOException {
		throw new UnsupportedOperationException();
	}

	public char readChar() throws IOException {
		throw new UnsupportedOperationException();
	}

	public int readInt() throws IOException {
		throw new UnsupportedOperationException();
	}

	public long readLong() throws IOException {
		throw new UnsupportedOperationException();
	}

	public String readUTF() throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public String readLine(){
		throw new UnsupportedOperationException();
	}
	
	public float readFloat(){
		throw new UnsupportedOperationException();
	}
	
	public double readDouble(){
		throw new UnsupportedOperationException();
	}

	public void write(int arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void write(byte[] arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void write(byte[] arg0, int arg1, int arg2) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeBoolean(boolean arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeByte(int arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeShort(int arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeChar(int arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeInt(int arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeLong(long arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeChars(String arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeUTF(String arg0) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public void writeBytes(String arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writeFloat(float arg0) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public void writeDouble(double arg0) throws IOException {
		throw new UnsupportedOperationException();
	}
}
