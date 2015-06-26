/*
 * Copyright (c) 2015-2015, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
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

package org.knopflerfish.tools.bundledexify;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class Main {

  private Dexifier dexifier;
  private String[] bundles;


  private Dexifier getDexifier() {
    if (dexifier == null) {
      dexifier = new Dexifier();
    }
    return dexifier;
  }


  private String [] getBundles() {
    return bundles;
  }


  void printHelp(PrintStream out) {
    out.println("Usage: bundledexify [options] <bundle.jar>...\n\n" +
                " -force           Rebuild all bundles even if unchanged\n" +
                " -help            Print this page\n" +
                " -apilevel <lvl>  Set target api level and requirements\n" +
                " -keepclassfiles  Do not remove class files\n" +
                " -outdir <dir>    Set output directory to <dir>\n" +
                " -replace         Replace original bundle with result\n" +
                " -verbose         Be verbose\n" +
                "");
  }

  public void parseArgs(String[] argv) {
    int i = 0;
    while (i < argv.length) {
      if ("-help".equals(argv[i])) {
        printHelp(System.out);
        exit("", null);
      } else if ("-apilevel".equals(argv[i]) && i+1 < argv.length) {
        getDexifier().setApiLevel(Integer.parseInt(argv[++i]));
      } else if ("-keepclassfiles".equals(argv[i])) {
        getDexifier().setKeepClassFiles(true);
      } else if ("-verbose".equals(argv[i])) {
        getDexifier().setVerbose(true);
      } else if ("-outdir".equals(argv[i]) && i+1 < argv.length) {
        getDexifier().setDestDir(argv[++i]);
      } else if (argv[i].startsWith("-")){
        System.err.println("Unknown option '" + argv[i] + "'\n");
        printHelp(System.err);
        exit("", null);
      } else {
        break;
      }
      i++;
    }
    if (i < argv.length) {
      bundles = Arrays.copyOfRange(argv, i, argv.length);
    } else {
      exit("No bundles specified", null);
    }
  }

  public static void main(String[] argv) {
    Main main = new Main();
    main.parseArgs(argv);
    for (String bundle : main.getBundles()) {
      try {
        main.getDexifier().dexify(bundle);
      } catch (IOException e) {
        Main.exit("Failed to dexify: " + bundle, e);
      }
    }
  }

  static void exit(String msg, Exception e) {
    System.err.println(msg);
    if (e != null) {
      e.printStackTrace();
    }
    System.exit(1);
  }

}
