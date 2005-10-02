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

public class Compare {

    /**
     * Returns true if the two specified arrays of bytes are equal to one
     * another. Two arrays are considered equal if both arrays contain the same
     * number of elements, and all corresponding pairs of elements in the two
     * arrays are equal. In other words, two arrays are equal if they contain
     * the same elements in the same order. Also, two array references are
     * considered equal if both are null.
     * 
     * @param a
     *            One array to be tested for equality.
     * @param a2
     *            The other array to be tested for equality.
     * @return <code>true</code> if the two arrays are equal.
     */
    public static boolean compareByteArrays(byte[] a, byte[] a2) {
        boolean equal = false;

        if (a == a2) {
            // If the array references are equal, the arrays are equal
            equal = true;
        } else if (a == null && a2 == null) {
            // Two array references are considered equal if both are null
            equal = true;
        } else if (a == null || a2 == null) {
            // If one reference is null and the other isn't, the arrays
            // are not equal
            equal = false;
        } else if (a.length != a2.length) {
            // If the arrays have different lengths, they're not equal
            equal = false;
        } else {
            // If the arrays are of equal length and all corresponding
            // pairs of elements are equal, the arrays are equal
            equal = true;
            for (int i = 0; i < a.length; i++) {
                if (a[i] != a2[i]) {
                    equal = false;
                    break;
                }
            }
        }

        return equal;
    }
}
