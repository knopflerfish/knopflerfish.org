/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.ant.taskdefs.bundle;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;

public class FileUtil
{

  public static byte [] loadURL(URL url) throws IOException {
    final int     bufSize = 1024 * 2;
    final byte [] buf     = new byte[bufSize];

    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final BufferedInputStream   in   = new BufferedInputStream(url.openStream());
    int n;
    while ((n = in.read(buf)) > 0) {
      bout.write(buf, 0, n);
    }
    try { in.close(); } catch (final Exception ignored) { }
    return bout.toByteArray();
  }

  public static String loadFile(String fname) throws IOException {
    final byte[] bytes = loadURL(new URL("file:" + fname));
    return new String(bytes, "UTF-8");
  }


  /**
   * Load entire contents of a file or URL into a string.
   */
  public static String load(String fileOrURL) throws IOException {
    try {
      final URL url = new URL(fileOrURL);

      return new String(loadURL(url));
    } catch (final Exception e) {
      return loadFile(fileOrURL);
    }
  }

  /**
   * Load an XML-formated file into a DOM-document.
   *
   * @param file The XML file to load.
   * @return DOM document.
   */
  public static Document loadXML(final File file) {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      final DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(file);
    } catch (final Exception e) {
      throw new BuildException("Failed to parse XML file '" +file +"': " +e, e);
    }
  }

  /**
   * Create an empty DOM-document.
   *
   * @param rootElement The name of the root element of the new document.
   * @return DOM document with a root element.
   */
  public static Document createXML(final String rootElement) {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document res = db.newDocument();
      res.appendChild(res.createElement(rootElement));
      return res;
    } catch (final Exception e) {
      throw new BuildException("Failed to create new DOM-document:" +e, e);
    }
  }

  // Always write files using UTF-8.
  static void writeStringToFile(File outFile, String s) throws IOException {
    OutputStreamWriter writer = null;
    try {
      outFile.getParentFile().mkdirs();
      final OutputStream out = new FileOutputStream(outFile);
      writer = new OutputStreamWriter(out, "UTF-8");
      writer.write(s, 0, s.length());
      //      System.out.println("wrote " + outFile);
    } finally {
      try { writer.close(); } catch (final Exception ignored) { }
    }
  }

  public static void writeDocumentToFile(final File outFile, final Document doc) {
    final Source source = new DOMSource(doc);
    final Result result = new StreamResult(outFile);

    // Write the DOM document to the file
    Transformer xformer;
    try {
      xformer = TransformerFactory.newInstance().newTransformer();
      xformer.transform(source, result);
    } catch (final Exception e) {
      throw new BuildException("Failed to write XML to '" + outFile +"', "+e, e);
    }
  }

}
