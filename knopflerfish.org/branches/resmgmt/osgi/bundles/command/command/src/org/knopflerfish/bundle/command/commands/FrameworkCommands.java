package org.knopflerfish.bundle.command.commands;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.lang.reflect.*;
import org.osgi.framework.*;

public class FrameworkCommands {

  public static String SCOPE      = "framework";
  public static String[] FUNCTION = new String[] { "ps" };

  BundleContext bc;
  public FrameworkCommands(BundleContext bc) {
    this.bc = bc;
  }

  public Bundle[] ps() {
    Bundle[] bl = bc.getBundles();
    for(int i = 0; bl != null && i < bl.length; i++) {
      System.out.println(bl[i].getBundleId() + ", " + bl[i].getLocation());
    }
    return bl;
  }
}
