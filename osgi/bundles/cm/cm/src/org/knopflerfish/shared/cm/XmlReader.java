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

package org.knopflerfish.shared.cm;

// TODO
// - Start-Tag, End-Tag, EmptyElement-Tag
// - positionAtXXX naming of methods
// - readToNextTag vs readStartOfTag

import java.io.PushbackReader;
import java.util.Dictionary;
import java.util.Hashtable;

public class XmlReader {
    protected final static String EOF = "EOF";

    protected void startElement(String elementType, Dictionary attributes)
            throws Exception {
    }

    protected void endElement(String elementType, Dictionary attributes,
            String content) throws Exception {
    }

    protected void read(PushbackReader r) throws Exception {
        while (readToNextTag(r)) {

            if (tagShouldBeIgnored(r)) {
                ignoreTag(r);
            } else if (isEndTag(r)) {
                return;
            } else {
                readElement(r);
            }
        }
    }

    boolean readToNextTag(PushbackReader r) throws Exception {
        boolean foundATag = true;
        try {
            char c = readAndPushbackNextNonWhitespaceChar(r);
            throwIfNotExpectedChar(c, '<', r);
        } catch (Exception e) {
            if (EOF.equals(e.getMessage())) {
                foundATag = false;
            } else {
                throw e;
            }
        }
        return foundATag;
    }

    boolean tagShouldBeIgnored(PushbackReader r) throws Exception {
        char first = readNextChar(r);
        char second = readNextChar(r);
        r.unread(second);
        r.unread(first);
        return second == '!' || second == '?';
    }

    void ignoreTag(PushbackReader r) throws Exception {
        char c = readNextChar(r);
        throwIfNotExpectedChar(c, '<');
        do {
            c = readNextChar(r);
        } while (c != '>');
        throwIfNotExpectedChar(c, '>');
    }

    boolean isEndTag(PushbackReader r) throws Exception {
        char first = readNextChar(r);
        char second = readNextChar(r);
        r.unread(second);
        r.unread(first);
        return first == '<' && second == '/';
    }

    // REFACTOR TO:
    // notEmptyElement = readStartTag(r)
    // if(notEmptyElement) readContent(r)
    // readEndTag
    void readElement(PushbackReader r) throws Exception {
        readStartOfTag(r);
        String elementType = readElementType(r);

        Dictionary attributes = readAttributes(r);
        boolean notEmptyElement = readEndOfStartTag(r);

        startElement(elementType, attributes);
        String content = null;
        if (notEmptyElement) {
            content = readContent(r, elementType, attributes);
            readEndTag(elementType, r);
        } else {
        }
        endElement(elementType, attributes, content);
    }

    void readEndTag(String startTagElementType, PushbackReader r)
            throws Exception {
        readStartOfEndTag(r);
        readAndMatchElementType(startTagElementType, r);
        readEndOfTag(r);
    }

    void readAndMatchElementType(String elementType, PushbackReader r)
            throws Exception {
        readAndMatchXMLName(elementType, r);
        char c = readAndPushbackNextChar(r);
        // Turn into a method
        if (c != '>' && c != '/' && isNotXMLWhitespace(c)) {
            throwMessage("Error while reading element type after: "
                    + elementType);
        }
    }

    void readAndMatchXMLName(String elementType, PushbackReader r)
            throws Exception {
        int pos = 0;
        char c = readNextNonWhitespaceChar(r);
        if (isXMLNameStartChar(c)) {
            throwIfNotExpectedChar(c, elementType.charAt(pos++));
        } else {
            throwMessage("Error while reading XML name: " + c
                    + " is not a valid start char.");
        }
        c = readNextChar(r);
        while (isXMLNameChar(c)) {
            throwIfNotExpectedChar(c, elementType.charAt(pos++));
            c = readNextChar(r);
        }
        r.unread(c);
    }

