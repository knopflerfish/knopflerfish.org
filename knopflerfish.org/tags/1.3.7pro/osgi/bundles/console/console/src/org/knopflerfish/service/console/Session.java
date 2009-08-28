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

package org.knopflerfish.service.console;

import java.util.Dictionary;

/**
 * Control interface for a command session.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 * @see ConsoleService
 */
public interface Session {
    /**
     * Constant for the session property that is the session's Authorization
     * object.
     */
    String PROPERTY_AUTHORIZATION = "Authorization";

    /**
     * Constant for the session property that indicates that the session is a
     * TCP session.
     */
    String PROPERTY_TCP = "tcp";

    /**
     * Constant for the session property that indicates that the session should
     * be closed if the user logs out. If this is not set, the session will be
     * kept open, but no Authorization object will be present and hence the user
     * will have no special rights.
     */
    String PROPERTY_EXIT_ON_LOGOUT = "ExitOnLogout";

    /**
     * Abort current command in session. Sends an interrupt to the thread
     * executing the command. The command should terminate as soon as possible
     * when it receives an interrupt.
     * 
     * @exception java.lang.IllegalStateException
     *                If this session is closed.
     */
    void abortCommand();

    /**
     * Get escape character.
     * 
     * @return Current escape character
     * @exception java.lang.IllegalStateException
     *                If this session is closed.
     * @see #setEscapeChar
     */
    char getEscapeChar();

    /**
     * Set escape character. The escape character is used to escape the
     * interrupt string and the escape character.
     * 
     * @param ch
     *            new escape character
     * @exception java.lang.IllegalStateException
     *                If this session is closed.
     */
    void setEscapeChar(char ch);

    /**
     * Get interrupt string.
     * 
     * @return Current interrupt string
     * @exception java.lang.IllegalStateException
     *                If this session is closed.
     * @see #setInterruptString
     */
    String getInterruptString();

    /**
     * Set interrupt string. When the read thread sees this string in the input
     * it will call {@link #abortCommand() abortCommand()} for this session.
     * 
     * @param str
     *            new interrupt string
     * @exception java.lang.IllegalStateException
     *                If this session is closed.
     */
    void setInterruptString(String str);

    /**
     * Get session name.
     * 
     * @return Current session name
     */
    String getName();

    /**
     * Close session. Interrupts all threads and waits for them to terminate.
     * 
     */
    void close();

    /**
     * Add session event listener. This listener will be called when the session
     * is closed.
     * 
     * @param l
     *            session listener
     * @exception java.lang.IllegalStateException
     *                If this session is closed.
     */
    void addSessionListener(SessionListener l);

    /**
     * Remove session event listener.
     * 
     * @param l
     *            session listener
     * @exception java.lang.IllegalStateException
     *                If this session is closed.
     * @see #addSessionListener
     */
    void removeSessionListener(SessionListener l);

    /**
     * Returns the property information tied to this session.
     * 
     * @return Properties associated with this Session.
     */
    Dictionary getProperties();
}
