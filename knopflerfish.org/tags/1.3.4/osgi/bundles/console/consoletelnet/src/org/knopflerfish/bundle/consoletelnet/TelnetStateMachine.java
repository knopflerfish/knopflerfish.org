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

import java.util.Vector;

public class TelnetStateMachine {

    private TelnetSession telnetSession; // callback reference

    private int state = 0; // initial state

    private Vector subNegBuffer; // Telnet command buffer

    private int subCode; // sub command code

    public TelnetStateMachine(TelnetSession telnetSession) {
        state = 0;
        subCode = 0;
        subNegBuffer = new Vector();
        this.telnetSession = telnetSession;
    }

    /** Return state of command state machine */
    public int getState() {
        return state;
    }

    /** Set state of command state machine */
    public void setState(int s) {
        state = s;
    }

    /**
     * * Telnet parser, a state machine * to extract telnet commands from an
     * input stream.
     */

    public int nextState(int state, int code) {
        int newState = 0;
        // System.out.println("State = " + String.valueOf(state) + " Code = " +
        // String.valueOf(code));

        switch (state) {
        case 0: // data mode, look for IAC only
            switch (code) {
            case TCC.IAC:
                newState = 1;
                break;
            default:
                newState = 0;
                break;
            }
            break;

        case 1: // command mode
            switch (code) {
            case TCC.DONT: // One option byte to read
                newState = 20;
                break;
            case TCC.DO: // One option byte to read
                newState = 21;
                break;
            case TCC.WONT: // One option byte to read
                newState = 22;
                break;
            case TCC.WILL: // One option byte to read
                newState = 23;
                break;
            case TCC.SB: // Sub negotiation start
                subNegBuffer.removeAllElements();
                newState = 30;
                break;
            case TCC.GA:
                telnetSession.execGA();
                newState = 0;
                break;
            case TCC.EL:
                telnetSession.execEL();
                newState = 0;
                break;
            case TCC.EC:
                telnetSession.execEC();
                newState = 0;
                break;
            case TCC.AYT:
                telnetSession.execAYT();
                newState = 0;
                break;
            case TCC.AO:
                telnetSession.execAO();
                newState = 0;
                break;
            case TCC.IP:
                telnetSession.execIP();
                newState = 0;
                break;
            case TCC.BRK:
                telnetSession.execBRK();
                newState = 0;
                break;
            case TCC.DM:
                telnetSession.execDM();
                newState = 0;
                break;
            case TCC.NOP:
                telnetSession.execNOP();
                newState = 0;
                break;
            case TCC.SE: // Sub negotiation end
                // convert the vector to a byte array
                byte[] subNegArray = new byte[subNegBuffer.size() - 1];
                for (int i = 0; i < subNegBuffer.size() - 1; i++) {
                    Byte b = (Byte) subNegBuffer.elementAt(i);
                    subNegArray[i] = b.byteValue();
                }
                telnetSession.execSE(subCode, subNegArray);
                newState = 0;
                break;
            default:
                break;
            }
            break;

        case 20: // DONT command option code
            telnetSession.execDONT(code);
            newState = 0;
            break;

        case 21: // DO command option code
            telnetSession.execDO(code);
            newState = 0;
            break;

        case 22: // WONT command option code
            telnetSession.execWONT(code);
            newState = 0;
            break;

        case 23: // WILL command option code
            telnetSession.execWILL(code);
            newState = 0;
            break;

        case 30: // Sub negotiation sub command code
            switch (code) {
            default:
                subCode = code;
                newState = 31;
                break;
            }
            break;

        case 31: // Sub negotiation parameter collection mode end ?
            switch (code) {
            case TCC.IAC:
                subNegBuffer.addElement(new Byte((byte) code));
                newState = 32;
                break;
            default:
                subNegBuffer.addElement(new Byte((byte) code));
                newState = 31;
                break;
            }
            break;

        case 32: // Sub negotiation parameter collection mode end ?
            switch (code) {
            case TCC.IAC: // Double written IAC, skip this
                newState = 31;
                break;
            case TCC.SE:
                // convert the vector to a byte array
                byte[] subNegArray = new byte[subNegBuffer.size() - 1];
                for (int i = 0; i < subNegBuffer.size() - 1; i++) {
                    Byte b = (Byte) subNegBuffer.elementAt(i);
                    subNegArray[i] = b.byteValue();
                }
                telnetSession.execSE(subCode, subNegArray);
                newState = 0;
                break;
            }
        default:
            break;
        }

        setState(newState);
        return state;
    }

} // TelnetStateMachine
