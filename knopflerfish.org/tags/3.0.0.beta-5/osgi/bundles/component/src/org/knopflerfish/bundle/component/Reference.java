/*
 * Copyright (c) 2006-2010, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;

import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.cm.*;


/**
 *
 */
class Reference
{
  final Component comp;
  final ReferenceDescription refDesc;
  final Filter targetFilter;

  ComponentMethod bindMethod = null;
  ComponentMethod unbindMethod = null;

  private ReferenceListener listener = null;
  private TreeMap factoryListeners = null;
  private int numListeners;
  private int available;


  /**
   *
   */
  Reference(Component comp, ReferenceDescription refDesc) {
    this.comp = comp;
    this.refDesc = refDesc;
    Filter target = getTarget(comp.compDesc.getProperties(),
                              "component description for " + comp);
    if (target == null) {
      target = refDesc.targetFilter;
    }
    targetFilter = target;
  }


  public String toString() {
    return "Reference " + refDesc.name + " in " + comp;
  }

  /**
   * Start listening for this reference. It is a bit tricky to
   * get initial state synchronized with the listener.
   *
   */
  void start(Configuration [] config) {
    available = 0;
    numListeners = 1;
    if (config != null && config.length > 0) {
      listener = new ReferenceListener(this, config[0]);
      for (int i = 1; i < config.length; i++) {
        update(config[i], false);
      }
    } else {
      listener = new ReferenceListener(this, null);
    }
  }


  /**
   *
   */
  void stop() {
    if (listener != null) {
      listener.stop();
      listener = null;
    } else {
      for (Iterator i = (new HashSet(factoryListeners.values())).iterator(); i.hasNext(); ) {
        ((ReferenceListener)i.next()).stop();
      }
      factoryListeners = null;
    }
  }


  /**
   * 
   */
  void update(Configuration c, boolean useNoPid) {
    String pid = c.getPid();
    if (listener != null) {
      // We only have one listener, check if it still is true;
      if (listener.checkTargetChanged(c)) {
        if (listener.isOnlyPid(useNoPid ? Component.NO_PID : pid)) {
          // Only one pid change listener target
          listener.setTarget(c);
        } else {
          // We have multiple listener we need multiple listeners
          factoryListeners = new TreeMap();
          for (Iterator i = listener.getPids(); i.hasNext(); ) {
            factoryListeners.put(i.next(), listener);
          }
          listener = null;
          // NYI, optimize, we don't have to checkTargetChanged again
        }
      } else if (useNoPid) {
        // No change, just make sure that pid is registered
        listener.addPid(pid, true);
      }
    } 
    if (factoryListeners != null) {
      ReferenceListener rl = (ReferenceListener)factoryListeners.get(pid);
      if (rl != null) {
        // Listener found, check if we need to change it
        if (rl.checkTargetChanged(c)) {
          if (rl.isOnlyPid(pid)) {
            rl.setTarget(c);
            return;
          } else {
            rl.removePid(pid);
            // Fall through to new listener creation
          }
        } else {
          // No change
          return;
        }
      } else {
        // Pid is new, check if we already have a matching listener
        for (Iterator i = new HashSet(factoryListeners.values()).iterator(); i.hasNext(); ) {
          rl = (ReferenceListener)i.next();
          if (!rl.checkTargetChanged(c)) {
            rl.addPid(pid, false);
            factoryListeners.put(pid, rl);
            return;
          }
        }
      }
      numListeners++;
      rl = new ReferenceListener(this, c);
      factoryListeners.put(pid, rl);
    }
  }


  /**
   *
   */
  void remove(String pid) {
    if (listener != null) {
      listener.removePid(pid);
      if (listener.noPids()) {
        listener.addPid(Component.NO_PID, false);
      }
    } else {
      ReferenceListener rl = (ReferenceListener)factoryListeners.remove(pid);
      rl.removePid(pid);
      if (rl.noPids()) {
        rl.stop();
        if (--numListeners == 1) {
          listener = (ReferenceListener)factoryListeners.get(factoryListeners.lastKey());
          factoryListeners = null;
        }
      }
    }
  }


  /**
   *
   */
  ReferenceListener getListener(String pid) {
    if (listener != null) {
      return listener;
    } else {
      System.out.println("GETLISTENER <" + pid + "> = " + factoryListeners.get(pid));
      if (factoryListeners.get(pid) == null){
        for (Iterator i = factoryListeners.keySet().iterator(); i.hasNext(); ) {
          System.out.println("GETLISTENER key <" + i.next() + ">");
        }        
      } 
      return (ReferenceListener)factoryListeners.get(pid);
    }
  }


  /**
   * Get target value for reference, if target is missing or the target
   * string is malformed return null.
   */
  Filter getTarget(Dictionary d, String src) {
    String res = (String)d.get(refDesc.name + ".target");
    if (res != null) {
      try {
        return FrameworkUtil.createFilter(res);
      } catch (InvalidSyntaxException ise) {
        Activator.logError(comp.bc, "Failed to parse target property. Source is " + src, ise);
      }
    }
    return null;
  }
    

  /**
   * Is reference optional?
   */
  boolean isOptional() {
    return refDesc.optional;
  }


  /**
   * Notify component if reference became available.
   *
   * @return True, if component became satisfied otherwise false.
   */
  boolean refAvailable() {
    if (available++ == 0) {
      return comp.refAvailable(this);
    }
    return false;
  }


  /**
   *
   * @return True, if component became unsatisfied otherwise false.
   */
  boolean refUnavailable() {
    if (--available == 0) {
      return comp.refUnavailable(this);
    }
    return false;
  }
}
