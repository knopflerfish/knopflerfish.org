package org.knopflerfish.bundle.soap.desktop;

import javax.wsdl.*;
import javax.wsdl.factory.*;
import javax.wsdl.xml.*;

import javax.wsdl.extensions.*;
import javax.xml.namespace.QName;
import javax.wsdl.Input;
import javax.wsdl.Output;

import java.net.*;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;

public class WSDLLoader {

  public Map flatMap = new HashMap();

  public XSDSchema loadWSDL(Definition def) {
    Map       typeMap = new HashMap();
    XSDSchema xsl     = scanTypes(def, typeMap);

    xsl.typeMap = typeMap;

    return xsl;
  }

  XSDSchema scanTypes(Definition def, Map typeMap) {
    Types types = def.getTypes();
    XSDSchema xsd = null;
    
    if(types != null) {
      List extList = types.getExtensibilityElements();
      
      for(Iterator it = extList.iterator(); it.hasNext();) {
	Object v = it.next();
	if(v instanceof UnknownExtensibilityElement) {
	  UnknownExtensibilityElement ueEl = 
	    (UnknownExtensibilityElement)v;
	  Element el = ueEl.getElement();

	  xsd = (XSDSchema)parseAnyElement(el, 0, typeMap);
	  	  
	} else {
	  System.out.println(v.getClass().getName() + ": " + v);
	}
      }
    } else {
      return new XSDSchema();
    }

    return xsd;
  }

  void printMessages(Definition def, Map typeMap) {
    Map messages = def.getMessages();
    Iterator msgIterator = messages.values().iterator();
    
    System.out.println("*** Messages:");
    
    while (msgIterator.hasNext()) {
      Message msg = (Message)msgIterator.next();
      if (!msg.isUndefined()) {
	System.out.println(msg.getQName().getLocalPart());
	
	Map partMap = msg.getParts();
	for(Iterator partIt = partMap.keySet().iterator(); partIt.hasNext();) {
	  Object key  = partIt.next();
	  Part   part = (Part)partMap.get(key);
	  
	  System.out.println("  " + part.getName() + ": " + part.getTypeName().getLocalPart());
	}
      }
    }
  }


  void printPorts(Definition def, Map typeMap, PrintWriter out) {

    out.println("*** Ports:");
    Map ports = def.getPortTypes();
    
    for(Iterator it = ports.keySet().iterator(); it.hasNext(); ) {
      QName name  = (QName)it.next();
      PortType pt = (PortType)ports.get(name);
      
	for(Iterator it2 = pt.getOperations().iterator(); it2.hasNext();) {
	  Operation op = (Operation)it2.next();

	  out.println(" " + op.getName());
	  
	  Input  in   = op.getInput();
	  Output msg_out = op.getOutput();
	  
	  out.println("  in:  "); //  + in.getMessage().getQName().getLocalPart());
	  if(in != null) {
	    printMessage(in.getMessage(), typeMap, 3, out);
	  } else {
	    System.out.println("    null input");
	  }
	  out.println("  out: "); //  + out.getMessage().getQName().getLocalPart());
	  if(msg_out != null) {
	    printMessage(msg_out.getMessage(), typeMap, 3, out);
	  } else {
	    System.out.println("    null output");
	  }
	}
    }
  }

  void printMessage(Message msg, Map typeMap, int level, PrintWriter out) {
    if (!msg.isUndefined()) {
      //      System.out.println(indent(level) + msg.getQName().getLocalPart());

      Map partMap = msg.getParts();
      if(partMap.size() == 0) {
	out.println(indent(level + 2) + "<void>");
      } else {
	for(Iterator partIt = partMap.keySet().iterator(); partIt.hasNext();) {
	  Object key  = partIt.next();
	  Part   part = (Part)partMap.get(key);

	  String type = Util.getTypeString(part);
	  
	  XSDElement el = (XSDElement)typeMap.get(type);	  
	  
	  if(el != null) {
	    XSDWalker.doVisit(el, new PrintVisitor(out), typeMap, level + 3);
	    //	    type = el.toString();
	  } else {
	    out.println(indent(level + 2) + part.getName() + 
			": " + type);
	  }
	}
      }
    }
  }


  String indent(int n) {
    StringBuffer sb = new StringBuffer();
    
    while(n --> 0) {
      sb.append(" ");
    }
    return sb.toString();
  }

  String prefix = "xsd:";


  XSDObj parseComplexContent(Element el, int level, Map typeMap) {
    //    throw new RuntimeException("parseComplexContent NYI");


    //    System.out.println("parseComplexContent " + el.getAttribute("name"));
    
    XSDSoapArray obj = null;

    NodeList children = el.getChildNodes();
    
    for(int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if(node instanceof Element) {
	Element child = (Element)node;
	if((prefix + "restriction").equals(child.getTagName()) &&
	   "soapenc:Array".equals(child.getAttribute("base"))) {
	  
	  NodeList children2 = child.getChildNodes();
	  for(int j = 0; j < children2.getLength(); j++) {
	    Node n2 = children2.item(j);
	    if(n2 instanceof Element) {

	      Element ch2 = (Element)n2;

	      if((prefix + "attribute").equals(ch2.getTagName()) &&
		 "soapenc:arrayType".equals(ch2.getAttribute("ref"))) {
		
		if(obj == null) {
		  obj = new XSDSoapArray(el.getAttribute("name"),
					 ch2.getAttribute("wsdl:arrayType"));
		} else {
		  throw new IllegalArgumentException("Multiple array defs");
		}
	      }
	    }
	  }
	}
      }
    }
    
    return obj;
  }

