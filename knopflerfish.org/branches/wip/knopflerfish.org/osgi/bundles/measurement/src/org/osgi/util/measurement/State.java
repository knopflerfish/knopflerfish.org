/*
 * $Header: /home/wistrand/cvs/knopflerfish.org/osgi/bundles/measurement/src/org/osgi/util/measurement/State.java,v 1.1.1.1 2004/03/05 20:35:13 wistrand Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2002).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative
 * (OSGI) Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of OSGi). OSGi is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.util.measurement;

/**
 * Groups a state name, value and timestamp.
 *
 * <p>The state itself is represented
 * as an integer and the time is measured in milliseconds since midnight,
 * January 1, 1970 UTC.
 *
 * <p>A <tt>State</tt> object is immutable so that it may be
 * easily shared.
 *
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */

public class State
{
	final int value;
	final long time;
	final String name;

	/**
	 * Create a new <tt>State</tt> object.
	 *
	 * @param value The value of the state.
	 * @param name The name of the state.
	 * @param time The time measured in milliseconds since midnight, January 1, 1970 UTC.
	 */
	public State(int value, String name, long time)
	{
		this.value = value;
		this.name = name;
		this.time = time;
	}

	/**
	 * Create a new <tt>State</tt> object with a time of 0.
	 *
	 * @param value The value of the state.
	 * @param name The name of the state.
	 */
	public State(int value, String name)
	{
		this(value, name, 0);
	}


	/**
	 * Returns the value of this <tt>State</tt>.
	 *
	 * @return The value of this <tt>State</tt> object.
	 */
	public final int getValue()
	{
		return value;
	}

	/**
	 * Returns the time with which this <tt>State</tt> was created.
	 *
	 * @return The time with which this <tt>State</tt> was created.
	 * The time is measured in milliseconds since midnight, January 1, 1970 UTC.
	 */
	public final long getTime()
	{
		return time;
	}

	/**
	 * Returns the name of this <tt>State</tt>.
	 *
	 * @return The name of this <tt>State</tt> object.
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * Returns a <tt>String</tt> object representing this object.
	 *
	 * @return a <tt>String</tt> object representing this object.
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();

		sb.append(value);

		if (name != null)
		{
			sb.append(" \"");
			sb.append(name);
			sb.append("\"");
		}

		return(sb.toString());
	}

	/**
	 * Returns a hash code value for this object.
	 *
	 * @return A hash code value for this object.
	 */
	public int hashCode()
	{
		int hash = value;

		if (name != null)
		{
			hash ^= name.hashCode();
		}

		return hash;
	}

	/**
	 * Return whether the specified object is equal to this object.
	 * Two <tt>State</tt> objects are equal if they have same value and name.
	 *
	 * @param obj The object to compare with this object.
	 * @return <tt>true</tt> if this object is equal to the specified
	 * object; <tt>false</tt> otherwise.
	 */
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (!(obj instanceof State))
		{
			return false;
		}

		State that = (State)obj;

		if (value != that.value)
		{
			return false;
		}

		if (name == that.name)
		{
			return true;
		}

		if (name == null)
		{
			return false;
		}

		return name.equals(that.name);
	}
}


