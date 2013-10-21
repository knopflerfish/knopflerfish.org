/*
 * Copyright (c) OSGi Alliance (2004, 2013). All Rights Reserved.
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

package org.osgi.service.monitor;

import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * A {@code StatusVariable} object represents the value of a status variable
 * taken with a certain collection method at a certain point of time. The type
 * of the {@code StatusVariable} can be {@code int}, {@code float},
 * {@code boolean} or {@code String}.
 * <p>
 * A {@code StatusVariable} is identified by an ID string that is unique within
 * the scope of a {@code Monitorable}. The ID must be a non- {@code null},
 * non-empty string that conforms to the "symbolic-name" definition in the OSGi
 * core specification. This means that only the characters [-_.a-zA-Z0-9] may be
 * used. The length of the ID must not exceed 32 bytes when UTF-8 encoded.
 * 
 * @author $Id: b25ccdfcf41ac2b6d5f8cec13c8d0d21f170fed4 $
 */
public final class StatusVariable {
	// ----- Public constants -----//
	/**
	 * Constant for identifying {@code int} data type.
	 */
	public static final int	TYPE_INTEGER				= 0;

	/**
	 * Constant for identifying {@code float} data type.
	 */
	public static final int	TYPE_FLOAT					= 1;

	/**
	 * Constant for identifying {@code String} data type.
	 */
	public static final int	TYPE_STRING					= 2;

	/**
	 * Constant for identifying {@code boolean} data type.
	 */
	public static final int	TYPE_BOOLEAN				= 3;

	/**
	 * Constant for identifying 'Cumulative Counter' data collection method.
	 */
	public static final int	CM_CC						= 0;

	/**
	 * Constant for identifying 'Discrete Event Registration' data collection
	 * method.
	 */
	public static final int	CM_DER						= 1;

	/**
	 * Constant for identifying 'Gauge' data collection method.
	 */
	public static final int	CM_GAUGE					= 2;

	/**
	 * Constant for identifying 'Status Inspection' data collection method.
	 */
	public static final int	CM_SI						= 3;

	// ----- Package private constants -----//

	static final String		SYMBOLIC_NAME_CHARACTERS	= "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "-_.";	// a
																																			// subset
																																			// of
																																			// the
																																			// characters
																																			// allowed
																																			// in
																																			// DMT
																																			// URIs

	static final int		MAX_ID_LENGTH				= 32;

	// ----- Private fields -----//
	private String			id;
	private Date			timeStamp;
	private int				cm;
	private int				type;

	private int				intData;
	private float			floatData;
	private String			stringData;
	private boolean			booleanData;

	// ----- Constructors -----//
	/**
	 * Constructor for a {@code StatusVariable} of {@code int} type.
	 * 
	 * @param id the identifier of the {@code StatusVariable}
	 * @param cm the collection method, one of the {@code CM_} constants
	 * @param data the {@code int} value of the {@code StatusVariable}
	 * @throws java.lang.IllegalArgumentException if the given {@code id} is not
	 *         a valid {@code StatusVariable} name, or if {@code cm} is not one
	 *         of the collection method constants
	 * @throws java.lang.NullPointerException if the {@code id} parameter is
	 *         {@code null}
	 */
	public StatusVariable(String id, int cm, int data) {
		setCommon(id, cm);
		type = TYPE_INTEGER;
		intData = data;
	}

	/**
	 * Constructor for a {@code StatusVariable} of {@code float} type.
	 * 
	 * @param id the identifier of the {@code StatusVariable}
	 * @param cm the collection method, one of the {@code CM_} constants
	 * @param data the {@code float} value of the {@code StatusVariable}
	 * @throws java.lang.IllegalArgumentException if the given {@code id} is not
	 *         a valid {@code StatusVariable} name, or if {@code cm} is not one
	 *         of the collection method constants
	 * @throws java.lang.NullPointerException if the {@code id} parameter is
	 *         {@code null}
	 */
	public StatusVariable(String id, int cm, float data) {
		setCommon(id, cm);
		type = TYPE_FLOAT;
		floatData = data;
	}

	/**
	 * Constructor for a {@code StatusVariable} of {@code boolean} type.
	 * 
	 * @param id the identifier of the {@code StatusVariable}
	 * @param cm the collection method, one of the {@code CM_} constants
	 * @param data the {@code boolean} value of the {@code StatusVariable}
	 * @throws java.lang.IllegalArgumentException if the given {@code id} is not
	 *         a valid {@code StatusVariable} name, or if {@code cm} is not one
	 *         of the collection method constants
	 * @throws java.lang.NullPointerException if the {@code id} parameter is
	 *         {@code null}
	 */
	public StatusVariable(String id, int cm, boolean data) {
		setCommon(id, cm);
		type = TYPE_BOOLEAN;
		booleanData = data;
	}

