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

import java.io.File;

public class Sort {

    /** No public default constructor in this class. */
    private Sort() throws Exception {
        throw new Exception("The Sort class is not instanciable.");
    }

    /**
     * Sorts the specified array of Integer objects into ascending order,
     * according to the natural ordering of its elements.
     * 
     * The sorting algorithm is mergesort. This algorithm offers n*log(n)
     * performance.
     * 
     * @param a
     *            The array to be sorted.
     */
    public static void sortIntegerArray(Integer[] a) {
        sortIntegerArray(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of Integer objects into
     * ascending order, according to the natural ordering of its elements. The
     * range to be sorted extends from index fromIndex, inclusive, to index
     * toIndex, exclusive.
     * 
     * The sorting algorithm is mergesort. This algorithm offers n*log(n)
     * performance.
     * 
     * @param a
     *            The array to be sorted.
     * @param fromIndex
     *            The index of the first element (inclusive) to be sorted.
     * @param toIndex
     *            The index of the last element (exclusive) to be sorted.
     */
    public static void sortIntegerArray(Integer[] a, int fromIndex, int toIndex) {
        int middle;

        if (a == null)
            return;

        if (fromIndex + 1 < toIndex) {
            middle = (fromIndex + toIndex) / 2;
            sortIntegerArray(a, fromIndex, middle);
            sortIntegerArray(a, middle, toIndex);
            mergeIntegerArray(a, fromIndex, toIndex);
        }
    }

    /**
     * Implementation of the merge-part of the mergesort algorithm for Integer
     * objects.
     * 
     * @param a
     *            The array to be merged.
     * @param fromIndex
     *            The index of the first element (inclusive) to be merged.
     * @param toIndex
     *            The index of the last element (exclusive) to be merged.
     */
    private static void mergeIntegerArray(Integer[] a, int fromIndex,
            int toIndex) {
        int i, j, k, middle, n;
        n = toIndex - fromIndex;
        Integer[] b = new Integer[n]; // temporary array

        k = 0;
        middle = (fromIndex + toIndex) / 2;

        // Copy lower half to array b
        for (i = fromIndex; i < middle; i++)
            b[k++] = a[i];
        // Copy upper half to array b in opposite order
        for (j = toIndex - 1; j >= middle; j--)
            b[k++] = a[j];

        i = 0;
        j = n - 1;
        k = fromIndex;

        // Copy back next-greatest element at each time
        // until i and j cross
        while (i <= j) {
            if (b[i].intValue() <= b[j].intValue())
                a[k++] = b[i++];
            else
                a[k++] = b[j--];
        }
    }

    /**
     * Sorts the specified array of String objects into ascending order,
     * according to the natural ordering of its elements.
     * 
     * The sorting algorithm is mergesort. This algorithm offers n*log(n)
     * performance.
     * 
     * @param a
     *            The array to be sorted.
     */
    public static void sortStringArray(String[] a) {
        sortStringArray(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of String objects into
     * ascending order, according to the natural ordering of its elements. The
     * range to be sorted extends from index fromIndex, inclusive, to index
     * toIndex, exclusive.
     * 
     * The sorting algorithm is mergesort. This algorithm offers n*log(n)
     * performance.
     * 
     * @param a
     *            The array to be sorted.
     * @param fromIndex
     *            The index of the first element (inclusive) to be sorted.
     * @param toIndex
     *            The index of the last element (exclusive) to be sorted.
     */
    public static void sortStringArray(String[] a, int fromIndex, int toIndex) {
        int middle;

        if (a == null)
            return;

        if (fromIndex + 1 < toIndex) {
            middle = (fromIndex + toIndex) / 2;
            sortStringArray(a, fromIndex, middle);
            sortStringArray(a, middle, toIndex);
            mergeStringArray(a, fromIndex, toIndex);
        }
    }

    /**
     * Implementation of the merge-part of the mergesort algorithm for String
     * objects.
     * 
     * @param a
     *            The array to be merged.
     * @param fromIndex
     *            The index of the first element (inclusive) to be merged.
     * @param toIndex
     *            The index of the last element (exclusive) to be merged.
     */
    private static void mergeStringArray(String[] a, int fromIndex, int toIndex) {
        int i, j, k, middle, n;
        n = toIndex - fromIndex;
        String[] b = new String[n]; // temporary array

        k = 0;
        middle = (fromIndex + toIndex) / 2;

        // Copy lower half to array b
        for (i = fromIndex; i < middle; i++)
            b[k++] = a[i];
        // Copy upper half to array b in oppsite order
        for (j = toIndex - 1; j >= middle; j--)
            b[k++] = a[j];

        i = 0;
        j = n - 1;
        k = fromIndex;

        // Copy back next-greatest element at each time
        // until i and j cross
        while (i <= j) {
            if (b[i].compareTo(b[j]) < 0)
                a[k++] = b[i++];
            else
                a[k++] = b[j--];
        }
    }

    /**
     * Sorts the specified array of File objects into ascending order, according
     * to the natural ordering of its elements.
     * 
     * The sorting algorithm is mergesort. This algorithm offers n*log(n)
     * performance.
     * 
     * @param a
     *            The array to be sorted.
     */
    public static void sortFileArray(File[] a) {
        sortFileArray(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of File objects into
     * ascending order, according to the natural ordering of its elements. The
     * range to be sorted extends from index fromIndex, inclusive, to index
     * toIndex, exclusive.
     * 
     * The sorting algorithm is mergesort. This algorithm offers n*log(n)
     * performance.
     * 
     * @param a
     *            The array to be sorted.
     * @param fromIndex
     *            The index of the first element (inclusive) to be sorted.
     * @param toIndex
     *            The index of the last element (exclusive) to be sorted.
     */
    public static void sortFileArray(File[] a, int fromIndex, int toIndex) {
        int middle;

        if (a == null)
            return;

        if (fromIndex + 1 < toIndex) {
            middle = (fromIndex + toIndex) / 2;
            sortFileArray(a, fromIndex, middle);
            sortFileArray(a, middle, toIndex);
            mergeFileArray(a, fromIndex, toIndex);
        }
    }

    /**
     * Implementation of the merge-part of the mergesort algorithm for File
     * objects.
     * 
     * @param a
     *            The array to be merged.
     * @param fromIndex
     *            The index of the first element (inclusive) to be merged.
     * @param toIndex
     *            The index of the last element (exclusive) to be merged.
     */
    private static void mergeFileArray(File[] a, int fromIndex, int toIndex) {
        int i, j, k, middle, n;
        n = toIndex - fromIndex;
        File[] b = new File[n]; // temporary array

        k = 0;
        middle = (fromIndex + toIndex) / 2;

        // Copy lower half to array b
        for (i = fromIndex; i < middle; i++)
            b[k++] = a[i];
        // Copy upper half to array b in oppsite order
        for (j = toIndex - 1; j >= middle; j--)
            b[k++] = a[j];

        i = 0;
        j = n - 1;
        k = fromIndex;

        // Copy back next-greatest element at each time
        // until i and j cross
        while (i <= j) {
            if (b[i].getName().compareTo(b[j].getName()) < 0)
                a[k++] = b[i++];
            else
                a[k++] = b[j--];
        }
    }

    /**
     * Sorts the specified array of Float objects into ascending order,
     * according to the natural ordering of its elements.
     * 
     * The sorting algorithm is mergesort. This algorithm offers n*log(n)
     * performance.
     * 
     * @param a
     *            The array to be sorted.
     */
    public static void sortFloatArray(Float[] a) {
        sortFloatArray(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of Float objects into
     * ascending order, according to the natural ordering of its elements. The
     * range to be sorted extends from index fromIndex, inclusive, to index
     * toIndex, exclusive.
     * 
     * The sorting algorithm is mergesort. This algorithm offers n*log(n)
     * performance.
     * 
     * @param a
     *            The array to be sorted.
     * @param fromIndex
     *            The index of the first element (inclusive) to be sorted.
     * @param toIndex
     *            The index of the last element (exclusive) to be sorted.
     */
    public static void sortFloatArray(Float[] a, int fromIndex, int toIndex) {
        int middle;

        if (a == null)
            return;

        if (fromIndex + 1 < toIndex) {
            middle = (fromIndex + toIndex) / 2;
            sortFloatArray(a, fromIndex, middle);
            sortFloatArray(a, middle, toIndex);
            mergeFloatArray(a, fromIndex, toIndex);
        }
    }

    /**
     * Implementation of the merge-part of the mergesort algorithm for Float
     * objects.
     * 
     * @param a
     *            The array to be merged.
     * @param fromIndex
     *            The index of the first element (inclusive) to be merged.
     * @param toIndex
     *            The index of the last element (exclusive) to be merged.
     */
    private static void mergeFloatArray(Float[] a, int fromIndex, int toIndex) {
        int i, j, k, middle, n;
        n = toIndex - fromIndex;
        Float[] b = new Float[n]; // temporary array

        k = 0;
        middle = (fromIndex + toIndex) / 2;

        // Copy lower half to array b
        for (i = fromIndex; i < middle; i++)
            b[k++] = a[i];
        // Copy upper half to array b in opposite order
        for (j = toIndex - 1; j >= middle; j--)
            b[k++] = a[j];

        i = 0;
        j = n - 1;
        k = fromIndex;

        // Copy back next-greatest element at each time
        // until i and j cross
        while (i <= j) {
            if (b[i].floatValue() <= b[j].floatValue())
                a[k++] = b[i++];
            else
                a[k++] = b[j--];
        }
    }

}
