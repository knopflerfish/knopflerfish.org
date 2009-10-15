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

package org.knopflerfish.service.um.useradmin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for one-way encryption of, for example, user passwords.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public class PasswdUtil {
    private static MessageDigest md;
    static {
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
        }
    }

    /**
     * Create salt and the a digest of the salt and the message.
     * 
     * @param message
     *            the message to calculate a digest for
     * @param digest
     *            buffer to write the resulting digest to, must be at least 20
     *            bytes long.
     * @return the salt used when calculating the digest.
     */
    public static synchronized byte[] saltAndDigest(String message,
            byte[] digest) {
        byte[] salt = Long.toString(System.currentTimeMillis()).getBytes();
        try {
            byte[] msg = message.getBytes("UTF-8");
            md.update(salt);
            md.update(msg);
            md.digest(digest, 0, 20);
        } catch (Exception ex) {
        }

        return salt;
    }

    /**
     * Create a digest from salt and a message.
     * 
     * @param message
     *            the message to calculate a digest for
     * @param salt
     *            the salt to use.
     * @return the resulting digest, will be 20 bytes long.
     */
    public static synchronized byte[] digest(String message, byte[] salt) {
        try {
            byte[] msg = message.getBytes("UTF-8");
            md.update(salt);
            md.update(msg);
        } catch (Exception ex) {
        }
        return md.digest();
    }

}
