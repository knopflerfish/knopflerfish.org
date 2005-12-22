/*
 * Copyright (c) 2005 Gatespace Telematics. All Rights Reserved.
 */


/**
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */

package org.knopflerfish.bundle.connectors.datagram;

import java.io.IOException;
import java.io.EOFException;
import java.io.UTFDataFormatException;
import java.io.DataInputStream; 
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.URI;
import javax.microedition.io.Datagram;

/*
  MO: This feels a bit "unclean" -- can someone think of a nicer way to do this?
*/

class DatagramImpl implements Datagram {
	
    private DatagramPacket dgram;
    private String address;
    private int pos;
    
    public DatagramImpl(byte[] buffer, int length, String address) {
	dgram = new DatagramPacket(buffer, length);
	
	if (address != null)
	    setAddress(address);
	
	pos = 0;
    }

    public DatagramPacket getDatagramPacket() {
	return dgram;
    }
    
    public String getAddress() {
	return "datagram://" + dgram.getAddress().getHostAddress() + ":"  + dgram.getPort();
    }

    public int getLength() {
	return dgram.getLength();
    }
    
    public int getOffset() {
	return dgram.getOffset();
    }

    public byte[] getData() {
	return dgram.getData();
    }

    public void setData(byte[] buf, int off, int len) {
	dgram.setData(buf, off, len);
    }
    
    public void setAddress(Datagram data) {
	setAddress(data.getAddress());
    }

    public void setAddress(String address) {
	
	/* 
	   I (M-O) have compared how this function behaves against
	   a sun implementation of javax.microedtion.io.Datagram 
	   and it seems to behave in the same way.	   
	 */
	
	if (address == null || !address.startsWith("datagram://")) {
	    throw new IllegalArgumentException("Excepted datagram://-address");
	}

	int portSep = address.lastIndexOf(':');
	
	if (portSep < 10) // the portSep needs to lie past "datagram://"
	    throw new IllegalArgumentException("Missing port");

	String port = null;
	port = address.substring(portSep + 1, address.length());

	if (!port.equals("")) {
	    try {
		int portNo = Integer.parseInt(port);
		dgram.setPort(portNo);
	    } catch (NumberFormatException e) {
		throw new IllegalArgumentException("Invalid port");
	    }
	}
	
	String host = address.substring(11, portSep); // 11 = "datagram://"

	try {   
	    dgram.setAddress(InetAddress.getByName(host));
	} catch(Exception e) {
	    throw new IllegalArgumentException("Invalid host");
	} 
    }
    
    public void setLength(int len) {
	dgram.setLength(len);
    }
    
    public void reset() {
	byte[] b = dgram.getData();
	pos = 0;
	setData(b, 0, 0);
    }

    /*
      I've tried to compare the read/write functions to a Sun implementation
      of the javax.microedition.io.Datagram but it did not seem to behave properly.
      One couldn't even execute simple examples like:
      
      <code>
       ...
       Datagram dg = con.newDatagram(100);
       dg.reset();
       dg.writeUTF("hej hopp i lingonskogen");
      </code>
      
      Gives exceptions.. therefor the read/write functions have not been tested
      against a "real" javax.microedition.io implementation. 
      This is a big "TODO".
      
      The functions have however been tested against "themselves".
      
    */

    public void readFully(byte[] buf) throws IOException {
	readFully(buf, 0, buf.length);
    }

    public void readFully(byte[] buf, int off, int len) throws IOException {
	if (off < 0 || len < 0 || off + len > buf.length)
	    throw new IndexOutOfBoundsException();

	for (int i = off; i < off + len; i++) {
	    buf[i] = readByte();
	}
    }
	
    public int skipBytes(int num) throws IOException {
	byte[] b = getData();
	int new_pos = Math.min(pos + num, getLength());
	int change = new_pos - pos;
	
	pos = new_pos;
	return change;
    }


    public boolean readBoolean() throws IOException {
	return readByte() != 0;
    }

    public byte readByte() throws IOException {
	
	byte[] b = getData();
	if (pos >= b.length)
	    throw new EOFException();

	// Is this really correct? Should getOffset be included?

	return b[getOffset() + pos++];
    }

    public int readUnsignedByte() throws IOException {
	return (int)readByte();
    }
    
