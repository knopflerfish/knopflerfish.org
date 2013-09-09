/*
 * Copyright (c) OSGi Alliance (2005, 2013). All Rights Reserved.
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

package org.osgi.service.application;

/**
 * This exception is used to indicate problems related to application lifecycle
 * management.
 * 
 * {@code ApplicationException} object is created by the Application Admin to
 * denote an exception condition in the lifecycle of an application.
 * {@code ApplicationException}s should not be created by developers.
 * {@code ApplicationException}s are associated with an error code. This code
 * describes the type of problem reported in this exception. The possible codes
 * are:
 * <ul>
 * <li> {@link #APPLICATION_LOCKED} - The application couldn't be launched
 * because it is locked.</li>
 * <li> {@link #APPLICATION_NOT_LAUNCHABLE} - The application is not in
 * launchable state.</li>
 * <li> {@link #APPLICATION_INTERNAL_ERROR} - An exception was thrown by the
 * application or its container during launch.</li>
 * <li> {@link #APPLICATION_SCHEDULING_FAILED} - The scheduling of an application
 * failed.</li>
 * <li> {@link #APPLICATION_DUPLICATE_SCHEDULE_ID} - The application scheduling
 * failed because the specified identifier is already in use.</li>
 * <li> {@link #APPLICATION_EXITVALUE_NOT_AVAILABLE} - The exit value is not
 * available for an application instance because the instance has not
 * terminated.</li>
 * <li> {@link #APPLICATION_INVALID_STARTUP_ARGUMENT} - One of the specified
 * startup arguments is invalid, for example its type is not permitted.</li>
 * </ul>
 * 
 * @author $Id: 5f2542a639033d6c4f85feaf1cbe7f1f0a9d892f $
 */
public class ApplicationException extends Exception {
	private static final long	serialVersionUID						= -7173190453622508207L;
	private final int			errorCode;

	/**
	 * The application couldn't be launched because it is locked.
	 */
	public static final int		APPLICATION_LOCKED						= 0x01;

	/**
	 * The application is not in launchable state, it's
	 * {@link ApplicationDescriptor#APPLICATION_LAUNCHABLE} attribute is false.
	 */
	public static final int		APPLICATION_NOT_LAUNCHABLE				= 0x02;

	/**
	 * An exception was thrown by the application or the corresponding container
	 * during launch. The exception is available from {@code getCause()}.
	 */
	public static final int		APPLICATION_INTERNAL_ERROR				= 0x03;

	/**
	 * The application schedule could not be created due to some internal error
	 * (for example, the schedule information couldn't be saved due to some
	 * storage error).
	 */
	public static final int		APPLICATION_SCHEDULING_FAILED			= 0x04;

	/**
	 * The application scheduling failed because the specified identifier is
	 * already in use.
	 */
	public static final int		APPLICATION_DUPLICATE_SCHEDULE_ID		= 0x05;

	/**
	 * The exit value is not available for an application instance because the
	 * instance has not terminated.
	 * 
	 * @since 1.1
	 */
	public static final int		APPLICATION_EXITVALUE_NOT_AVAILABLE		= 0x06;

	/**
	 * One of the specified startup arguments is invalid, for example its type
	 * is not permitted.
	 * 
	 * @since 1.1
	 */
	public static final int		APPLICATION_INVALID_STARTUP_ARGUMENT	= 0x07;

	/**
	 * Creates an {@code ApplicationException} with the specified error code.
	 * 
	 * @param errorCode The code of the error
	 */
	public ApplicationException(int errorCode) {
		super();
		this.errorCode = errorCode;
	}

	/**
	 * Creates a {@code ApplicationException} that wraps another exception.
	 * 
	 * @param errorCode The code of the error
	 * @param cause The cause of this exception.
	 */
	public ApplicationException(int errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}

	/**
	 * Creates an {@code ApplicationException} with the specified error code.
	 * 
	 * @param errorCode The code of the error
	 * @param message The associated message
	 */
	public ApplicationException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * Creates a {@code ApplicationException} that wraps another exception.
	 * 
	 * @param errorCode The code of the error
	 * @param message The associated message.
	 * @param cause The cause of this exception.
	 */
	public ApplicationException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/**
	 * Returns the cause of this exception or {@code null} if no cause was set.
	 * 
	 * @return The cause of this exception or {@code null} if no cause was set.
	 */
	public Throwable getCause() {
		return super.getCause();
	}

	/**
	 * Returns the error code associated with this exception.
	 * 
	 * @return The error code of this exception.
	 */
	public int getErrorCode() {
		return errorCode;
	}
}
