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

package org.knopflerfish.bundle.consoletelnet;

/**
 * The STATUS command has one sub negotiable parameter that may be IS
 * or SEND.
 */

public class TelnetCommandStatus extends TelnetCommand {

    public TelnetCommandStatus(TelnetSession ts, int commandCode,
            boolean doStatus, boolean show) {
        super(ts, commandCode, doStatus, show);
    }

    /**
     * Option negotiation and execution mechanism. To follow the
     * intentions of RFC 854, a change in status is always followed by
     * a response but if trying to enter a mode that we are already
     * in, no response is returned. This is essential to prevent
     * negotiation loops.
     *
     * @parameter action, one of the telnet protocol basic actions DO, DONT,
     *            WILL, WONT or SE
     * @parameter optionCode, the option code
     * @parameter parameters, a string with optional parameters to the option
     *            code.
     * @return a String with the response to the command.
     */

    public String execute(int action, int optionCode, byte[] parameters) {
        // printCommand(action, optionCode, parameters);
        StringBuffer sb = new StringBuffer();

        switch (action) {
        case TCC.DO:
            if (doStatus == true) {
                // willing and ready, send no resonse,
                // to prevent creation of negotiation loop
            } else {
                doStatus = true;
                sb.append(getWILL());
            }
            break;

        case TCC.WILL:
            if (doStatus == true) {
                // willing and ready, send no resonse,
                // to prevent creation of negotiation loop
            } else {
                doStatus = true;
                sb.append(getDO());
            }
            break;

        case TCC.DONT:
            if (doStatus == true) {
                sb.append(getWONT());
                doStatus = false;
                // now not willing, send no resonse,
                // to prevent creation of negotiation loop
            } else {
                doStatus = false;
            }
            break;

        case TCC.WONT:
            if (doStatus == true) {
                // no appropriate answer to send
                doStatus = false;
                // now not willing, send no resonse,
                // to prevent creation of negotiation loop
            } else {
                doStatus = false;
            }
            break;

        // SB .... SE, command execution, when negotiations
        // are finished and both parties have agreed

        case TCC.SE:
            if (doStatus == true) {
                sb.append(doCommand(action, optionCode, parameters));
            } else { // not in right state
                sb.append(getDONT());
            }
            break;

        default:
            break;
        }
        return sb.toString();
    }

    public String doCommand(int action, int optionCode, byte[] parameters) {
        // printCommand( action, optionCode, parameters);

        StringBuffer sb = new StringBuffer();
        if (parameters != null && (parameters[0] == (byte) TCC.SEND)) {
            // assume SEND
            sb.append(TCC.IAC_string + TCC.SB_string
                    + String.valueOf((char) optionCode) + TCC.IS_string);

            TelnetCommand[] tcs = getCommands();
            for (int i = 0; i < tcs.length; i++) {
                TelnetCommand tc = tcs[i];
                if (tc != null) {
                    sb.append(TCC.WILL_string);
                    sb.append(String.valueOf((char) i));

                    if (tc.getDoStatus() == true) {
                        sb.append(TCC.DO_string);
                        sb.append(String.valueOf((char) i));
                    } else {
                        sb.append(TCC.DONT_string);
                        sb.append(String.valueOf((char) i));
                    }
                }
            }
            sb.append(TCC.IAC_string + TCC.SE_string);
        } else if (parameters != null && (parameters[0] == (byte) TCC.IS)) {

        }
        return sb.toString();
    }
}
