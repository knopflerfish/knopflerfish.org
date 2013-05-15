/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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
package org.knopflerfish.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Capabilities {

  /**
   * List of registered capabilities indexed by name space.
   * The list is order in manifest order.
   */
  private HashMap<String, ArrayList<BundleCapabilityImpl>> namespaceCapabilties =
      new HashMap<String, ArrayList<BundleCapabilityImpl>>();


  List<BundleCapabilityImpl> getCapabilities(String namespace) {
    return namespaceCapabilties.get(namespace);
  }

  void addCapabilities(Map<String, List<BundleCapabilityImpl>> capabilities) {
    for (Entry<String, List<BundleCapabilityImpl>> e : capabilities.entrySet()) {
      final String ns = e.getKey();
      ArrayList<BundleCapabilityImpl> bcl = namespaceCapabilties.get(ns);
      if (bcl == null) {
        bcl = new ArrayList<BundleCapabilityImpl>();
        namespaceCapabilties.put(ns, bcl);  
      }
      bcl.addAll(e.getValue());
    }
  }

  void removeCapabilities(Map<String, List<BundleCapabilityImpl>> capabilities) {
    for (Entry<String, List<BundleCapabilityImpl>> e : capabilities.entrySet()) {
      final String ns = e.getKey();
      ArrayList<BundleCapabilityImpl> bcl = namespaceCapabilties.get(ns);
      if (bcl != null) {
        int before = bcl.size();
        bcl.removeAll(e.getValue());
        if (bcl.isEmpty()) {
          namespaceCapabilties.remove(ns);
        }
        if (before != bcl.size() + e.getValue().size()) {
          throw new RuntimeException("Internal error, tried to remove unknown capabilities");          
        }
      } else {
        throw new RuntimeException("Internal error, tried to remove unknown name space with capabilities");
      }
    }
  }

  Collection<ArrayList<BundleCapabilityImpl>> getAll() {
    return namespaceCapabilties.values();
  }

}
