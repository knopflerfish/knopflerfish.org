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

import java.util.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.table.*;

import org.xml.sax.helpers.*;
import java.lang.reflect.*;

// Axis imports
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.description.*;




/*
java -Ddebug=xtrue -cp "resources/xerces.jar;resources/wsdl4j.jar;resources/qname.jar;../../../jars/soap_desktop/soap_desktop-1.0.0.jar;resources/axis.jar;resources/jaxrpc.jar;resources/commons-logging.jar;resources/commons-discovery.jar;resources/saaj.jar" org.knopflerfish.bundle.soap.desktop.Main http://localhost:8080/axis/services/soapdemo1 "http://localhost:8080/axis/services/soapdemo1?wsdl"
*/

public class SwingRenderer {

  static boolean bDebug = "true".equals(System.getProperty("debug", "false"));

  Font stdFont = new Font("dialog", Font.PLAIN, 12);

  void test(WSDL wsdl) {
    JFrame frame = new JFrame("");
    
    JComponent comp = createComponent(wsdl);

    frame.getContentPane().add(comp);

    frame.pack();
    frame.setVisible(true);
    frame.show();
  }


  
  JComponent createComponent(WSDL wsdl) {
    JPanel panel = new JPanel(new BorderLayout());
    
    JTabbedPane tabPane = new JTabbedPane();


    //    BoxLayout box = new BoxLayout(panel, BoxLayout.Y_AXIS);

    //    panel.setLayout(box);

    Map ports = wsdl.def.getPortTypes();
    
    for(Iterator it = ports.keySet().iterator(); it.hasNext(); ) {
      QName name  = (QName)it.next();
      PortType pt = (PortType)ports.get(name);
      
      int ix = 0;
      for(Iterator it2 = pt.getOperations().iterator(); it2.hasNext();) {
	Operation op = (Operation)it2.next();
	
	JComponent opComp = createOperationComponent(wsdl, op);
	opComp.setBorder(null);

	JScrollPane scroll = new JScrollPane(opComp);
	scroll.setBorder(null);

	scroll.setPreferredSize(new Dimension(400, 300));

	tabPane.add(op.getName(), scroll);
	tabPane.setToolTipTextAt(ix, "SOAP operation " + op.getName());

	ix++;
      }
    }

    JLabel label = new JLabel(wsdl.endPoint);
    label.setToolTipText("SOAP endpoint");

    label.setHorizontalAlignment(SwingConstants.LEFT);
    label.setBackground(panel.getBackground().brighter().brighter());
    panel.add(tabPane, BorderLayout.CENTER);
    panel.add(label,   BorderLayout.SOUTH);

    
    return panel;
  }

  Color headerColor = new Color(210, 210, 210);

