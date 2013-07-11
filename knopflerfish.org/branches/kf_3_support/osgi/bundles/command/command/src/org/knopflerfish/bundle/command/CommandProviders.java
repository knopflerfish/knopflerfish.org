package org.knopflerfish.bundle.command;

import java.util.*;
import org.osgi.service.command.*;


public interface CommandProviders {
  public Object   convert(Class desiredType, Object from);
  public Collection  findCommands(String scope, String name);
}
