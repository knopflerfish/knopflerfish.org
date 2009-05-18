package org.knopflerfish.bundle.command;

import java.util.*;
import java.io.*;
import java.net.URL;

public class Tokenizer {
  Set whiteTokens = new HashSet();

  public static final String PIPE    = "|";
  public static final String SEP     = ";";
  public static final String ASSIGN  = "=";

  public static void main(String[] argv) {
    Tokenizer tz = new Tokenizer();

    try {
      StringBuffer sb = new StringBuffer();
      {
        byte[] buf = new byte[1024];
        int n;
        URL url = new URL(argv[0]);
        InputStream is = url.openStream();
        while(-1 != (n = is.read(buf))) {
          sb.append(new String(buf));
        }
      }
      tz.init(sb.toString(), 0);

      int n = 0;
      List tokens = tz.tokenize();
      for(Iterator it = tokens.iterator(); it.hasNext(); ) {        
        System.out.println(n + ": " + it.next());
        n++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Tokenizer() {
  }

  public Tokenizer(CharSequence cs) {
    init(cs, 0);
  }

  public Tokenizer(String s) {
    init(new StringBuffer(s), 0);
  }

  public void addWhiteToken(String s) {
    whiteTokens.add(s);
  }

  public List tokenize() {
    ArrayList al = new ArrayList();
    while(hasMore()) {
      String token = getToken();
      if(token.trim().length() > 0 && !whiteTokens.contains(token)) {        
        al.add(token);
      }
    }
    return al;
  }


  int length;
  int pos;
  CharSequence cs;

  public void init(CharSequence cs, int pos) {
    this.cs  = cs;
    this.pos = pos;
    this.length = cs.length();
  }

  public boolean hasMore() {
    return pos < length;
  }


  protected CharSequence rest() {
    return cs.subSequence(pos, cs.length());
  }

  
  void d(String s) {
    System.out.println(s + ": " + pos + ", " + rest());
  }

  char peek() {
    return cs.charAt(pos);
  }

  char get() {
    char c = cs.charAt(pos++);
    if(c == '\\') {
      c = cs.charAt(pos++);
      switch(c) {
      case '\\':  return '\\';
      case 't':   return '\t';
      case 'b':   return '\b';
      case 'f':   return '\f';
      case 'n':   return '\n';
      case 'r':   return '\r';
      default:
        throw new IllegalArgumentException("Unknown escape " + c);
      }
    } else {
      return c;
    }
  }
  
  public String getToken() {
    char c = peek();
    StringBuffer sb = new StringBuffer();
    if(isWS(c)) {      
      get();
      return "";
    } else if(isCIT(c)) {
      get();
      // sb.append(c);
      getuntil(sb, c);
      // sb.append(c);
      return sb.toString();
    } else {
      char mChar = matchChar(c);
      if(isJIP(c)) {
        sb.append(get());
        while(hasMore()) {
          c = peek();
          if(Character.isJavaIdentifierPart(c) 
             || (c == '{' || c == '}' || c == '-' || c == '.' || c == ':')
             /*|| !isSPECIAL(c) */
             && !isWS(c)) {
            sb.append(get());
          } else {
            break;
          }
        }
        return sb.toString();
      } else if(!isSPECIAL(c)) {
        sb.append(get());
        while(hasMore()) {
          c = peek();
          if(!isSPECIAL(c) && !isWS(c)) {
            sb.append(get());
          } else {
            break;
          }
        }
        return sb.toString();
      } else if(NO_CHAR != mChar) {
        sb.append(get());
        matchRecursive(sb, c, mChar);
        return sb.toString();
      } else if(c == '$') {
        sb.append(get());
        String t = getToken();
        if(t.length() == 0) {
          throw new IllegalArgumentException("Missing token after $");
        }
        sb.append(t);
        return sb.toString();
      } else if(isOPERATOR(c)) {
        sb.append(get());
        return sb.toString();
      } else if(c == ';') {
        sb.append(get());
        return sb.toString();
      } else if(c == '|') {
        sb.append(get());
        return sb.toString();
      }
    }
    throw new IllegalArgumentException("Unexpected char '" + c + "' at " + rest());
  }

  static char matchChar(char c) {
    switch(c) {
    case '{': return '}';
    case '<': return '>';
    case '(': return ')';
    case '[': return ']';
    default: return NO_CHAR;
    }
  }

  void getuntil(StringBuffer sb, char c1) {
    while(hasMore()) {
      char c = get();
      if(c == c1) {
        return;
      } else {
        sb.append(c);
      }
    }
    throw new IllegalArgumentException("Missing ending " + c1);
  }

  protected static final char NO_CHAR = '\0';
  
  void matchRecursive(StringBuffer sb, char c1, char c2) {
    // d("mR");
    char citC = NO_CHAR;
    while(hasMore()) {
      char c = get();
      sb.append(c);
      if(citC != NO_CHAR) {
        if(c == citC) {
          citC = NO_CHAR;
        }
      } else {
        if(isCIT(c)) {
          citC = c;
        } else {
          if(c == c1) {
            matchRecursive(sb, c1, c2);
          } if(c == c2) {
            return;
          } 
        }
      }
    }
    throw new IllegalArgumentException("Missing closing '" + c2 + "'");
  }

  public static boolean isCIT(char c) {
    return c == '\'' || c == '\"';
  }

  public static boolean isJIP(char c) {
    return c != '$' && Character.isJavaIdentifierPart(c);
  }

  public static boolean isWS(char c) {
    return Character.isWhitespace(c);
  }

  public static boolean isSPECIAL(char c) {
    return -1 != "=|;<{[()]}>$,".indexOf(c);
  }

  public static boolean isOPERATOR(char c) {
    return -1 != "=!~`#$%?^&*-:,/?@.".indexOf(c);
  }

  /**
   * Check if a String is an array
   */
  public static boolean isArray(String s) {
    return s != null && s.startsWith("[");
  }
  
  /**
   * Check if a string is an execution block
   */
  public static boolean isExecutionBlock(String s) {
    return s != null && s.startsWith("<");
  }

  /**
   * Parse a string into arrays recursively   
   */
  public static List parseArray(String arg) {
    Tokenizer tz = new Tokenizer(trimBlock(arg));
    tz.addWhiteToken(",");
    
    List l = tz.tokenize();
    
    for(int i = 0; i < l.size(); i++) {
      String s = (String)l.get(i);
      if(isArray(s)) {
        l.set(i, parseArray(s));
      }
    }
    return l;
  }

  public static CharSequence trimBlock(String arg) {
    StringBuffer sb = new StringBuffer(arg.trim());
    sb.deleteCharAt(0);
    sb.deleteCharAt(sb.length()-1);
    return sb;
  }
}
