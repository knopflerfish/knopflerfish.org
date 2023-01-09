package org.knopflerfish.bundle.log;

import org.osgi.framework.Bundle;
import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LoggerFactoryImpl implements LoggerFactory {
  private Map<String, Logger> loggers = new HashMap<>();
  private Bundle bundle;

  @Override
  public Logger getLogger(String name) {
    return getLogger(name, bundle);
  }

  private Logger getLogger(String name, Bundle bundle) {
    return loggers.computeIfAbsent(name, k ->
        new LoggerImpl(name, bundle.getBundleContext())
    );
  }

  @Override
  public Logger getLogger(Class<?> clazz) {
    return getLogger(clazz.getName());
  }

  @Override
  public <L extends Logger> L getLogger(String name, Class<L> loggerType) {
    checkLoggerType(loggerType);
    //noinspection unchecked
    return (L) getLogger(name);
  }

  @Override
  public <L extends Logger> L getLogger(Class<?> clazz, Class<L> loggerType) {
    checkLoggerType(loggerType);
    //noinspection unchecked
    return (L) getLogger(clazz);
  }

  @Override
  public <L extends Logger> L getLogger(Bundle bundle, String name, Class<L> loggerType) {
    checkLoggerType(loggerType);
    checkBundle(bundle);
    //noinspection unchecked
    return (L) getLogger(name, bundle);
  }

  private void checkBundle(Bundle bundle) {
    if (bundle.getBundleContext() == null) {
      throw new IllegalArgumentException("Bundle has no context: " + bundle);
    }
  }

  private <L extends Logger> void checkLoggerType(Class<L> loggerType) {
    if (!loggerType.equals(Logger.class) && !loggerType.equals(FormatterLogger.class)) {
      throw new IllegalArgumentException("Unsupported Logger type: " + loggerType);
    }
  }
}
