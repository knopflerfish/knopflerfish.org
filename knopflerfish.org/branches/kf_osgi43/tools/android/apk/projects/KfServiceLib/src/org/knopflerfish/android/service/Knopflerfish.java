package org.knopflerfish.android.service;

import java.io.IOException;
import java.io.Serializable;

import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class Knopflerfish extends Service {
  public static final String ACTION_INIT = "org.knopflerfish.android.action.INIT";
  public static final String MSG = "msg";
  public static final String EXTRA = "extra";

  public enum Msg { STARTING, STARTED, NOP, NOT_STARTED, STOPPED, NOT_STOPPED };
  
  private Framework fw;
  private Object fwLock = new Object();
  
  private Messenger messenger; // messenger for starting activity
                               // should be list of messengers???
  
  public Knopflerfish() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, final int startId) {
	  
	  Bundle bundle = intent.getExtras();
	  if (bundle != null) {
	    messenger = (Messenger) bundle.get("messenger");
	  }
    
    synchronized (fwLock) {
      if (fw == null) {
        try {
          //sendMessage(StartResult.STARTING);
          // intent == null if restarted by the system -> start without init
          String action = intent != null ? intent.getAction() : null;

          fw = KfApk.newFramework(getStorage(),
                                  ACTION_INIT.equals(action),
                                  this.getAssets());
          if (fw != null) {
            sendMessage(Msg.STARTED, (Serializable) KfApk.getFrameworkProperties());
            new Thread(new Runnable() {
              public void run() {
                KfApk.waitForStop(fw); // does not return until shutdown
                stopSelf();            // shut down, stop service                //TODO: only stop if shut down from the inside?
              }
            },"KF wait for stop").start();
          } else {
            // framework did not init/start
            sendMessage(Msg.NOT_STARTED);
          }
        } catch (IOException ioe) {
          sendMessage(Msg.NOT_STARTED);
          Log.e(getClass().getName(), "Error starting framework", ioe);          
        }
      } else {
        // no op start-command
        sendMessage(Msg.NOP);
      }
    }
    return Service.START_STICKY; 
 	};
	
  private void sendMessage(Msg res) {
    sendMessage(res, null);
  }

    private void sendMessage(Msg res, Serializable extra) {
 	  if (messenger != null) {
 	    Bundle data = new Bundle();
 	    data.putSerializable(MSG, res);
 	    if (extra != null) {
 	      data.putSerializable(EXTRA, extra);
 	    }
 	    Message msg = Message.obtain();
 	    msg.setData(data);
 	    try {
 	      messenger.send(msg);
 	    } catch (RemoteException e) {}
 	  }
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
	      // Shut down after failed start?
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
