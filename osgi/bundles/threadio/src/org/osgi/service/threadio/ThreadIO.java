package org.osgi.service.threadio;

import java.io.InputStream;
import java.io.PrintStream;

public interface ThreadIO {
  public void close();
  public void setStreams(InputStream in, 
                         PrintStream out, 
                         PrintStream err);
}
