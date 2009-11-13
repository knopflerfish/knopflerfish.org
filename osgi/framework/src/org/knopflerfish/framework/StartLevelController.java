/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
import org.osgi.service.startlevel.*;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;

import java.io.File;


/**
 * StartLevel service implementation.
 *
 */
public class StartLevelController
  implements Runnable, ServiceFactory
{
  Thread        wc;
  long          wcDelay  = 2000;
  boolean       bRun     = false;
  Queue         jobQueue = new Queue(100);

  final static int START_MIN = 0;
  final static int START_MAX = Integer.MAX_VALUE;

  final static String LEVEL_FILE = "currentlevel";
  final static String INITIAL_LEVEL_FILE = "initiallevel";

  int currentLevel     = 0;
  int initStartLevel   = 1;
  int targetStartLevel = currentLevel;
  boolean acceptChanges = true;

  FrameworkContext framework;

  FileTree storage;

  // Set to true indicates startlevel compatability mode.
  // all bundles and current start level will be 1
  boolean  bCompat /*= false*/;
  static final String COMPAT_PROP
    = "org.knopflerfish.framework.startlevel.compat";

  public static final String SPEC_VERSION = "1.1";


  StartLevelController(FrameworkContext framework)
  {
    this.framework = framework;
    bCompat = "true".equals(framework.props.getProperty(COMPAT_PROP, "false"));

    storage = Util.getFileStorage(framework, "startlevel");
  }

  void open() {
    if(framework.props.debug.startlevel) {
      framework.props.debug.println("startlevel: open");
    }

    if (jobQueue.isEmpty()) {
      int beginningLevel = 1;
      final String sBeginningLevel
        = framework.props.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
                                      "1");
      try {
        beginningLevel = Integer.parseInt(sBeginningLevel);
      } catch (NumberFormatException nfe) {
        framework.props.debug.printStackTrace
          ("Invalid number '" +sBeginningLevel +"' in value of property named '"
           +Constants.FRAMEWORK_BEGINNING_STARTLEVEL +"'.", nfe);
      }
      setStartLevel0(beginningLevel, false, false, true);
    }
    Runnable firstJob = (Runnable)jobQueue.firstElement();
    wc   = new Thread(this, "startlevel job thread");
    synchronized (firstJob) {
      bRun = true;
      wc.start();
      if (!acceptChanges) {
        acceptChanges = true;
        restoreState();
      }
      // Wait for first job to complete before return
      try {
        firstJob.wait();
      } catch (InterruptedException _ignore) { }
    }
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
    if (framework.props.debug.startlevel) {
      framework.props.debug.println("startlevel: restoreState");
    }
    // Skip level load in mem storage since bundle levels
    // isn't saved anyway
    if (!framework.props.bIsMemoryStorage) {
      try {
        String s = Util.getContent(new File(storage, LEVEL_FILE));
        if (s != null) {
          int oldStartLevel = Integer.parseInt(s);
          if (oldStartLevel != -1) {
            setStartLevel0(oldStartLevel, false, false, true);
          }
        }
      } catch (Exception _ignored) { }
      try {
        String s = Util.getContent(new File(storage, INITIAL_LEVEL_FILE));
        if (s != null) {
          setInitialBundleStartLevel0(Integer.parseInt(s), false);
        }
      } catch (Exception _ignored) { }
    }
  }


  void close() {
    if (framework.props.debug.startlevel) {
      framework.props.debug.println("*** closing startlevel service");
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

  void shutdown() {
    acceptChanges = false;
    setStartLevel0(0, false, true, false);
    while (currentLevel > 1) {
      synchronized (wc) {
        try { wc.wait(); } catch (Exception e) {}
      }
    }
    close();
  }

  public void run() {
    while(bRun) {
      try {
        Runnable job = (Runnable)jobQueue.removeWait((float)(wcDelay / 1000.0));
        if (job != null) {
          job.run();
          synchronized (job) {
            job.notify();
          }
        }
      } catch (Exception ignored) {
        ignored.printStackTrace();
      }
    }
  }


  int getStartLevel() {
    return currentLevel;
  }


  void setStartLevel(final int startLevel) {
    framework.perm.checkStartLevelAdminPerm();
    if(startLevel <= 0) {
      throw new IllegalArgumentException("Initial start level must be > 0, is " + startLevel);
    }
    if (acceptChanges) {
      setStartLevel0(startLevel, true, false, true);
    }
  }


  private void setStartLevel0(final int startLevel,
                              final boolean notifyFw,
                              final boolean notifyWC,
                              final boolean storeLevel)
  {
    if (framework.props.debug.startlevel) {
      framework.props.debug.println("startlevel: setStartLevel " + startLevel);
    }

    jobQueue.insert(new Runnable() {
      public void run() {
        int sl = bCompat ? 1 : startLevel;
        targetStartLevel = sl;

        while (targetStartLevel > currentLevel) {
          increaseStartLevel();
        }

        while (targetStartLevel < currentLevel) {
          decreaseStartLevel();
        }

        // Skip level save in mem storage since bundle levels
        // won't be saved anyway
        if (storeLevel && !framework.props.bIsMemoryStorage) {
          try {
            Util.putContent(new File(storage, LEVEL_FILE),
                            Integer.toString(currentLevel));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        if (notifyFw) {
          framework.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, framework.systemBundle, null));
        }
        if (notifyWC && wc != null) {
          synchronized (wc) {
            wc.notifyAll();
          }
        }
      }
    });
  }


  Object lock = new Object();


  void increaseStartLevel() {
    synchronized (lock) {

      currentLevel++;

      if (framework.props.debug.startlevel) {
        framework.props.debug.println("startlevel: increaseStartLevel currentLevel=" + currentLevel);
      }
      Vector set = new Vector();

      List bundles = framework.bundles.getBundles();

      for (Iterator i = bundles.iterator(); i.hasNext(); ) {
        BundleImpl bs  = (BundleImpl)i.next();
        if (canStart(bs)) {
          if (bs.getStartLevel() == currentLevel) {
            if (bs.archive.getAutostartSetting()!=-1) {
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
          if (bs.archive.getAutostartSetting()!=-1) {
            if (framework.props.debug.startlevel) {
              framework.props.debug.println("startlevel: start " + bs);
            }
            int startOptions = Bundle.START_TRANSIENT;
            if (isBundleActivationPolicyUsed(bs)) {
              startOptions |= Bundle.START_ACTIVATION_POLICY;
            }
            bs.start(startOptions);
          }
        } catch (Exception e) {
          framework.listeners.frameworkError(bs, e);
        }
      }
    }
  }


  void decreaseStartLevel() {
    synchronized (lock) {
      currentLevel--;

      Vector set = new Vector();

      List bundles = framework.bundles.getBundles();

      for (Iterator i = bundles.iterator(); i.hasNext(); ) {
        BundleImpl bs  = (BundleImpl)i.next();

        if (bs.getState() == Bundle.ACTIVE ||
            (bs.getState() == Bundle.STARTING && bs.lazyActivation)) {
          if (bs.getStartLevel() == currentLevel + 1) {
            set.addElement(bs);
          }
        }
      }

      Util.sort(set, BSComparator, true);

      synchronized (framework.packages) {
        for (int i = 0; i < set.size(); i++) {
          BundleImpl bs = (BundleImpl)set.elementAt(i);
          if (bs.getState() == Bundle.ACTIVE ||
              (bs.getState() == Bundle.STARTING && bs.lazyActivation)) {
            if (framework.props.debug.startlevel) {
              framework.props.debug.println("startlevel: stop " + bs);
            }

            try {
              bs.stop(Bundle.STOP_TRANSIENT);
            } catch (Throwable t) {
              framework.listeners.frameworkError(bs, t);
            }
          }
        }
      }
    }
  }


  boolean canStart(BundleImpl b) {
    return b.getState() != Bundle.UNINSTALLED;
  }


  static final Util.Comparator BSComparator = new Util.Comparator() {
    public int compare(Object o1, Object o2) {
      BundleImpl b1 = (BundleImpl)o1;
      BundleImpl b2 = (BundleImpl)o2;

      int res = b1.getStartLevel() - b2.getStartLevel();
      if (res == 0) {
        res = (int)(b1.getBundleId() - b2.getBundleId());
      }
      return res;
    }
  };


  int getBundleStartLevel(Bundle bundle) {
    if(bundle.getBundleId() == 0) {
      return 0;
    }
    BundleImpl bs = (BundleImpl)bundle;
    return bs.getStartLevel();
  }


  void setBundleStartLevel(Bundle bundle, final int startLevel) {
    framework.perm.checkExecuteAdminPerm(bundle);

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
      }
    });
  }


  void syncStartLevel(BundleImpl bs) {
    try {
      synchronized(lock) {
        synchronized (framework.packages) {
          if (bs.getStartLevel() <= currentLevel) {
            if ( (bs.getState() == Bundle.INSTALLED
                  || bs.getState() == Bundle.RESOLVED)
                 && bs.archive.getAutostartSetting()!=-1) {
              if (framework.props.debug.startlevel) {
                framework.props.debug.println("startlevel: start " + bs);
              }
              int startOptions = Bundle.START_TRANSIENT;
              if (isBundleActivationPolicyUsed(bs)) {
                startOptions |= Bundle.START_ACTIVATION_POLICY;
              }
              bs.start(startOptions);
            }
          } else if (bs.getStartLevel() > currentLevel) {
            if (bs.getState() == Bundle.ACTIVE ||
                (bs.getState() == Bundle.STARTING && bs.lazyActivation)) {
              if (framework.props.debug.startlevel) {
                framework.props.debug.println("startlevel: stop " + bs);
              }
              bs.stop(Bundle.STOP_TRANSIENT);
            }
          }
        }
      }
    } catch (Throwable t) {
      framework.listeners.frameworkError(bs, t);
    }
  }


  int getInitialBundleStartLevel() {
    return initStartLevel;
  }


  void setInitialBundleStartLevel(int startLevel) {
    framework.perm.checkStartLevelAdminPerm();
    setInitialBundleStartLevel0(startLevel, true);
  }

  private void setInitialBundleStartLevel0(int startLevel, boolean save) {
    if(startLevel <= 0) {
      throw new IllegalArgumentException("Initial start level must be > 0, is " + startLevel);
    }
    initStartLevel = bCompat ? 1 : startLevel;
    if (!framework.props.bIsMemoryStorage && save) {
      try {
        Util.putContent(new File(storage, INITIAL_LEVEL_FILE),
                        Integer.toString(initStartLevel));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  boolean isBundlePersistentlyStarted(Bundle bundle) {
    return ((BundleImpl)bundle).archive.getAutostartSetting() != -1;
  }


  boolean isBundleActivationPolicyUsed(BundleImpl bundle) {
    return bundle.archive.getAutostartSetting() == Bundle.START_ACTIVATION_POLICY;
  }


  public Object getService(Bundle bundle,
                           ServiceRegistration registration)
  {
    return new StartLevelImpl(this, bundle);
  }

  public void ungetService(Bundle bundle,
                           ServiceRegistration registration,
                           Object service)
  {
  }

  static class StartLevelImpl
    implements StartLevel
  {
    private StartLevelController st;
    private Bundle bundle;

    StartLevelImpl(StartLevelController st, Bundle bundle) {
      this.bundle = bundle;
      this.st = st;
    }

    public int getBundleStartLevel(Bundle bundle) {
      return st.getBundleStartLevel(bundle);
    }

    public int getInitialBundleStartLevel() {
      return st.getInitialBundleStartLevel();
    }

    public int getStartLevel() {
      return st.getStartLevel();
    }

    public boolean isBundleActivationPolicyUsed(Bundle bundle) {
      return st.isBundleActivationPolicyUsed((BundleImpl) bundle);
    }

    public boolean isBundlePersistentlyStarted(Bundle bundle) {
      return st.isBundlePersistentlyStarted(bundle);
    }

    public void setBundleStartLevel(Bundle bundle, int startlevel) {
      st.setBundleStartLevel(bundle, startlevel);
    }

    public void setInitialBundleStartLevel(int startlevel) {
      st.setInitialBundleStartLevel(startlevel);
    }

    public void setStartLevel(int startlevel) {
      st.setStartLevel(startlevel);
    }
  }
}