  JComponent createOperationComponent(final WSDL wsdl, 
				      final Operation op) {


    JPanel    panel = new JPanel(new BorderLayout());

    JScrollPane scroll = new JScrollPane(panel);

    JPanel    msgPanel = new JPanel();

    BoxLayout box   = new BoxLayout(msgPanel, BoxLayout.Y_AXIS);
    msgPanel.setLayout(box);

    Message  msg_in  = op.getInput().getMessage();
    Message  msg_out = op.getOutput().getMessage();
   
    final JComponent inComp  = createMessageComponent(wsdl, msg_in, "", false);
    //    final JComponent outComp = createMessageComponent(wsdl, msg_out, "result: ", true);
    
    final JPanel outComp = new JPanel(new BorderLayout());


    if(msg_in.getParts().size() > 0) {
      inComp.setBorder(BorderFactory.createTitledBorder("Arguments"));
    }

    msgPanel.add(inComp);
    JButton callB = new JButton("Call " + op.getName());
    callB.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ev) {
	  doOperation(wsdl, op, inComp, outComp);
	}
      });
    callB.setToolTipText("Call operation " + op.getName());
    JPanel p2 = new JPanel(new BorderLayout());

    p2.add(callB, BorderLayout.WEST);
    msgPanel.add(p2);
    msgPanel.add(outComp);
    
    JPanel spacer = new JPanel();
    msgPanel.add(spacer);

    panel.add(msgPanel, BorderLayout.NORTH);

    return scroll;
  }

  void doOperation(WSDL wsdl, 
		   Operation op, 
		   JComponent inComp, 
		   final JComponent outComp) {

    try {

      Message  msg_in  = op.getInput().getMessage();
      Message  msg_out = op.getOutput().getMessage();
      
      Map map = wsdl.def.getServices();
      if(map.size() == 0) {
	throw new IllegalArgumentException("No services");
      }

      String endpoint = wsdl.endPoint;

      Service  service = new Service();
      Call     call    = (Call) service.createCall();

      call.setTargetEndpointAddress( new URL(endpoint) );
      call.setOperationName(new QName(op.getName()));
      
      java.util.List values = new ArrayList();

      Object         result  = null;
      java.util.List names = null;

      try {
	getParams(call, values, inComp, 0);	

	if(Activator.bVerbose) {
	  System.out.println("call with values=" + values);
	}

	Object[]       params = values.toArray();
	
      	result  = call.invoke(params);

	if(Activator.bVerbose) {
	  System.out.println("got result=" + result);
	}
	names   = Util.getPartNames(msg_out);
      } catch (IllegalArgumentException e) {
	result = e.getMessage();
	names  = new ArrayList();
	names.add("Exception");
      } catch (Exception e) {
	result = e;
	names  = new ArrayList();
	names.add("Exception");
      }

      final Object         result2 = result;
      final java.util.List names2  = names;

      SwingUtilities.invokeLater(new Runnable() {
	  public void run() {
	    JComponent comp = showOperationResult(names2, result2);
	    
	    comp.setBorder(BorderFactory.createTitledBorder("Result"));
	    
	    outComp.removeAll();
	    outComp.add(comp);
	    outComp.revalidate();
	    outComp.invalidate();
	    outComp.repaint();
	  }
	});
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  JComponent showOperationResult(java.util.List names,
				 Object result) {
    
    if(names == null || names.size() == 0) {
      if(result == null) {
	return new MyLabel("empty result");
      }
      throw new RuntimeException("no names found, object=" + result);
    }

    if(result == null) {
      throw new RuntimeException("null object result");
    }
    
    if(result.getClass().isArray() &&
       Array.getLength(result) == names.size()) {
      JPanel panel = new JPanel();

      BoxLayout box = new BoxLayout(panel, BoxLayout.Y_AXIS);
      panel.setLayout(box);

      for(int i = 0; i < names.size(); i++) {
	java.util.List l = new ArrayList();
	l.add(names.get(i));
	Object obj = Array.get(result, i);
	JComponent comp = showOperationResult(l, obj);
	panel.add(comp);
      }
      
      return panel;
    } else {
      if(names.size() > 1) {
	throw new RuntimeException("found " + names.size() + " names, " + 
				   "but just one result");
      }

      JPanel panel = new JPanel(new BorderLayout());
      JLabel name = new MyLabel(names.get(0).toString());
      name.setPreferredSize(new Dimension(120, 15));
      name.setVerticalAlignment(SwingConstants.TOP);

      name.setToolTipText(result.getClass().getName());

      JComponent rComp = makeResultComp(result);

      panel.add(name, BorderLayout.WEST);
      panel.add(rComp, BorderLayout.CENTER);

      return panel;
    }
  }

  JComponent makeResultComp(Object val) {
    if(val.getClass().isArray()) {
      JPanel panel = new JPanel();
      BoxLayout box = new BoxLayout(panel, BoxLayout.Y_AXIS);
      panel.setLayout(box);

      for(int i = 0; i < Array.getLength(val); i++) {
	Object obj = Array.get(val, i);
	JPanel p = new JPanel(new BorderLayout());
	
	panel.add(makeResultComp(obj));
      }

      return panel;
    } else if(val instanceof Map) {
      return makeResultMap((Map)val);
    } else if(val instanceof Collection) {
      return makeResultCollection((Collection)val);
    } else if(val instanceof Exception) {
      Exception e = (Exception)val;
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      JTextArea text = new JTextArea();
      text.setText(sw.toString());
      
      text.setEditable(false);
      return text;
    } else {
      return new MyLabel("" + val);
    }
  }

  JComponent makeResultCollection(final Collection c) {    
    
    TableModel model = new AbstractTableModel() {
	public int getColumnCount() { 
	  return 1; 
	}

	public int getRowCount() { 
	  return c.size();
	}

	public String getColumnName(int col) {
	  return "Values";
	}

	public Object getValueAt(int row, int col) { 
	  int i = 0;
	  for(Iterator it = c.iterator(); it.hasNext();) {
	    Object key = it.next();
	    if(i == row) {
	      return key;
	    }
	    i++;
	  }
	  return null;
	}
      };
    
    JTable table = new JTable(model);

    JScrollPane scroll = new JScrollPane(table);
    scroll.setPreferredSize(new Dimension(150, 100));
    return scroll;
  }

  JComponent makeResultMap(final Map map) {    
    TableModel model = new AbstractTableModel() {
	public int getColumnCount() { 
	  return 2; 
	}

	public int getRowCount() { 
	  return map.size();
	}

	public String getColumnName(int col) {
	  return (col == 0 ? "Key" : "Value");
	}

	public Object getValueAt(int row, int col) { 
	  int i = 0;
	  for(Iterator it = map.keySet().iterator(); it.hasNext();) {
	    Object key = it.next();
	    Object val = map.get(key);
	    if(i == row) {
	      return col == 0 ? key : Util.toDisplay(val);
	    }
	    i++;
	  }
	  return null;
	}
      };
	
    JTable table = new JTable(model);
	
    JScrollPane scroll = new JScrollPane(table);
    scroll.setPreferredSize(new Dimension(150, 100));
    return scroll;
  }



  void showObjectResult(Object result, JComponent comp) {
    Component[] children = comp.getComponents();
    
    if(comp instanceof JElement) {
      JElement je = (JElement)comp;
      if(comp instanceof JXSDPrimitiveComponent) {
	JXSDPrimitiveComponent p = (JXSDPrimitiveComponent)comp;
	p.setValue(result);
      } else {
	for(int i = 0; i < children.length; i++) {
	  if(children[i] instanceof JComponent) {
	    showObjectResult(result, (JComponent)children[i]);
	  }
	}
      }
    }
  }


  void getParams(Call call, 
		 java.util.List values, 
		 JComponent comp,
		 int dim) throws Exception {
    Component[] children = comp.getComponents();

    if(comp instanceof JElement) {
      JElement je = (JElement)comp;
      if(je.bArray) {
	if(Activator.bVerbose) {
	  System.out.println("array " + comp.getClass().getName());
	}
	java.util.List subValues = new ArrayList();

	for(int i = 0; i < children.length; i++) {
	  if(children[i] instanceof JComponent) {
	    getParams(call, subValues, (JComponent)children[i], 1);
	  }
	}

	if(Activator.bVerbose) {
	  System.out.println("adding subValues=" + subValues);
	}

	Object[] subParams = subValues.toArray();

	values.add(subParams);

	return;
      }
      if(comp instanceof JXSDPrimitiveComponent) {
	JXSDPrimitiveComponent p = (JXSDPrimitiveComponent)comp;

	values.add(p.getValue());

	if(false) {
	  QName type = new QName("http://www.w3.org/2001/XMLSchema", 
				 "xsd:" + p.xsd.getType());

	  call.addParameter(p.xsd.getName(),
			    type,
			    javax.xml.rpc.ParameterMode.IN);

	  System.out.println("addParam " + p.xsd.getName() + 
			     " = " + p.getValue() + 
			     ", type=" + type);
	}
      }
    }

    for(int i = 0; i < children.length; i++) {
      if(children[i] instanceof JComponent) {
	getParams(call, values, (JComponent)children[i], dim);
      }
    }
  }


  JComponent createMessageComponent(WSDL wsdl, 
				    Message msg,
				    String prefix,
				    boolean bReadonly) {

    GridBagLayout       gridBag = new GridBagLayout();
    GridBagConstraints  c       = new GridBagConstraints();

    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0;
    c.weighty = 1.0;
    
    JPanel panel = new JPanel(gridBag);    
    
    Map partMap = msg.getParts();
    if(partMap.size() == 0) {
      // No parts
    } else {
      for(Iterator partIt = partMap.keySet().iterator(); partIt.hasNext();) {
	Object key  = partIt.next();
	Part   part = (Part)partMap.get(key);


	JLabel nameC = !bReadonly 
	  ? new MyLabel(part.getName())
	  : new MyLabel(part.getName());

	nameC.setVerticalTextPosition(SwingConstants.TOP);
	nameC.setPreferredSize(new Dimension(100, 10));
	nameC.setToolTipText(prefix + part.getName() + 
			     ": " + Util.getTypeString(part));
	
	c.gridwidth = GridBagConstraints.BOTH; //end row
	c.weightx   = 0.0;
	gridBag.setConstraints(nameC, c);
	panel.add(nameC);

	JComponent partC = createPartComponent(wsdl, part, bReadonly); 

	c.gridwidth = GridBagConstraints.REMAINDER;
	c.weightx   = 1.0;
	gridBag.setConstraints(partC, c);
	panel.add(partC);
      }
    }

    return panel;
  }

  JComponent createPartComponent(WSDL wsdl, 
				 Part part,
				 boolean bReadonly) {
    String type = Util.getTypeString(part);
    
    XSDElement el = (XSDElement)wsdl.schema.getTypeMap().get(type);
    
    
    if(el != null) {
      return createXSDElementComponent(wsdl, el, bReadonly, false);
    } else {
      return new JXSDPrimitiveComponent(wsdl, 
					new XSDElement(part.getName(), type),
					bReadonly);
    }
  }


  String pkgName = "org.knopflerfish.bundle.soap.desktop";

  JElement createXSDElementComponent(WSDL wsdl, 
				     XSDElement el,
				     boolean bReadonly,
				     boolean bArray) {
    String type = el.getType();
    
    JElement top = null;

    if(el instanceof XSDSoapArray) {
      bArray = true;
    }

    top = new JElement(new BorderLayout(), el);


    if(bDebug) {
      top.setBorder(BorderFactory.createTitledBorder(el.getClass().getName().substring(pkgName.length()+1)));
    }
    

    JElement comp;
		    
    if(type.startsWith("typens:")) {
      XSDElement ref =(XSDElement)wsdl.schema.getTypeMap().get(type.substring(7));
      if(ref != null) {
	comp = createXSDElementComponent(wsdl, ref, bReadonly, bArray);
      } else {
	comp = new JElement(new BorderLayout(), null);
	comp.add(new MyLabel("unresolved " + type), BorderLayout.CENTER);
      }
    } else {
      int n = 0; 
      for(Iterator it = el.getChildren(); it.hasNext();) {
	XSDElement subEl = (XSDElement)it.next();
	n++;
      }
      comp = createXSDElementComponentList(wsdl, el, bReadonly, bArray,
					   n == 0);
    }

    top.add(comp, BorderLayout.CENTER);

    return top;
  }

  class JXSDElementComponentSingle extends JElement {
    public JXSDElementComponentSingle(XSDElement xsd) {
      this(null, xsd);
    }

    public JXSDElementComponentSingle(LayoutManager layout, XSDElement xsd) {
      super(layout, xsd);
    }
  }

  class JXSDElementComponentList extends JElement {

    public JXSDElementComponentList(XSDElement xsd) {
      this(null, xsd);
    }

    public JXSDElementComponentList(LayoutManager layout, XSDElement xsd) {
      super(layout, xsd);
    }
  }

  class JXSDPrimitiveComponent extends JElement {
    public JComponent comp;
    public Class      clazz;

    public JXSDPrimitiveComponent(WSDL       wsdl, 
				  XSDElement xsd,
				  boolean    bReadonly) {
      super(new BorderLayout(), xsd);

      String type = xsd.getType();
      
      if(type.startsWith(wsdl.prefix)) {
	type = type.substring(wsdl.prefix.length());
      }
      
      if("int".equals(type)) {
	comp  = new JTextField("0");
	clazz = Integer.class;
      } else  if("string".equals(type)) {
	comp  = new JTextField("");
	clazz = String.class;
      } else  if("long".equals(type)) {
	comp  = new JTextField("");
	clazz = Long.class;
      } else if("boolean".equals(type)) {
	comp  = new JCheckBox();
	clazz = Boolean.class;
      } else if("double".equals(type)) {
	comp  = new JTextField("0.0");
	clazz = Double.class;
      } else if("float".equals(type)) {
	comp  = new JTextField("0.0");
	clazz = Float.class;
      } else {
	comp  = new MyLabel("unhandled type " + type);
	clazz = String.class;
      }
      
      comp.setEnabled(!bReadonly);

      add(comp, BorderLayout.CENTER);
    }


    public String getStringValue() {
      String s = null;
      if(comp instanceof JTextField) {
	s = ((JTextField)comp).getText();
      } else if(comp instanceof JCheckBox) {
	s = "" + ((JCheckBox)comp).isSelected();
      } else {
	throw new RuntimeException("NYI: output from " + comp);
      }

      return s;
    }

    public void setValue(Object val) {
      StringBuffer sb = new StringBuffer();
      if(val == null) {
	sb.append("<null>");
      } else if(val.getClass().isArray()) {
	for(int i = 0; i < Array.getLength(val); i++) {
	  Object obj = Array.get(val, i);
	  sb.append("" + obj);
	  if(i < Array.getLength(val) - 1) {
	    sb.append("\n");
	  }
	}
      } else {
	sb.append(val.toString());
      }
      System.out.println(xsd.getName() + ": setValue " + sb);
    }

    public Object getValue() {
      String s = getStringValue();

      if(clazz == String.class) {
	return s;
      } else if(clazz == Boolean.class) {
	return "true".equals(s.toLowerCase()) 
	  ? Boolean.TRUE
	  : Boolean.FALSE;
      } else {
	try {
	  Constructor cons = clazz.getConstructor(new Class[] { String.class });

	  Object val = cons.newInstance(new Object[] { s });
	  
	  return val;
	} catch (Exception e) {
	  throw new IllegalArgumentException("Failed to convert '" + s + "' to " + 
				     clazz.getName());
	}
      }
    }
  }

  JXSDElementComponentSingle 
    createXSDElementComponentSingle(WSDL wsdl, 
				    XSDElement el,
				    boolean bReadonly,
				    boolean bArray) {
    
    String type = el.getType();

    JXSDElementComponentSingle panel = new JXSDElementComponentSingle(el);
    
    String name = el.getName();

    if(bDebug) {
      if(name != null && !"".equals(name)) {
	panel.setBorder(BorderFactory.createTitledBorder(el.getName() + ":" + el.getType() + ":" + bArray));
      }
    }

    panel.setLayout(new BorderLayout());

    JLabel label = new MyLabel(el.getName());
    label.setToolTipText(el.getName() + ": " + type);
    label.setPreferredSize(new Dimension(100, 10));

    JXSDPrimitiveComponent 
      comp = new JXSDPrimitiveComponent(wsdl, el, bReadonly);
    
    panel.add(label, BorderLayout.WEST);
    panel.add(comp,  BorderLayout.CENTER);
    
    return panel;
  }
  
  JXSDElementComponentList 
    createXSDElementComponentList(final WSDL wsdl, 
				  final XSDElement el,
				  final boolean bReadonly,
				  final boolean bArray,
				  final boolean bSingle) {
    String type = el.getType();

    final JXSDElementComponentList panel 
      = new JXSDElementComponentList(new BorderLayout(), el);
    
    String name = el.getName();
    if(name != null && !"".equals(name)) {
      panel.setBorder(BorderFactory.createTitledBorder(name + (bDebug ? (":" + el.getType() + ":" + bArray) : "")));
    }

    boolean bAutoExpand = 
      (el instanceof XSDAll) ||
      (el instanceof XSDSoapArray);
    
    if(bArray && (!bAutoExpand || bSingle)) {
      final JPanel subPanel = new JPanel();
      BoxLayout box = new BoxLayout(subPanel, BoxLayout.Y_AXIS);
      subPanel.setLayout(box);

      panel.bArray = true;
      
      JButton newButton = new JButton("Add item");
      newButton.setToolTipText("Add item to array");
      newButton.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    JComponent comp = makeBox(wsdl, el, bReadonly, true, bSingle);
	    subPanel.add(comp);
	    //	    System.out.println("created " + comp);
	    panel.invalidate();
	    subPanel.invalidate();

	    panel.revalidate();
	    subPanel.revalidate();

	    panel.repaint();
	    subPanel.repaint();
	  }
	});

      JPanel panelB = new JPanel(new BorderLayout());
      panelB.add(newButton, BorderLayout.WEST); 

      panel.add(panelB,    BorderLayout.NORTH); 
      panel.add(subPanel,  BorderLayout.CENTER); 
    } else {
      panel.add(makeBox(wsdl, el, bReadonly, false, bSingle), 
		BorderLayout.CENTER);
    }

    return panel;
  }

  JComponent makeBox(WSDL wsdl, 
		     XSDElement el,
		     boolean bReadonly,
		     boolean bArray,
		     boolean bSingle) {
    final JPanel panel = new JPanel(new BorderLayout());

    JPanel subPanel = new JPanel();
    BoxLayout box = new BoxLayout(subPanel, BoxLayout.Y_AXIS);
    subPanel.setLayout(box);
    
    JButton delButton = null;
    if(bArray) {
      String name = el.getName();

      if(name != null && !"".equals(name)) {
	subPanel.setBorder(BorderFactory.createTitledBorder(el.getName()));
      }
      JPanel panelB = new JPanel(new BorderLayout());

      delButton = new JButton("Delete");
      delButton.setToolTipText("Delete this item from array");
      delButton.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    JComponent parent = (JComponent)panel.getParent();
	    parent.remove(panel);
	    parent.invalidate();
	    parent.revalidate();
	    parent.repaint();
	  }
	});

      if(!bSingle) {
	panelB.add(delButton, BorderLayout.WEST);
	subPanel.add(panelB, BorderLayout.NORTH);
      }
    }

    if(bSingle) { 
      JComponent 
	subComp = createXSDElementComponentSingle(wsdl, el, bReadonly, bArray);

      if(delButton != null) {
	subComp.add(delButton, BorderLayout.WEST);
      }

      subPanel.add(subComp, BorderLayout.CENTER);
    } else {
      for(Iterator it = el.getChildren(); it.hasNext();) {
	XSDElement subEl = (XSDElement)it.next();
	
	JComponent subComp = createXSDElementComponent(wsdl, 
						       subEl,
						       bReadonly,
						       bArray);
	subPanel.add(subComp);
      }
    }

    panel.add(subPanel, BorderLayout.CENTER);

    return panel;
  }
    

  class MyLabel extends JLabel {
    public MyLabel() {
      this("");
    }

    public MyLabel(String s) {
      super(s);
      setFont(stdFont);
      setVerticalAlignment(SwingConstants.TOP);
    }
  }
}
