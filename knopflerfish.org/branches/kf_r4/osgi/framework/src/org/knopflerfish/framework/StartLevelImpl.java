/*
 * Copyright (c) 2003-2005, KNOPFLERFISH project
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
import org.osgi.framework.AdminPermission;
import org.osgi.service.startlevel.*;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;

import java.io.File;

import java.security.AccessController;


/**
 * StartLevel service implementation.
 *
 */
public class StartLevelImpl implements StartLevel, Runnable {
  Thread        wc;
  long          wcDelay  = 2000;
  boolean       bRun     = false;
  Queue         jobQueue = new Queue(100);

  final static int START_MIN = 0;
  final static int START_MAX = Integer.MAX_VALUE;

  final static String LEVEL_FILE = "currentlevel";

  int currentLevel   /*  = 0*/;
  int initStartLevel   = 1;
  int targetStartLevel = currentLevel;

  Framework framework;

  FileTree storage;

  // Set to true indicates startlevel compatability mode.
  // all bundles and current start level will be 1
  boolean  bCompat /*= false*/;

  public static final String SPEC_VERSION = "1.0";
  
  private AdminPermission SYSTEM_ADMIN_STARTLEVEL_PERM;

  public StartLevelImpl(Framework framework) {
    this.framework = framework;
    
    if(framework.permissions != null){
    	SYSTEM_ADMIN_STARTLEVEL_PERM = new AdminPermission(framework.systemBundle, AdminPermission.STARTLEVEL);
    }
    
    storage = Util.getFileStorage("startlevel");

    setStartLevel(1);
  }
  
  private void checkStartLevelAdminSystem(){
	  if(SYSTEM_ADMIN_STARTLEVEL_PERM != null){  
			AccessController.checkPermission(SYSTEM_ADMIN_STARTLEVEL_PERM);  
	  } 
  }

  void open() {
    
    if(Debug.startlevel) {
      Debug.println("startlevel: open");
    }

    wc   = new Thread(this, "startlevel job thread");
    bRun = true;
    wc.start();

  }

  /**
   * Load persistent state from storage and
   * set up all actions necessary to bump bundle
   * states. 
   * 
   * After this call, getStartLevel will have the correct value.
   *
   * <p>
   * Note that open() needs to be called for any work to 
   * be done.
   * </p>
   */
  void restoreState() {
    if(Debug.startlevel) {
      Debug.println("startlevel: restoreState");
    }
    // Skip level load in mem storage since bundle levels
    // isn't saved anyway
    if(!Framework.bIsMemoryStorage) 
      {
	try {
	  String s = Util.getContent(new File(storage, LEVEL_FILE));
	  if(s != null) {
	    int oldStartLevel = Integer.parseInt(s);
	    if(oldStartLevel != -1) {
	      setStartLevel(oldStartLevel);
	    }
	  }
	} catch (Exception ignored) {
	}
      }
  }
  
  void close() {
    if(Debug.startlevel) {
      Debug.println("*** closing startlevel service");
    }

    bRun = false;
    if(wc != null) {
      try {
	wc.join(wcDelay * 2);
      } catch (Exception ignored) { 
      }
      wc = null;
    }
  }

  public void run() {
    while(bRun) {
      try {
	Runnable job = (Runnable)jobQueue.removeWait((float)(wcDelay / 1000.0));	
	if(job != null) {
	  job.run();
	}
      } catch (Exception ignored) { 
	ignored.printStackTrace();
      }
    }
  }

  public int getStartLevel() {
    return currentLevel;
  }
  
  

  public void setStartLevel(final int startLevel) {
	    
	checkStartLevelAdminSystem();
	
    if(startLevel <= 0) {
      throw new IllegalArgumentException("Initial start level must be > 0, is " + startLevel);
    }


    if(Debug.startlevel) {
      Debug.println("startlevel: setStartLevel " + startLevel);
    }

    jobQueue.insert(new Runnable() {
	public void run() {
	  int sl = bCompat ? 1 : startLevel;
	  targetStartLevel = sl;

	  while(targetStartLevel > currentLevel) {
	    increaseStartLevel();
	  }

	  while(targetStartLevel < currentLevel) {
	    decreaseStartLevel();
	  }


	  // Skip level save in mem storage since bundle levels
	  // won't be saved anyway
	  if(!Framework.bIsMemoryStorage) 
	    {
	      try {
		Util.putContent(new File(storage, LEVEL_FILE), 
				Integer.toString(currentLevel));
	      } catch (Exception e) {
		e.printStackTrace();
	      }
	    }
	  notifyFramework();
	}
      });
  }


  Object lock = new Object();
  

