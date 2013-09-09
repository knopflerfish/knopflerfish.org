/*
 * Copyright (c) OSGi Alliance (2001, 2013). All Rights Reserved.
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

package org.osgi.service.metatype;

/**
 * An interface to describe an attribute.
 * 
 * <p>
 * An {@code AttributeDefinition} object defines a description of the data type
 * of a property/attribute.
 * 
 * @ThreadSafe
 * @author $Id: f2aa9ead0136d63493a5025892bdf80efa2e9019 $
 */
public interface AttributeDefinition {
	/**
	 * The {@code STRING} (1) type.
	 * 
	 * <p>
	 * Attributes of this type should be stored as {@code String},
	 * {@code Vector} with {@code String} or {@code String[]} objects, depending
	 * on the {@code getCardinality()} value.
	 */
	int	STRING		= 1;
	/**
	 * The {@code LONG} (2) type.
	 * 
	 * Attributes of this type should be stored as {@code Long}, {@code Vector}
	 * with {@code Long} or {@code long[]} objects, depending on the
	 * {@code getCardinality()} value.
	 */
	int	LONG		= 2;
	/**
	 * The {@code INTEGER} (3) type.
	 * 
	 * Attributes of this type should be stored as {@code Integer},
	 * {@code Vector} with {@code Integer} or {@code int[]} objects, depending
	 * on the {@code getCardinality()} value.
	 */
	int	INTEGER		= 3;
	/**
	 * The {@code SHORT} (4) type.
	 * 
	 * Attributes of this type should be stored as {@code Short}, {@code Vector}
	 * with {@code Short} or {@code short[]} objects, depending on the
	 * {@code getCardinality()} value.
	 */
	int	SHORT		= 4;
	/**
	 * The {@code CHARACTER} (5) type.
	 * 
	 * Attributes of this type should be stored as {@code Character},
	 * {@code Vector} with {@code Character} or {@code char[]} objects,
	 * depending on the {@code getCardinality()} value.
	 */
	int	CHARACTER	= 5;
	/**
	 * The {@code BYTE} (6) type.
	 * 
	 * Attributes of this type should be stored as {@code Byte}, {@code Vector}
	 * with {@code Byte} or {@code byte[]} objects, depending on the
	 * {@code getCardinality()} value.
	 */
	int	BYTE		= 6;
	/**
	 * The {@code DOUBLE} (7) type.
	 * 
	 * Attributes of this type should be stored as {@code Double},
	 * {@code Vector} with {@code Double} or {@code double[]} objects, depending
	 * on the {@code getCardinality()} value.
	 */
	int	DOUBLE		= 7;
	/**
	 * The {@code FLOAT} (8) type.
	 * 
	 * Attributes of this type should be stored as {@code Float}, {@code Vector}
	 * with {@code Float} or {@code float[]} objects, depending on the
	 * {@code getCardinality()} value.
	 */
	int	FLOAT		= 8;
	/**
	 * The {@code BIGINTEGER} (9) type.
	 * 
	 * Attributes of this type should be stored as {@code BigInteger},
	 * {@code Vector} with {@code BigInteger} or {@code BigInteger[]} objects,
	 * depending on the {@code getCardinality()} value.
	 * 
	 * @deprecated As of 1.1.
	 */
	int	BIGINTEGER	= 9;
	/**
	 * The {@code BIGDECIMAL} (10) type.
	 * 
	 * Attributes of this type should be stored as {@code BigDecimal},
	 * {@code Vector} with {@code BigDecimal} or {@code BigDecimal[]} objects
	 * depending on {@code getCardinality()}.
	 * 
	 * @deprecated As of 1.1.
	 */
	int	BIGDECIMAL	= 10;
	/**
	 * The {@code BOOLEAN} (11) type.
	 * 
	 * Attributes of this type should be stored as {@code Boolean},
	 * {@code Vector} with {@code Boolean} or {@code boolean[]} objects
	 * depending on {@code getCardinality()}.
	 */
	int	BOOLEAN		= 11;

	/**
	 * The {@code PASSWORD} (12) type.
	 * 
	 * Attributes of this type must be stored as {@code String}, {@code Vector}
	 * with {@code String} or {@code String[]} objects depending on {link
	 * getCardinality()}. A {@code PASSWORD} must be treated as a string but the
	 * type can be used to disguise the information when displayed to a user to
	 * prevent others from seeing it.
	 * 
	 * @since 1.2
	 */
	int	PASSWORD	= 12;

	/**
	 * Get the name of the attribute. This name may be localized.
	 * 
	 * @return The localized name of the definition.
	 */
	String getName();

	/**
	 * Unique identity for this attribute.
	 * 
	 * Attributes share a global namespace in the registry. E.g. an attribute
	 * {@code cn} or {@code commonName} must always be a {@code String} and the
	 * semantics are always a name of some object. They share this aspect with
	 * LDAP/X.500 attributes. In these standards the OSI Object Identifier (OID)
	 * is used to uniquely identify an attribute. If such an OID exists, (which
	 * can be requested at several standard organisations and many companies
	 * already have a node in the tree) it can be returned here. Otherwise, a
	 * unique id should be returned which can be a Java class name (reverse
	 * domain name) or generated with a GUID algorithm. Note that all LDAP
	 * defined attributes already have an OID. It is strongly advised to define
	 * the attributes from existing LDAP schemes which will give the OID. Many
	 * such schemes exist ranging from postal addresses to DHCP parameters.
	 * 
	 * @return The id or oid
	 */
	String getID();

