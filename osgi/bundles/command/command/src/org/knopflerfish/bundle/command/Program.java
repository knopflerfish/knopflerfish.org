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

package org.knopflerfish.bundle.command;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Program {
  protected Program parent;
  protected CommandProviders cp;
  protected Map<String, Object> varMap = new HashMap<>();
  
  public Program(Program parent) {
    this(parent, null);
  }

  public Program(Program parent, CommandProviders cp) {
    this.parent = parent;
    this.cp     = cp;
  }
  
  public static void main(String[] argv) {
    try {
      CommandProviders cp = new CommandProvidersTest();
      Program p = new Program(null, cp);
      
      StringBuilder sb =
        (argv[0].endsWith(".tsl")) 
        ? Util.load(new URL(argv[0]))
        : new StringBuilder(argv[0]);
      Object r = p.exec(sb);
      System.out.println(sb + "\n--\n" + r);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  

  protected CommandProviders getCP() {
    if(cp != null) {
      return cp;
    }
    if(parent != null) {
      return parent.getCP();
    }
    return null;
  }

  protected List<Object> getStatements(Iterator<Object> it, String stop) {
    List<Object> toks = new ArrayList<>();
    while(it.hasNext()) {
      String t = (String)it.next();
      if(stop.equals(t)) {
        break;
      } else {
        toks.add(t);
      }
    }
    return toks.size() > 0 ? toks : null;
  }
  
  public Object exec(CharSequence cs) {
    Tokenizer tz = new Tokenizer(cs);

    List<Object> tokens = tz.tokenize();
    List<List<List<Object>>> pipes  = new ArrayList<>();
    List<Object> toks;

    Iterator<Object> it = tokens.iterator();
    while(null != (toks = getStatements(it, Tokenizer.PIPE))) {
      List<List<Object>> pipe = new ArrayList<>();
      List<Object> statToks;
      Iterator<Object> it2 = toks.iterator();
      while(null != (statToks = getStatements(it2, Tokenizer.SEP))) {
        pipe.add(statToks);
      }
      
      pipes.add(pipe);
    }    
    return execPipes(pipes);
  }
  
  Object execPipes(List<List<List<Object>>> pipes) {
    Object pipeObj = null;
    for (List<List<Object>> pipe : pipes) {
      pipeObj = execPipe(pipe, pipeObj);
    }
    return pipeObj;
  }

  Object execPipe(List<List<Object>> pipe, Object pipeObj) {
    Object r = null;
    for (List<Object> statement : pipe) {
      if(pipeObj != null) {
        List<Object> pipeStmt = new ArrayList<>(statement);
        pipeStmt.add(pipeObj);
        statement = pipeStmt;
      }
      r = execStatementOrAssignment(statement);
    }
    return r;
  }


  public Map<String, Object> getVarMap() {
    return varMap;
  }

  Object getVar(String key) {    
    Object r;

    // first, check if the key uses recursive variables
    int ix = key.indexOf("${");
    if(ix != -1) {
      // if so, resolve them as programs after grabbing
      // the ${....} part
      Tokenizer tz = new Tokenizer(key.substring(ix+2));
      StringBuilder sb2 = new StringBuilder("${");
      tz.matchRecursive(sb2, '{', '}');
      Object k2  = execAsChild(sb2);
      String k2s = k2 != null ? k2.toString() : "";

      // insert result into key and continue lookup
      key = key.substring(0, ix) + k2s + key.substring(ix+sb2.length());
    }

    if(parent != null && !varMap.containsKey(key)) {
      // if we don't have the key but have a parent, ask parent
      r = parent.getVar(key);
    } else {
      // otherwise get stored value
      r = varMap.get(key);
    }

    if(r == null) {
      // if no result found, check system properties
      r = System.getProperty(key);
    }
    return r; 
  }
  
  Object putVar(String key, Object val) {
    varMap.put(key, val);
    return val;
  }

  Object removeVar(String key) {
    Object old = varMap.get(key);
    varMap.remove(key);
    return old != null ? old : "";    
  }


  /**
   * Execute one of
   * <pre>
   *  VAR '=' 
   *  VAR '=' STATEMENT
   *  STATEMENT
   * </pre>
   */
  Object execStatementOrAssignment(List<Object> statement)  {
    if(statement.size() == 2 && 
       Tokenizer.ASSIGN.equals(statement.get(1))) {
      return removeVar(statement.get(0).toString());
    } else if(statement.size() > 2 && 
              Tokenizer.ASSIGN.equals(statement.get(1))) {
      return putVar(statement.get(0).toString(),
                    execStatement(statement.subList(2, statement.size())));
    } else {
      return execStatement(statement);
    }
  }

  Object execStatement(List<Object> statement_in)  {
    // System.out.println("execStatement " + statement_in);
    String cmd = null;
    String scope;
    String mName = null;
    Object cmdInstance = null;
    MethodInfo mi = null;
    List<Object> paramList = new ArrayList<>();

    try {
      List<Object> statement = new ArrayList<>();
      
      {
        int i = 0; 
        while(i < statement_in.size()) {
          Object obj = statement_in.get(i++);
          Object r   = parseArg(obj);
          if(r != null) {
            statement.add(r);
          } else {
            statement.add("");
          }
        }
      }

      List<Object> inArgs  = statement.subList(1, statement.size());
      List<Object> outArgs = new ArrayList<>();

      Object first = statement.get(0);
      if (first instanceof CharSequence) {
        cmd = first.toString();
      } else {
        cmdInstance = first;
      }

      // System.out.println(" cmd=" + cmd + ", cmdInstance= " + cmdInstance);

      if(inArgs.size() == 100) {
        if(cmd != null) {
          return cmd;
        } else {
          return cmdInstance;
        }
      } else {
        if(cmd != null) {
          scope = null;
          int ix = cmd.indexOf(":");
          if(ix != -1) {
            scope = cmd.substring(0, ix);
            cmd   = cmd.substring(ix+1);
          }          
          Collection<?> candidates = getCP().findCommands(scope, cmd);
          // System.out.println(" candidates=" + candidates);
          for (Object candidate : candidates) {
            cmdInstance = candidate;
            mi = findMethod(cmdInstance, cmd, inArgs, outArgs);
            if (mi != null) {
              mName = mi.m.getName();
              break;
            }
          }
          if(mName == null) {
            System.out.println("no matching method");
          }
        } else {
          mName  = inArgs.get(0).toString();
          inArgs = inArgs.subList(1, inArgs.size());
          // System.out.println("inst=" + cmdInstance + ", mName=" + mName + ", inArgs=" + inArgs);
          mi = findMethod(cmdInstance, 
                          mName,
                          inArgs, 
                          outArgs);
        }
        if(mi == null) {
          final String className = cmdInstance == null
              ? ""
              : cmdInstance.getClass().getName() + ".";
          throw new NoSuchMethodException(className + mName + "(" + inArgs + ")");
        }
        
        // System.out.println("mi=" + mi);
        int offset = 0; // mi.type == MethodInfo.TYPE_MAIN ? 1 : 0;
        Class<?>[]  pTypes = mi.m.getParameterTypes();
        Object[] params = new Object[pTypes.length + offset];
        
        // if(offset == 1) {
          // params[0] = cmd;
        // }
        for(int i = offset; i < pTypes.length; i++) {
          Object from = outArgs.get(i);
          params[i] = getCP().convert(pTypes[i], from);
          // System.out.println("params " + i + ": " + from + "->" + params[i]);
          paramList.add(params[i]);
        }
        
        Class<?> retType = mi.m.getReturnType();
        if(retType == Void.TYPE) {
          mi.m.invoke(cmdInstance, params);
          return "";
        } else {
          return mi.m.invoke(cmdInstance, params);
        }        
      }
    } catch (Exception e) {
      e.printStackTrace();
      if(cmdInstance != null && mi != null) {
        throw new RuntimeException("Failed to exec " + 
                                   cmdInstance.getClass().getName() 
                                   + "." + mi.m.getName() + 
                                   "(" + paramList + ")", e);
      } else {
        throw new RuntimeException("Failed to exec " + cmd, e);
      }    
    }
  }

  
  Object parseArg(Object obj) {
    Object r;
    if(obj instanceof CharSequence) {
      String arg = obj.toString();
      
      if(Tokenizer.isArray(arg)) {
        r = Tokenizer.parseArray(arg);
      } else if(Tokenizer.isExecutionBlock(arg)) {
        r = execAsChild(Tokenizer.trimBlock(arg));
      } else if(arg.startsWith("$")) {
        if(arg.startsWith("${")) {
          r = execAsChild(Tokenizer.trimBlock(arg.substring(1)));
          if(r != null) {
            r = getVar(r.toString());
          }
        } else {
          r = getVar(arg.substring(1));
        }
      } else {
        r = arg;
      }
    } else {
      r = obj;
    }
    return r;
  }

  Object execAsChild(CharSequence cs) {
    return (new Program(this)).exec(cs);
  }

  static class MethodInfo {
    static final int TYPE_ALL          = 1;
    static final int TYPE_NULL_PADDED  = 2;
    static final int TYPE_VARARGS      = 3;
    static final int TYPE_MAIN         = 4;
    public Method m;
    public int type;
    
    MethodInfo(Method m, int type) {
      this.m = m;
      this.type = type;
    }

    public String toString() {
      return "MethodInfo[" + 
        "m=" + m + 
        ", type=" + type +
        "]";
    }
  }

  MethodInfo findMethod(Object obj, String cmd, List<Object> args, List<Object> out) {
    try {
      // System.out.println("findMethod obj=" + obj + ", cmd=" + cmd + ", args=" + args);
      Method[] ml = obj.getClass().getMethods();
      for (Method method : ml) {
        if (method.getName().equalsIgnoreCase(cmd)) {
          out.clear();
          if (matchMethodAllArgs(method, args, out)) {
            //             System.out.println("found all " + ml[i]);
            new MethodInfo(method, MethodInfo.TYPE_ALL);
          }
        }
      }
      for (Method method : ml) {
        if (method.getName().equalsIgnoreCase(cmd)) {
          out.clear();
          if (matchMethodNullPaddedArgs(method, args, out)) {
            // System.out.println("found null " + ml[i]);
            return new MethodInfo(method, MethodInfo.TYPE_NULL_PADDED);
          }
        }
      }
      for (Method method : ml) {
        if (method.getName().equalsIgnoreCase(cmd)) {
          out.clear();
          if (matchMethodVarArgs(method, args, out)) {
            // System.out.println("found var " + ml[i] + ", out=" + out);
            return new MethodInfo(method, MethodInfo.TYPE_VARARGS);
          }
        }
      }
      for (Method method : ml) {
        if (method.getName().equalsIgnoreCase("main")) {
          out.clear();
          List<Object> mainArgs = new ArrayList<>();
          mainArgs.add(cmd);
          mainArgs.addAll(args);
          if (matchMethodVarArgs(method, mainArgs, out)) {
            // System.out.println("found main " + ml[i] + ", out=" + out);
            return new MethodInfo(method, MethodInfo.TYPE_MAIN);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  boolean matchMethodNullPaddedArgs(Method m, List<Object> args, List<Object> out) {
    Class<?>[] pTypes = m.getParameterTypes();
    if(args.size() < pTypes.length) {
      if(pTypes[pTypes.length-1].isArray()) {
        return false;
      }
      out.addAll(args);
      while(out.size() < pTypes.length) {
        out.add(null);
      }
      return matchMethodAllArgs(m, out, null);
    }
    return false;
  }

  boolean matchMethodVarArgs(Method m, List<Object> args, List<Object> out) {
    Class<?>[] pTypes = m.getParameterTypes();

    // if(args.size() < pTypes.length) {
      // return false;
    //}

    int last = pTypes.length-1;

    if(!convertParams(pTypes, last, args, out)) {
      return false;
    }
    
    Class<?> pType = pTypes[last];
    if(!pType.isArray()) {      
      return false;
    }
    
    Class<?> aType = pType.getComponentType();
    Object[] array = (Object[])Array.newInstance(aType, args.size() - last);      
    int n = 0;
    for(int i = last; i < args.size(); i++) {
      Object from = args.get(i);
      Object to = null;
      if(from != null) {
        to = getCP().convert(aType, from);
        if(to == null) {
          return false;
        }
      }
      array[n++] = to;
    }
    out.add(array);
    return true; 
  }

  protected boolean matchMethodAllArgs(Method m, List<Object> args, List<Object> out) {
    Class<?>[] pTypes = m.getParameterTypes();
    if(pTypes.length != args.size()) {
      return false;
    }
    return convertParams(pTypes, pTypes.length, args, out);
  }

  protected boolean convertParams(Class<?>[] pTypes, int n, List<Object> args, List<Object> out) {
    for(int i = 0; i < n; i++) {
      Object from = args.get(i);
      Object to = null;
      if(from != null) {
        to = getCP().convert(pTypes[i], from);
        if(to == null) {
          return false;
        }
      }
      if(out != null) {
        out.add(to);
      }
    }
    return true;
  }



}
