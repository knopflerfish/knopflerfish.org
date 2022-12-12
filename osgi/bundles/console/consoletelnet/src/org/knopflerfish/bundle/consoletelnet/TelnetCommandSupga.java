/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.consoletelnet;

/**
 * * The SUPGA command has no sub negotiable parameter
 */
public class TelnetCommandSupga extends TelnetCommand {

    public TelnetCommandSupga(TelnetSession ts, int commandCode,
            boolean servDO, boolean show) {
        super(ts, commandCode, servDO, show);
    }

    /**
     * Option negotiation and execution mechanism
     *
     * To follow the intentions of RFC 854, a change in status
     * is always followed by a response but if trying to enter a mode
     * that we are already in, no response is returned.
     *
     * This is essential to prevent negotiation loops.
     *
     * @param action, one of the telnet protocol basic actions DO, DONT, WILL, WONT or SE
     * @param optionCode, the option code
     * @param parameters, a string with optional parameters to the option code.
     *
     * @return a String with the response to the command.
     */
    @Override
    public String execute(int action, int optionCode, byte[] parameters) {
        // printCommand(action, optionCode, parameters);
        StringBuilder sb = new StringBuilder();

        switch (action) {
        case TCC.DO:
        case TCC.WILL:
            //noinspection StatementWithEmptyBody
            if (doStatus) {
                // willing and ready, send no response,
                // to prevent creation of negotiation loop
            } else {
                doStatus = true;
            }
            break;

        case TCC.DONT:
            if (doStatus) {
                sb.append(getWONT());
                // now not willing, send no response,
                // to prevent creation of negotiation loop
            }
            doStatus = false;
            break;

        case TCC.WONT:
            //noinspection StatementWithEmptyBody
            if (doStatus) {
                // no appropriate answer to send
                // now not willing, send no response,
                // to prevent creation of negotiation loop
            }
            doStatus = false;
            break;

        // SB .... SE, command execution, when negotiations
        // are finished and both parties have agreed

        case TCC.SE:
            if (!doStatus) { // not in right state
                sb.append(getDONT());
            }
            break;

        default:
            break;
        }
        return sb.toString();
    }
}
