package org.knopflerfish.android.service;

import java.io.IOException;

import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class Knopflerfish extends Service {
  public static final String ACTION_INIT = "org.knopflerfish.android.action.INIT";

  private Framework fw;
  private Object fwLock = new Object();
  
  public Knopflerfish() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, final int startId) {
	  
    synchronized (fwLock) {
      if (fw == null) {
        try {
          // intent == null if restarted by the system -> start without init
          String action = intent != null ? intent.getAction() : null;

          showToast("Starting framework.");
          fw = KfApk.newFramework(getStorage(),
                                  ACTION_INIT.equals(action),
                                  this.getAssets());
          if (fw != null) {
            new Thread(new Runnable() {
              public void run() {
                KfApk.waitForStop(fw); // does not return until shutdown
                stopSelf();            // shut down, stop service                //TODO: only stop if shut down from the inside?
              }
            },"KF wait for stop").start();
          } else {
            // framework did not init/start
          }
        } catch (IOException ioe) {
          Log.e(getClass().getName(), "Error starting framework", ioe);          
          showToast("Error starting framework!");
        }
      } else {
        // no op start-command
        showToast("Framework is already running.");
      }
    }
    return Service.START_STICKY; 
 	};
	
	
	@Override
	public void onDestroy() {
	  synchronized (fwLock) {
	    if (fw != null) {
	      // service destroyed, shut down framework
	      try {
	        fw.stop();
	        fw = null;
	        showToast("Framework was shut down.");
	      } catch (BundleException be) {
	        Log.e(getClass().getName(), "Error shutting down framework", be);
	        showToast("Error shutting down framework!");
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
	
	private void showToast(String msg) {
	  Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	private String getStorage() {
	  return getApplication().getFilesDir().getAbsolutePath();
	}
	
}
