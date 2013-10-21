/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.repository.xml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import org.kxml2.io.KXmlParser;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RepositoryXmlParser {
  
  public static class ParseResult {
    String name;
    Long increment;
    Collection<Resource> resources;
  }

  public static Collection<Resource> parse(String url) throws Exception {
    final URL repositoryUrl = new URL(url);
    
    InputStream is = repositoryUrl.openStream(); 

    Collection<Resource> rs = parse(is);
    
    ensureOsgiContentUrlsAreAbsolute(repositoryUrl, rs);

    return rs;
  }

  public static Collection<Resource> parse(InputStream is)
      throws XmlPullParserException, IOException, Exception {
    XmlPullParser p = new KXmlParser();
    p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    p.setInput(new InputStreamReader(is));
    ArrayList<Resource> rs = new ArrayList<Resource>();
    // TODO Parse the attributes in the repository tag?
    for (int  e = p.getEventType();
              e != XmlPullParser.END_DOCUMENT;
              e = p.next()) {
      Resource r = null;
      if (e == XmlPullParser.START_TAG) {
        r = parseResource(p);
      }
      if (r != null) {
        rs.add(r);
      }
    }
    return rs;
  }

  private static void ensureOsgiContentUrlsAreAbsolute(URL repositoryUrl, Collection<Resource> rs) throws Exception {
    for(Resource r : rs) {
      for(Capability c : r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE)) {
        String url = (String)c.getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        url = new URL(repositoryUrl, url).toExternalForm();
        c.getAttributes().put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, url);
      }
    }
  }

  private static Resource parseResource(XmlPullParser p) throws Exception {
    if ("resource".equalsIgnoreCase(p.getName())) {
      return parseReqsAndCaps(p);
    } else {
      return null;
    }
  }

  private static Resource parseReqsAndCaps(XmlPullParser p) throws Exception {
    if (p.isEmptyElementTag()) {
      p.next();
      return null;
    }
    ResourceImpl r = new ResourceImpl();
    int startDepth = p.getDepth();
    p.next();
    while( !(p.getEventType() == XmlPullParser.END_DOCUMENT)
        && !(p.getEventType() == XmlPullParser.END_TAG 
        && p.getDepth() == startDepth)) {
      if (p.getEventType() != XmlPullParser.START_TAG) {
        p.next();
        continue;
      } 
      if ("requirement".equalsIgnoreCase(p.getName())) {
        RequirementImpl req = parseReq(p);
        if(req != null) {
          req.d.resource = r;
          r.addReq(req);
        }
        p.next();
        continue;
      }
      if ("capability".equalsIgnoreCase(p.getName())) {
        CapabilityImpl cap = parseCap(p);
        if(cap != null) {
          cap.d.resource = r;
          r.addCap(cap);
        }
        p.next();
        continue;
      }
    }
    return r;
  }

 

  private static CapabilityImpl parseCap(XmlPullParser p) throws Exception {
    Data d = parseData(p);
    if(d == null) return null;
    return new CapabilityImpl(d);
  }

  private static RequirementImpl parseReq(XmlPullParser p) throws Exception {
    Data d = parseData(p);
    if(d == null) return null;
    return new RequirementImpl(d);
  }
  
  private static Data parseData(XmlPullParser p) throws Exception {
    Data d = new Data();
    d.namespace = p.getAttributeCount() == 1 && "namespace".equals(p.getAttributeName(0)) ? p.getAttributeValue(0) : "";
    if (p.isEmptyElementTag()) {
      p.next(); // We still get an END_TAG
      return d;
    }
    int startDepth = p.getDepth();
    p.next();
    while( !(p.getEventType() == XmlPullParser.END_DOCUMENT)
        && !(p.getEventType() == XmlPullParser.END_TAG 
        && p.getDepth() == startDepth)) {
      if (p.getEventType() != XmlPullParser.START_TAG) {
        p.next();
        continue;
      }
      if ("attribute".equalsIgnoreCase(p.getName())) {
        parseAttribute(p, d);
        p.next();
        continue;
      }
      if ("directive".equalsIgnoreCase(p.getName())) {
        parseDirective(p, d);
        p.next();
        continue;
      }
    }
    return d;
  }



  private static void parseDirective(XmlPullParser p, Data d) throws Exception {
    String name = null;
    String value = null;
    for(int i = 0; i < p.getAttributeCount(); ++i) {
      if("name".equalsIgnoreCase(p.getAttributeName(i))) {
        name = p.getAttributeValue(i);
        continue;
      }
      if("value".equalsIgnoreCase(p.getAttributeName(i))) {
        value = p.getAttributeValue(i);
        continue;
      }
    }
    if(name != null && value != null) {
      d.directives.put(name, value);
    } else {
      throw new Exception("Missing name or value for directive!");
    }
    p.next();
  }

  private static void parseAttribute(XmlPullParser p, Data d) throws Exception {
    String name = null;
    Object value = null;
    String type = null;
    for(int i = 0; i < p.getAttributeCount(); ++i) {
      if("name".equalsIgnoreCase(p.getAttributeName(i))) {
        name = p.getAttributeValue(i);
        continue;
      }
      if("value".equalsIgnoreCase(p.getAttributeName(i))) {
        value = p.getAttributeValue(i);
        continue;
      }
      if("type".equalsIgnoreCase(p.getAttributeName(i))) {
        type = p.getAttributeValue(i);
        continue;
      }
    }
    if(name != null && value != null) {
      value = convertValueIfNecessary(value, type);
      d.attributes.put(name, value);
    } else {
      throw new Exception("Missing name or value for attribute:" + p.getName() + " " + name + " " + value + " " + type);
    }
    p.next();
  }

  private static Object convertValueIfNecessary(Object value, String type)
      throws Exception {
    if(type == null) {
      // No conversion needed
    } else if("String".equals(type)) {
      // No conversion needed
    } else if ("Version".equals(type)) {
      value = Version.parseVersion(((String)value).trim());
    } else if ("Long".equals(type)) {
      value = Long.parseLong(((String)value).trim());
    } else if ("Double".equals(type)) {
      value = Double.parseDouble(((String)value).trim());
    } else if (type.startsWith("List<")) {
      String scalarType = type.substring("List<".length(), type.length() - 1);
      StringTokenizer values = new StringTokenizer((String)value, ",\\", true);
  
      ArrayList<Object> list =  new ArrayList<Object>();
      String t = null;
      String v = null;
      while(t != null || values.hasMoreTokens()) {
        if(t == null) t = values.nextToken();
        if(t.equals("\\")) {
          if(values.hasMoreTokens()) {
            t = values.nextToken();
            if(t.equals("\\")) {
              v = v == null ? "\\" : v + "\\" ;
              t = null;
            } else if(t.equals(",")) {
              v = v == null ? "," : v + "," ;
              t = null;
            } else {
              v = v == null ? "\\" : v + "\\" ;
            }
          } else {
            v = v == null ? "\\" : v + "\\" ;
            t = null;
          }
        } else if(t.equals(",")) {
          if(v == null) {
            list.add(convertValueIfNecessary("", scalarType));
          } else if(v.endsWith("\\")) {
            v += ",";
          } else {
            list.add(convertValueIfNecessary(v.trim(), scalarType));
            v = null;
          }
          t = null;
        } else {
          v = v == null ? t : v + t ;
          t = null;
        }
      }
      if(v != null) {
        list.add(convertValueIfNecessary(v.trim(), scalarType));
      }
      value = list;
    } else {
      throw new Exception("Unknown or unsupported type: " + type);
    }
    return value;
  }
  
  public static void debug(Collection<Resource> rs) {
    System.out.println("======= BEGIN PARSED REPOSITORY XML =======");
    for(Resource r : rs) {
     System.out.println(r.toString()); 
    }
    System.out.println("======== END PARSED REPOSITORY XML ========");
    System.out.println("======== RESOURCES FOUND: " + rs.size());
    System.out.flush();
  }
  
  public static void main(String[] a) {
    try {
      Collection<Resource> rs = RepositoryXmlParser.parse(a[0]);
      debug(rs);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
