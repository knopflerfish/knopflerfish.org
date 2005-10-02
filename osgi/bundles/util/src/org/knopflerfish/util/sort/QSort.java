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

package org.knopflerfish.util.sort;

import java.util.Vector;

/**
 * Quicksort utility class.
 */
public class QSort {
    /**
     * Use static methods, please
     */
    protected QSort() {
    }

    /**
     * Sort a vector with objects compareble using a comparison function.
     * 
     * @param a
     *            Vector to sort
     * @param cf
     *            comparison function
     */
    static public void sort(Vector a, CompareFunc cf) {
        sort(a, 0, a.size() - 1, cf);
    }

    /**
     * Sort an array with objects compareble using a comparison function.
     * 
     * @param a
     *            Array to sort
     * @param cf
     *            comparison function
     */
    static public void sort(Object a[], CompareFunc cf) {
        sort(a, 0, a.length - 1, cf);
    }

    /**
     * This is a generic version of C.A.R Hoare's Quick Sort algorithm. This
     * will handle arrays that are already sorted, and arrays with duplicate
     * keys.
     * 
     * @param a
     *            an array with items compareble using a sort function
     * @param lo0
     *            left boundary of array partition
     * @param hi0
     *            right boundary of array partition
     * @param cf
     *            comparison function
     */
    static void sort(Object a[], int lo0, int hi0, CompareFunc cf) {
        int lo = lo0;
        int hi = hi0;
        Object mid;

        if (hi0 > lo0) {

            /*
             * Arbitrarily establishing partition element as the midpoint of the
             * array.
             */
            mid = a[(lo0 + hi0) / 2];

            // loop through the array until indices cross
            while (lo <= hi) {
                /*
                 * find the first element that is greater than or equal to the
                 * partition element starting from the left Index.
                 */
                while ((lo < hi0) && (cf.compare(a[lo], mid) < 0)) {
                    ++lo;
                }

                /*
                 * find an element that is smaller than or equal to the
                 * partition element starting from the right Index.
                 */
                while ((hi > lo0) && (cf.compare(a[hi], mid) > 0)) {
                    --hi;
                }

                // if the indexes have not crossed, swap
                if (lo <= hi) {
                    swap(a, lo, hi);
                    ++lo;
                    --hi;
                }
            }

            /*
             * If the right index has not reached the left side of array must
             * now sort the left partition.
             */
            if (lo0 < hi) {
                sort(a, lo0, hi, cf);
            }

            /*
             * If the left index has not reached the right side of array must
             * now sort the right partition.
             */
            if (lo < hi0) {
                sort(a, lo, hi0, cf);
            }
        }
    }

    /**
     * Vector implementation...exactly as array version above
     */
    static void sort(Vector a, int lo0, int hi0, CompareFunc cf) {
        int lo = lo0;
        int hi = hi0;
        Object mid;

        if (hi0 > lo0) {

            mid = a.elementAt((lo0 + hi0) / 2);

            while (lo <= hi) {
                while ((lo < hi0) && (cf.compare(a.elementAt(lo), mid) < 0)) {
                    ++lo;
                }

                while ((hi > lo0) && (cf.compare(a.elementAt(hi), mid) > 0)) {
                    --hi;
                }

                if (lo <= hi) {
                    swap(a, lo, hi);
                    ++lo;
                    --hi;
                }
            }

            if (lo0 < hi) {
                sort(a, lo0, hi, cf);
            }

            if (lo < hi0) {
                sort(a, lo, hi0, cf);
            }
        }
    }

    private static void swap(Object a[], int i, int j) {
        Object tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    private static void swap(Vector a, int i, int j) {
        Object tmp = a.elementAt(i);
        a.setElementAt(a.elementAt(j), i);
        a.setElementAt(tmp, j);
    }
}
