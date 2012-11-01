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
 * This abstract class is designed to make it simpler to create and
 * add new telnet commands in this implementation. Each command is a
 * TelnetCommand object, has a callback mechanism to the session it is
 * instantiated in and has one execute method.
 */

public abstract class TelnetCommand {
    public TelnetSession ts; // the instantiating telnet session

    protected int commandCode; // the command code of this command

    protected boolean doStatus; // true if the server is ready to execute the

    // command

    protected boolean show; // if the command will be shown at initial

    // command negotiation

    public TelnetCommand() {
    } // parameterless dummy constructor

    /**
     * Parameters in the constructor:
     *
     * @parameter ts TelnetSession, to provide callback to the telnet session
     * @parameter int commandCode
     * @parameter boolean doStatus, if true the command has been asked to be
     *            active
     * @parameter boolean show, if true the commands is shown
     */
    public TelnetCommand(TelnetSession ts, int commandCode, boolean doStatus,
            boolean show) {
        this.ts = ts;
        this.commandCode = commandCode;
        this.doStatus = doStatus;
        this.show = show;
    }

    /** WILL string of command */

    public String getWILL() {
        return TCC.IAC_string + TCC.WILL_string
                + String.valueOf((char) commandCode);
    }

    /** WONT string of command */

    public String getWONT() {
        return TCC.IAC_string + TCC.WONT_string
                + String.valueOf((char) commandCode);
    }

    /** DONT string of command */

    public String getDONT() {
        return TCC.IAC_string + TCC.DONT_string
                + String.valueOf((char) commandCode);
    }

    /** DO string of command */

    public String getDO() {
        return TCC.IAC_string + TCC.DO_string
                + String.valueOf((char) commandCode);
    }

    public void setDoStatus(boolean state) {
        doStatus = state;
    }

    public boolean getDoStatus() {
        return doStatus;
    }

    public boolean getShow() {
        return show;
    }

    /**
     * * Get all registered commands from the session
     */

    public TelnetCommand[] getCommands() {
        return ts.getCommands();
    }

    /**
     * * Debug printout
     */

    void printCommand(int action, int optionCode, byte[] parameters) {
        System.out.print("Telnet Command code: " + String.valueOf(action)
                + " option: " + String.valueOf(optionCode) + " status now: "
                + String.valueOf(doStatus));
        if (parameters != null) {
            System.out.print(" parameters:");
            for (int i = 0; i < parameters.length; i++) {
                System.out.print(" " + String.valueOf(parameters[i]));
            }
            System.out.println();
        } else {
            // System.out.println(" no parameters");
            System.out.println();
        }
    }

    /**
     * Option negotiation and execution mechanism. To follow the
     * intentions of RFC 854, a change in status is always followed by
     * a response but if trying to enter a mode that we are already
     * in, no response is returned. This is essential to prevent
     * negotiation loops.
     *
     * @parameter action, one of the telnet protocol basic actions
     *            DO, DONT, WILL, WONT or SE
     * @parameter optionCode, the option code
     * @parameter parameters, a byte array with optional parameters,
     *            addition data to the option command.
     *
     * @return a String with the response of the command.
     */

    public abstract String execute(int action, int optionCode, byte[] parameters);
}
