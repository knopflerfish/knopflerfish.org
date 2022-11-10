/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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

package org.knopflerfish.bundle.command;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.Converter;
import org.osgi.util.tracker.ServiceTracker;

public class CommandProvidersService implements CommandProviders {
  ServiceTracker<?, ?> cpTracker;
  ServiceTracker<Converter, Converter> convTracker;

  public CommandProvidersService() {
    
  }
  
  public void open() {
    try {
      Filter filter =  Activator.bc
        .createFilter("(&" + 
                      "(" + CommandProcessor.COMMAND_SCOPE + "=*)" + 
                      "(" + CommandProcessor.COMMAND_FUNCTION + "=*)" + 
                      ")");
      
      cpTracker = new ServiceTracker<>(Activator.bc,
                                       filter,
                                       null);
      cpTracker.open();
    } catch (Exception e) {
      throw new RuntimeException("Failed to init command provider tracker " + e); 
    }
    convTracker = new ServiceTracker<>(Activator.bc,
                                       Converter.class.getName(),
                                       null);
    convTracker.open();
  }

  public void close() {
    cpTracker.close();
    cpTracker = null;

    convTracker.close();
    convTracker = null;
  }

  public <T> T convert(Class<T> desiredType, Object from) {
    if(from == null) {
      return null;
    }

    // Give service converters a chance
    try {
      ServiceReference<Converter>[] srl = convTracker.getServiceReferences();
      Filter filter =  Activator.bc
        .createFilter("(" + Converter.CONVERTER_CLASSES + "=" + desiredType.getName() + ")");
      for(int i = 0; srl != null && i < srl.length; i++) {
        if(filter.match(srl[i])) {
          Converter conv = convTracker.getService(srl[i]);
          Object to = conv.convert(desiredType, from);
          if (to != null) {
            return desiredType.cast(to);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // final attempt, try java casting
    if (desiredType.isAssignableFrom(from.getClass())) {
      return desiredType.cast(from);
    }

    return null;
  }
  
  public Collection<?> findCommands(String scope, String name) {
    // System.out.println("findCommands " + scope + ", " + name);
    Collection<Object> candidates = new LinkedHashSet<>();
    try {
      Filter filter =  Activator.bc
        .createFilter("(&" + 
                      (scope != null ? ("(" + CommandProcessor.COMMAND_SCOPE + "=" + scope + ")") : "") + 
                      "(" + CommandProcessor.COMMAND_FUNCTION + "=*)" + 
                      ")");
      ServiceReference<?>[] srl = cpTracker.getServiceReferences();
      if (srl != null) {
        for (ServiceReference<?> serviceReference : srl) {
          if (filter.match(serviceReference)) {
            Object fobj = serviceReference.getProperty(CommandProcessor.COMMAND_FUNCTION);
            Object service = Activator.bc.getService(serviceReference);
            if (matchName(service, fobj, name)) {
              candidates.add(Activator.bc.getService(serviceReference));
            }
            Activator.bc.ungetService(serviceReference);
          }
        }
      }
      return candidates;
    } catch (Exception e) {
      throw new RuntimeException("Failed to find scope=" + scope + ", name=" + name + ", ", e);
    }
  }

  String getNamePart(String s) {
    int ix = s.indexOf(" ");
    if(ix != -1) {
      return s.substring(0, ix);
    }
    return s;
  }

  String getHelpPart(String s) {
    int ix = s.indexOf(" ");
    if(ix != -1) {
      return s.substring(ix+1);
    }
    return "";
  }

  void addNames(Collection<String> names, Object service, String name) {
    if(name.endsWith("*")) {
      String prefix = name.substring(0, name.length()-1);
      Method[] ml = service.getClass().getMethods();
      for (Method method : ml) {
        String s = method.getName();
        if (s.startsWith(prefix)) {
          names.add(getNamePart(s));
        }
      }
    } else {
      names.add(getNamePart(name));
    }
  }

  boolean matchName(Object service, Object fobj, String name) {
    Set<String> names = new HashSet<>();
    if(fobj instanceof List) {
      for (Object object : (List<?>) fobj) {
        String s = object.toString();
        addNames(names, service, s);

      }
    } else if(fobj.getClass().isArray()) {
      for (int i = 0; i < Array.getLength(fobj); i++) {
        addNames(names, service, Array.get(fobj, i).toString());
      }
    } else {
      addNames(names, service, fobj.toString());
    }

    return names.contains(name);

  }
}
