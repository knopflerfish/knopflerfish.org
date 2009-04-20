package org.knopflerfish.bundle.soap.desktop;

import javax.wsdl.*;
import javax.wsdl.factory.*;
import javax.wsdl.xml.*;

import javax.wsdl.extensions.*;

import java.util.*;
import java.io.*;

public class WSDL {
  public Definition def;
  public XSDSchema  schema;
  public String     endPoint;
  public String     wsdlURL;
  public String     prefix = "";
  public String     base   = ".";
  public WSDLLoader loader;

  public WSDL() {
  }

  public void load(String endPoint, String wsdlURL) throws Exception {
    WSDLFactory factory = WSDLFactory.newInstance();
    WSDLReader  reader  = factory.newWSDLReader();
    
    reader.setFeature("javax.wsdl.verbose", Activator.bVerbose);
    reader.setFeature("javax.wsdl.importDocuments", true);

    this.endPoint = endPoint;
    this.wsdlURL  = wsdlURL;

    this.def      = reader.readWSDL(base, wsdlURL);
    this.loader   = new WSDLLoader();
    this.schema   = loader.loadWSDL(def);
    this.prefix   = loader.prefix;
  }
}