	/**
	 * Return a description of this attribute.
	 * 
	 * The description may be localized and must describe the semantics of this
	 * type and any constraints.
	 * 
	 * @return The localized description of the definition.
	 */
	String getDescription();

	/**
	 * Return the cardinality of this attribute.
	 * 
	 * The OSGi environment handles multi valued attributes in arrays ([]) or in
	 * {@code Vector} objects. The return value is defined as follows:
	 * 
	 * <pre>
	 * 
	 *    x = Integer.MIN_VALUE    no limit, but use Vector
	 *    x &lt; 0                    -x = max occurrences, store in Vector
	 *    x &gt; 0                     x = max occurrences, store in array []
	 *    x = Integer.MAX_VALUE    no limit, but use array []
	 *    x = 0                     1 occurrence required
	 * 
	 * </pre>
	 * 
	 * @return The cardinality of this attribute.
	 */
	int getCardinality();

	/**
	 * Return the type for this attribute.
	 * 
	 * <p>
	 * Defined in the following constants which map to the appropriate Java
	 * type. {@code STRING},{@code LONG},{@code INTEGER}, {@code CHAR},
	 * {@code BYTE},{@code DOUBLE},{@code FLOAT}, {@code BOOLEAN}.
	 * 
	 * @return The type for this attribute.
	 */
	int getType();

	/**
	 * Return a list of option values that this attribute can take.
	 * 
	 * <p>
	 * If the function returns {@code null}, there are no option values
	 * available.
	 * 
	 * <p>
	 * Each value must be acceptable to validate() (return "") and must be a
	 * {@code String} object that can be converted to the data type defined by
	 * getType() for this attribute.
	 * 
	 * <p>
	 * This list must be in the same sequence as {@code getOptionLabels()}. I.e.
	 * for each index i in {@code getOptionValues}, i in
	 * {@code getOptionLabels()} should be the label.
	 * 
	 * <p>
	 * For example, if an attribute can have the value male, female, unknown,
	 * this list can return
	 * <code>new String[] { "male", "female", "unknown" }</code>.
	 * 
	 * @return A list values
	 */
	String[] getOptionValues();

	/**
	 * Return a list of labels of option values.
	 * 
	 * <p>
	 * The purpose of this method is to allow menus with localized labels. It is
	 * associated with {@code getOptionValues}. The labels returned here are
	 * ordered in the same way as the values in that method.
	 * 
	 * <p>
	 * If the function returns {@code null}, there are no option labels
	 * available.
	 * <p>
	 * This list must be in the same sequence as the {@code getOptionValues()}
	 * method. I.e. for each index i in {@code getOptionLabels}, i in
	 * {@code getOptionValues()} should be the associated value.
	 * 
	 * <p>
	 * For example, if an attribute can have the value male, female, unknown,
	 * this list can return (for dutch)
	 * <code>new String[] { "Man", "Vrouw", "Onbekend" }</code>.
	 * 
	 * @return A list values
	 */
	String[] getOptionLabels();

	/**
	 * Validate an attribute in {@code String} form.
	 * 
	 * An attribute might be further constrained in value. This method will
	 * attempt to validate the attribute according to these constraints. It can
	 * return three different values:
	 * 
	 * <pre>
	 *  null           No validation present
	 *  ""             No problems detected
	 *  "..."          A localized description of why the value is wrong
	 * </pre>
	 * 
	 * If the cardinality of this attribute is multi-valued then this string
	 * must be interpreted as a comma delimited string. The complete value must
	 * be trimmed from white space as well as spaces around commas. Commas (
	 * {@code ','} &#92;u002C) and spaces ({@code ' '} &#92;u0020) and
	 * backslashes ({@code '\'} &#92;u005C) can be escaped with another
	 * backslash. Escaped spaces must not be trimmed. For example:
	 * 
	 * <pre>
	 * value="  a\,b,b\,c,\ c\\,d   " => [ "a,b", "b,c", " c\", "d" ]
	 * </pre>
	 * 
	 * @param value The value before turning it into the basic data type. If the
	 *        cardinality indicates a multi-valued attribute then the given
	 *        string must be escaped.
	 * @return {@code null}, "", or another string
	 */
	String validate(String value);

	/**
	 * Return a default for this attribute.
	 * 
	 * The object must be of the appropriate type as defined by the cardinality
	 * and {@code getType()}. The return type is a list of {@code String}
	 * objects that can be converted to the appropriate type. The cardinality of
	 * the return array must follow the absolute cardinality of this type. E.g.
	 * if the cardinality = 0, the array must contain 1 element. If the
	 * cardinality is 1, it must contain 0 or 1 elements. If it is -5, it must
	 * contain from 0 to max 5 elements. Note that the special case of a 0
	 * cardinality, meaning a single value, does not allow arrays or vectors of
	 * 0 elements.
	 * 
	 * @return Return a default value or {@code null} if no default exists.
	 */
	String[] getDefaultValue();
}
