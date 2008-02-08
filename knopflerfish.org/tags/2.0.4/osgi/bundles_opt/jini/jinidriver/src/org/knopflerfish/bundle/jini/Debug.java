/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
package org.knopflerfish.bundle.jini;

/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
public class Debug {
    private static int debuglevel = 0;

    /**
     * DOCUMENT ME!
     *
     * @param level DOCUMENT ME!
     */
    public static void setDebugLevel(int level) {
        debuglevel = level;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static int getDebugLevel() {
        return debuglevel;
    }

    /**
     * DOCUMENT ME!
     *
     * @param level DOCUMENT ME!
     * @param message DOCUMENT ME!
     */
    public static void printDebugInfo(int level, String message) {
        printDebugInfo(level, message, null);
    }

    /**
     * DOCUMENT ME!
     *
     * @param level DOCUMENT ME!
     * @param message DOCUMENT ME!
     * @param e DOCUMENT ME!
     */
    public static void printDebugInfo(int level, String message, Exception e) {
        if (level >= debuglevel) {
            System.err.print("##DEBUG## ");

            if (message != null) {
                System.err.println(message);
            } else {
                System.err.println();
            }

            if (e != null) {
                System.err.println("##Stacktrace##");
                e.printStackTrace(System.err);
            }
        }
    }
}
