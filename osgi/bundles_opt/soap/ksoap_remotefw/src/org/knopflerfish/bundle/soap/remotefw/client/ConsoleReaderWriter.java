package org.knopflerfish.bundle.soap.remotefw.client;

import java.io.PrintWriter;
import java.io.Reader;

import org.knopflerfish.service.soap.remotefw.RemoteFW;

public class ConsoleReaderWriter {

  RemoteFW fw;
  Reader in;
  PrintWriter out;
  Thread runner = null;
  boolean bRun = false;

  public ConsoleReaderWriter(RemoteFW fw, Reader in, PrintWriter out) {
    this.fw = fw;
    this.in = in;
    this.out = out;
    start();
  }

  void start() {
    if(runner == null) {
      runner = new Thread() {
          public void run() {
            StringBuffer buf = new StringBuffer();
            while(bRun) {
              try {
                char ch = 0;
                do {
                  int i = in.read();
                  if (i != -1) {
                    ch = (char) i;
                    if (ch != '\r' && ch != '\n') {
                      buf.append(ch);
                    }
                  }
                } while (bRun && ch != '\n');
                if (buf.length() == 0) { // No command - new prompt
                  out.print("> ");
                  out.flush();
                } else {
                  String result = fw.runCommand(buf.toString());
                  out.print(result);
                  out.flush();
                }
                buf = new StringBuffer();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
        };
      bRun = true;
      runner.start();
    }
  }

  void stop() {
    if(runner != null) {
      bRun = false;
      runner = null;
    }
  }

}
