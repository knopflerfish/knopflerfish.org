package org.knopflerfish.bundle.repository.xml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import org.kxml2.io.KXmlParser;
import org.osgi.resource.Resource;
import org.xmlpull.v1.XmlPullParser;

public class RepositoryXmlParser {

  public static Collection<Resource> parse(InputStream is) throws Exception {
    XmlPullParser p = new KXmlParser();
    p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    p.setInput(new InputStreamReader(is));
    ArrayList<Resource> rs = new ArrayList<Resource>();
    // TODO Parse the repository tag
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

  private static Resource parseResource(XmlPullParser p) throws Exception {
    if ("resource".equalsIgnoreCase(p.getName())) {
      return parseReqsAndCaps(p);
    } else {
      return null;
    }
  }

  private static Resource parseReqsAndCaps(XmlPullParser p) throws Exception {
    if (p.isEmptyElementTag()) {
      p.next(); // We still get an END_TAG
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
          r.addReq(req);
        }
        p.next();
        continue;
      }
      if ("capability".equalsIgnoreCase(p.getName())) {
        CapabilityImpl cap = parseCap(p);
        if(cap != null) {
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
    String value = null;
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
    if(name != null && value != null && type != null) {
      // TODO: Type parsing
      d.directives.put(name, value);
    } else {
      throw new Exception("Missing name or value for directive!");
    }
    p.next();
  }

  public static void main(String[] a) {
    try {
      Collection<Resource> rs = RepositoryXmlParser.parse(new FileInputStream(
          a[0]));
      System.out.println("Parsed: " + rs.size());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
