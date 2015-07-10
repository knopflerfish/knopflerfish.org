/*
 * Copyright (c) 2003-2012,2015 KNOPFLERFISH project
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

package org.knopflerfish.bundle.http;

import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.knopflerfish.service.log.LogRef;

public class TransactionManager
  extends ThreadGroup
{

  // private fields

  private final int maxWorkerThreads;
  
  private final int maxKeepAlive;
  
  private final int idleThreadTimeout;

  private static int managerCount = 0;

  private int transactionCount = 0;
  
  private int totalRequestCount = 0;

  private final LogRef log;

  final Registrations registrations;

  private LinkedList<Transaction> transQueue = new LinkedList<Transaction>();
  
  private LinkedList<WorkerThread> workers = new LinkedList<WorkerThread>();
  private LinkedList<WorkerThread> idle_workers = new LinkedList<WorkerThread>();

  private int num_workers = 0;
  private int num_started_workers = 0;

  private int num_keep_alive = 0;

  private int num_idle_workers = 0;
  
  private boolean isRunning = true;
  
  HttpSessionManager sessionManager;
  
     // constructors

  public TransactionManager(HttpConfig httpConfig, final LogRef log,
                            final Registrations registrations,
                            final HttpSessionManager sessionManager)
  {
    super("HttpServer-TransactionGroup-" + managerCount++);

    this.log = log;
    this.registrations = registrations;
    this.sessionManager = sessionManager;
    
    maxWorkerThreads = httpConfig.getMaxThreads();
    maxKeepAlive = httpConfig.getKeepAliveThreads();
    idleThreadTimeout = httpConfig.getThreadIdleTimeout();
    
    log.info("Transaction Manager started with configuration:");
    log.info("  max threads: " + maxWorkerThreads);
    log.info("  max keep alive threads: " + maxKeepAlive);
    log.info("  idle thread time out: " + idleThreadTimeout);
    isRunning = true;
  }

  // public methods

  public void startTransaction(final Socket client,
                               final HttpConfigWrapper httpConfig)
  {
    // Stop refusing transaction when we are stopping or have stopped
    if (!isRunning) {
      return;
    }
    
    final Transaction transaction = new Transaction(TransactionManager.this, ++transactionCount, log, registrations); 
    transaction.init(client, httpConfig);
    
    if (log.doDebug()) {
      log.debug("Starting new transaction: " + (transactionCount));
      log.debug("Checking worker threads");
      log.debug("  total threads: " + num_workers);
      log.debug("  idle: "  + num_idle_workers);
      log.debug("  keep-alive: "  + num_keep_alive);
    }
   
    // Assign the job (transaction) to an idle thread, or start a new thread, or queue it
    // as a last step
    synchronized (workers) {
      if (num_idle_workers > 0) {
        WorkerThread t = idle_workers.removeFirst();
        num_idle_workers--;
        synchronized(t) {
          t.assignJob(transaction);
          // log.info("Waking up idle thread: " + t.getName());
          t.notifyAll();
          return;
        }
      }
    
      if (num_workers == maxWorkerThreads) {
        final int size;
        synchronized (transQueue) {
          transQueue.add(transaction);
          size = transQueue.size();
        }
        if (log.doDebug()) {
          log.debug("Transaction queued: " + transaction + " size is: " + size);
        }
        return;
      }
      
      if (num_workers == 0 || num_idle_workers == 0) {
        WorkerThread wt = new WorkerThread(this, "HttpServer-WorkerThread-" + ++num_started_workers);
        num_workers++;
        workers.addFirst(wt);
        if (log.doDebug())
            log.debug("Starting new Worker Thread: " + wt.getName() );
        wt.assignJob(transaction);
        wt.start();
        return;
      }
    }
  }

  public void initialize() {
    isRunning = true;
  }
  
  public void shutdown() {
    log.debug("shutting down transaction mananger");
    
    isRunning = false;
    
    synchronized (workers) {
      for (Iterator<WorkerThread> li = workers.iterator(); li.hasNext(); ) {
        WorkerThread wt = li.next();
        log.debug(wt.getName() + " - calling shutdown for worker");
        wt.shutdown();
      }
    }
    
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
   
    for (Iterator<WorkerThread> li = workers.iterator(); li.hasNext(); ) {
      WorkerThread wt = li.next();
      if (wt.isAlive())
        log.debug(wt.getName() + " - thread is still alive, calling join()");
        try {
          wt.join(5000);
        } catch (final InterruptedException ignore) {
        }
      if (wt.isAlive()) {
        Activator.log.error("Thread " + wt + ", refuse to stop");  
      }
    }

  }
  
  // extends ThreadGroup
  @Override
  public void uncaughtException(Thread thread, Throwable t)
  {
    if (t instanceof ThreadDeath) {

    } else if (log.doDebug()) {
      log.debug("Internal error", t);
    }
  }

  public int getTransactionCount()  {
    return transactionCount;
  }
  
  public int getRequestCount()  {
    return totalRequestCount;
  }

  private Transaction nextJob() {
    Transaction t = null;
    
    synchronized (transQueue) {
      if (!transQueue.isEmpty()) {
        t = transQueue.removeFirst();
      }
    }
    return t;
  }
    
  public void releaseKeepAlive()  {
    synchronized (this) {
      num_keep_alive--;
    }
  }

  public boolean reserveKeepAlive()
  {
    synchronized(this) {
      if (num_keep_alive < maxKeepAlive) {
        num_keep_alive++;
        return(true);
      }
      else
        return false;
    }
  }

  
  // Internal helper class for workers. A worker 
  // is assigned a job (transaction), completes it and then 
  // looks for a new job, or if no one is present, turn idle
  
  class WorkerThread extends Thread {
    Transaction job = null;
    boolean running = true;
    boolean beenIdle = false;
    RequestImpl requestImpl;
    ResponseImpl responseImpl;
    private int requestCount = 0;
    
    public WorkerThread(ThreadGroup g, String name) {
      super(g, name);
      requestImpl = new RequestImpl(TransactionManager.this.registrations, 
                                    TransactionManager.this.sessionManager);
      responseImpl = new ResponseImpl();
    }
    
    public void assignJob(Transaction r) {
      if (job != null) {
        log.info(getName() + " - this is BAD, job is not null");
      }
      job = r;
    }
    
    @Override
    public void run() {
   
      try {
        while (running == true) {
          if (job != null) {
            job.init(requestImpl, responseImpl);
            // long startTime = System.currentTimeMillis();
            job.run();
//            long endTime = System.currentTimeMillis();
//            if (endTime - startTime > 1000) {
//              log.info(Thread.currentThread().getName() + " long transaction: " + (endTime - startTime));
//            }
             requestImpl.reset(false);
             responseImpl.resetHard();
            
            requestCount += job.getRequestCount();
            totalRequestCount += requestCount;
            
            if (log.doDebug()) {
              log.debug(getName() + " Dispatching Transaction Done: " + job 
                        + " reqquests handled=" + job.getRequestCount()
                        + " total requests=" + requestCount);
            }
            job = nextJob();
          } 
          else {
            
            // No job to run, get idle
            // log.info(getName() + " - no more jobs, idling");
            synchronized (workers) {
              // if job is not null that would indicate a threading issue
              // log a warning or throw an exception? 
              if (job != null) {
                log.warn("This is strange. Possible threading issue!");
                continue;
              }
              job = nextJob();
              if (job != null) {
                //  a new job has been queued up, continue with it
                continue;
              }
              // Prepare to go idle, must leave this synchronized block first
              idle_workers.addLast(this);
              num_idle_workers++;
              beenIdle = true;
            }
            synchronized (this) {
              // Check so that a job hasn't been assigned, which is possible
              // once the thread is marked as idle
              if (job != null) {
                continue;
              }
              this.wait(idleThreadTimeout);
            }
            synchronized (workers) {
              // Check if a job has been assigned, if not we did time out,
              // then we simply give up
              if (job != null) {
                continue;
              }
              else {
                running = false;
//                if (log.doInfo())
//                  log.info(getName() + "Hmm, no more job, giving up");
              }
            }
          }
        }
      } catch (InterruptedException e) {
        log.info(getName() + " - InterruptedException: " +e);
      } catch (Throwable e) {
        log.info("Unhandled Throwable: " + e, e);
      }
      finally {
        if (log.doDebug()) {
          log.debug(getName() + " Thread about to die, removing it from workers");
        }
        synchronized (workers) {
          if (job != null && log.doWarn()) {
            log.warn(getName() + " job is not null. may be bad unless we are shutting down");
          }
          workers.remove(this);
          num_workers--;
          if (running == false) {
            idle_workers.remove(this);
            num_idle_workers--;
          }
          requestImpl.destroy();
          responseImpl.destroy();
        }
      }
    }
    
    public void shutdown() {
      if (!running)
        return;
      
      synchronized(this) {
        running = false;
        if (job != null)
          job.closeConnection();
      }
      this.interrupt();
    }
  }
  
 
} // TransactionManager