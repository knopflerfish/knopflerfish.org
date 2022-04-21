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

import java.util.*;
import org.osgi.service.command.*;
import java.lang.reflect.Method;
import org.knopflerfish.bundle.command.commands.*;


public class CommandProvidersTest implements CommandProviders {
  
  JavaLangConverter conv = new JavaLangConverter();
  
  Map commands = new HashMap() {{
    put("echocmd", new EchoCmd());
  }};
  
  public Object convert(Class desiredType, Object from) {
    return conv.convert(desiredType, from);
  }
  

  public Collection findCommands(String scope, String name) {
    Collection candidates = new LinkedHashSet();
    if(scope != null) {
      Object r = commands.get(scope);
      if(r != null) {
        candidates.add(r);
      }
    } else {
      candidates.addAll(commands.values());
    }
    
    for(Iterator it = candidates.iterator(); it.hasNext(); ) {
      Object obj = it.next();
      
      Method[] ml = obj.getClass().getMethods();
      for(int i = 0; i < ml.length; i++) {
        if(ml[i].getName().equalsIgnoreCase(name)) {
          candidates.add(obj);
        }
      }
    }
    return candidates;
  }
}
