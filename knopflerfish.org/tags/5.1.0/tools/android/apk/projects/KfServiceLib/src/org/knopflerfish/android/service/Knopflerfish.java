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
