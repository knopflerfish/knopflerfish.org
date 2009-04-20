/*
 * SAX-like interface for kXML.
 * Copyright (c) 2004, Didier Donsez
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Didier Donsez <didier.donsez@imag.fr>
 * Contributor(s):
 *
**/
package kxml.sax;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.kxml.Attribute;
import org.kxml.Xml;
import org.kxml.parser.ParseEvent;
import org.kxml.parser.XmlParser;

/**
 * The KXmlSAXParser extends the XmlParser from kxml. This is a very
 * simple parser that does not take into account the DTD
 *
 * @version 	1.0 08 Nov 2002
 * @version 	1.1 24 Apr 2004
 * @author 	Humberto Cervantes, Didier Donsez
 */
public class KXmlSAXParser extends XmlParser {
	/**
	* The constructor for a parser, it receives a java.io.Reader.
	*
	* @param   reader  The reader
	* @exception   IOException thrown by the superclass
	*/
	public KXmlSAXParser(Reader r) throws IOException {
		super(r);
	}

	/**
	* Parser from the reader provided in the constructor, and call
	* the startElement and endElement in a KxmlHandler
	*
	* @param   reader  The reader
	* @exception   Exception thrown by the superclass
	*/
	public void parseXML(KXmlSAXHandler handler) throws Exception {
		ParseEvent evt = null;
		do {
			evt = read();
			if (evt.getType() == Xml.START_TAG) {
				Properties props = new Properties();
				for (int i = 0; i < evt.getAttributeCount(); i++) {
					Attribute attr = evt.getAttribute(i);
					props.put(attr.getName(), attr.getValue());
				}
				handler.startElement(
					"uri",
					evt.getName(),
					evt.getName(),
					props);
			} else if (evt.getType() == Xml.END_TAG) {
				handler.endElement("uri", evt.getName(), evt.getName());
			} else if (evt.getType() == Xml.TEXT) {
				String text = evt.getText();
				handler.characters(text.toCharArray(),0,text.length());
			} else {
				// do nothing
			}
		} while (evt.getType() != Xml.END_DOCUMENT);
	}
}
