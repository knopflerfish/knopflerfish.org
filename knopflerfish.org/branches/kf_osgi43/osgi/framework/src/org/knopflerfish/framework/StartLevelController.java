/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.startlevel.StartLevel;


/**
 * StartLevel service implementation.
 *
 */
public class StartLevelController
  implements Runnable, ServiceFactory<StartLevel>
{
  // The version of the StartLevel service API
  public static final String SPEC_VERSION = "1.1";

  // The version of the StartLevel API
  public static final String API_SPEC_VERSION = "1.0";

  final static int START_MIN = 0;
  final static int START_MAX = Integer.MAX_VALUE;

  final static String LEVEL_FILE = "currentlevel";
  final static String INITIAL_LEVEL_FILE = "initiallevel";

  Thread        wc;
  long          wcDelay  = 2000;
  boolean       bRun     = false;
  Queue         jobQueue = new Queue(100);

  int currentLevel     = 0;
  int initStartLevel   = 1;
  int targetStartLevel = currentLevel;
  boolean acceptChanges = true;

  final FrameworkContext fwCtx;

  FileTree storage;

  // Set to true indicates startlevel compatibility mode.
  // all bundles and current start level will be 1
  boolean  bCompat /*= false*/;


  StartLevelController(FrameworkContext fwCtx)
  {
    this.fwCtx = fwCtx;
    bCompat = fwCtx.props.getBooleanProperty(FWProps.STARTLEVEL_COMPAT_PROP);

    storage = Util.getFileStorage(fwCtx, "startlevel");
  }

  void open() {
    if (fwCtx.debug.startlevel) {
      fwCtx.debug.println("startlevel: open");
    }

    final Runnable lastJob = (Runnable)jobQueue.lastElement();
    wc = new Thread(fwCtx.threadGroup, this, "startlevel job");
    synchronized (lastJob) {
      bRun = true;
      wc.start();
      if (!acceptChanges) {
        acceptChanges = true;
        restoreState();
      }
      // Wait for the last of the jobs scheduled before starting the
      // framework to complete before return
      try {
        lastJob.wait();
      } catch (InterruptedException _ignore) { }
    }
  }

  /**
   * Load persistent state from storage and set up all actions
   * necessary to bump bundle states. If no persistent state was found,
   * try to set the target start level from the beginning start level
   * framework property.
   *
   * <p>Note that {@link open()} needs to be called for any work to
   * be done.</p>
   */
  void restoreState() {
    if (fwCtx.debug.startlevel) {
      fwCtx.debug.println("startlevel: restoreState");
    }
    if (storage != null) {
      // Set the target start level to go to when open() is called.
      int startLevel = -1;
      try {
        final String s = Util.getContent(new File(storage, LEVEL_FILE));
        if (s != null) {
          startLevel = Integer.parseInt(s);
          if (fwCtx.debug.startlevel) {
            fwCtx.debug.println("startlevel: restored level " + startLevel);
          }
        }
      } catch (Exception _ignored) { }
      if (startLevel == -1) {
        // No stored start level to restore, try the beginning start level
        final String sBeginningLevel
          = fwCtx.props.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
        try {
          startLevel = Integer.parseInt(sBeginningLevel);
          if (fwCtx.debug.startlevel) {
            fwCtx.debug.println("startlevel: beginning level " + startLevel);
          }
        } catch (NumberFormatException nfe) {
          fwCtx.debug.printStackTrace("Invalid number '" + sBeginningLevel +
                                      "' in value of property named '"
                                      + Constants.FRAMEWORK_BEGINNING_STARTLEVEL
                                      + "'.", nfe);
        }
      }
      if (startLevel<0) {
        startLevel = 1;
      }
      setStartLevel0(startLevel, false, false, true);

      // Restore the initial bundle start level
      try {
        String s = Util.getContent(new File(storage, INITIAL_LEVEL_FILE));
        if (s != null) {
          setInitialBundleStartLevel0(Integer.parseInt(s), false);
        }
      } catch (Exception _ignored) { }
    }
  }


  void close() {
    if (fwCtx.debug.startlevel) {
      fwCtx.debug.println("*** closing startlevel service");
    }

    bRun = false;
    jobQueue.insert(new Runnable() {
        public void run() {
          jobQueue.close();
        }
      });
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
    synchronized (wc) {
      setStartLevel0(0, false, true, false);
      while (currentLevel > 0) {
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

  void setStartLevel(final int startLevel)
  {
    setStartLevel(startLevel, (FrameworkListener[]) null);
  }

  void setStartLevel(final int startLevel, final FrameworkListener... listeners)
  {
    fwCtx.perm.checkStartLevelAdminPerm();
    if (startLevel <= 0) {
      throw new IllegalArgumentException("Initial start level must be > 0, is "
                                         + startLevel);
    }
    if (acceptChanges) {
      setStartLevel0(startLevel, true, false, true, listeners);
    }
  }

  private void setStartLevel0(final int startLevel,
                              final boolean notifyFw,
                              final boolean notifyWC,
                              final boolean storeLevel,
                              final FrameworkListener... listeners)
  {
    if (fwCtx.debug.startlevel) {
      fwCtx.debug.println("startlevel: setStartLevel " + startLevel);
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
        if (storeLevel && storage != null) {
          try {
            Util.putContent(new File(storage, LEVEL_FILE),
                            Integer.toString(currentLevel));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        if (notifyFw) {
          final FrameworkEvent event
            = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
                                 fwCtx.systemBundle, null);
          // Send event to all registered framework listeners
          fwCtx.listeners.frameworkEvent(event);
          // Send event to one-time listeners for this particular operation.
          if (null!=listeners) {
            for (FrameworkListener listener : listeners) {
              listener.frameworkEvent(event);
            }
          }
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

      if (fwCtx.debug.startlevel) {
        fwCtx.debug.println("startlevel: increaseStartLevel currentLevel="
                            + currentLevel);
      }
      Vector<BundleImpl> set = new Vector<BundleImpl>();

      List<BundleImpl> bundles = fwCtx.bundles.getBundles();

      for (Iterator<BundleImpl> i = bundles.iterator(); i.hasNext(); ) {
        BundleImpl bs  = i.next();
        if (canStart(bs)) {
          if (bs.getStartLevel() == currentLevel) {
            if (bs.gen.archive.getAutostartSetting()!=-1) {
              set.addElement(bs);
            }
          }
        }
      }

      Util.sort(set, BSComparator, false);

      for (int i = 0; i < set.size(); i++) {
        BundleImpl bs = set.elementAt(i);
        try {
          if (bs.gen.archive.getAutostartSetting()!=-1) {
            if (fwCtx.debug.startlevel) {
              fwCtx.debug.println("startlevel: start " + bs);
            }
            int startOptions = Bundle.START_TRANSIENT;
            if (isBundleActivationPolicyUsed(bs.gen.archive)) {
              startOptions |= Bundle.START_ACTIVATION_POLICY;
            }
            bs.start(startOptions);
          }
        } catch (IllegalStateException ignore) {
          // Tried to start an uninstalled bundle, skip
        } catch (Exception e) {
          fwCtx.listeners.frameworkError(bs, e);
        }
      }
    }
  }


  void decreaseStartLevel() {
    synchronized (lock) {
      currentLevel--;

      Vector<BundleImpl> set = new Vector<BundleImpl>();

      List<BundleImpl> bundles = fwCtx.bundles.getBundles();

      for (Iterator<BundleImpl> i = bundles.iterator(); i.hasNext(); ) {
        BundleImpl bs  = i.next();

        if (bs.getState() == Bundle.ACTIVE ||
            (bs.getState() == Bundle.STARTING && bs.gen.lazyActivation)) {
          if (bs.getStartLevel() == currentLevel + 1) {
            set.addElement(bs);
          }
        }
      }

      Util.sort(set, BSComparator, true);

      synchronized (fwCtx.packages) {
        for (int i = 0; i < set.size(); i++) {
          BundleImpl bs = set.elementAt(i);
          if (bs.getState() == Bundle.ACTIVE ||
              (bs.getState() == Bundle.STARTING && bs.gen.lazyActivation)) {
            if (fwCtx.debug.startlevel) {
              fwCtx.debug.println("startlevel: stop " + bs);
            }

            try {
              bs.stop(Bundle.STOP_TRANSIENT);
            } catch (Throwable t) {
              fwCtx.listeners.frameworkError(bs, t);
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


  int getBundleStartLevel(BundleImpl bundle) {
    if (bundle.getBundleId() == 0) {
      return 0;
    }
    return bundle.getStartLevel();
  }


  void setBundleStartLevel(final BundleImpl bundle, final int startLevel) {
    fwCtx.perm.checkExecuteAdminPerm(bundle);

    if (startLevel <= 0) {
      throw new IllegalArgumentException("Initial start level must be > 0, is " + startLevel);
    }

    if (bundle.getBundleId() == 0) {
      throw new IllegalArgumentException("System bundle start level cannot be changed");
    }

    fwCtx.perm.callSetStartLevel(bundle, bCompat ? 1 : startLevel);

    jobQueue.insert(new Runnable() {
      public void run() {
        syncStartLevel(bundle);
      }
    });
  }


  void syncStartLevel(BundleImpl bs) {
    try {
      if (fwCtx.debug.startlevel) {
        fwCtx.debug.println("syncstartlevel: " + bs);
      }
      synchronized(lock) {
        synchronized (fwCtx.packages) {
          if (bs.getStartLevel() <= currentLevel) {
            if ((bs.getState() & (Bundle.INSTALLED|Bundle.RESOLVED|Bundle.STOPPING)) != 0
                && bs.gen.archive.getAutostartSetting()!=-1) {
              if (fwCtx.debug.startlevel) {
                fwCtx.debug.println("startlevel: start " + bs);
              }
              int startOptions = Bundle.START_TRANSIENT;
              if (isBundleActivationPolicyUsed(bs.gen.archive)) {
                startOptions |= Bundle.START_ACTIVATION_POLICY;
              }
              bs.start(startOptions);
            }
          } else if (bs.getStartLevel() > currentLevel) {
            if ((bs.getState() & (Bundle.ACTIVE|Bundle.STARTING)) != 0) {
              if (fwCtx.debug.startlevel) {
                fwCtx.debug.println("startlevel: stop " + bs);
              }
              bs.stop(Bundle.STOP_TRANSIENT);
            }
          }
        }
      }
    } catch (Throwable t) {
      fwCtx.listeners.frameworkError(bs, t);
    }
  }


  int getInitialBundleStartLevel() {
    return initStartLevel;
  }


  void setInitialBundleStartLevel(int startLevel) {
    fwCtx.perm.checkStartLevelAdminPerm();
    fwCtx.perm.callSetInitialBundleStartLevel0(this, startLevel);
  }


  void setInitialBundleStartLevel0(int startLevel, boolean save) {
    if(startLevel <= 0) {
      throw new IllegalArgumentException("Initial start level must be > 0, is " + startLevel);
    }
    initStartLevel = bCompat ? 1 : startLevel;
    if (storage != null && save) {
      try {
        Util.putContent(new File(storage, INITIAL_LEVEL_FILE),
                        Integer.toString(initStartLevel));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  boolean isBundlePersistentlyStarted(BundleArchive archive) {
    return archive == null || archive.getAutostartSetting() != -1;
  }


  boolean isBundleActivationPolicyUsed(BundleArchive archive) {
    return archive != null && archive.getAutostartSetting() == Bundle.START_ACTIVATION_POLICY;
  }


  public StartLevel getService(Bundle bundle,
                               ServiceRegistration<StartLevel> registration)
  {
    return new StartLevelImpl(this);
  }

  public void ungetService(Bundle bundle,
                           ServiceRegistration<StartLevel> registration,
                           StartLevel service)
  {
  }

  static class StartLevelImpl
    implements StartLevel
  {
    private StartLevelController st;

    StartLevelImpl(StartLevelController st) {
      this.st = st;
    }

    public int getBundleStartLevel(Bundle bundle) {
      return st.getBundleStartLevel(checkBundle(bundle));
    }

    public int getInitialBundleStartLevel() {
      return st.getInitialBundleStartLevel();
    }

    public int getStartLevel() {
      return st.getStartLevel();
    }

    public boolean isBundleActivationPolicyUsed(Bundle bundle) {
      return st.isBundleActivationPolicyUsed(getBundleArchive(bundle));
    }

    public boolean isBundlePersistentlyStarted(Bundle bundle) {
      return st.isBundlePersistentlyStarted(getBundleArchive(bundle));
    }

    public void setBundleStartLevel(Bundle bundle, int startlevel) {
      st.setBundleStartLevel(checkBundle(bundle), startlevel);
    }

    public void setInitialBundleStartLevel(int startlevel) {
      st.setInitialBundleStartLevel(startlevel);
    }

    public void setStartLevel(int startlevel) {
      st.setStartLevel(startlevel);
    }

    private BundleImpl checkBundle(Bundle b) {
      if (b instanceof BundleImpl) {
        BundleImpl res = (BundleImpl)b;
        if (res.fwCtx == st.fwCtx) {
          if (res.state != Bundle.UNINSTALLED) {
            return res;
          }
          throw new IllegalArgumentException("Bundle is in UNINSTALLED state");
        }
      }
      throw new IllegalArgumentException("Bundle doesn't belong to the same framework as the StartLevel service");
    }

    private BundleArchive getBundleArchive(Bundle b) {
      BundleImpl bi = checkBundle(b);
      BundleArchive res = bi.gen.archive;
      if (res == null && bi.id != 0) {
        throw new IllegalArgumentException("Bundle is in UNINSTALLED state");
      }
      return res;
    }

  }

  BundleStartLevel bundleStartLevel(final BundleImpl bi) {
    return new BundleStartLevelImpl(this, bi);
  }

  static class BundleStartLevelImpl
    implements BundleStartLevel
  {
    final StartLevelController st;
    final BundleImpl bi;

    BundleStartLevelImpl(final StartLevelController st, final BundleImpl bi) {
      this.st = st;
      this.bi = bi;
    }

    public Bundle getBundle()
    {
      return bi;
    }

    public int getStartLevel()
    {
      return st.getBundleStartLevel(bi);
    }

    public void setStartLevel(int startlevel)
    {
      st.setBundleStartLevel(bi, startlevel);
    }

    public boolean isPersistentlyStarted()
    {
      return st.isBundlePersistentlyStarted(getBundleArchive());
    }

    public boolean isActivationPolicyUsed()
    {
      return st.isBundleActivationPolicyUsed(getBundleArchive());
    }

    private BundleArchive getBundleArchive() {
      BundleArchive res = bi.gen.archive;
      if (res == null && bi.id != 0) {
        throw new IllegalArgumentException("Bundle is in UNINSTALLED state");
      }
      return res;
    }

  }

  FrameworkStartLevel frameworkStartLevel(final BundleImpl bi)
  {
    return new FrameworkStartLevelImpl(this, bi);
  }

  static class FrameworkStartLevelImpl
    implements FrameworkStartLevel
  {
    final StartLevelController st;
    final BundleImpl bi;

    public FrameworkStartLevelImpl(StartLevelController startLevelController,
        BundleImpl bi)
    {
      this.st = startLevelController;
      this.bi = bi;
    }

    public Bundle getBundle()
    {
      return bi;
    }

    public int getStartLevel()
    {
      return st.getStartLevel();
    }

    public void setStartLevel(int startlevel, FrameworkListener... listeners)
    {
      st.setStartLevel(startlevel, listeners);
    }

    public int getInitialBundleStartLevel()
    {
      return st.getInitialBundleStartLevel();
    }

    public void setInitialBundleStartLevel(int startlevel)
    {
      st.setInitialBundleStartLevel(startlevel);
    }
  }
}
