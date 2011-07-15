package org.knopflerfish.bundle.threadio;


import java.util.*;
import java.io.*;
import org.osgi.framework.*;
import org.osgi.service.threadio.*;

public class ThreadIOImpl implements ThreadIO {
  Bundle b;
  
  ArrayList stack = new ArrayList();
  Streams sTop;

  public ThreadIOImpl(Bundle b) {
    this.b = b;
    this.sTop = new Streams(System.in, System.out, System.err, null);
    Activator.bc.addBundleListener(bundleListener);
  }
  
  BundleListener bundleListener = new BundleListener() {
      public void bundleChanged(BundleEvent ev) {
        if(ev.getBundle().getBundleId() != b.getBundleId()) {
          return;
        }

        switch(ev.getType()) {
        case BundleEvent.STOPPED:
          stop();
          break;
        default:
          // noop;
        }
      }
    };

  void stop() {
    synchronized(stack) {
      while(stack.size() > 0) {
        close();
      }
    }
    sTop = null;
    Activator.bc.removeBundleListener(bundleListener);
  }

  public void close() {
    synchronized(stack) {
      if(stack.size() > 0) {
        Streams s = (Streams)stack.get(stack.size() -1);
        stack.remove(stack.size()-1);
        
        if(s.orig != null) {
          setSystemStreams(s.orig);
        }
      } else {
        throw new IllegalStateException("No streams on stack");
      }
    }
  }

  public void setStreams(InputStream in, 
                         PrintStream out, 
                         PrintStream err) {
    synchronized(stack) {
      Streams orig = new Streams(System.in, System.out, System.err, null);
      Streams s    = new Streams(in, out, err, orig); 
      
      setSystemStreams(s);
      stack.add(s);
    }
  }

  protected void setSystemStreams(Streams s) {
    synchronized(stack) {      
      // sTop.out.println("#" + b.getBundleId() + ": setSystemStreams " + s);

      System.setOut(s.out);
      System.setIn(s.in);
      System.setErr(s.err);
    }
  }
}
