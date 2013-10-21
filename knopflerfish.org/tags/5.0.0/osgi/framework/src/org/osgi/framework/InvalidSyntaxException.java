/*
 * Copyright (c) OSGi Alliance (2000, 2012). All Rights Reserved.
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

package org.osgi.framework;

/**
 * A Framework exception used to indicate that a filter string has an invalid
 * syntax.
 * 
 * <p>
 * An {@code InvalidSyntaxException} object indicates that a filter string
 * parameter has an invalid syntax and cannot be parsed. See {@link Filter} for
 * a description of the filter string syntax.
 * 
 * <p>
 * This exception conforms to the general purpose exception chaining mechanism.
 * 
 * @version $Id: 8820ca2db85b557cef8da09ee861249dfb5ee914 $
 */

public class InvalidSyntaxException extends Exception {
	static final long		serialVersionUID	= -4295194420816491875L;
	/**
	 * The invalid filter string.
	 */
	private final String	filter;

	/**
	 * Creates an exception of type {@code InvalidSyntaxException}.
	 * 
	 * <p>
	 * This method creates an {@code InvalidSyntaxException} object with the
	 * specified message and the filter string which generated the exception.
	 * 
	 * @param msg The message.
	 * @param filter The invalid filter string.
	 */
	public InvalidSyntaxException(String msg, String filter) {
		super(message(msg, filter));
		this.filter = filter;
	}

	/**
	 * Creates an exception of type {@code InvalidSyntaxException}.
	 * 
	 * <p>
	 * This method creates an {@code InvalidSyntaxException} object with the
	 * specified message and the filter string which generated the exception.
	 * 
	 * @param msg The message.
	 * @param filter The invalid filter string.
	 * @param cause The cause of this exception.
	 * @since 1.3
	 */
	public InvalidSyntaxException(String msg, String filter, Throwable cause) {
		super(message(msg, filter), cause);
		this.filter = filter;
	}

	/**
	 * Return message string for super constructor.
	 */
	private static String message(String msg, String filter) {
		if ((msg == null) || (filter == null) || msg.indexOf(filter) >= 0) {
			return msg;
		}
		return msg + ": " + filter;
	}

	/**
	 * Returns the filter string that generated the
	 * {@code InvalidSyntaxException} object.
	 * 
	 * @return The invalid filter string.
	 * @see BundleContext#getServiceReferences(Class, String)
	 * @see BundleContext#getServiceReferences(String, String)
	 * @see BundleContext#addServiceListener(ServiceListener,String)
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * Returns the cause of this exception or {@code null} if no cause was set.
	 * 
	 * @return The cause of this exception or {@code null} if no cause was set.
	 * @since 1.3
	 */
	public Throwable getCause() {
		return super.getCause();
	}

	/**
	 * Initializes the cause of this exception to the specified value.
	 * 
	 * @param cause The cause of this exception.
	 * @return This exception.
	 * @throws IllegalArgumentException If the specified cause is this
	 *         exception.
	 * @throws IllegalStateException If the cause of this exception has already
	 *         been set.
	 * @since 1.3
	 */
	public Throwable initCause(Throwable cause) {
		return super.initCause(cause);
	}
}
