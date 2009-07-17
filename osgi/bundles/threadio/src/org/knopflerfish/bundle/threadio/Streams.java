package org.knopflerfish.bundle.threadio;


import java.io.InputStream;
import java.io.PrintStream;

public class Streams {
  InputStream in;
  PrintStream out;
  PrintStream err;
  Streams     orig;
  
  public Streams(InputStream in,
                 PrintStream out, 
                 PrintStream err,
                 Streams orig) {
    this.in   = in;
    this.out  = out;
    this.err  = err;
    this.orig = orig;
  }
}
