/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

package org.knopflerfish.util.metatype;

import org.osgi.framework.*;
import org.osgi.service.metatype.*;

import java.net.URL;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;

/**
 * Main class for testing metatype XML files
 *
 * <pre>
 * Usage: Loader [options]
 *
 * Options:
 *  -help        print help text
 *  -test URL    try loading metatype XML
 *  -dump URL    load and print metatype XML
 *
 * </pre>
 */
public class Main {

  static boolean bDumpXML       = false;
  static boolean bDumpValues    = false;
  static boolean bIncludeValues = false;

  public static void main(String[] argv) {

    int i = 0;

    while(i < argv.length) {
      try {
        if("-test".equals(argv[i]) && i+2 < argv.length) {
          i++;
          testMetatype(argv[i], argv[i+1], false);
          i++;
        } else if("-dump".equals(argv[i]) && i+2 < argv.length) {
          i++;
          testMetatype(argv[i], argv[i+1], true);
          i++;
        } else if("-xml".equals(argv[i])) {
          bDumpXML = true;
        } else if("-values".equals(argv[i])) {
          bDumpValues = true;
        } else if("-ivalues".equals(argv[i])) {
          bIncludeValues = true;
        } else if("-help".equals(argv[i])) {
          printHelp(System.out);
          System.exit(0);
        } else if(argv[i].endsWith(".jar") || argv[i].endsWith(".xml")) {
          testJar(argv[i], true);
        } else {
          System.err.println("Unknown option '" + argv[i] + "'");
          printHelp(System.err);
          System.exit(0);
        }
      } catch (XMLException e) {
        System.err.println(argv[i]);
        System.err.println("element '" + e.getXMLElement().getName() +
                           "' at line " + e.getXMLElement().getLineNr());
        System.err.println(e.getMessage());
      } catch (Exception e) {
        e.printStackTrace();
        System.err.println(e.getMessage());
      }
      i++;
    }
  }

  static void printHelp(PrintStream ps) {
    ps.println("Usage: Main [options]\n" +
               "\n" +
               " -help        print help text\n" +
               " -test URL    try loading metatype XML\n" +
               " -dump URL    load and print metatype XML\n");
  }


  static void testJar(String urlDef,
                      boolean bDump) throws Exception {
    if(urlDef.indexOf(":") == -1) {
      urlDef = "file:" + urlDef;
    }

    InputStream in = null;
    try {
      URL url = new URL(urlDef);
      in = url.openStream();
      JarInputStream jar = new JarInputStream(in);

      Manifest mf = jar.getManifest();

      String mtURL = mf.getMainAttributes().getValue(SystemMetatypeProvider.ATTRIB_METATYPEURL);

      String cmURL = mf.getMainAttributes().getValue(SystemMetatypeProvider.ATTRIB_CMDEFAULTSURL);

      if(mtURL == null || "".equals(mtURL)) {
        mtURL = SystemMetatypeProvider.METATYPE_RESOURCE;
      }
      if(mtURL.startsWith("!")) {
        mtURL = "jar:" + urlDef + mtURL;
      }

      if(cmURL == null || "".equals(cmURL)) {
        cmURL = SystemMetatypeProvider.CMDEFAULTS_RESOURCE;
      }
      if(cmURL.startsWith("!")) {
        cmURL = "jar:" + urlDef + cmURL;
      }

      testMetatype(mtURL, cmURL, bDump);

    } finally {
      try { in.close(); } catch (Exception ignored) { }
    }
  }

  static void testMetatype(String urlDef,
                           String urlVal,
                           boolean bDump) throws Exception {
    if(urlDef.endsWith(".jar")) {
      testJar(urlDef, bDump);
    } else {
      if(urlDef.indexOf(":") == -1) {
        urlDef = "file:" + urlDef;
      }
      if(!"".equals(urlVal) && urlVal.indexOf(":") == -1) {
        urlVal = "file:" + urlVal;
      }
      MTP mtp = Loader.loadMTPFromURL(null, new URL(urlDef));
      //      System.out.println("loaded metatype XML from " + urlDef);
      List propList = new ArrayList();
      if(!"".equals(urlVal)) {
        propList = Loader.loadDefaultsFromURL(mtp, new URL(urlVal));
        //      System.out.println("loaded defaults XML from " + urlVal);
      }
      if(bDumpXML || bDumpValues) {
        if(bDumpXML) {
          Loader.printMetatypeXML(mtp,
                                  mtp.getPids(),
                                  mtp.getFactoryPids(),
                                  true,
                                  true,
                                  bIncludeValues  ? propList : null,
                                  new PrintWriter(System.out, true));
        }
        if(bDumpValues) {
          Loader.printValuesXML(propList,
                                true,
                                new PrintWriter(System.out, true));
        }
      } else {
        if(bDump) {
          System.out.println(printMTP(mtp));
        }
      }
    }
  }

  static String printMTP(MTP mtp) {
    StringBuffer sb = new StringBuffer();

    sb.append(printPIDs(mtp, mtp.getPids(), " (service)"));

    sb.append(printPIDs(mtp, mtp.getFactoryPids(), " (factory)"));

    return sb.toString();
  }

  static String printPIDs(MTP mtp, String[] pids, String suffix) {
    StringBuffer sb = new StringBuffer();

    for(int i = 0; i < pids.length; i++) {
      String pid = pids[i];
      AttributeDefinition[] ads =
        mtp.getObjectClassDefinition(pid, null)
        .getAttributeDefinitions(ObjectClassDefinition.ALL);

      sb.append(pid + suffix + "\n");
      sb.append(printDefinition(ads));
    }

    return sb.toString();
  }

  static String printDefinition(AttributeDefinition[] ads) {
    StringBuffer sb = new StringBuffer();

    for(int i = 0; i < ads.length; i++) {
      StringBuffer sb2 = new StringBuffer();
      sb2.append("  " + ads[i].getName());
      padTo(sb2, 25, ' ');
      sb2.append("  ");
      if(ads[i].getCardinality() < 0) {
        sb2.append("Vector of ");
      }
      sb2.append(AD.getClass(ads[i].getType()).getName().substring(10));
      if(ads[i].getCardinality() > 0) {
        sb2.append("[]");
      }
      padTo(sb2, 45, ' ');
      sb2.append(" " + AD.toString(ads[i].getDefaultValue()));


      padTo(sb2, 60, ' ');
      sb2.append(" '" + ads[i].getDescription() + "'");

      sb.append(sb2.toString());


      sb.append("\n");
    }
    return sb.toString();
  }

  static void padTo(StringBuffer sb, int len, char c) {
    while(sb.length() < len) {
      sb.append(c);
    }
  }
}
