package org.knopflerfish.bundle.soap.desktop;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import javax.wsdl.*;
import javax.wsdl.factory.*;
import javax.wsdl.xml.*;

import javax.wsdl.extensions.*;
import javax.xml.namespace.QName;

// Axis imports
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;


import java.net.URL;


public class JSOAPUI extends JPanel {

  // String
  Set services = new TreeSet();

  String urlBase;

  JSplitPane splitPane;
  JPanel     servicePanel;
  JComponent callPanel;

  ImageIcon soapIcon;

  HashMap labels = new HashMap();
  public JSOAPUI(String urlBase) {
    super(new BorderLayout());

    this.urlBase = urlBase;

    soapIcon      = new ImageIcon(getClass().getResource("/soap.png"));

    callPanel     = new JPanel(new BorderLayout());

    JPanel leftPanel = new JPanel(new BorderLayout());

    servicePanel  = new JPanel();

    JScrollPane serviceScroll = new JScrollPane(servicePanel);

    //    BoxLayout box = new BoxLayout(servicePanel, BoxLayout.Y_AXIS);

    LayoutManager box = new GridLayout(0, 1);

    servicePanel.setLayout(box);

    serviceScroll.setPreferredSize(new Dimension(100, 100));
    callPanel.setPreferredSize(new Dimension(500, 400));

    JPanel buttonPanel = new JPanel();
    BoxLayout buttonBox = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
    buttonPanel.setLayout(new GridLayout(0, 1));

    JButton openHostButton = new JButton("Connect to...");
    openHostButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  openHost();
	}
      });

    JButton reloadHostButton = new JButton("Reload");
    reloadHostButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  getServices();
	}
      });
    
    buttonPanel.add(openHostButton);
    buttonPanel.add(reloadHostButton);

    leftPanel.add(buttonPanel,    BorderLayout.NORTH);
    leftPanel.add(serviceScroll,  BorderLayout.CENTER);

    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			       leftPanel,
			       callPanel);

    
    add(splitPane, BorderLayout.CENTER);

    getServices();
    
  }

  void openHost() {
    String s = (String)JOptionPane
      .showInputDialog(servicePanel,
		       "Enter address to SOAP host",
		       "Connect to new SOAP host",
		       JOptionPane.QUESTION_MESSAGE, // optionType 
		       null, // icon,
		       null, // Object[] init
		       urlBase);
    
    if ((s != null) && (s.length() > 0)) {
      urlBase = s;
      getServices();
    }
  }



  void update() {
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  synchronized(services) {
	    servicePanel.removeAll();
	    labels.clear();
	    for(Iterator it = services.iterator(); it.hasNext();) {
	      final String name = (String)it.next();
	      final JLabel label = new JLabel2(name, 
					       soapIcon, 
					       SwingConstants.CENTER);
	      
	      label.setToolTipText("Show SOAP service '" + name + "'");

	      labels.put(name, label);
	      MouseListener l = new MouseAdapter() {
		  public void mouseClicked(MouseEvent ev) {
		    selectService(name);
		  }
		};
	    
	      label.addMouseListener(l);

	      servicePanel.add(label);
	    }
	    
	    servicePanel.invalidate();
	    servicePanel.revalidate();
	    servicePanel.repaint();

	    callPanel.removeAll();

	    callPanel.invalidate();
	    callPanel.revalidate();
	    callPanel.repaint();

	  }
	}
      });
  }

  void getServices() {
    String endpoint = urlBase + "axisadmin";
    try {
  
      Service  service = new Service();
      Call     call    = (Call) service.createCall();
      
      call.setTargetEndpointAddress( new URL(endpoint) );
      call.setOperationName(new QName("getPublishedServiceNames"));
      
      Object[]       params = new Object[] { };
      
      String[] result  = (String[])call.invoke(params);
      
      //      System.out.println("found " + result.length + " services at " + endpoint);
      synchronized(services) {
	services.clear();
	for(int i = 0; i < result.length; i++) {
	  services.add(result[i]);
	}
      }
      update();
    } catch (Exception e) {
      JTextArea text = new JTextArea();
      text.setText("Failed to set base SOAP URL\n" + 
		   endpoint + 
		   "\n\n" + 
		   e.toString());
      text.setEditable(false);
      e.printStackTrace();
      
      setCallComponent(text);
    }

  }

  String selectedName = null;

  public void selectService(String name) {
    JComponent    comp;

    String endPoint  = urlBase + name;
    String wsdlURL   = endPoint + "?wsdl";
    
    try {
      WSDLFactory factory = WSDLFactory.newInstance();
      WSDLReader  reader  = factory.newWSDLReader();
      
      reader.setFeature("javax.wsdl.verbose",         Activator.bVerbose);
      reader.setFeature("javax.wsdl.importDocuments", true);
      
      
      
      WSDL wsdl = new WSDL();
      wsdl.load(endPoint, wsdlURL);
      
      SwingRenderer renderer = new SwingRenderer();
      comp     = renderer.createComponent(wsdl);

      selectedName = name;

      for(Iterator it = labels.keySet().iterator(); it.hasNext();) {
	String s = (String)it.next();
	JLabel2 label = (JLabel2)labels.get(s);
	label.setSelected(false);
      }

      JLabel2 label = (JLabel2)labels.get(name);

      if(label != null) {
	label.setSelected(true);

      }

    } catch (Exception e) {
      comp = new JLabel("Failed to set service\n" + 
			"URL: " + endPoint + 
			"\n\n" + e);
      e.printStackTrace();
    }
    
    setCallComponent(comp);
  }

  void setCallComponent(JComponent comp) {
    callPanel.removeAll();
    callPanel.add(comp, BorderLayout.CENTER);
    callPanel.invalidate();
    callPanel.revalidate();
    callPanel.repaint();
  }
    
  public void addService(String name) {
    synchronized(services) {
      services.add(name);
    }
    update();
  }

  public void removeService(String name) {
    synchronized(services) {
      services.remove(name);
    }
    update();
  }
}


class JLabel2 extends JLabel {
  
  Color       selColor = new Color(200, 200, 255);

  boolean bSelected = false;

  JLabel2(String name, Icon icon, int pos) {
    super(name, icon, SwingConstants.CENTER);
    
    setVerticalTextPosition(AbstractButton.BOTTOM);
    setHorizontalTextPosition(AbstractButton.CENTER);
    setOpaque(false);
  }

  public void paintComponent(Graphics g) {
    Dimension size = getSize();
    g.setColor(getBackground());
    g.fillRect(0,0,size.width, size.height);
    super.paintComponent(g);
  }

  public Color getBackground() {
    return bSelected ? selColor : super.getBackground();
  }

  public void setSelected(boolean b) {
    this.bSelected = b;
    repaint();
  }
}
