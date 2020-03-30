package org.knopflerfish.bundle.command;

import java.io.*;
import java.net.URL;

public class Util {
  static StringBuilder load(URL url) {
    try {
      StringBuilder sb = new StringBuilder();
      byte[] buf = new byte[1024];
      InputStream is = url.openStream();
      while(-1 != is.read(buf)) {
        sb.append(new String(buf));
      }
      return sb;
    } catch (Exception e) {
      throw new RuntimeException("Failed to load " + url, e);
    }
  }
}