	/**
	 * Constructor for a {@code StatusVariable} of {@code String} type.
	 * 
	 * @param id the identifier of the {@code StatusVariable}
	 * @param cm the collection method, one of the {@code CM_} constants
	 * @param data the {@code String} value of the {@code StatusVariable}, can
	 *        be {@code null}
	 * @throws java.lang.IllegalArgumentException if the given {@code id} is not
	 *         a valid {@code StatusVariable} name, or if {@code cm} is not one
	 *         of the collection method constants
	 * @throws java.lang.NullPointerException if the {@code id} parameter is
	 *         {@code null}
	 */
	public StatusVariable(String id, int cm, String data) {
		setCommon(id, cm);
		type = TYPE_STRING;
		stringData = data;
	}

	// ----- Public methods -----//
	/**
	 * Returns the ID of this {@code StatusVariable}. The ID is unique within
	 * the scope of a {@code Monitorable}.
	 * 
	 * @return the ID of this {@code StatusVariable}
	 */
	public String getID() {
		return id;
	}

	/**
	 * Returns information on the data type of this {@code StatusVariable}.
	 * 
	 * @return one of the {@code TYPE_} constants indicating the type of this
	 *         {@code StatusVariable}
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns the timestamp associated with the {@code StatusVariable}. The
	 * timestamp is stored when the {@code StatusVariable} instance is created,
	 * generally during the {@link Monitorable#getStatusVariable(String)} method
	 * call.
	 * 
	 * @return the time when the {@code StatusVariable} value was queried,
	 *         cannot be {@code null}
	 * 
	 */
	public Date getTimeStamp() {
		return timeStamp;
	}

	/**
	 * Returns the {@code StatusVariable} value if its type is {@code String}.
	 * 
	 * @return the {@code StatusVariable} value as a {@code String}
	 * @throws java.lang.IllegalStateException if the type of the
	 *         {@code StatusVariable} is not {@code String}
	 */
	public String getString() throws IllegalStateException {
		if (type != TYPE_STRING)
			throw new IllegalStateException("This StatusVariable does not contain a String value.");
		return stringData;
	}

	/**
	 * Returns the {@code StatusVariable} value if its type is {@code int}.
	 * 
	 * @return the {@code StatusVariable} value as an {@code int}
	 * @throws java.lang.IllegalStateException if the type of this
	 *         {@code StatusVariable} is not {@code int}
	 */
	public int getInteger() throws IllegalStateException {
		if (type != TYPE_INTEGER)
			throw new IllegalStateException("This StatusVariable does not contain an integer value.");
		return intData;
	}

	/**
	 * Returns the {@code StatusVariable} value if its type is {@code float}.
	 * 
	 * @return the {@code StatusVariable} value as a {@code float}
	 * @throws java.lang.IllegalStateException if the type of this
	 *         {@code StatusVariable} is not {@code float}
	 */
	public float getFloat() throws IllegalStateException {
		if (type != TYPE_FLOAT)
			throw new IllegalStateException("This StatusVariable does not contain a float value.");
		return floatData;
	}

	/**
	 * Returns the {@code StatusVariable} value if its type is {@code boolean}.
	 * 
	 * @return the {@code StatusVariable} value as a {@code boolean}
	 * @throws java.lang.IllegalStateException if the type of this
	 *         {@code StatusVariable} is not {@code boolean}
	 */
	public boolean getBoolean() throws IllegalStateException {
		if (type != TYPE_BOOLEAN)
			throw new IllegalStateException("This StatusVariable does not contain a boolean value.");
		return booleanData;
	}

	/**
	 * Returns the collection method of this {@code StatusVariable}. See section
	 * 3.3 b) in [ETSI TS 132 403]
	 * 
	 * @return one of the {@code CM_} constants
	 */
	public int getCollectionMethod() {
		return cm;
	}

