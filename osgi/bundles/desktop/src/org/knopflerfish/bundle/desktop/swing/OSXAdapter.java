/*
 * Copyright (c) 2003-2020, KNOPFLERFISH project
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


// Modified version of sample code: apple.dts.samplecode.osxadapter.OSXAdapter
/**
 * The implementation uses the new EAWT API introduced in Java for Mac OS X 10.6
 * Update 3 and in Java for Mac OS X 10.5 Update 8. This API is also provided by
 * Oracles JavaSE 7 and onwards.
 *
 * @author ekolin
 *
 */
public class OSXAdapter implements InvocationHandler {

  /**
   * The registered quit-handler to be unregistered when
   * {@link #clearApplicationHandlers()} is called.
   */
  static private Object registeredQuitHandler = null;

  /**
   * The registered about-handler to be unregistered when
   * {@link #clearApplicationHandlers()} is called.
   */
  static private Object registeredAboutHandler = null;

  /**
   * The registered preferences-handler to be unregistered when
   * {@link #clearApplicationHandlers()} is called.
   */
  static private Object registeredPreferencesHandler = null;

  protected Object targetObject;
  protected Method targetMethod;
  protected String proxySignature;

  public static boolean isMacOSX()
  {
    return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  }

  static Object macOSXApplication;

  /**
   * Initialize the {@code macOSXApplication}-object. Must always be called
   * before using the{@code macOSXApplication}-object.
   *
   * @return the current {@code macOSXApplication}-object
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws NoSuchMethodException
   */
  static Object getApplication()
      throws ClassNotFoundException, InstantiationException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    final Class<?> applicationClass
      = Class.forName("com.apple.eawt.Application");
    if (macOSXApplication == null) {
      macOSXApplication = applicationClass
        .getConstructor((Class[])null).newInstance((Object[])null);
    }
    return macOSXApplication;
  }


  /**
   * Set up an object and a method to call on it when the Application receives a
   * quit request. The installed application quit handler will always cancel the
   * quit request so it is up to the quitHandler-method on the target object to
   * shut-down the JRE by calling {@link System#exit} if it decides that it is
   * time to quit.
   *
   * @param target
   *          target object to call the quitHandler method on.
   * @param quitHandler
   *          method to call when requested to quit.
   */
  public static void setQuitHandler(Object target, Method quitHandler)
  {
    Activator.log.debug("Installing OS X quit-handler.");

    // Create and register a QuiteHandler.
    try {
      final OSXAdapter adapter =
          new OSXAdapter("handleQuitRequestWith", target, quitHandler);
      final Class<?> quitHandlerClass =
        Class.forName("com.apple.eawt.QuitHandler");

      // Create a proxy object around this handler that can be
      // reflectively added as an Apple QuiteHandler
      final Object osxAdapterProxy =
        Proxy.newProxyInstance(OSXAdapter.class.getClassLoader(),
                               new Class[] { quitHandlerClass }, adapter);
      registerQuitHandler(osxAdapterProxy);
    } catch (final ClassNotFoundException cnfe) {
      Activator.log.warn("This version of Mac OS X does not support the "
                         + "Apple EAWT.  Application quit handling has been "
                         + "disabled (" + cnfe + ")", cnfe);
    } catch (final Exception ex) {
      // Likely a NoSuchMethodException or an IllegalAccessException
      // loading/invoking eawt.Application methods
      Activator.log.error("Mac OS X Adapter could not talk to EAWT: " + ex, ex);
    }
  }

  /**
   * Set up an object and a method to call on it when the Application receives an
   * about request.
   *
   * @param target
   *          target object to call the aboutHandler method on.
   * @param aboutHandler
   *          method to call when requested to quit.
   */
  public static void setAboutHandler(Object target, Method aboutHandler)
  {
    final boolean enableAboutMenu = (target != null && aboutHandler != null);

    // Create and register an AboutHandler.
    Activator.log.debug("Installing OS X about-handler.");
    try {
      Object osxAdapterProxy = null;
      if (enableAboutMenu) {
        final OSXAdapter adapter =
          new OSXAdapter("handleAbout", target, aboutHandler);
        final Class<?> aboutHandlerClass =
          Class.forName("com.apple.eawt.AboutHandler");

        // Create a proxy object around this handler that can be
        // reflectively added as an Apple AboutHandler
        osxAdapterProxy =
          Proxy.newProxyInstance(OSXAdapter.class.getClassLoader(),
                                 new Class[] { aboutHandlerClass }, adapter);
      }
      registerAboutHandler(osxAdapterProxy);
    } catch (final ClassNotFoundException cnfe) {
      Activator.log.warn("This version of Mac OS X does not support the "
                         + "Apple EAWT.  Application about handling has been "
                         + "disabled (" + cnfe + ")", cnfe);
    } catch (final Exception ex) {
      // Likely a NoSuchMethodException or an IllegalAccessException
      // loading/invoking eawt.Application methods
      Activator.log.error("Mac OS X Adapter could not talk to EAWT: " + ex, ex);
    }
  }

  /**
   * Set up an object and a method to call on it when the Application receives a
   * preferences request.
   *
   * @param target
   *          target object to call the preferencesHandler method on.
   * @param prefsHandler
   *          method to call when requested to quit.
   */
  public static void setPreferencesHandler(Object target, Method prefsHandler)
  {
    final boolean enableAboutMenu = (target != null && prefsHandler != null);

    // Create and register an AboutHandler.
    Activator.log.debug("Installing OS X preferences-handler.");
    try {
      Object osxAdapterProxy = null;
      if (enableAboutMenu) {
        final OSXAdapter adapter =
          new OSXAdapter("handlePreferences", target, prefsHandler);
        final Class<?> preferencesHandlerClass =
          Class.forName("com.apple.eawt.PreferencesHandler");

        // Create a proxy object around this handler that can be
        // reflectively added as an Apple PreferencesHandler
        osxAdapterProxy =
          Proxy.newProxyInstance(OSXAdapter.class.getClassLoader(),
                                 new Class[] { preferencesHandlerClass },
                                 adapter);
      }
      registeredPreferencesHandler(osxAdapterProxy);
    } catch (final ClassNotFoundException cnfe) {
      Activator.log
          .warn("This version of Mac OS X does not support the "
                + "Apple EAWT.  Application preferences handling has been "
                + "disabled (" + cnfe + ")", cnfe);
    } catch (final Exception ex) {
      // Likely a NoSuchMethodException or an IllegalAccessException
      // loading/invoking eawt.Application methods
      Activator.log.error("Mac OS X Adapter could not talk to EAWT: " + ex, ex);
    }
  }

  // Pass this method an Object and a Method equipped to handle
  // document events from the Finder Documents are registered with the
  // Finder via the CFBundleDocumentTypes dictionary in the
  // application bundle's Info.plist
  // TODO: Rewrite using new API
