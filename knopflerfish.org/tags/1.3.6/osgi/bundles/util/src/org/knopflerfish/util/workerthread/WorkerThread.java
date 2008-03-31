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

package org.knopflerfish.util.workerthread;

public class WorkerThread extends Thread {
    private boolean quit = false;

    private boolean started = false;

    private static int nameTick = 0;

    /**
     * Creates a Workerthread. The start() method must be called externally.
     * 
     */
    public WorkerThread() {
        super("WorkerThread-" + (nameTick++));
    }

    /**
     * Creates a named Workerthread. The start() method must be called
     * externally.
     * 
     */
    public WorkerThread(String name) {
        super(name);
    }

    /**
     * Creates a named Workerthread belonging toi the specified ThreadGroup. The
     * start() method must be called externally.
     * 
     */
    public WorkerThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public void run() {
        started = true;
        //
        // Set up things that are too heavy to set up in the constructor.
        //
        preMainLoopHook();
        //
        // Enter the mainloop.
        //
        Job job = null;
        while (!quit) {
            try {
                job = waitForJob();
                status("Got Job", null, null);
                if (job != null) {
                    if (job instanceof RepeatingJob)
                        ((RepeatingJob) job).run(this);
                    else
                        job.run();
                }
                status("run() OK", job, null);
            } catch (Exception e) {
                status("", null, e);
                // We could do some logging here if we were allowed to have a
                // dependency on LogRef or similar.
            }
        }
        postMainLoopHook();
    }

    /**
     * Override this method in subclasses. It is called by the job thread before
     * the event processing is commenced.
     */
    protected void preMainLoopHook() {
    }

    /**
     * Override this method in subclasses. It is called by the job thread after
     * the job processing has stopped.
     */
    protected void postMainLoopHook() {
    }

    /**
     * Call this method to stop job processing and cause this Workerthread to
     * exit its run() method.
     */
    public void shutdown() {
        quit = true;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Returns true if this Workerthread is running. I.e. if it has been stated
     * and the shutdown() method has not been called.
     */
    public boolean isRunning() {
        return started && (!quit);
    }

    //
    // Methods and variables to handle the job queues
    //
    private JobQueue jobQueue = new JobQueue();

    private JobQueue delayedJobQueue = new JobQueue();

    // For internal use. Called by RepeatingJob.
    void delayedJobQueueAdd(Job job, long delay) {
        delayedJobQueue.add(job, delay);
    }

    private class Link {
        Job job;

        long runAt = 0;

        Link prev = null;

        Link next = null;

        Link(Job job) {
            this.job = job;
        }

        void insertBefore(Link l) {
            this.next = l;
            this.prev = l.prev;
            this.prev.next = this;
            l.prev = this;
        }

        void unlink() {
            prev.next = next;
            next.prev = prev;
        }

        void setDelay(long delayMillis) {
            runAt = delayMillis + System.currentTimeMillis();
        }

        long getTimeout() {
            return runAt - System.currentTimeMillis();
        }

    }

    private class JobQueue {
        int size = 0;

        Link preFirst = new Link(null);

        Link postLast = new Link(null);

        JobQueue() {
            preFirst.next = postLast;
            postLast.prev = preFirst;
        }

        void add(Job job) {
            add(new Link(job));
        }

        void add(Link l) {
            l.insertBefore(postLast);
            size++;
        }

        void add(Job job, long delay) {
            Link l = new Link(job);
            l.setDelay(delay);
            size++;
            if (size == 1) { // This is the first job.
                l.insertBefore(postLast);
                return;
            }
            Link al = preFirst.next;
            while (al != postLast && al.runAt <= l.runAt) {
                al = al.next;
            }
            l.insertBefore(al);
        }

        Job removeFirst() {
            if (size <= 0)
                return null;
            Job j = preFirst.next.job;
            preFirst.next.unlink();
            size--;
            return j;
        }

        void removeJob(Job job) {
            Link l = preFirst.next;
            Link n = null;
            while (l != postLast) {
                n = l.next;
                if (l.job.equals(job)) {
                    l.unlink();
                    size--;
                }
                l = n;
            }
        }

    }

    /**
     * Adds a job to be processed last in the job queue.
     */
    public synchronized void addJob(Job job) {
        jobQueue.add(job);
        if (job instanceof RepeatingJob) {
            ((RepeatingJob) job).repeatsMade = 0;
            ((RepeatingJob) job).quit = false;
        }
        notifyAll();
    } // addJob(Job)

    /**
     * Adds a job to be processed after given delay. When (at least)
     * <code>delayMillis</code> milliseconds has passed, the job will be
     * placed last in the job queue.
     */
    public synchronized void addJob(Job job, long delayMillis) {
        // System.out.println("-- addJob(Job,long) \""+job+"\" got
        // synchronization
        // lock on Workerthread");
        delayedJobQueue.add(job, delayMillis);
        if (job instanceof RepeatingJob) {
            ((RepeatingJob) job).repeatsMade = 0;
            ((RepeatingJob) job).quit = false;
        }
        notifyAll();
    } // addJob(Job, long)

    /**
     * Removes a job from the job queue (linear-time operation).
     */
    public synchronized void removeJob(Job job) {
        jobQueue.removeJob(job);
        delayedJobQueue.removeJob(job);
    } // removeJob(Job)

    private synchronized Job waitForJob() {
        // System.out.println("-- waitForJob() got synchronization lock on
        // Workerthread");
        //
        // Wait for a job to run.
        //
        while (jobQueue.size == 0 && !quit) {
            //
            // Figure out a suitable timeout
            //
            long timeout = 0;
            if (delayedJobQueue.size > 0) {
                Link djl = delayedJobQueue.preFirst.next;
                timeout = djl.getTimeout();
                //
                // If the first timed job has timed out, we move it to the
                // normal job queue.
                //
                if (timeout <= 0) {
                    delayedJobQueue.removeFirst();
                    jobQueue.add(djl);
                    break;
                }
            }
            //
            // When we get here, the jobQueue must be empty.
            //
            try {
                status("Waiting for job (" + timeout + ")", null, null);
                if (delayedJobQueue.size > 0) {
                    wait(timeout);
                } else {
                    wait();
                }
            } catch (InterruptedException e) { // Ignore
            }
        }
        if (quit) {
            status("Quitting!", null, null);
            return null;
        }
        Job j = jobQueue.removeFirst();
        return j;
    } // waitForJob()

    // Override for debugging.
    protected void status(String msg, Job job, Exception e) {
    }

} // class Workerthread