	/**
	 * Compares the specified object with this {@code StatusVariable}. Two
	 * {@code StatusVariable} objects are considered equal if their full path,
	 * collection method and type are identical, and the data (selected by their
	 * type) is equal.
	 * 
	 * @param obj the object to compare with this {@code StatusVariable}
	 * @return {@code true} if the argument represents the same
	 *         {@code StatusVariable} as this object
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof StatusVariable))
			return false;

		StatusVariable other = (StatusVariable) obj;

		if (!equals(id, other.id) || cm != other.cm || type != other.type)
			return false;

		switch (type) {
			case TYPE_INTEGER :
				return intData == other.intData;
			case TYPE_FLOAT :
				return floatData == other.floatData;
			case TYPE_STRING :
				return equals(stringData, other.stringData);
			case TYPE_BOOLEAN :
				return booleanData == other.booleanData;
		}

		return false; // never reached
	}

	/**
	 * Returns the hash code value for this {@code StatusVariable}. The hash
	 * code is calculated based on the full path, collection method and value of
	 * the {@code StatusVariable}.
	 * 
	 * @return the hash code of this object
	 */
	public int hashCode() {
		int hash = hashCode(id) ^ cm;

		switch (type) {
			case TYPE_INTEGER :
				return hash ^ intData;
			case TYPE_FLOAT :
				return hash ^ hashCode(new Float(floatData));
			case TYPE_BOOLEAN :
				return hash ^ hashCode(new Boolean(booleanData));
			case TYPE_STRING :
				return hash ^ hashCode(stringData);
		}

		return 0; // never reached
	}

	// String representation: StatusVariable(path, cm, time, type, value)
	/**
	 * Returns a {@code String} representation of this {@code StatusVariable}.
	 * The returned {@code String} contains the full path, collection method,
	 * timestamp, type and value parameters of the {@code StatusVariable} in the
	 * following format:
	 * 
	 * <pre>
	 * StatusVariable(&lt;path&gt;, &lt;cm&gt;, &lt;timestamp&gt;, &lt;type&gt;, &lt;value&gt;)
	 * </pre>
	 * 
	 * The collection method identifiers used in the string representation are
	 * "CC", "DER", "GAUGE" and "SI" (without the quotes). The format of the
	 * timestamp is defined by the {@code Date.toString} method, while the type
	 * is identified by one of the strings "INTEGER", "FLOAT", "STRING" and
	 * "BOOLEAN". The final field contains the string representation of the
	 * value of the status variable.
	 * 
	 * @return the {@code String} representation of this {@code StatusVariable}
	 */
	public String toString() {
		String cmName = null;
		switch (cm) {
			case CM_CC :
				cmName = "CC";
				break;
			case CM_DER :
				cmName = "DER";
				break;
			case CM_GAUGE :
				cmName = "GAUGE";
				break;
			case CM_SI :
				cmName = "SI";
				break;
		}

		String beg = "StatusVariable(" + id + ", " + cmName + ", " + timeStamp + ", ";

		switch (type) {
			case TYPE_INTEGER :
				return beg + "INTEGER, " + intData + ")";
			case TYPE_FLOAT :
				return beg + "FLOAT, " + floatData + ")";
			case TYPE_STRING :
				return beg + "STRING, " + stringData + ")";
			case TYPE_BOOLEAN :
				return beg + "BOOLEAN, " + booleanData + ")";
		}

		return null; // never reached
	}

	// ----- Private methods -----//

	private void setCommon(String id, int cm) throws IllegalArgumentException, NullPointerException {
		checkId(id, "StatusVariable ID");

		if (cm != CM_CC && cm != CM_DER && cm != CM_GAUGE && cm != CM_SI)
			throw new IllegalArgumentException("Unknown data collection method constant '" + cm + "'.");

		this.id = id;
		this.cm = cm;
		timeStamp = new Date();
	}

	private boolean equals(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}

	private int hashCode(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	private static void checkId(String id, String idName) throws IllegalArgumentException, NullPointerException {
		if (id == null)
			throw new NullPointerException(idName + " is null.");
		if (id.length() == 0)
			throw new IllegalArgumentException(idName + " is empty.");

		byte[] nameBytes;
		try {
			nameBytes = id.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// never happens, "UTF-8" must always be supported
			throw new IllegalStateException(e.getMessage());
		}
		if (nameBytes.length > MAX_ID_LENGTH)
			throw new IllegalArgumentException(idName + " is too long " + "(over " + MAX_ID_LENGTH + " bytes in UTF-8 encoding).");

		if (id.equals(".") || id.equals(".."))
			throw new IllegalArgumentException(idName + " is invalid.");

		if (!containsValidChars(id))
			throw new IllegalArgumentException(idName + " contains invalid characters.");
	}

	private static boolean containsValidChars(String name) {
		char[] chars = name.toCharArray();
		for (int i = 0; i < chars.length; i++)
			if (SYMBOLIC_NAME_CHARACTERS.indexOf(chars[i]) == -1)
				return false;

		return true;
	}
}
