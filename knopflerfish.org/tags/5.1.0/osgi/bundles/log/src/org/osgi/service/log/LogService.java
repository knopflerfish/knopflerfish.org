/*
 * Copyright (c) OSGi Alliance (2000, 2013). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.log;

import org.osgi.framework.ServiceReference;

/**
 * Provides methods for bundles to write messages to the log.
 * 
 * <p>
 * {@code LogService} methods are provided to log messages; optionally with a
 * {@code ServiceReference} object or an exception.
 * 
 * <p>
 * Bundles must log messages in the OSGi environment with a severity level
 * according to the following hierarchy:
 * <ol>
 * <li>{@link #LOG_ERROR}</li>
 * <li>{@link #LOG_WARNING}</li>
 * <li>{@link #LOG_INFO}</li>
 * <li>{@link #LOG_DEBUG}</li>
 * </ol>
 * 
 * @ThreadSafe
 * @noimplement
 * @author $Id: dc7a9f634f0f0ccf9bd224e12d15d092861efc0e $
 */
public interface LogService {
	/**
	 * An error message (Value 1).
	 * 
	 * <p>
	 * This log entry indicates the bundle or service may not be functional.
	 */
	public static final int	LOG_ERROR	= 1;
	/**
	 * A warning message (Value 2).
	 * 
	 * <p>
	 * This log entry indicates a bundle or service is still functioning but may
	 * experience problems in the future because of the warning condition.
	 */
	public static final int	LOG_WARNING	= 2;
	/**
	 * An informational message (Value 3).
	 * 
	 * <p>
	 * This log entry may be the result of any change in the bundle or service
	 * and does not indicate a problem.
	 */
	public static final int	LOG_INFO	= 3;
	/**
	 * A debugging message (Value 4).
	 * 
	 * <p>
	 * This log entry is used for problem determination and may be irrelevant to
	 * anyone but the bundle developer.
	 */
	public static final int	LOG_DEBUG	= 4;

	/**
	 * Logs a message.
	 * 
	 * <p>
	 * The {@code ServiceReference} field and the {@code Throwable} field of the
	 * {@code LogEntry} object will be set to {@code null}.
	 * 
	 * @param level The severity of the message. This should be one of the
	 *        defined log levels but may be any integer that is interpreted in a
	 *        user defined way.
	 * @param message Human readable string describing the condition or
	 *        {@code null}.
	 * @see #LOG_ERROR
	 * @see #LOG_WARNING
	 * @see #LOG_INFO
	 * @see #LOG_DEBUG
	 */
	public void log(int level, String message);

	/**
	 * Logs a message with an exception.
	 * 
	 * <p>
	 * The {@code ServiceReference} field of the {@code LogEntry} object will be
	 * set to {@code null}.
	 * 
	 * @param level The severity of the message. This should be one of the
	 *        defined log levels but may be any integer that is interpreted in a
	 *        user defined way.
	 * @param message The human readable string describing the condition or
	 *        {@code null}.
	 * @param exception The exception that reflects the condition or
	 *        {@code null}.
	 * @see #LOG_ERROR
	 * @see #LOG_WARNING
	 * @see #LOG_INFO
	 * @see #LOG_DEBUG
	 */
	public void log(int level, String message, Throwable exception);

	/**
	 * Logs a message associated with a specific {@code ServiceReference}
	 * object.
	 * 
	 * <p>
	 * The {@code Throwable} field of the {@code LogEntry} will be set to
	 * {@code null}.
	 * 
	 * @param sr The {@code ServiceReference} object of the service that this
	 *        message is associated with or {@code null}.
	 * @param level The severity of the message. This should be one of the
	 *        defined log levels but may be any integer that is interpreted in a
	 *        user defined way.
	 * @param message Human readable string describing the condition or
	 *        {@code null}.
	 * @see #LOG_ERROR
	 * @see #LOG_WARNING
	 * @see #LOG_INFO
	 * @see #LOG_DEBUG
	 */
	public void log(ServiceReference sr, int level, String message);

	/**
	 * Logs a message with an exception associated and a
	 * {@code ServiceReference} object.
	 * 
	 * @param sr The {@code ServiceReference} object of the service that this
	 *        message is associated with.
	 * @param level The severity of the message. This should be one of the
	 *        defined log levels but may be any integer that is interpreted in a
	 *        user defined way.
	 * @param message Human readable string describing the condition or
	 *        {@code null}.
	 * @param exception The exception that reflects the condition or
	 *        {@code null}.
	 * @see #LOG_ERROR
	 * @see #LOG_WARNING
	 * @see #LOG_INFO
	 * @see #LOG_DEBUG
	 */
	public void log(ServiceReference sr, int level, String message, Throwable exception);
}
