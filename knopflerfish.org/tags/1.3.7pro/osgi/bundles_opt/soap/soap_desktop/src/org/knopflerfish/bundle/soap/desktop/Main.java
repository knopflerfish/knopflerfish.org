package org.knopflerfish.bundle.soap.desktop;

import javax.wsdl.*;
import javax.wsdl.factory.*;
import javax.wsdl.xml.*;

import javax.wsdl.extensions.*;

import java.util.*;
import java.io.*;
import javax.swing.*;

/*

java -Ddebug=xtrue -cp "resources/xerces.jar;resources/wsdl4j.jar;resources/qname.jar;../../../jars/soap_desktop/soap_desktop-1.0.0.jar;resources/axis.jar;resources/jaxrpc.jar;resources/commons-logging.jar;resources/commons-discovery.jar;resources/saaj.jar" org.knopflerfish.bundle.soap.desktop.Main http://api.google.com/search/beta2 GoogleSearch.wsdl

java -cp "resources/xerces.jar;resources/wsdl4j.jar;resources/qname.jar;../../../jars/soap_desktop/soap_desktop-1.0.0.jar" org.knopflerfish.bundle.soap.desktop.Main . GoogleSearch.wsdl
*/

public class Main {
  public static void main(String[] argv) {
    try {
      String systemLF = UIManager.getSystemLookAndFeelClassName();
      UIManager.setLookAndFeel(systemLF);
    } catch (Exception ignored) {
    }

    Main main = new Main();
    main.start2(argv);
  }
  
  void start2(String[] argv) {
    JFrame frame = new JFrame("");
    
    JSOAPUI soapUI = new JSOAPUI("http://localhost:8080/axis/services/");
    
    frame.getContentPane().add(soapUI);

    for(int i = 0; i < argv.length; i++) {
      soapUI.addService(argv[i]);
    }

    frame.pack();
    frame.setVisible(true);
    frame.show();
  }

  void start(String[] argv) {
    try {
      WSDLFactory factory = WSDLFactory.newInstance();
      WSDLReader  reader  = factory.newWSDLReader();
      
      reader.setFeature("javax.wsdl.verbose", Activator.bVerbose);
      reader.setFeature("javax.wsdl.importDocuments", true);
      
 
      String endPoint  = argv[0];   
      String wsdlURL   = endPoint + "?wsdl";

      if(argv.length > 1) {
	wsdlURL = argv[1];
      }

      WSDL wsdl = new WSDL();
      wsdl.load(endPoint, wsdlURL);

      if(true) {
	StringWriter sw = new StringWriter();
	
	wsdl.loader.printPorts(wsdl.def, 
			       wsdl.schema.getTypeMap(), 
			       new PrintWriter(sw));
	
	System.out.println(sw.toString());
      }
      if(true) {
	SwingRenderer swing = new SwingRenderer();

	swing.test(wsdl);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