  void increaseStartLevel() {
    synchronized(lock) {

      currentLevel++;

      if(Debug.startlevel) {
	Debug.println("startlevel: increaseStartLevel currentLevel=" + currentLevel);
      }
      Vector set = new Vector();

      List bundles = framework.bundles.getBundles();

      for(Iterator i = bundles.iterator(); i.hasNext(); ) {
	BundleImpl bs  = (BundleImpl)i.next();

	if(canStart(bs)) {
	  if(bs.getStartLevel() == currentLevel) {
	    if(bs.archive.isPersistent()) {
	      set.addElement(bs);
	    }
	  }
	} else {

	}
      }

      Util.sort(set, BSComparator, false);

      for(int i = 0; i < set.size(); i++) {
	BundleImpl bs = (BundleImpl)set.elementAt(i);
	try {
	  if(bs.archive.isPersistent()) {
	    if(Debug.startlevel) {
	      Debug.println("startlevel: start " + bs);
	    }
	    bs.start();
	  }
	} catch (Exception e) {
	  framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bs, e));
	}
      }
    }
  }

  
  void decreaseStartLevel() {
    synchronized(lock) {
      currentLevel--;

      Vector set = new Vector();
      
      List bundles = framework.bundles.getBundles();

      for(Iterator i = bundles.iterator(); i.hasNext(); ) {
	BundleImpl bs  = (BundleImpl)i.next();

	if(bs.getState() == Bundle.ACTIVE) {
	  if(bs.getStartLevel() == currentLevel + 1) {
	    set.addElement(bs);
	  }
	}
      }

      Util.sort(set, BSComparator, true);

      for(int i = 0; i < set.size(); i++) {
	BundleImpl bs = (BundleImpl)set.elementAt(i);

	try {
	  bs.stop();
          
	} catch (Exception e) {
	  framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bs, e));
	} finally {

          bs.bDelayedStart = true;
          bs.setPersistent(true);
          
        }
      }
    }
  }

  boolean canStart(BundleImpl b) {
    return 
      b.getState() != Bundle.UNINSTALLED;
    //      b.getState() != Bundle.ACTIVE;
  }
  
  Util.Comparator BSComparator = new Util.Comparator() {
      public int compare(Object o1, Object o2) {
	BundleImpl b1 = (BundleImpl)o1;
	BundleImpl b2 = (BundleImpl)o2;
	
	return (int)(b1.getBundleId() - b2.getBundleId());
      }
    };


  public int getBundleStartLevel(Bundle bundle) {
    if(bundle.getBundleId() == 0) {
      return 0;
    }
    BundleImpl bs = (BundleImpl)bundle;
    return bs.getStartLevel();
  }
  
  public void setBundleStartLevel(Bundle bundle, final int startLevel) {

	if(framework.permissions != null){  
		AccessController.checkPermission(new AdminPermission(bundle, AdminPermission.EXECUTE));  
	} 
	  
    if(startLevel <= 0) {
      throw new IllegalArgumentException("Initial start level must be > 0, is " + startLevel);
    }

    if(bundle.getBundleId() == 0) {
      throw new IllegalArgumentException("System bundle start level cannot be changed");
    }
    
    final BundleImpl bs = (BundleImpl)bundle;

    if(bs.getState() == Bundle.UNINSTALLED) {
      throw new IllegalArgumentException("uninstalled bundle start level cannot be changed");
    }

    jobQueue.insert(new Runnable() {
	public void run() {
	  int sl  = bCompat ? 1 : startLevel;

	  bs.setStartLevel(sl);
	  syncStartLevel(bs);
	  notifyFramework();
	}
      });
  }
  

  void syncStartLevel(BundleImpl bs) {
    synchronized(lock) {

      if(bs.getStartLevel() <= currentLevel) {
	if(canStart(bs)) {
	  if(bs.archive.isPersistent() ||  (bs.getState() == Bundle.RESOLVED)) {
	    try {
	      bs.start();
	    } catch (Exception e) {
	      framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bs, e));
	    }
	  } else {
	    bs.bDelayedStart = true;
	  }
	}
      } else if(bs.getStartLevel() > currentLevel) {
	if(bs.getState() == Bundle.ACTIVE) {
	  try {
	    bs.stop();
	    bs.bDelayedStart = true; // Reset this since stop sets to false
	  } catch (Exception e) {
	    framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bs, e));
	  }
	}
      }
    }
  }

  
  public int getInitialBundleStartLevel() {
    return initStartLevel;
  }
  
  public void setInitialBundleStartLevel(int startLevel) {
	checkStartLevelAdminSystem();  
	  
    if(startLevel <= 0) {
      throw new IllegalArgumentException("Initial start level must be > 0, is " + startLevel);
    }
    initStartLevel = bCompat ? 1 : startLevel;
  }

  
  public boolean isBundlePersistentlyStarted(Bundle bundle) {
    return ((BundleImpl)bundle).isPersistent();
  }

 
  private void notifyFramework() {
    framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, framework.systemBundle, null));
  }
}