//  public static void setFileHandler(Object target, Method fileHandler)
//  {
//    Activator.log.debug("Installing OS X file-handler.");
//    setHandler(new OSXAdapter("handleOpenFile", target, fileHandler)
//      {
//        // Override OSXAdapter.callTarget to send information on the
//        // file to be opened
//        @Override
//        public boolean callTarget(Object appleEvent) {
//          if (appleEvent != null) {
//            try {
//              final Method getFilenameMethod
//                = appleEvent.getClass().getDeclaredMethod("getFilename",
//                                                          (Class[])null);
//              final String filename = (String) getFilenameMethod
//                .invoke(appleEvent, (Object[])null);
//              this.targetMethod.invoke(this.targetObject,
//                                       new Object[]{filename});
//            } catch (final Exception ex) {
//            }
//          }
//          return true;
//        }
//      });
//  }

  // This method requires Java 6 or higher.
  public static void setDockIconImage(Image image)
  {
    if (null==image) {
      return;
    }

    // Set the dock icon image reflectively via com.apple.eawt.Application
    try {
      getApplication();

      try {
        final Method setDockIconImageMethod = macOSXApplication.getClass()
          .getDeclaredMethod("setDockIconImage", new Class[]{Image.class});
        setDockIconImageMethod.invoke(macOSXApplication, new Object[]{image});
        Activator.log.debug("OS X doc icon image set.");
      } catch (final NoSuchMethodException nsme) {
        Activator.log.info("OSXAdapter: setDockIconImage(Image) not provided",
                           nsme);
      }
    } catch (final Exception ex) {
      Activator.log.warn("OSXAdapter could not set the doc icon image", ex);
    }
  }


  // unregisters all registered Application Handlers.
  public static void clearApplicationHandlers()
  {
    try {
      // Unregister the quit handler
      if (registeredQuitHandler != null) {
        registerQuitHandler(null);
      }
      // Unregister the about handler
      if (registeredAboutHandler != null) {
        registerAboutHandler(null);
      }
      // Unregister the preferences handler
      if (registeredPreferencesHandler != null) {
        registeredPreferencesHandler(null);
      }
    } catch (final ClassNotFoundException cnfe) {
      Activator.log.warn("This version of Mac OS X does not support the "
                         + "Apple EAWT.  ApplicationEvent handling has been "
                         + "disabled (" + cnfe + ")", cnfe);
    } catch (final Exception ex) {
      // Likely a NoSuchMethodException or an IllegalAccessException
      // loading/invoking eawt.Application methods
      Activator.log.error("Mac OS X Adapter could not talk to EAWT: " + ex, ex);
    }
  }

  private static Object registerHandler(final String handlerClassName,
                                final String setHandlerMethodName,
                                final Object handler)
      throws ClassNotFoundException, NoSuchMethodException,
      IllegalAccessException, InvocationTargetException,
      IllegalArgumentException, SecurityException, InstantiationException
  {
    getApplication();

    final Class<?> quitHandlerClass = Class.forName(handlerClassName);
    final Method setQuitHandlerMethod =
      macOSXApplication.getClass()
          .getDeclaredMethod(setHandlerMethodName,
                             new Class[] { quitHandlerClass });
    setQuitHandlerMethod.invoke(macOSXApplication, new Object[] { handler });
    if (handler == null) {
      Activator.log.debug("Unregistered " + handlerClassName + ".");
    } else {
      Activator.log.debug("Registered " + handlerClassName + ": " + handler);
    }
    return handler;
  }

  private static void registerQuitHandler(final Object quitHandler)
      throws ClassNotFoundException, NoSuchMethodException,
      IllegalAccessException, InvocationTargetException,
      IllegalArgumentException, SecurityException, InstantiationException
  {
    registeredQuitHandler =
      registerHandler("com.apple.eawt.QuitHandler", "setQuitHandler",
                      quitHandler);
  }

  private static void registerAboutHandler(Object aboutHandler)
      throws ClassNotFoundException, InstantiationException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    registeredAboutHandler =
      registerHandler("com.apple.eawt.AboutHandler", "setAboutHandler",
                      aboutHandler);
  }

  private static void registeredPreferencesHandler(Object prefsHandler)
      throws ClassNotFoundException, InstantiationException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    registeredPreferencesHandler =
      registerHandler("com.apple.eawt.PreferencesHandler",
                      "setPreferencesHandler", prefsHandler);
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
  // comes with the various callback methods See setFileHandler above for an
  // example
  private boolean callTarget(Object appleEvent)
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
    if ("handleQuitRequestWith".equals(method.getName())) {
      Activator.log.debug("QuiteHandler.handleQuitRequestWith() called.");
      callTarget(args[0]);
      // Always cancel the quit here since we will eventually call System.exit()
      // via the call to framework.stop() if the users selects to actually quit.
      callQuitResponseCancel(args[1]);
    } else if ("handleAbout".equals(method.getName())) {
      Activator.log.debug("AboutHandler.handleAbout() called.");
      callTarget(args[0]);
    } else if ("handlePreferences".equals(method.getName())) {
      Activator.log.debug("PreferencesHandler.handlePreferences() called.");
      callTarget(args[0]);
    } else {
      // Must always handle equals, hashCode and toString.
      final String name = method.getName();
      final int argLen = null == args ? 0 : args.length;
      if ("equals".equals(name) && 1 == argLen) {
        // Proxy with same InvocationHandler (i.e., OSXAdapter)?
        final Object other = args[0];
        if (Proxy.isProxyClass(other.getClass())) {
          final InvocationHandler oih = Proxy.getInvocationHandler(other);
          return this.equals(oih) ? Boolean.TRUE : Boolean.FALSE;
        }
        return Boolean.FALSE;
      } else if ("hashCode".equals(name) && 0 == argLen) {
        // delegate to OSXAdapter
        return new Integer(this.hashCode());
      } else if ("toString".equals(name) && 0 == argLen) {
        return this.toString();
      }
    }
    // All of the ApplicationListener methods are void; return null
    // regardless of what happens
    return null;
  }

  /**
   * Call the {@code QuitResponse.cancelQuit()}-method in the given
   * object.
   *
   * @param object
   *          quite response object to call cancel on.
   */
  private void callQuitResponseCancel(Object object)
  {
    try {
      final Class<?> quitResponseClass =
        Class.forName("com.apple.eawt.QuitResponse");
      final Method cancelQuitMethod =
        quitResponseClass.getDeclaredMethod("cancelQuit", (Class<?>[]) null);
      cancelQuitMethod.invoke(object, (Object[]) null);
      Activator.log.debug("QuiteResponse.cancelQuit() called.");
    } catch (final Exception e) {
      Activator.log.error("Failed to cancel quit operation: " + e, e);
    }
  }

}
