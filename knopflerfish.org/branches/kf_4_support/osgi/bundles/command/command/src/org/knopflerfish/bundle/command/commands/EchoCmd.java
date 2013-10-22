package org.knopflerfish.bundle.command.commands;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.lang.reflect.*;

public class EchoCmd {

  public static String   SCOPE = EchoCmd.class.getName();
  public static String[] FUNCTION = new String[] {
    "echo",
    "print",
    "id",
    "new",
    "toint",
    "echo2",
  };
    
  
  public void print(String[] args) {
    System.out.println("print String[]");
    for(int i = 0; i < args.length; i++) {
      System.out.println("#" + i + "=" + args[i].toString());
    }
  }

  public String echo(String a, String b, String c) {
    System.out.println("echo 3 args");
    System.out.println("a=" + a);
    System.out.println("b=" + b);
    System.out.println("c=" + c);

    return "echo3(" + a + ", " + b + ", " + c + ")";
  }

  public void echo(Object[] args) {
    System.out.println("echo 0 + var");
    for(int i = 0; i < args.length; i++) {
      System.out.println("#" + i + "=" + args[i].toString());
    }
  }

  public Object id(Object s) {
    return s;
  }

  public Integer toInt(Object obj) {
    return new Integer(obj.toString());
  }

  public Object New(String name) throws Exception {
    return New(name, new Object[0]);
  }
  
  public Object New(String name, Object[] args) throws Exception {
    Class clazz;
    try {
      clazz = Class.forName(name);
    } catch (Exception e) {
      e.printStackTrace();
      clazz = Class.forName("java.lang." + name);
    }
    Class[] types = new Class[args.length];
    for(int i = 0; i < args.length; i++) {
      types[i] = args[i].getClass();
    }
    Constructor cons = clazz.getConstructor(types);
    return cons.newInstance(args);
  }

  public boolean toBoolean(Object obj) {
    return "true".equals(obj.toString());
  }

  public String echo(String a, Object[] args) {
    System.out.println("echo 1 + var\na=" + a);
    StringBuffer sb = new StringBuffer();
    sb.append("echo1+var(");
    for(int i = 0; i < args.length; i++) {
      String s = "#" + i + "=" + args[i].toString();
      sb.append(" " + s);
      System.out.println(s);
    }
    sb.append(")");
    return sb.toString();
  }


  public String echo2(String a, String b, String c) {
    System.out.println("echo2");
    System.out.println("a=" + 1);
    System.out.println("b=" + b);
    System.out.println("c=" + c);

    return "echo2(" + a + ", " + b + ", " + c + ")";
  }

  public void main(Object[] args) {
    for(int i = 0; i < args.length; i++) {
      System.out.println("#" + i + "=" + args[i].toString());
    }
  }
}