    public short readShort() throws IOException {
	return (short)(((short)readByte()) << 8 | readByte());
    }
    
    public int readUnsignedShort() throws IOException {
	return (int)readShort();
    }

    public char readChar() throws IOException {
	return (char)readShort();
    }

    public int readInt() throws IOException {
	return (int)(((int)readShort()) << 16 | readShort());
    }

    public long readLong() throws IOException {
	return (long)(((long)readInt() << 32) | readInt());
    }

    public String readUTF() throws IOException {
	return DataInputStream.readUTF(this);
	
    }
    
    public String readLine() throws IOException {
	StringBuffer bf = new StringBuffer();
	char c;
	
	while (true) {
	    c = readChar();
	    bf.append(c);
	    
	    if (c == '\n' || c == '\r') {
		break;
	    }
	}
	
	return bf.toString();
    } 
	
    public float readFloat() throws IOException {
	return Float.intBitsToFloat(readInt());
    }
	
    public double readDouble() throws IOException {
	return Double.longBitsToDouble(readLong());
    }

    public void write(int val) throws IOException {
	byte[] b = getData();
	
	// is this really correct, should getOffset be added?
	b[getOffset() + pos] = (byte)val; // should throw ArrayIndexOutOfBounds
	pos++;
    }

    public void write(byte[] buf) throws IOException {
	write(buf, 0, buf.length);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
	if (off < 0 || len < 0 || off + len > buf.length)
	    throw new IndexOutOfBoundsException();
	
	for (int i = off; i < off + len; i++) {
	    write(buf[i]); 
	}
    }

    public void writeBoolean(boolean bool) throws IOException {
	write(bool ? 1 : 0);
    }

    public void writeByte(int b) throws IOException {
	write(b);
    }

    public void writeShort(int sh) throws IOException {
	write(0xff & (sh >>> 8));
	write(sh & 0xff);
    }

    public void writeChar(int ch) throws IOException {
	writeShort(ch);
    }

    public void writeInt(int val) throws IOException {
	writeShort(val >>> 16);
	writeShort(val & 0xffff);
    }

    public void writeLong(long val) throws IOException {
	writeInt((int)(val >>> 32));
	writeInt((int)val);
    }

    public void writeChars(String str) throws IOException {
	for (int i = 0; i < str.length(); i++)
	    writeChar((int)str.charAt(i));
    }

    public void writeUTF(String str) throws IOException {
	int bytes = 0;

	for (int i = 0; i < str.length(); i++) {
	    if (str.charAt(i) >= '\u0001' && str.charAt(i) <= '\u007f') {
		bytes++;
	    } else if (str.charAt(i) == '\u0000' ||
		       (str.charAt(i) >= '\u0080' && str.charAt(i) <= '\u07ff')) {
		bytes += 2;
	    } else if (str.charAt(i) >= '\u0800' && str.charAt(i) <= '\uffff') {
		bytes += 3;
	    }
	}

	if (bytes > 65535)
	    throw new UTFDataFormatException("String is to long");
	
	writeShort((short)bytes);

	for (int i = 0; i < str.length(); i++) {
	    if (str.charAt(i) >= '\u0001' && str.charAt(i) <= '\u007f') {
		write(str.charAt(i));

	    } else if (str.charAt(i) == '\u0000' ||
		       (str.charAt(i) >= '\u0080' && str.charAt(i) <= '\u07ff')) {
		
		write(0xc0 | (0x1f & (str.charAt(i) >> 6)));
		write(0x80 | (0x3f & str.charAt(i)));

	    } else if (str.charAt(i) >= '\u0800' && str.charAt(i) <= '\u07ff') {
		write(0xe0 | (0x0f & (str.charAt(i) >> 12)));
		write(0x80 | (0x3f & (str.charAt(i) >>  6)));
		write(0x80 | (0x3f & str.charAt(i)));
	    }
	}
    }
    
    public void writeBytes(String str) throws IOException {
	byte[] bytes = str.getBytes();
	write(bytes);
    }

    public void writeFloat(float val) throws IOException {
	writeInt((int)Float.floatToIntBits(val));
    }
	
    public void writeDouble(double val) throws IOException {
	writeLong(Double.doubleToLongBits(val));
    } 
}