    String readElementType(PushbackReader r) throws Exception {
        String elementType = readXMLName(r);
        char c = readAndPushbackNextChar(r);
        // Turn into a method
        if (c != '>' && c != '/' && isNotXMLWhitespace(c)) {
            throwMessage("Error while reading element type after: "
                    + elementType);
        }
        return elementType;
    }

    String readXMLName(PushbackReader r) throws Exception {
        char c = readNextNonWhitespaceChar(r);
        StringBuffer xmlName = new StringBuffer();
        if (isXMLNameStartChar(c)) {
            xmlName.append(c);
        } else {
            throwMessage("Error while reading XML name: " + c
                    + " is not a valid start char.");
        }
        c = readNextChar(r);
        while (isXMLNameChar(c)) {
            xmlName.append(c);
            c = readNextChar(r);
        }
        r.unread(c);
        return xmlName.toString();
    }

    Dictionary readAttributes(PushbackReader r) throws Exception {
        Dictionary attributes = null;
        while (nextNonWhitespaceIsANameStartChar(r)) {
            String name = readXMLName(r);
            char c = readNextChar(r);
            throwIfNotExpectedChar(c, '=');
            String value = readAttributeValue(r);
            if (attributes == null) {
                attributes = new Hashtable();
            }
            attributes.put(name, value);
        }
        return attributes;
    }

    String readAttributeValue(PushbackReader r) throws Exception {
        char c = readNextChar(r);
        throwIfNotExpectedChar(c, '\"');
        StringBuffer value = new StringBuffer();
        c = readNextChar(r);
        while (isXMLAttributeValueChar(c)) {
            if (isXMLEscapeCharacter(c)) {
                c = readEscapedCharacter(r);
            }
            value.append(c);
            c = readNextChar(r);
        }
        throwIfNotExpectedChar(c, '\"');
        return value.toString();
    }

    boolean nextNonWhitespaceIsANameStartChar(PushbackReader r)
            throws Exception {
        char c = readAndPushbackNextNonWhitespaceChar(r);
        return isXMLNameStartChar(c);
    }

    void readStartOfTag(PushbackReader r) throws Exception {
        char c = readNextNonWhitespaceChar(r);
        throwIfNotExpectedChar(c, '<');
    }

    void readStartOfEndTag(PushbackReader r) throws Exception {
        char c = readNextNonWhitespaceChar(r);
        throwIfNotExpectedChar(c, '<');
        c = readNextChar(r);
        throwIfNotExpectedChar(c, '/');
    }

    void readEndOfTag(PushbackReader r) throws Exception {
        char c = readNextNonWhitespaceChar(r);
        throwIfNotExpectedChar(c, '>');
    }

    boolean readEndOfStartTag(PushbackReader r) throws Exception {
        char c = readNextNonWhitespaceChar(r);
        boolean emptyElement = c == '/';
        if (emptyElement) {
            c = readNextChar(r);
        }
        throwIfNotExpectedChar(c, '>');
        return !emptyElement;
    }

    String readContent(PushbackReader r, String elementType,
            Dictionary attributes) throws Exception {
        char c = readAndPushbackNextNonWhitespaceChar(r);
        if (c == '<') {
            read(r);
            return "";
        }
        return readText(r);
    }

    String readText(PushbackReader r) throws Exception {
        char c = readNextNonWhitespaceChar(r);
        StringBuffer text = null;
        while (c != '<') {
            if (text == null) {
                text = new StringBuffer();
            }
            if (isXMLEscapeCharacter(c)) {
                c = readEscapedCharacter(r);
            }
            text.append(c);
            c = readNextChar(r);
        }
        r.unread(c);
        if (text == null) {
            return null;
        }
        return text.toString().trim();
    }

