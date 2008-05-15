/*
 * Generic Metadata XML Parser
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
package fr.imag.adele.metadataparser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import kxml.sax.KXmlSAXParser;


/**
 * handles the metadata in XML format
 * (use kXML (http://kxml.enhydra.org/) a open-source very light weight XML parser
 * @version 	1.00 11 Nov 2003
 * @author 	Didier Donsez
 */
public class KXmlMetadataHandler /*implements MetadataHandler*/ {

	private XmlCommonHandler handler;

	public KXmlMetadataHandler() {
		handler = new XmlCommonHandler();
	}

	/**
	* Called to parse the InputStream and set bundle list and package hash map
	*/
	public void parse(InputStream is) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		KXmlSAXParser parser;
		parser = new KXmlSAXParser(br);
		parser.parseXML(handler);
	}

	/**
	 * return the metadata
	 * @return a Objet
	 */
	public Object getMetadata() {
		return handler.getRoot();
	}

	public void addType(String qname, Class clazz) {
		handler.addType(qname, clazz);
	}

	public void setDefaultType(Class clazz) {
		handler.setDefaultType(clazz);
	}
}
