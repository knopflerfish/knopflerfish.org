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

package org.knopflerfish.bundle.logcommands;

import java.util.Hashtable;

import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.log.LogConfig;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * * Bundle activator implementation. * *
 * 
 * @author Jan Stein *
 * @version $Revision: 1.1.1.1 $
 */
public class LogCommands implements BundleActivator {

    static final String COMMAND_GROUP = org.knopflerfish.service.console.CommandGroup.class
            .getName();

    static BundleContext bc;

    static ServiceTracker logConfigTracker;

    /*---------------------------------------------------------------------------*
     *			  BundleActivator implementation
     *---------------------------------------------------------------------------*/

    /**
     * Called by the framework when this bundle is started.
     * 
     * @param bc
     *            Bundle context.
     */
    public void start(BundleContext bc) {
        LogCommands.bc = bc;

        // Create service tracker for log config service
        LogCommands.logConfigTracker = new ServiceTracker(bc, LogConfig.class
                .getName(), null);
        LogCommands.logConfigTracker.open();

        // Register log commands
        CommandGroup logCommandGroup = new LogCommandGroup(bc);
        Hashtable props = new Hashtable();
        props.put("groupName", logCommandGroup.getGroupName());
        bc.registerService(COMMAND_GROUP, logCommandGroup, props);

        // Register log config commands
        CommandGroup logConfigCommandGroup = new LogConfigCommandGroup();
        props = new Hashtable();
        props.put("groupName", logConfigCommandGroup.getGroupName());
        bc.registerService(COMMAND_GROUP, logConfigCommandGroup, props);
    }

    /**
     * Called by the framework when this bundle is stopped.
     * 
     * @param bc
     *            Bundle context.
     */
    public void stop(BundleContext bc) {
        // Close service tracker for log config service
        if (LogCommands.logConfigTracker != null)
            LogCommands.logConfigTracker.close();

    }

}
