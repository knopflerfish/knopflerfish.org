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

public abstract class RepeatingJob extends Job {
    private int[] delays;

    int repeatsMade = 0;

    private int repeatPolicy;

    boolean quit = false;

    /**
     * Creates a repetetive event. This Job still needs to be added to an
     * WorkerThread, but after its <code>run</code> has been called the first
     * time, it will add itself after a delay indicated by
     * <code>delays[0]</code>. Subsequent runs will occur as indicated by the
     * array <code>delays</code>. All delays are in milliseconds.
     * 
     * @param delays
     *            The delays (in milliseconds) to make between the calls to this
     *            RepeatingJobs <code>run()</code> method.
     * @param repeatPolicy
     *            Indicates what to do when this RepeatingJob has been called as
     *            many times as the <code>delays</code> parameter specifies by
     *            its length. A positive value will keep on calling the
     *            <code>run()</code> method indefinitely, each time using
     *            <code>repeatPolicy</code> milliseconds as the delay. A
     *            negative value or zero, will cause this event not to be
     *            repeated any more.
     */
    public RepeatingJob(int[] delays, int repeatPolicy) {
        this.delays = delays;
        this.repeatPolicy = repeatPolicy;
    }

    /**
     * Internal method. Called by an WorkerThread to enable the RepeatingJob to
     * add itself to the WorkerThread after the <code>run()</code> is called.
     * If that method throws an Exception, no further repeats will be scheduled.
     */
    final void run(WorkerThread workerthread) throws Exception {
        if (quit)
            return;
        run();
        int nextdelay = repeatPolicy;
        if (delays != null && repeatsMade < delays.length)
            nextdelay = delays[repeatsMade];
        repeatsMade++;
        if (quit)
            return;
        if (nextdelay > 0)
            workerthread.delayedJobQueueAdd(this, nextdelay);
    }

    /**
     * This method can be called to stop repeating this Job.
     */
    public void quit() {
        quit = true;
    }

    /**
     * Returns the number of successful repeats this job has made. I.e., the
     * number of times its run() method has been invoked and terminated normally
     * since it was last added to the WorkerThread.
     */
    public int getRepeatNo() {
        return repeatsMade;
    }

}
