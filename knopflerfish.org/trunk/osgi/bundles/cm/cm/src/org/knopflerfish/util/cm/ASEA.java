/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.util.cm;

import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.framework.*;

import org.knopflerfish.service.log.LogRef;

/**
 * This is the nuclear power plant builder.
 *
 * <p>
 * The <code>ASEA</code> abstract class manages an internal table
 * of created configurations (which all must be instances of
 * <code>Ringhals</code>). It uses the abstract <code>create</code>
 * method to create new instances, and will call the instances'
 * <code>updated()</code> as necessary.
 * </p>
 *
 * <p>
 * <b>Example</b>: Create a factory which will create instances
 * of <code>MyConfig</code>
 * <pre>
 *   ASEA cmFactory = 
 *     new ASEA(bc, log,
 *              "com.acme.example.factory", "Acme factory") {
 *        public Ringhals create(BundleContext bc, LogRef log, String pid) {
 *          return new AcmeConfig(bc, log, pid);
 *        }
 *       };
 * 
 *   cmFactory.register();
 * 
 * ...
 *
 * // Config created by factory above
 * public class MyConfig extends Ringhals {
 * 
 *   public AcmeConfig(BundleContext bc, LogRef log, String pid) {
 *     super(bc, log, pid, -1);
 *   }
 * 
 *   int    CM_PROP_value1 = 0;
 *   int    CM_PROP_value2 = 2;
 *   String CM_PROP_value3 = "apa";
 *
 *   public void close() {
 *     super.close();
 *     // any necessary custom cleanup (unregister anything
 *     // created during update()
 *   }
 *
 *   public void updated(String checkpoint, Dictionary props) {
 *     // ...handle checkpoint.
 *     // 
 *     // Possible register some service using values CM_PROP_xxxx
 *   }
 * }
 *
 * </pre>
 *
 * <p>
 * <b>Note</b>: If you use this utility class, you must still make
 * sure your bundle have an <code>Import-package</code> manifest entry for
 * <code>org.osgi.service.cm</code>. 
 * </p>
 *
 * @see Ringhals
 */
public abstract class ASEA implements ManagedServiceFactory {

  protected BundleContext bc;
  protected LogRef        log;
  protected String        pid;
  protected String        name;

  private ServiceRegistration reg           = null;

  // String (pid) -> Ringhals (configuration)
  protected Hashtable configs = new Hashtable();

  /**
   * Create a new <code>Ringhals</code> configuration factory.
   *
   * @param bc BundleContext used for registering configuration factory.
   * @param log Optional LogRef. Can be null.
   * @param pid service.pid to register fgactory config as.
   */
  public ASEA(BundleContext bc, 
	      LogRef        log, 
	      String        pid,
	      String        name) {
    this.bc          = bc;
    this.log         = log;
    this.pid         = pid;
    this.name        = name;
  }

  /**
   * Create method for new configurations. Used by <code>updated()</code>
   *
   * <p>
   * This method should create a new instance of the <code>Ringhals</code>
   * class. The new instance will be saved by <code>ASEA</code> until
   * <code>close</code> (or <code>unregister</code>) is called.
   * </p>
   *
   * @param bc BundleContext used when creating the ASEA
   * @param log LogRef used when creating the ASEA
   * @param pid pid for the new configuration
   */
  public abstract Ringhals create(BundleContext bc, 
				  LogRef log, 
				  String pid);    

  /**
   * Implements ManagedServiceFactory.
   */
  public String getName() {
    return name;
  }
  
  /**
   * Implements ManagedServiceFactory.
   *
   * <p>
   * If <code>pid</code> already exists, call <code>updated(props)</code>
   * on the configuration. If <code>pid</code> does not exist, create
   * it using the abstract method <code>create</code>, then call
   * <code>updated(props)</code> on the new configuration.
   * </p>
   *
   * @throws RuntimeException if creation failes.
   */
  public void updated(String pid, Dictionary props) {
    Ringhals r = (Ringhals)configs.get(pid);
    if(r == null) {
      try {
	r = create(bc, log, pid);
	configs.put(pid, r);
	r.open();
      } catch (Exception e) {
	throw new 
	  RuntimeException("Failed to create new config, pid="+pid+", err="+e);
      }
    }

    if(log != null && log.doDebug()) log.debug("updating " + pid);

    r.updated(props);
  }
  
  /**
   * Implements ManagedServiceFactory.
   *
   * <p>
   * If <code>pid</code> exists, call its <code>close</code> method and
   * remove it from internal table.
   * </p>
   *
   * @param pid PID to remove.
   */
  public void deleted(String pid) {
    Ringhals r = (Ringhals)configs.get(pid);
    if(r != null) {
      if(log != null && log.doDebug()) {
	log.debug("removing config " + pid);
      }
      configs.remove(pid);
      r.close();
    }
  }
  
  /**
   * Register this config as <code>org.osgi.service.cm.ManagedServiceFactory</code> so CM can find it. 
   */
  public void register() {
    if(reg == null) {
      Hashtable props = new Hashtable();
      props.put("service.pid", pid);
      props.put("service.name", getName());

      reg = bc.registerService(ManagedServiceFactory.class.getName(), 
			       this, 
			       props);	
    }
  }
  
  /**
   * Unregister this factory and close all created configurations.
   */
  public void unregister() {
    if(reg != null) {
      reg.unregister();
      reg = null;
    }
    close();
  }
  
  /**
   * Close all created configurations.
   */
  public void close() {
    for(Enumeration e = configs.keys(); e.hasMoreElements(); ) {
      String pid = (String)e.nextElement();
      Ringhals r = (Ringhals)configs.get(pid);
      r.close();
    }
    configs.clear();
  }
}


