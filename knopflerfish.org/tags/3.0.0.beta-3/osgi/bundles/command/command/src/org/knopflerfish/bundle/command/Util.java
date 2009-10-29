package org.knopflerfish.bundle.command;

import java.util.*;
import java.io.*;
import java.net.URL;

public class Util {
  static StringBuffer load(URL url) {
    try {
      StringBuffer sb = new StringBuffer();
      {
        byte[] buf = new byte[1024];
        int n;
        InputStream is = url.openStream();
        while(-1 != (n = is.read(buf))) {
          sb.append(new String(buf));
        }
      }
      return sb;
    } catch (Exception e) {
      throw new RuntimeException("Failed to load " + url, e);
    }
  }
}
