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

package org.knopflerfish.bundle.desktop.swing;

import java.awt.Image;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Modified version of sample code: apple.dts.samplecode.osxadapter.OSXAdapter

public class OSXAdapter implements InvocationHandler {

  // The set of handlers registered by this OSXAdapter, they will all
  // be unreigsted by clearApplicationListeners() and removed form the set.
  static private final List/*<com.apple.eawt.ApplicationListener>*/
    registeredApplicationListeners = new ArrayList();

  protected Object targetObject;
  protected Method targetMethod;
  protected String proxySignature;

  static Object macOSXApplication;

  public static boolean isMacOSX()
  {
    return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  }

  // Pass this method an Object and Method equipped to perform
  // application shutdown logic The method passed should return a
  // boolean stating whether or not the quit should occur
  public static void setQuitHandler(Object target, Method quitHandler)
  {
    Activator.log.debug("Installing OS X quit-handler.");
    setHandler(new OSXAdapter("handleQuit", target, quitHandler));
  }

  // Pass this method an Object and Method equipped to display
  // application info They will be called when the About menu item
  // is selected from the application menu
  public static void setAboutHandler(Object target, Method aboutHandler)
  {
    final boolean enableAboutMenu = (target != null && aboutHandler != null);
    if (enableAboutMenu) {
      Activator.log.debug("Installing OS X about-handler.");
      setHandler(new OSXAdapter("handleAbout", target, aboutHandler));
    }
    // If we're setting a handler, enable the About menu item by calling
    // com.apple.eawt.Application reflectively
    try {
      final Method enableAboutMethod = macOSXApplication.getClass()
        .getDeclaredMethod("setEnabledAboutMenu",
                           new Class[]{boolean.class});
      enableAboutMethod.invoke(macOSXApplication,
                               new Object[]{ Boolean.valueOf(enableAboutMenu)});
      Activator.log.debug("OS X About-handler activated.");
    } catch (Exception ex) {
      Activator.log.error("OSXAdapter could not access the About Menu", ex);
    }
  }

  // Pass this method an Object and a Method equipped to display
  // application options They will be called when the Preferences menu
  // item is selected from the application menu
  public static void setPreferencesHandler(Object target, Method prefsHandler)
  {
    boolean enablePrefsMenu = (target != null && prefsHandler != null);
    if (enablePrefsMenu) {
      Activator.log.debug("Installing OS X preferences-handler.");
      setHandler(new OSXAdapter("handlePreferences", target, prefsHandler));
    }
    // If we're setting a handler, enable the Preferences menu item by calling
    // com.apple.eawt.Application reflectively
    try {
      Method enablePrefsMethod
        = macOSXApplication.getClass()
        .getDeclaredMethod("setEnabledPreferencesMenu",
                           new Class[] { boolean.class });
      enablePrefsMethod.invoke(macOSXApplication,
                               new Object[]{Boolean.valueOf(enablePrefsMenu)});
      Activator.log.debug("OS X preferences-handler activated.");
    } catch (Exception ex) {
      Activator.log.error("OSXAdapter could not access the About Menu", ex);
    }
  }

  // Pass this method an Object and a Method equipped to handle
  // document events from the Finder Documents are registered with the
  // Finder via the CFBundleDocumentTypes dictionary in the
  // application bundle's Info.plist
  public static void setFileHandler(Object target, Method fileHandler)
  {
    Activator.log.debug("Installing OS X file-handler.");
    setHandler(new OSXAdapter("handleOpenFile", target, fileHandler)
      {
        // Override OSXAdapter.callTarget to send information on the
        // file to be opened
        public boolean callTarget(Object appleEvent) {
          if (appleEvent != null) {
            try {
              final Method getFilenameMethod
                = appleEvent.getClass().getDeclaredMethod("getFilename",
                                                          (Class[])null);
              final String filename = (String) getFilenameMethod
                .invoke(appleEvent, (Object[])null);
              this.targetMethod.invoke(this.targetObject,
                                       new Object[]{filename});
            } catch (Exception ex) {
            }
          }
          return true;
        }
      });
  }

  // setHandler creates a Proxy object from the passed OSXAdapter and
  // adds it as an ApplicationListener
  public static void setHandler(final OSXAdapter adapter)
  {
    try {
      final Class applicationClass
        = Class.forName("com.apple.eawt.Application");
      if (macOSXApplication == null) {
        macOSXApplication = applicationClass
          .getConstructor((Class[])null).newInstance((Object[])null);
      }
      final Class applicationListenerClass
        = Class.forName("com.apple.eawt.ApplicationListener");
      final Method addListenerMethod = applicationClass
        .getDeclaredMethod("addApplicationListener",
                           new Class[]{applicationListenerClass});
      // Create a proxy object around this handler that can be
      // reflectively added as an Apple ApplicationListener
      final Object osxAdapterProxy = Proxy
        .newProxyInstance(OSXAdapter.class.getClassLoader(),
                          new Class[] { applicationListenerClass },
                          adapter);
      addListenerMethod.invoke(macOSXApplication,
                               new Object[] { osxAdapterProxy });
      synchronized(registeredApplicationListeners) {
        registeredApplicationListeners.add(osxAdapterProxy);
      }
    } catch (ClassNotFoundException cnfe) {
      Activator.log.warn("This version of Mac OS X does not support the "
                         +"Apple EAWT.  ApplicationEvent handling has been "
                         +"disabled (" + cnfe + ")", cnfe);
    } catch (Exception ex) {
      // Likely a NoSuchMethodException or an IllegalAccessException
      // loading/invoking eawt.Application methods
      Activator.log.error("Mac OS X Adapter could not talk to EAWT: " +ex, ex);
    }
  }