  XSDObj parseComplexType(Element el, int level, Map typeMap) {
    //    System.out.println("parse: " + indent(level) + " complexType name=" +    el.getAttribute("name"));

    XSDComplexType obj = new XSDComplexType(el.getAttribute("name"));

    NodeList children = el.getChildNodes();
    
    for(int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if(node instanceof Element) {
	Element child = (Element)node;
	XSDObj sub = null;
	if((prefix + "all").equals(child.getTagName())) {
	  sub = parseAll(child, level + 1, typeMap);
	} else if((prefix + "complexContent").equals(child.getTagName())) {
	  sub = parseComplexContent(child, level + 1, typeMap);
	} else if((prefix + "sequence").equals(child.getTagName())) {
	  return parseSequence(child, level, typeMap);
	} else {
	  System.out.println("skip " + child);
	}
	if(sub != null) {
	  obj.add(sub);
	} else {
	  throw new IllegalArgumentException("Unparsed element " + child.getTagName() + " in xsd:complexType " + el);
	}
      }
    }
    return obj;
  }

  XSDObj parseElement(Element el, int level, Map typeMap) {
    XSDElement obj = new XSDElement(el.getAttribute("name"),
				    el.getAttribute("type"));

    NodeList children = el.getChildNodes();
    
    if(children.getLength() > 0) {
      for(int i = 0; i < children.getLength(); i++) {
	Node node = children.item(i);
	if(node instanceof Element) {
	  Element child = (Element)node;
	  if((prefix + "all").equals(child.getTagName())) {
	    obj.add(parseAll(child, level+1, typeMap));
	  } else if((prefix + "sequence").equals(child.getTagName())) {
	    obj.add(parseSequence(child, level+1, typeMap));
	  } else if((prefix + "complexType").equals(child.getTagName())) {
	    obj.add(parseComplexType(child, level+1, typeMap));
	  }  else {
	    throw new IllegalArgumentException("Unexpected tag " + child.getTagName() + " in element tag");
	  }
	}
      }
    }

    return obj;
  }

  XSDObj parseAll(Element el, int level, Map typeMap) {
    XSDAll obj = new XSDAll();
    
    NodeList children = el.getChildNodes();
    
    for(int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if(node instanceof Element) {
	Element child = (Element)node;
	obj.add(parseAnyElement(child, level + 1, typeMap));
      }
    }
    return obj;
  }

  XSDObj parseSequence(Element el, int level, Map typeMap) {
    XSDSequence obj = new XSDSequence();

    NodeList children = el.getChildNodes();
    
    //    System.out.println("parseSequence "+ el);
    for(int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if(node instanceof Element) {
	Element child = (Element)node;
	XSDObj ch = parseElement(child, level + 1, typeMap);
	//	System.out.println("  add " + ch);
	obj.add(ch);
      }
    }
    return obj;
  }
  
  XSDObj parseAnyElement(Element el, int level, Map typeMap) {
    //    System.out.println("parse: " + indent(level) + "" + el.getTagName() + ", name=" + el.getAttribute("name"));

    String tagName = el.getTagName();

    if((prefix + "element").equals(tagName)) {
      return parseElement(el, level, typeMap);
    } else if((prefix + "restriction").equals(tagName)) {
      throw new IllegalArgumentException("Unexpected restriction element in " + 
					 el);
    } else if((prefix + "all").equals(tagName)) {
      return parseAll(el, level, typeMap);
    } else if((prefix + "sequence").equals(tagName)) {
      return parseSequence(el, level, typeMap);
    } else if((prefix + "complexType").equals(tagName)) {
      return parseComplexType(el, level, typeMap);
    } else if((prefix + "import").equals(tagName)) {
      //      System.out.println("skip import namespace=" + el.getAttribute("namespace"));
      return new XSDObj("import");
    } else if("schema".equals(tagName)) {
      prefix = "";
      return parseSchema(el, level, typeMap);
    } else if(tagName.endsWith(":schema")) {
      prefix = tagName.substring(0, tagName.length() - 7) + ":";
      System.out.println("prefix=" + prefix);
      return parseSchema(el, level, typeMap);
    } else {
      throw new IllegalArgumentException("Unexpected element " + el);
    }
  }

  XSDSchema parseSchema(Element el, int level, Map typeMap) {

    XSDSchema obj = new XSDSchema();

    NodeList children = el.getChildNodes();
    
    for(int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if(node instanceof Element) {
	XSDObj xsl = parseAnyElement((Element)node, level+1, typeMap); 
	obj.add(xsl);

	//	System.out.println("schema add " + xsl.getName() + " " + xsl.getClass().getName());
	if(xsl.getName() != null) {
	  typeMap.put(xsl.getName(), xsl);
	}
      }
    }

    return obj;
  }
}







