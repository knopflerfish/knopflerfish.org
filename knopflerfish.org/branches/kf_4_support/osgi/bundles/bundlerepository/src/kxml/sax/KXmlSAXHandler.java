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

import java.util.Properties;

/**
 * Interface for SAX handler with kXML
 *
 * @author Didier Donsez (didier.donsez@imag.fr)
 */
public interface KXmlSAXHandler {

	/**
	* Method called when parsing text
	*
	* @param   ch
	* @param   offset
	* @param   length
	* @exception   SAXException
	*/
	public void characters(char[] ch, int offset, int length) throws Exception;

	/**
	* Method called when a tag opens
	*
	* @param   uri
	* @param   localName
	* @param   qName
	* @param   attrib
	* @exception   SAXException
	**/
	public void startElement(
		String uri,
		String localName,
		String qName,
		Properties attrib)
		throws Exception;
	/**
	* Method called when a tag closes
	*
	* @param   uri
	* @param   localName
	* @param   qName
	* @exception   SAXException
	*/
	public void endElement(
		java.lang.String uri,
		java.lang.String localName,
		java.lang.String qName)
		throws Exception;
}