  // clearApplicationListeners() unregisters all ApplicationListener
  // Proxy objects that was registered by setHandler(OSXAdapter).
  public static void clearApplicationListeners()
  {
    try {
      final Class applicationClass
        = Class.forName("com.apple.eawt.Application");
      if (macOSXApplication == null) {
        macOSXApplication = applicationClass
          .getConstructor((Class[])null).newInstance((Object[])null);
      }
      final Class applicationListenerClass
        = Class.forName("com.apple.eawt.ApplicationListener");
      final Method removeListenerMethod = applicationClass
        .getDeclaredMethod("removeApplicationListener",
                           new Class[]{applicationListenerClass});
      // Remove all the listeners in that we have registered.
      synchronized(registeredApplicationListeners) {
        for (int i = 0; i<registeredApplicationListeners.size(); i++ ) {
          final Object appListener = registeredApplicationListeners.get(i);
          Activator.log.debug("Removing eawt application listener: "
                              +appListener);
          removeListenerMethod.invoke(macOSXApplication,
                                      new Object[]{appListener});
        }
        registeredApplicationListeners.clear();
      }
    } catch (ClassNotFoundException cnfe) {
      Activator.log.warn("This version of Mac OS X does not support the "
                         +"Apple EAWT.  ApplicationEvent handling has been "
                         +"disabled (" + cnfe + ")", cnfe);
    } catch (Exception ex) {
      // Likely a NoSuchMethodException or an IllegalAccessException
      // loading/invoking eawt.Application methods
      Activator.log.error("Mac OS X Adapter could not talk to EAWT: " +ex, ex);
    }
  }


  // This method requires Java 6 or higher.
  public static void setDockIconImage(Image image)
  {
    if (null==image) return;

    // Set the dock icon image reflectively via com.apple.eawt.Application
    try {
      final Class applicationClass
        = Class.forName("com.apple.eawt.Application");
      if (macOSXApplication == null) {
        macOSXApplication = applicationClass
          .getConstructor((Class[])null).newInstance((Object[])null);
      }

      try {
        final Method setDockIconImageMethod = macOSXApplication.getClass()
          .getDeclaredMethod("setDockIconImage", new Class[]{Image.class});
        setDockIconImageMethod.invoke(macOSXApplication, new Object[]{image});
        Activator.log.debug("OS X doc icon image set.");
      } catch (NoSuchMethodException nsme) {
        Activator.log.info("OSXAdapter: setDockIconImage(Image) not provided",
                           nsme);
      }
    } catch (Exception ex) {
      Activator.log.warn("OSXAdapter could not set the doc icon image", ex);
    }
  }



  // Each OSXAdapter has the name of the EAWT method it intends to
  // listen for (handleAbout, for example), the Object that will
  // ultimately perform the task, and the Method to be called on that
  // Object
  protected OSXAdapter(String proxySignature, Object target, Method handler)
  {
    this.proxySignature = proxySignature;
    this.targetObject = target;
    this.targetMethod = handler;
  }

  // Override this method to perform any operations on the event that
  // comes with the various callbacks See setFileHandler above for an
  // example
  public boolean callTarget(Object appleEvent)
    throws InvocationTargetException, IllegalAccessException
  {
    final Object result = targetMethod.invoke(targetObject, (Object[])null);
    if (result == null) {
      return true;
    }
    return Boolean.valueOf(result.toString()).booleanValue();
  }

  // InvocationHandler implementation
  // This is the entry point for our proxy object; it is called every
  // time an ApplicationListener method is invoked
  public Object invoke (Object proxy, Method method, Object[] args)
    throws Throwable
  {
    if (isCorrectMethod(method, args)) {
      final boolean handled = callTarget(args[0]);
      setApplicationEventHandled(args[0], handled);
    } else {
      // Must always handle equals, hashCode and toString.
      final String name = method.getName();
      final int argLen  = null==args ? 0 : args.length;
      if ("equals".equals(name) && 1==argLen) {
        // Proxy with same InvocationHandler (i.e., OSXAdapter)?
        final Object other = args[0];
        if (Proxy.isProxyClass(other.getClass())) {
          final InvocationHandler oih = Proxy.getInvocationHandler(other);
          return this.equals(oih) ? Boolean.TRUE : Boolean.FALSE;
        }
        return Boolean.FALSE;
      } else if ("hashCode".equals(name) && 0==argLen) {
        // delegate to OSXAdapter
        return new Integer(this.hashCode());
      } else if ("toString".equals(name) && 0==argLen) {
        return this.toString();
      }
    }
    // All of the ApplicationListener methods are void; return null
    // regardless of what happens
    return null;
  }

  // Compare the method that was called to the intended method when
  // the OSXAdapter instance was created (e.g. handleAbout,
  // handleQuit, handleOpenFile, etc.)
  protected boolean isCorrectMethod(Method method, Object[] args)
  {
    return (targetMethod != null
            && proxySignature.equals(method.getName())
            && args.length == 1);
  }

  // It is important to mark the ApplicationEvent as handled and
  // cancel the default behavior This method checks for a boolean
  // result from the proxy method and sets the event accordingly
  protected void setApplicationEventHandled(Object event, boolean handled)
  {
    if (event != null) {
      try {
        final Method setHandledMethod = event.getClass()
          .getDeclaredMethod("setHandled", new Class[]{boolean.class});
        // If the target method returns a boolean, use that as a hint
        setHandledMethod.invoke(event, new Object[]{Boolean.valueOf(handled)});
      } catch (Exception ex) {
        Activator.log.error("OSXAdapter was unable to handle an "
                            +"ApplicationEvent: " + event, ex);
      }
    }
  }
}