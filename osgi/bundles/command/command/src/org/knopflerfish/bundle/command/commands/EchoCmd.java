/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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

package org.knopflerfish.bundle.command.commands;

import java.lang.reflect.Constructor;

@SuppressWarnings("unused")
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
      System.out.println("#" + i + "=" + args[i]);
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
    Class<?> clazz;
    try {
      clazz = Class.forName(name);
    } catch (Exception e) {
      e.printStackTrace();
      clazz = Class.forName("java.lang." + name);
    }
    Class<?>[] types = new Class[args.length];
    for(int i = 0; i < args.length; i++) {
      types[i] = args[i].getClass();
    }
    Constructor<?> cons = clazz.getConstructor(types);
    return cons.newInstance(args);
  }

  public boolean toBoolean(Object obj) {
    return "true".equals(obj.toString());
  }

  public String echo(String a, Object[] args) {
    System.out.println("echo 1 + var\na=" + a);
    StringBuilder sb = new StringBuilder();
    sb.append("echo1+var(");
    for(int i = 0; i < args.length; i++) {
      String s = "#" + i + "=" + args[i].toString();
      sb.append(" ").append(s);
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
