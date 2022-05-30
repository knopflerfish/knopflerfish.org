/*
 * Copyright (c) 2013-2022 KNOPFLERFISH project
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

package org.knopflerfish.android.service;

import java.io.IOException;
import java.io.Serializable;

import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class Knopflerfish extends Service {
  public static final String ACTION_INIT = "org.knopflerfish.android.action.INIT";
  public static final String MSG = "msg";
  public static final String FW_PROPS = "fwProps";

  public enum Msg { STARTING, STARTED, NOP, NOT_STARTED, STOPPED, NOT_STOPPED };
  
  private Framework fw;
  private Object fwLock = new Object();
  
	@Override
	public int onStartCommand(Intent intent, int flags, final int startId) {
	  // intent == null if restarted by the system -> start without init
	  final String action = intent != null ? intent.getAction() : null;
	  new Thread(new Runnable() {
	    public void run() {
	      synchronized (fwLock) {
	        if (fw == null) {
	          sendMessage(Msg.STARTING);
	          try {
	            fw = KfApk.newFramework(getStorage(),
	                                    ACTION_INIT.equals(action),
	                                    Knopflerfish.this.getAssets());
	            if (fw != null) {
                sendMessage(Msg.STARTED,
                            (Serializable) KfApk.getFrameworkProperties());               
	            } else {
                // framework did not init/start
                sendMessage(Msg.NOT_STARTED);
                stopSelf();
                return;
	            }
            } catch (IOException ioe) {
              sendMessage(Msg.NOT_STARTED);
              Log.e(getClass().getName(), "Error starting framework", ioe);          
              stopSelf();
              return;
	          }
	        } else {
	          // no op start
            sendMessage(Msg.NOP,
                        (Serializable) KfApk.getFrameworkProperties());               
	          return;
	        }
	      }

	      Framework fw_ = fw;
	      if (fw_ != null) {
	        KfApk.waitForStop(fw_); // does not return until shutdown
	        stopSelf(); // shut down (then stop service) or stop():ed
	      }
	    }
	  },"KF starter-waiter").start();

	  return Service.START_STICKY; 
	};
	
  private void sendMessage(Msg res) {
    sendMessage(res, null);
  }

  private void sendMessage(Msg res, Serializable fwProps) {
    Intent i = new Intent(getClass().getName());
    i.putExtra(MSG, res);
    if (fwProps != null) {
      i.putExtra(FW_PROPS, fwProps);
    }
    sendBroadcast(i);
  } 	
 	
	@Override
	public void onDestroy() {
	  synchronized (fwLock) {
	    if (fw != null) {
	      // service destroyed, shut down framework
	      try {
	        fw.stop();
	        fw = null;
          sendMessage(Msg.STOPPED);
	      } catch (BundleException be) {
	        Log.e(getClass().getName(), "Error shutting down framework", be);
          sendMessage(Msg.NOT_STOPPED);
	      }
	    } else {
	      // stopService after failed start?
	    }
    }
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private String getStorage() {
	  return getApplication().getFilesDir().getAbsolutePath();
	}
	
}
