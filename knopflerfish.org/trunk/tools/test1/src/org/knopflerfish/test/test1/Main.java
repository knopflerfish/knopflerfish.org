package org.knopflerfish.test.test1;

public class Main {
  public static void start(String[] argv) {
    System.out.println("started " + Main.class.getName());
    for(int i = 0; i < argv.length; i++) {
      System.out.println("argv" + i + ": " + argv[i]);
    }
  }

  public static void stop() {
    System.out.println("stopped " + Main.class.getName());
  }
}
