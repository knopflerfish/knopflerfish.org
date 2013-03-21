/*
 * Copyright (c) 2010-2011, KNOPFLERFISH project
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

import org.osgi.framework.*;

class BundleThread extends Thread {
  final private static int OP_IDLE = 0;
  final private static int OP_BUNDLE_EVENT = 1;
  final private static int OP_START = 2;
  final private static int OP_STOP = 3;

  final private static int KEEP_ALIVE = 1000;

  final static String ABORT_ACTION_STOP = "stop";
  final static String ABORT_ACTION_MINPRIO = "minprio";
  final static String ABORT_ACTION_IGNORE = "ignore";

  final private FrameworkContext fwCtx;
  private long startStopTimeout = 0;
  final private Object lock = new Object();
  volatile private BundleEvent be;
  volatile private BundleImpl bundle;
  volatile private int operation = OP_IDLE;
  volatile private Object res;
  volatile private boolean doRun;


  BundleThread(FrameworkContext fc) {
    super(fc.threadGroup, "BundleThread waiting");
    setDaemon(true);
    fwCtx = fc;
    doRun = true;
    
    // get bundlethread timeout property value
    String timeout = fwCtx.props.getProperty(FWProps.BUNDLETHREAD_TIMEOUT);
    if (timeout != null) {
      try {
        startStopTimeout = 1000 * Integer.parseInt(timeout);
      } catch (NumberFormatException nfe) {
        fwCtx.debug.println("Property " + FWProps.BUNDLETHREAD_TIMEOUT
                            + " has a non integer value, ignoring");
      }
    }
    
    start();
  }


  /**
   *
   */
  void quit() {
    doRun = false;
    interrupt();
  }


  public void run() {
    while (doRun) {
      synchronized (lock) {
        while (doRun && operation == OP_IDLE) {
          try {
            lock.wait(KEEP_ALIVE);
            if (operation != OP_IDLE) {
              break;
            }
            synchronized (fwCtx.bundleThreads) {
              if (fwCtx.bundleThreads.remove(this)) {
                return;
              }
            }
          } catch (InterruptedException ie) {
          }
        }
        if (!doRun) {
          break;
        }
        Object tmpres = null;
        try {
          switch (operation) {
          case OP_BUNDLE_EVENT:
            setName("BundleChanged #" + be.getBundle().getBundleId());
            fwCtx.listeners.bundleChanged(be);
            break;
          case OP_START:
            setName("BundleStart #" + bundle.getBundleId());
            tmpres = bundle.start0();
            break;
          case OP_STOP:
            setName("BundleStop #" + bundle.getBundleId());
            tmpres = bundle.stop1();
            break;
          }
        } catch (Throwable t) {
          fwCtx.listeners.frameworkError(bundle, t);
        }
        operation = OP_IDLE;
        res = tmpres;
      }
      synchronized (fwCtx.packages) {
        fwCtx.packages.notifyAll();
      }
    }
  }


  /**
   * Note! Must be called while holding packages lock.
   */
  void bundleChanged(final BundleEvent be) {
    this.be = be;
    startAndWait((BundleImpl)be.getBundle(), OP_BUNDLE_EVENT);
  }


  /**
   * Note! Must be called while holding packages lock.
   */
  BundleException callStart0(final BundleImpl b) {
    return (BundleException)startAndWait(b, OP_START);
  }


  /**
   * Note! Must be called while holding packages lock.
   */
  BundleException callStop1(final BundleImpl b) {
    return (BundleException)startAndWait(b, OP_STOP);
  }


  /**
   * Note! Must be called while holding packages lock.
   */
  private Object startAndWait(final BundleImpl b, final int op) {
    synchronized (lock) {
      res = Boolean.FALSE;
      bundle = b;
      operation = op;
      lock.notifyAll();
    }

    // timeout for waiting on op to finish can be set for start/stopp
    long left = 0;
    if (op == OP_START || op == OP_STOP) {
      b.aborted = null; // clear aborted status
      left = startStopTimeout;
    }    
    boolean timeout = false;
    boolean uninstall = false;

    long waitUntil = System.currentTimeMillis() + left;
    do {
      try {
        fwCtx.packages.wait(left);
      } catch (InterruptedException ie) {}
      
      // Abort start/stop operation if bundle has been uninstalled
      if ((op == OP_START || op == OP_STOP) && b.getState() == Bundle.UNINSTALLED) {
        uninstall = true;
        res = null;
      } else if (left > 0) { // we were waiting with a timeout
        left = waitUntil - System.currentTimeMillis();
        
        // check time-out for Bundle.start and .stop
        if (left <= 0 && ((op == OP_START && b.getState() == Bundle.STARTING)
                       || (op == OP_STOP && b.getState() == Bundle.STOPPING))) {
          timeout = true;
          res = null;
        }
      }
      
    } while (res == Boolean.FALSE);

    // if b.aborted is set, BundleThread has/will concluded start/stop
    if (b.aborted == null && (timeout || uninstall)) {
      // BundleThreda is still in BundleActivator.start/.stop, 

      b.aborted = Boolean.TRUE; // signal to BundleThread that this
                                // thread is acting on uninstall/time-out

      String opType = op == OP_START ? "start" : "stop";
      String reason = timeout ? "Time-out during bundle " + opType + "()"
                              : "Bundle uninstalled during " + opType + "()";

      String s = fwCtx.props.getProperty(FWProps.BUNDLETHREAD_ABORT);
      if (s == null) {
        s = ABORT_ACTION_IGNORE;
      }
      fwCtx.debug.println("bundle thread aborted during " + opType
          + " of bundle #" + b.getBundleId() + ", abort action set to '"
          + s + "'");

      if (timeout) {
        if (op == OP_START) {
          // set state, send events, do clean-up like when Bundle.start()
          // throws an exception
          // TODO: startFailed() calls BundleListener.bundleChanged and
          // should not be called with the packages lock as we do here
          b.startFailed();
          
        } else {
          // STOP, like when Bundle.stop() returns/throws an exception
          b.bactivator = null;
          b.stop2();
        }
      }
      
      quit();

      // Check what abort action to use
      if (ABORT_ACTION_STOP.equalsIgnoreCase(s)) {
        stop();
      } else if (ABORT_ACTION_MINPRIO.equalsIgnoreCase(s)) {
        setPriority(Thread.MIN_PRIORITY);
      }

      res = new BundleException("Bundle " + opType + " failed",
          BundleException.STATECHANGE_ERROR, new Exception(reason));
      return res;
    } else {
      synchronized (fwCtx.bundleThreads) {
        fwCtx.bundleThreads.addFirst(this);
        if (op != operation) {
          // NYI! Handle when operation has changed.
          // i.e. uninstall during operation?
        }
        return res;
      }
    }
  }
}