    char readEscapedCharacter(PushbackReader r) throws Exception {
        StringBuffer escapeCode = new StringBuffer();
        char first = readNextChar(r);
        boolean isCharCode = first == '#';
        if (!isCharCode) {
            escapeCode.append(first);
        }
        char second = readNextChar(r);
        boolean isHexCode = isCharCode && second == 'x';
        if (!isHexCode) {
            escapeCode.append(second);
        }

        char next = readNextChar(r);
        while (next != ';') {
            escapeCode.append(next);
            next = readNextChar(r);
        }

        String s = escapeCode.toString();
        char c = 0;
        if (isHexCode) {
            c = (char) Integer.parseInt(s, 16);
        } else if (isCharCode) {
            c = (char) Integer.parseInt(s);
        } else if ("amp".equals(s)) {
            c = '&';
        } else if ("lt".equals(s)) {
            c = '<';
        } else if ("gt".equals(s)) {
            c = '>';
        } else if ("apos".equals(s)) {
            c = '\'';
        } else if ("quot".equals(s)) {
            c = '\"';
        } else {
            throwMessage("Invalid or unsupported escape character: " + s);
        }
        return c;
    }

    char readNextNonWhitespaceChar(PushbackReader r) throws Exception {
        char c = readNextChar(r);
        while (isXMLWhitespace(c)) {
            c = readNextChar(r);
        }
        return c;
    }

    char readAndPushbackNextNonWhitespaceChar(PushbackReader r)
            throws Exception {
        char c = readNextNonWhitespaceChar(r);
        r.unread(c);
        return c;
    }

    static char readAndPushbackNextChar(PushbackReader r) throws Exception {
        char c = readNextChar(r);
        r.unread(c);
        return c;
    }

    static char readNextChar(PushbackReader r) throws Exception {
        int c = r.read();
        if (c == -1) {
            throw new Exception(EOF);
        }
        return (char) c;
    }

    // Character classification
    boolean isXMLNameStartChar(char c) {
        return isXMLLetter(c) || ('_' == c) || (':' == c);
    }

    boolean isXMLNameChar(char c) {
        return isXMLLetter(c) || isXMLDigit(c) || ('.' == c) || ('-' == c)
                || ('_' == c) || (':' == c);
        // Missing CombiningChar and Extender in XML 1.0 spec
    }

    boolean isXMLAttributeValueChar(char c) {
        return c != '\"';
    }

    boolean isXMLLetter(char c) {
        return Character.isLetter(c);
        // Needs to be verified against XML 1.0 spec
    }

    boolean isXMLDigit(char c) {
        return Character.isDigit(c);
        // Needs to be verified against XML 1.0 spec
    }

    boolean isXMLWhitespace(char c) {
        return Character.isWhitespace(c);
        // Needs to be verified against XML 1.0 spec
    }

    boolean isNotXMLWhitespace(char c) {
        return !isXMLWhitespace(c);
    }

    boolean isXMLEscapeCharacter(char c) {
        return c == '&';
    }

    // Error helpers
    protected void throwIfNotExpectedChar(char c, char expected)
            throws Exception {
        if (c != expected) {
            throw new Exception("Expected " + expected + " but found " + c); // TODO
        }
    }

    protected void throwIfNotExpectedChar(char c, char expected,
            PushbackReader pbr) throws Exception {
        if (c != expected) {
            StringBuffer msg = new StringBuffer();
            msg.append("Expected " + expected + " but found " + c + "\n");
            msg.append("At:");
            for (int i = 0; i < 20; ++i) {
                int rc = pbr.read();
                if (rc == -1) {
                    break;
                }
                msg.append((char) rc);
            }
            throw new Exception(msg.toString()); // TODO
        }
    }

    protected void throwMessage(String message) throws Exception {
        throw new Exception(message); // TODO
    }

    protected void throwIfMissingAttribute(String element, String name,
            String value) throws Exception {
        if (value == null) {
            throwMessage("Missing " + name + " attribute in <" + element
                    + "> tag.");
        }
    }

    protected void throwMisplacedTagException(String element) throws Exception {
        throw new Exception("Misplaced <" + element + "> tag.");
    }
}
