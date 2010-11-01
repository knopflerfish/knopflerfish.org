/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
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

  final private FrameworkContext fwCtx;
  final private Object lock = new Object();
  volatile private BundleEvent be;
  volatile private BundleImpl bundle;
  volatile private int operation = OP_IDLE;
  volatile private Object res;
  volatile private boolean doRun;


  BundleThread(FrameworkContext fc) {
    super(fc.threadGroup, "BundleThread waiting");
    setDaemon(false);
    fwCtx = fc;
    doRun = true;
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
          } catch (InterruptedException ie) { }
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
    do {
      try {
          fwCtx.packages.wait();
      } catch (InterruptedException ie) {
      }
    } while (res == Boolean.FALSE);
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
