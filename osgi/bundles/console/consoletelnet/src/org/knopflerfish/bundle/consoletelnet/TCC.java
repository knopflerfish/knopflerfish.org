/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
 * Telnet command codes and option codes. The RFC where the code is
 * defined is specified for each of code.
 */
public class TCC {
    // General constants.

    /** Telnet null */
    protected static final int NULL = 0;

    protected static final char NULL_char = (char) NULL;

    /** Telnet BELL, control G, 0x07, 7 */
    protected static final int BELL = 7;

    protected static final char BELL_char = (char) BELL;

    /** Telnet BS, control H, 0x08, 8 */
    protected static final int BS = 8;

    protected static final char BS_char = (char) BS;

    /** Telnet HT, control I, 0x09, 9 */
    protected static final int HT = 9;

    protected static final char HT_char = (char) HT;

    /** Telnet LF, control J, 0x0A, 10 */
    protected static final int LF = 10;

    protected static final char LF_char = (char) LF;

    /** Telnet CR, control M, 0x0D, 13 */
    protected static final int CR = 13;

    protected static final char CR_char = (char) CR;

    /** Telnet CRLF, as a string */
    protected static final String CRLF = "\015\012";

    // Telnet protocol constants for commands.

    /** RFC 854: Interpret As Command */
    protected static final int IAC = 255;

    protected static final String IAC_string = String.valueOf((char) IAC);

    /** RFC 854: Don't option */
    protected static final int DONT = 254;

    protected static final String DONT_string = String.valueOf((char) DONT);

    /** RFC 854: Do option */
    protected static final int DO = 253;

    protected static final String DO_string = String.valueOf((char) DO);

    /** RFC 854: Won't option */
    protected static final int WONT = 252;

    protected static final String WONT_string = String.valueOf((char) WONT);

    /** RFC 854: Will option */
    protected static final int WILL = 251;

    protected static final String WILL_string = String.valueOf((char) WILL);

    /** RFC 854: Subnegotiation begin */
    protected static final int SB = 250;

    protected static final String SB_string = String.valueOf((char) SB);

    /** RFC 854: Go Ahead */
    protected static final int GA = 249;

    protected static final String GA_string = String.valueOf((char) GA);

    /** RFC 854: Erase Line */
    protected static final int EL = 248;

    protected static final String EL_string = String.valueOf((char) EL);

    /** RFC 854: Erase Char */
    protected static final int EC = 247;

    protected static final String EC_string = String.valueOf((char) EC);

    /** RFC 854: Are You There */
    protected static final int AYT = 246;

    protected static final String AYT_string = String.valueOf((char) AYT);

    /** RFC 854: Abort Output */
    protected static final int AO = 245;

    protected static final String AO_string = String.valueOf((char) AO);

    /** RFC 854: Interrupt Process */
    protected static final int IP = 244;

    protected static final String IP_string = String.valueOf((char) IP);

    /** RFC 854: Break */
    protected static final int BRK = 243;

    protected static final String BRK_string = String.valueOf((char) BRK);

    /** RFC 854: Data Mark */
    protected static final int DM = 242;

    protected static final String DM_string = String.valueOf((char) DM);

    /** RFC 854: No Operation */
    protected static final int NOP = 241;

    protected static final String NOP_string = String.valueOf((char) NOP);

    /** RFC 854: End of subnegotiation parameters */
    protected static final int SE = 240;

    protected static final String SE_string = String.valueOf((char) SE);

    /** RFC 856: Telnet negotiable option: Transmit Binary */
    protected static final int TRANSMIT_BINARY = 0;

    protected static final String TRANSMIT_BINARY_string = String
            .valueOf((char) TRANSMIT_BINARY);

    /** RFC 857: Telnet negotiable option: ECHO */
    protected static final int ECHO = 1;

    protected static final String ECHO_string = String.valueOf((char) ECHO);

    /** NIC 15391: Telnet negotiable option: Reconnection */
    protected static final int RECONNECTION = 2;

    protected static final String RECONNECTION_string = String
            .valueOf((char) RECONNECTION);

    /** RFC 858: Telnet negotiable option: Supress Go Ahead */
    protected static final int SUPGA = 3;

    protected static final String SUPGA_string = String.valueOf((char) SUPGA);

    /** NIC 7104: Telnet negotiable option: Approx Message Size Negotiation */
    protected static final int AMSN = 4;

    protected static final String AMSN_string = String.valueOf((char) AMSN);

    /** RFC 859: Telnet negotiable option: Status */
    protected static final int STATUS = 5;

    protected static final String STATUS_string = String.valueOf((char) STATUS);

    /** RFC 859: Telnet negotiable option: Status, SEND */
    protected static final int SEND = 1;

    protected static final String SEND_string = String.valueOf((char) SEND);

    /** RFC 859: Telnet negotiable option: Status, IS */
    protected static final int IS = 0;

    protected static final String IS_string = String.valueOf((char) IS);

    /** RFC 860: Telnet negotiable option: Timing mark */
    protected static final int TIMING_MARK = 6;

    protected static final String TIMING_MARK_string = String
            .valueOf((char) TIMING_MARK);

    /** RFC 1091: Telnet negotiable option: Terminal Type */
    protected static final int TERMTYPE = 24;

    protected static final String TERMTYPE_string = String
            .valueOf((char) TERMTYPE);

    /** RFC 1079: Telnet negotiable option: Terminal Speed */
    protected static final int TERMSPEED = 32;

    protected static final String TERMSPEED_string = String
            .valueOf((char) TERMSPEED);

    /** RFC 1184 Telnet negotiable option: Linemode */
    protected static final int LINEMODE = 34;

    protected static final String LINEMODE_string = String
            .valueOf((char) LINEMODE);

    /** RFC 1184 Telnet negotiable option: Linemode, MODE */
    protected static final int MODE = 1;

    protected static final String MODE_string = String.valueOf((char) MODE);

    /** RFC 1184 Telnet negotiable option: Linemode, EDIT */
    protected static final int EDIT = 1;

    protected static final String EDIT_string = String.valueOf((char) EDIT);

    /** RFC 1096: Telnet negotiable option: X Display Location */
    protected static final int XDL = 35;

    protected static final String XDL_string = String.valueOf((char) XDL);

    /** RFC 1572: Telnet negotiable option: New Environment Option */
    protected static final int NEO = 37;

    protected static final String NEO_string = String.valueOf((char) NEO);

    /** RFC 861 Telnet negotiable option: Extended Options List */
    protected static final int EXOPL = 255;

    protected static final String EXOPL_string = String.valueOf((char) EXOPL);
}
