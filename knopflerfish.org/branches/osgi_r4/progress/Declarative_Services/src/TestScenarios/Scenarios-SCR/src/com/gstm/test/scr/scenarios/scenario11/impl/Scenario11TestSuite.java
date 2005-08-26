/*
 * Created on Aug 17, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario11.impl;

import java.io.IOException;
import java.text.Format;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.BundleContext;

import com.gstm.test.scr.scenarios.scenario11.Scenario11;

/**
 * @author magnus
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Scenario11TestSuite extends TestSuite implements Scenario11 {
	/** variable holding the context */
	private BundleContext bundleContext;

	
	/**
	 * The constructor for scenario 11
	 * 
	 * @param context
	 */
	public Scenario11TestSuite(BundleContext context){
		
		super("Scenario 11");
		/* assign the context */
		bundleContext=context;
		/* variable holding the result from Bundle.getLocation() */
		String location=null;
		/* variable holding the formatted locaton */
		String formattedLocation=null;
		
		try{
			/* get the location */
			location = bundleContext.getBundle().getLocation();
			/* create the formatted location */
			formattedLocation= location.substring(5,location.length());
			/* create the jar file */
			JarFile jarFile = new JarFile(formattedLocation);
			
			this.addTest(new Setup());
			
			String path="OSGI-RESOURCE/Scenario11";
			/* set up scenario 11a */
			ZipEntry[] entriesA = {jarFile.getEntry(path+"/Scenario11a.xml")};
			/* add the test case */
			this.addTest(new A(jarFile,entriesA,"scenario 11a"));
			
			/* set up scenario 11b */
			ZipEntry[] entriesB = {jarFile.getEntry(path+"/Scenario11b.xml")};
			/* add the test case */
			this.addTest(new B(jarFile,entriesB,"scenario 11b"));
			
			
			/* set up scenario 11c */
			ZipEntry[] entriesC = {jarFile.getEntry(path +"/Scenario11c1.xml"),
					jarFile.getEntry(path +"/Scenario11c2.xml"),
					jarFile.getEntry(path +"/Scenario11c3.xml"),
					jarFile.getEntry(path +"/Scenario11c4.xml"),
					jarFile.getEntry(path +"/Scenario11c5.xml"),
					jarFile.getEntry(path +"/Scenario11c6.xml"),
					jarFile.getEntry(path +"/Scenario11c7.xml"),
					jarFile.getEntry(path +"/Scenario11c8.xml")
					};
			/* add the test case */
			this.addTest(new C(jarFile,entriesC,"scenario 11c"));
			
			
			/* set up scenario 11d */
			ZipEntry[] entriesD = {jarFile.getEntry(path +"/Scenario11d.xml")};
			/* add the test case */
			this.addTest(new D(jarFile,entriesD,"scenario 11d"));
			
			/* TODO Implement E here
			 */
			
			/* set up scenario 11f */
			ZipEntry[] entriesF = {jarFile.getEntry(path +"/Scenario11f1.xml"),
					jarFile.getEntry(path +"/Scenario11f2.xml"),
					jarFile.getEntry(path +"/Scenario11f3.xml"),
					jarFile.getEntry(path +"/Scenario11f4.xml"),
					jarFile.getEntry(path +"/Scenario11f5.xml"),
					jarFile.getEntry(path +"/Scenario11f6.xml"),
					jarFile.getEntry(path +"/Scenario11f7.xml")
					};
			/* add the test case */
			this.addTest(new F(jarFile,entriesF,"scenario 11f"));
			
			
			
			/* set up scenario 11g */
			ZipEntry[] entriesG = {jarFile.getEntry(path +"/Scenario11g1.xml"),
					jarFile.getEntry(path +"/Scenario11g2.xml"),
					jarFile.getEntry(path +"/Scenario11g3.xml"),
					jarFile.getEntry(path +"/Scenario11g4.xml"),
					jarFile.getEntry(path +"/Scenario11g5.xml"),
					jarFile.getEntry(path +"/Scenario11g6.xml"),
					jarFile.getEntry(path +"/Scenario11g7.xml")
					};
			/* add the test case */
			this.addTest(new G(jarFile,entriesG,"scenario 11g"));
			
			
			/* set up scenario 11h */
			ZipEntry[] entriesH = {jarFile.getEntry(path +"/Scenario11h1.xml")};
			/* add the test case */
			this.addTest(new H(jarFile,entriesH,"scenario 11h"));
			
			
			/* set up scenario 11i */
			ZipEntry[] entriesI = {jarFile.getEntry(path +"/Scenario11i1.xml"),
					jarFile.getEntry(path +"/Scenario11i2.xml"),
					jarFile.getEntry(path +"/Scenario11i3.xml"),
					jarFile.getEntry(path +"/Scenario11i4.xml"),
					jarFile.getEntry(path +"/Scenario11i5.xml"),
					jarFile.getEntry(path +"/Scenario11i6.xml"),
					jarFile.getEntry(path +"/Scenario11i7.xml")
					};
			/* add the test case */
			this.addTest(new I(jarFile,entriesI,"scenario 11i"));
			
			
			/* set up scenario 11g */
			ZipEntry[] entriesJ = {jarFile.getEntry(path +"/Scenario11j1.xml"),
					jarFile.getEntry(path +"/Scenario11j2.xml"),
					jarFile.getEntry(path +"/Scenario11j3.xml"),
					jarFile.getEntry(path +"/Scenario11j4.xml"),
					jarFile.getEntry(path +"/Scenario11j5.xml"),
					jarFile.getEntry(path +"/Scenario11j6.xml"),
					jarFile.getEntry(path +"/Scenario11j7.xml"),
					jarFile.getEntry(path +"/Scenario11j8.xml"),
					jarFile.getEntry(path +"/Scenario11j9.xml"),
					};
			
			/* add the test case */
			this.addTest(new J(jarFile,entriesJ,"scenario 11j"));
			
			
			/* set up scenario 11i */
			ZipEntry[] entriesK = {jarFile.getEntry(path +"/Scenario11k1.xml"),
					jarFile.getEntry(path +"/Scenario11k2.xml"),
					jarFile.getEntry(path +"/Scenario11k3.xml"),
					jarFile.getEntry(path +"/Scenario11k4.xml"),
					jarFile.getEntry(path +"/Scenario11k5.xml"),
					jarFile.getEntry(path +"/Scenario11k6.xml"),
					jarFile.getEntry(path +"/Scenario11k7.xml")
					};
			
			/* add the test case */
			this.addTest(new K(jarFile,entriesK,"scenario 11k"));
			
			/* TODO FOUND THESE FILES!!!
			/* set up scenario 11l */
			ZipEntry[] entriesL = {jarFile.getEntry(path +"/Scenario11L1.xml"),
					jarFile.getEntry(path +"/Scenario11L2.xml")
					}
			;
			/* add the test case */
			this.addTest(new L(jarFile,entriesL,"scenario 11l"));
			
			/* set up scenario 11m */
			ZipEntry[] entriesM = {jarFile.getEntry(path +"/Scenario11m1.xml"),
					jarFile.getEntry("OSGI-RESOURCE/Scenario11m2.xml")
					}
			;
			/* add the test case */
			this.addTest(new M(jarFile,entriesM,"scenario 11m"));
			
			
			
			/* set up scenario 11n */
			ZipEntry[] entriesN = {jarFile.getEntry(path +"/Scenario11n1.xml"),
					jarFile.getEntry(path +"/Scenario11n2.xml"),
					jarFile.getEntry(path +"/Scenario11n3.xml"),
					jarFile.getEntry(path +"/Scenario11n4.xml"),
					jarFile.getEntry(path +"/Scenario11n5.xml"),
					jarFile.getEntry(path +"/Scenario11n6.xml"),
					jarFile.getEntry(path +"/Scenario11n7.xml")
					};
			
			/* add the test case */
			this.addTest(new N(jarFile,entriesN,"scenario 11n"));
			
			
			/* set up scenario 11n */
			ZipEntry[] entriesO = {jarFile.getEntry(path +"/Scenario11o1.xml"),
					jarFile.getEntry(path +"/Scenario11o2.xml"),
					jarFile.getEntry(path +"/Scenario11o3.xml"),
					jarFile.getEntry(path +"/Scenario11o4.xml"),
					jarFile.getEntry(path +"/Scenario11o5.xml"),
					jarFile.getEntry(path +"/Scenario11o6.xml"),
					jarFile.getEntry(path +"/Scenario11o7.xml")
					};
			
			/* add the test case */
			this.addTest(new O(jarFile,entriesO,"scenario 11o"));
			
			
			this.addTest(new Cleanup());
		
		}catch(IOException e){
			System.err.println("Error locating jarfile:" + formattedLocation);
		
		}
	
	}
	
	/**
	 * this is the class for scenario 11a
	 * @author Magnus Klack
	 */
	private class A extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public A(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				/* create a parser */
				CustomParser parser = new CustomParser();
				try{
					 /* parse the document */
					 parser.readXML(entries[i],jarFile);
					 
				}catch(IllegalXMLException e){
					/* print the stack trace */
					e.printStackTrace();
					/* set the fail */
					fail("The parser should not throw any exceptions for scenario 11a:\n"+
							"The exception is:" + e);
					
				}
			}
			
		}
	}
	
	/**
	 * this is the class for scenario 11b
	 * @author Magnus Klack
	 */
	private class B extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public B(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			/* variable indicating that the test is done */
			boolean testDone=false;
			
			/* go through the zip entries */
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				/* create the parser */
				CustomParser parser = new CustomParser();
				try{
					/* start to parse and get a declaration in return */
					 ComponentDeclaration declaration =
					 	parser.readXML(entries[i],jarFile);
					 
					 /* if this is the file we want to check */
					 if(entries[i].getName().equals("OSGI-RESOURCE/Scenario11/scenario11b.xml")){
					 	
					 	
					 	assertTrue("the XML-file should declare enable",
					 			declaration.isAutoEnable());
					 	
					 	ComponentPropertyInfo property = (ComponentPropertyInfo)
						declaration.getPropertyInfo().get(0);
					 	
					 	assertNotNull("property info should not be null",property);
					 	
					 	assertTrue("the XML-file should declare property type String"
					 			,property.getType().equals("String"));
					 	
					 	assertFalse("the XML-file should not declare servicefactory",
					 			declaration.isServiceFactory());
					 	
					 	ComponentReferenceInfo reference = (ComponentReferenceInfo)
							declaration.getReferenceInfo().get(0);
					 	
					 	assertNotNull("reference info should not be null",reference);
					 	
					 	assertTrue("cardinality should be 1..1 not "+
					 			reference.getCardinality(), 
								reference.getCardinality().equals("1..1"));
					 	
					 	
					 	/* set that the test is done */
					 	testDone=true;
					 	
					 }else if(i>0){
					 	fail("Scenario 11b should only contain 1 XML-file");
					 }
					 
				}catch(IllegalXMLException e){
					e.printStackTrace();
					fail("The parser should not throw any exceptions for scenario 11b:\n"+
							"The exception is:" + e);
					
				}
			}
			
			assertTrue("Test haven't been done missmatch files", testDone);
			
		}
	}
	
	/**
	 * this is the class for scenario 11c
	 * @author Magnus Klack
	 */
	private class C extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public C(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");

			Vector passed= new Vector();
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					 parser.readXML(entries[i],jarFile);
					 passed.add(entries[i]);
					 
					 switch(i){
					 	case 0:
					 		fail("an exception should be thrown for the file:"+ 
					 				entries[i].getName());
					 		break;
					 	case 1:
					 		fail("an exception should be thrown for the file:"+ 
					 				entries[i].getName());
					 		break;
					 	case 2:
					 		fail("an exception should be thrown for the file:"+ 
					 				entries[i].getName());
					 		break;
					 	case 3:
					 		fail("an exception should be thrown for the file:"+ 
					 				entries[i].getName());
					 		break;
					 	case 4:
					 		fail("an exception should be thrown for the file:"+ 
					 				entries[i].getName());
					 		break;
					 	case 5:
					 		fail("an exception should be thrown for the file:"+ 
					 				entries[i].getName());
					 		break;
					 		
					 }
					 
				}catch(IllegalXMLException e){
					if(i==6 || i==7){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage() );
					}
				}
			}
			
			
	
		}
	}
	
	/**
	 * this is the class for scenario 11d
	 * @author Magnus Klack
	 */
	private class D extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public D(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					 parser.readXML(entries[i],jarFile);
					 
				}catch(IllegalXMLException e){
					assertTrue("Wrong error type in scenario 11c",
							e.getMessage().equals("Error parsing in while statement:Only one implementation tag allowed"));
		
				}
			}
			
		}
	}

	/*TODO IMPLEMENT class E */
	
	/**
	 * this is the class for scenario 11f
	 * @author Magnus Klack
	 */
	private class F extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public F(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
			
				try{
					 
					 ComponentDeclaration declaration =
					 	parser.readXML(entries[i],jarFile);
					 
					 String name =null;
					 try{
					 	declaration.getComponentName();
					 }catch(Exception e){}
					 
					 /* TODO CHECK THE RULES HERE */ 
					 switch (i){
					 	case 0:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 1:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 2:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 3:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 4:
					 		assertNotNull("scenario 11f should be able to find component"+
					 				" name in XML file " + entries[i].getName() ,name);
					 		break;
					 	case 5:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 6:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 }
					 
				}catch(IllegalXMLException e){
					if(i==4){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage());
					}
				
				}
			}
			
			
		}
	}
	
	/**
	 * this is the class for scenario 11g
	 * @author Magnus Klack
	 */
	private class G extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public G(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
		
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					 ComponentDeclaration declaration =
					 	parser.readXML(entries[i],jarFile);
					 
					 String name =null;
					 try{
					 	 name = declaration.getImplementation();
					 }catch(Exception e){}
					 
					 /* TODO CHECK THE RULES HERE */ 
					 switch (i){
					 	case 0:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 1:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 2:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 3:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 4:
					 		assertNotNull("scenario 11f should be able to find implementation"+
					 				" name in XML file " + entries[i].getName() ,name);
					 		break;
					 	case 5:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 6:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 }
					 
					 
				}catch(IllegalXMLException e){
					if(i==4){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage());
					}
				}
			}
			
			
			
		}
	}

	/**
	 * this is the class for scenario 11h
	 * @author Magnus Klack
	 */
	private class H extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public H(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					 parser.readXML(entries[i],jarFile);
					 
					 fail("Exception should have been thrown for the file:" + entries[i]);
				}catch(IllegalXMLException e){
					if(i==0){
					System.err.println("Expected failure:" + entries[i].getName() +
							" due to:\n"+ e);
					}else{
						fail("this should never happen for file:"+ entries[i]);
					}
				}
			}
			
		}
	}
	
	/**
	 * this is the class for scenario 11i
	 * @author Magnus Klack
	 */
	private class I extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public I(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					 ComponentDeclaration declaration =
					 	parser.readXML(entries[i],jarFile);
					
					 ComponentReferenceInfo reference =null;
					 String name=null;
					 
					 try{
					  reference = (ComponentReferenceInfo)declaration.getReferenceInfo().get(0);
					  name = reference.getReferenceName();
					 }catch(Exception e){}
					 
					 /* TODO CHECK THE RULES HERE */ 
					 switch (i){
					 	case 0:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 1:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 2:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 3:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 4:
					 		assertNotNull("scenario 11f should be able to find reference"+
					 				" name in XML file " + entries[i].getName() ,name);
					 		break;
					 	case 5:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 6:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 }
					 
				}catch(IllegalXMLException e){
					if(i==4){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage());
					}
				}
			}
			
		}
	}
	
	/**
	 * this is the class for scenario 11j
	 * @author Magnus Klack
	 */
	private class J extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public J(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					 ComponentDeclaration declaration =
					 	parser.readXML(entries[i],jarFile);
					 
					 String name=null;
					 try{
					 	ComponentReferenceInfo reference = (ComponentReferenceInfo)declaration.getReferenceInfo().get(0);
					 	name=reference.getReferenceName();
					 	
					 }catch(Exception e){}
					 
					 /* TODO CHECK THE RULES HERE */ 
					 switch (i){
					 	case 0:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 1:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 2:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 3:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 4:
					 		assertNotNull("scenario 11f should be able to find reference"+
					 				" name in XML file " + entries[i].getName() ,name);
					 		break;
					 	case 5:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 6:
					 		assertNotNull("scenario 11f should be able to find reference"+
					 				" name in XML file " + entries[i].getName() ,name);
					 		break;
					 	case 7:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 8:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 }
					 
				}catch(IllegalXMLException e){
					if(i==4 || i==6){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage());
					}
				}
			}
			
		}
	}
	
	/**
	 * this is the class for scenario 11k
	 * @author Magnus Klack
	 */
	private class K extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public K(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					ComponentDeclaration declaration = parser.readXML(entries[i],jarFile);
					 
					 String name=null;
					 try{
					 	ComponentReferenceInfo reference = (ComponentReferenceInfo) 
													declaration.getReferenceInfo().get(0);
					 	name=reference.getInterfaceType();
					 	
					 }catch(Exception e){}
					 
					 switch (i){
					 	case 0:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 1:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 2:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 3:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 4:
					 		assertNotNull("scenario 11f should be able to find interface"+
					 				" name in XML file " + entries[i].getName() ,name);
					 		break;
					 	case 5:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 6:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 }
					 
				}catch(IllegalXMLException e){
					if(i==4){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage());
					}
				}
			}
			
		}
	}
	
	/**
	 * this is the class for scenario 11l
	 * @author Magnus Klack
	 */
	private class L extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public L(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					 parser.readXML(entries[i],jarFile);
					 
					 if(i==1){
					 		fail("an exeption should have been thrown for the file:" + entries[i].getName());
					 }
					 
				}catch(IllegalXMLException e){
					if(i==0){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage());
					}
				}
			}
			
		}
	}
	
	/**
	 * this is the class for scenario 11m
	 * @author Magnus Klack
	 */
	private class M extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public M(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					 parser.readXML(entries[i],jarFile);
					 
					 if(i==1){
				 		fail("an exeption should have been thrown for the file:" + entries[i].getName());
					 }
				 
					 
				}catch(IllegalXMLException e){
					if(i==0){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage());
					}
				}
			}
			
		}
	}

	/**
	 * this is the class for scenario 11n
	 * @author Magnus Klack
	 */
	private class N extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public N(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				try{
					 
					ComponentDeclaration declaration =
						parser.readXML(entries[i],jarFile);
					 
					 String name=null;
					 try{
					 	ComponentReferenceInfo reference = (ComponentReferenceInfo) 
													declaration.getReferenceInfo().get(0);
					 	name=reference.getBind();
					 	
					 }catch(Exception e){}
					 
					 switch (i){
					 	case 0:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 1:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 2:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 3:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 4:
					 		assertNotNull("scenario 11f should be able to find interface"+
					 				" name in XML file " + entries[i].getName() ,name);
					 		break;
					 	case 5:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 6:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 }
					 
				}catch(IllegalXMLException e){
					if(i==4){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage());
					}
				}
			}
			
		}
	}
	
	/**
	 * this is the class for scenario 11o
	 * @author Magnus Klack
	 */
	private class O extends TestCase{
		/** local jarfile */
		private JarFile jarFile;
		/** local zip entries */
		private ZipEntry[] entries;
		/** local zip entries */
		private String scenarioName;
		
		public O(JarFile jar,ZipEntry[] zips,String name ){
			super(name);
			jarFile=jar;
			entries = zips;
			scenarioName=name;
		}
	    
		 public void runTest() throws Throwable {
			System.out.println("****************" + scenarioName +" starting to parse " + 
					entries.length +
					" file(s) **************");
			
			for(int i=0;i<entries.length;i++){
				System.out.println("\n" +
						"*********************** PARSING: " + entries[i] +
						"***********************");
				
				assertNotNull("zip entry number "+ i + " should not be null",entries[i]);
				
				CustomParser parser = new CustomParser();
				 
				try{
					 
					ComponentDeclaration declaration =
						parser.readXML(entries[i],jarFile);
					 
					 String name=null;
					 try{
					 	
					 	
					 	ComponentReferenceInfo reference = (ComponentReferenceInfo) 
													declaration.getReferenceInfo().get(0);
					 	name=reference.getUnbind();
					 	
					 }catch(Exception e){}
					 
					 switch (i){
					 	case 0:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 1:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 2:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 3:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 4:
					 		assertNotNull("scenario 11f should be able to find interface"+
					 				" name in XML file " + entries[i].getName() ,name);
					 		break;
					 	case 5:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 	case 6:
					 		fail("an exception should have been thrown for the file:" + entries[i]);
					 		break;
					 }
					 
				}catch(IllegalXMLException e){
					if(i==4){
						fail("the file:" + entries[i].getName() +
								" should not fail to:" + e.getMessage() );
					}
				}
			}
			
		}
	}
	
	
	
	 /**
     * Sets up neccessary environment
     *
     *@author Magnus Klack
     */
    class Setup extends TestCase {
        public Setup(){
          
        }
        public void runTest() throws Throwable {
           
        }
        public String getName() {
            String name = getClass().getName();
            int ix = name.lastIndexOf("$");
            if(ix == -1) {
              ix = name.lastIndexOf(".");
            }
            if(ix != -1) {
              name = name.substring(ix + 1);
            }
            return name;
          }
    }
    
    /**
     * Clean up the test suite
     * 
     * @author Magnus Klack
     */
    class Cleanup extends TestCase {
        public void runTest() throws Throwable {
            
        }
        public String getName() {
            String name = getClass().getName();
            int ix = name.lastIndexOf("$");
            if(ix == -1) {
              ix = name.lastIndexOf(".");
            }
            if(ix != -1) {
              name = name.substring(ix + 1);
            }
            return name;
          }
    }
   
	
	
	
	
}
