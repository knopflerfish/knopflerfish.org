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

/**
 * A converter class used to read primitive datatypes from, or write them to, a
 * bytearray, starting at position index. The data is stored as big-endian in
 * the array (the most significant value in the sequence is stored first at the
 * lowest storage address).
 */
public class ByteArray {

    /**
     * Returns a byte read from the bytearray, starting at position index.
     * 
     * @param array
     *            the bytearray to read from
     * @param index
     *            the index to start at in the bytearray
     * @return the byte just read
     */
    public static byte getByte(byte[] array, int index) {
        return (byte) toLong(array, index, 1);
    }

    /**
     * Returns a short read from the bytearray, starting at position index.
     * 
     * @param array
     *            the bytearray to read from
     * @param index
     *            the index to start at in the bytearray
     * @return the short just read
     */
    public static short getShort(byte[] array, int index) {
        return (short) toLong(array, index, 2);
    }

    /**
     * Returns an integer read from the bytearray, starting at position index.
     * 
     * @param array
     *            the bytearray to read from
     * @param index
     *            the index to start at in the bytearray
     * @return the integer just read
     */
    public static int getInt(byte[] array, int index) {
        return (int) toLong(array, index, 4);
    }

    /**
     * Returns a float read from the bytearray, starting at the position index.
     * 
     * @param array
     *            the bytearray to read from
     * @param index
     *            the index to start at in the bytearray
     * @return the float just read
     */
    public static float getFloat(byte[] array, int index) {
        return Float.intBitsToFloat(getInt(array, index));
    }

    /**
     * Returns a long read from the bytearray, starting at position index.
     * 
     * @param array
     *            the bytearray to read from
     * @param index
     *            the index to start at in the bytearray
     * @return the long just read
     */
    public static long getLong(byte[] array, int index) {
        return toLong(array, index, 8);
    }

    public static double getDouble(byte[] array, int index) {
        return Double.longBitsToDouble(getLong(array, index));
    }

    /**
     * Writes a byte into the bytearray, starting at position index.
     * 
     * @param b
     *            the byte to write down
     * @param array
     *            the bytearray to write to
     * @param index
     *            the index to start at in the bytearray
     * @return the number of written bytes
     */
    public static int setByte(byte b, byte[] array, int index) {
        return toByteArray(b, array, index, 1);
    }

    /**
     * Writes a short into the bytearray, starting at position index.
     * 
     * @param s
     *            the short to write down
     * @param array
     *            the bytearray to write to
     * @param index
     *            the index to start at in the bytearray
     * @return the number of written bytes
     */
    public static int setShort(short s, byte[] array, int index) {
        return toByteArray(s, array, index, 2);
    }

    /**
     * Writes an integer into the bytearray, starting at position index.
     * 
     * @param i
     *            the integer to write down
     * @param array
     *            the bytearray to write to
     * @param index
     *            the index to start at in the bytearray
     * @return the number of written bytes
     */
    public static int setInt(int i, byte[] array, int index) {
        return toByteArray(i, array, index, 4);
    }

    public static int setFloat(float f, byte[] array, int index) {
        return toByteArray(Float.floatToIntBits(f), array, index, 4);
    }

    /**
     * Writes a long into the bytearray, starting at position index.
     * 
     * @param l
     *            the long to write down
     * @param array
     *            the bytearray to write to
     * @param index
     *            the index to start at in the bytearray
     * @return the number of written bytes
     */
    public static int setLong(long l, byte[] array, int index) {
        return toByteArray(l, array, index, 8);
    }

    public static int setDouble(double d, byte[] array, int index) {
        return toByteArray(Double.doubleToLongBits(d), array, index, 8);
    }

    /**
     * Reads size-number of bytes from the bytearray and puts it into a long
     * that is returned, starting at position index. If the number of bytes
     * don't fill out the long, it will be filled with extra zeroes.
     * 
     * @param array
     *            the bytearray to read from
     * @param index
     *            the index to start at in the bytearray
     * @param size
     *            the number of bytes to read
     * @return the read bytes in a long
     */
    private static long toLong(byte[] array, int index, int size) {
        long result = 0;

        for (int i = 0; i < size; i++) {
            result <<= 8;
            result |= (array[index + i] & 0xFF);
        }

        return result;
    }

    /**
     * Writes size-number of bytes from value (backwards) down to the bytearray,
     * starting at position index.
     * 
     * @param value
     *            the value to write down to the bytearray
     * @param array
     *            the bytearray to write to
     * @param index
     *            the index to start at in the bytearray
     * @param size
     *            the number of bytes to write
     * @return the number of written bytes
     */
    private static int toByteArray(long value, byte[] array, int index, int size) {
        for (int i = (size - 1); i >= 0; i--) {
            array[index + i] = (byte) (value & 0xFF);
            value >>>= 8;
        }

        return size;
    }

}
