/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Base64 {
    /*
     * private static final char encodeTable[] = {
     * 'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
     * 'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
     * 'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
     * 'w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/' };
     */

    private static final byte encTab[] = { 0x41, 0x42, 0x43, 0x44, 0x45, 0x46,
            0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51,
            0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x61, 0x62,
            0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d,
            0x6e, 0x6f, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
            0x79, 0x7a, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x2b, 0x2f };

    private static final byte decTab[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1,
            -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
            14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
            -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
            42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1 };

    /*
     * private static final byte decodeTable[]={ -1, -1, -1, -1, -1, -1, -1, -1,
     * -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
     * -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62,
     * -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1,
     * -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
     * 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29,
     * 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
     * 48, 49, 50, 51, -1, -1, -1, -1, -1 };
     */

    /**
     * Encode a string of binary data using Base64.
     */
    /*
     * public static String encode(String in) { return encode(in.getBytes()); }
     * 
     * public static String encode(byte[] in) { StringBuffer sb= new
     * StringBuffer(); int ilen=in.length; int bits=0; int nbits=0;
     * 
     * for(int i=0;i<ilen;i++) { int c=(int)in[i] & 0xff; if(c>=256) throw new
     * IllegalArgumentException("Illegal character: " + in[i]); bits=(bits<<8)|c;
     * nbits+=8; while(nbits>=6) { nbits-=6;
     * sb.append(encodeTable[0x3f&(bits>>nbits)]); } }
     * 
     * switch(nbits) { case 2: sb.append(encodeTable[0x3f&(bits<<4)]);
     * sb.append('='); sb.append('='); break; case 4:
     * sb.append(encodeTable[0x3f&(bits<<2)]); sb.append('='); break; }
     * 
     * return sb.toString(); }
     */

    /**
     * Decode a string of Base64 data.
     */
    /*
     * public static String decode(String in) { StringBuffer sb= new
     * StringBuffer(); int ilen=in.length(); int i=0; int bits=0; int nbits=0; //
     * Check for complete groups and proper termination if(ilen%4!=0) throw new
     * IllegalArgumentException("Not a multiple of 4 characters"); for(;ilen>0 &&
     * in.charAt(ilen-1)=='=';ilen--); if(in.length()-ilen>2) throw new
     * IllegalArgumentException("Too many trailing ="); // Decode for(;i<ilen;i++) {
     * char c=in.charAt(i); byte b=c<128 ? decodeTable[c] : -1; if(b<0) throw
     * new IllegalArgumentException("Illegal character"); bits=(bits<<6)|b;
     * nbits+=6; if(nbits>=8) { nbits-=8; sb.append((char)(0xff&(bits>>nbits))); } }
     * 
     * return sb.toString(); }
     */

    /**
     * Decode a string of Base64 data.
     */

    public static byte[] decode(byte[] in) throws IOException {
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            baos = new ByteArrayOutputStream();
            bais = new ByteArrayInputStream(in);
            decode(bais, baos);
            return (baos.toByteArray());
        } finally {
            if (baos != null)
                baos.close();
            if (bais != null)
                bais.close();
        }
    }

    public static byte[] decode(String in) throws IOException {
        return decode(in.getBytes());
    }

    // Base64 encode/decode streams
    public static void decode(InputStream in, OutputStream out)
            throws IOException {

        // Read input stream until end of file, "=" or 0x0d,0x0a
        boolean quit = false;
        int bits = 0;
        int nbits = 0;
        int nbytes = 0;
        int b;
        int[] mem = new int[4];

        while (!quit && (b = in.read()) != -1) {
            byte c = b < 128 ? decTab[b] : -1;
            mem[3] = mem[2];
            mem[2] = mem[1];
            mem[1] = mem[0];
            mem[0] = b;
            if (c != -1) {
                // Found base64 character
                nbytes++;
                bits = (bits << 6) | c;
                nbits += 6;
                if (nbits >= 8) {
                    nbits -= 8;
                    out.write(0xff & (bits >> nbits));
                }
            } else if (b == 0x3d) {
                // Found '=' character
                quit = true;
                nbytes++;
                if (nbytes % 4 != 0) {
                    if (in.read() == 0x3d) {
                        nbytes++;
                    } else
                        throw new IOException(
                                "Stream not terminated with correct number of '='");
                }
            } else if (mem[0] == 0x0a && mem[1] == 0x0d && mem[2] == 0x0a
                    && mem[3] == 0x0d) {
                // Found '\r\n\r\n'
                quit = true;
            }
        }

        if (nbytes % 4 != 0)
            throw new IOException(
                    "Base64 stream not a multiple of 4 characters");
    }

    /**
     * Encode a raw byte array to a Base64 String.
     * 
     * @param in
     *            Byte array to encode.
     */
    public static String encode(byte[] in) throws IOException {
        return encode(in, 0);
    }

    /**
     * Encode a raw byte array to a Base64 String.
     * 
     * @param in
     *            Byte array to encode.
     * @param len
     *            Length of Base64 lines. 0 means no line breaks.
     */
    public static String encode(byte[] in, int len) throws IOException {
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            baos = new ByteArrayOutputStream();
            bais = new ByteArrayInputStream(in);
            encode(bais, baos, len);
            // ASCII byte array to String
            return (new String(baos.toByteArray()));
        } finally {
            if (baos != null)
                baos.close();
            if (bais != null)
                bais.close();
        }
    }

    public static void encode(InputStream in, OutputStream out, int len)
            throws IOException {

        // Check that length is a multiple of 4 bytes
        if (len % 4 != 0)
            throw new IllegalArgumentException("Length must be a multiple of 4");

        // Read input stream until end of file
        int bits = 0;
        int nbits = 0;
        int nbytes = 0;
        int b;

        while ((b = in.read()) != -1) {
            bits = (bits << 8) | b;
            nbits += 8;
            while (nbits >= 6) {
                nbits -= 6;
                out.write(encTab[0x3f & (bits >> nbits)]);
                nbytes++;
                // New line
                if (len != 0 && nbytes >= len) {
                    out.write(0x0d);
                    out.write(0x0a);
                    nbytes -= len;
                }
            }
        }

        switch (nbits) {
        case 2:
            out.write(encTab[0x3f & (bits << 4)]);
            out.write(0x3d); // 0x3d = '='
            out.write(0x3d);
            break;
        case 4:
            out.write(encTab[0x3f & (bits << 2)]);
            out.write(0x3d);
            break;
        }

        if (len != 0) {
            if (nbytes != 0) {
                out.write(0x0d);
                out.write(0x0a);
            }
            out.write(0x0d);
            out.write(0x0a);
        }
    }
}
