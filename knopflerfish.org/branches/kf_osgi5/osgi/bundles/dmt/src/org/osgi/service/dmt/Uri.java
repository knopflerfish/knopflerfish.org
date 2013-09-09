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

package org.osgi.service.dmt;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains static utility methods to manipulate DMT URIs.
 * <p>
 * Syntax of valid DMT URIs:
 * <ul>
 * <li>A slash ({@code '/'} &#92;u002F) is the separator of the node names.
 * Slashes used in node name must therefore be escaped using a backslash slash (
 * {@code "\/"}). The backslash must be escaped with a double backslash
 * sequence. A backslash found must be ignored when it is not followed by a
 * slash or backslash.</li>
 * <li>The node name can be constructed using full Unicode character set (except
 * the Supplementary code, not being supported by CLDC/CDC). However, using the
 * full Unicode character set for node names is discouraged because the encoding
 * in the underlying storage as well as the encoding needed in communications
 * can create significant performance and memory usage overhead. Names that are
 * restricted to the URI set {@code [-a-zA-Z0-9_.!~*'()]} are most efficient.</li>
 * <li>URIs used in the DMT must be treated and interpreted as case sensitive.</li>
 * <li>No End Slash: URI must not end with the delimiter slash ({@code '/'}
 * &#92;u002F). This implies that the root node must be denoted as {@code "."}
 * and not {@code "./"}.</li>
 * <li>No parent denotation: URI must not be constructed using the character
 * sequence {@code "../"} to traverse the tree upwards.</li>
 * <li>Single Root: The character sequence {@code "./"} must not be used
 * anywhere else but in the beginning of a URI.</li>
 * </ul>
 * 
 * @author $Id: 6cc99c9a05cf3d1aa6735bac6e9f1b78cba761ed $
 */
public final class Uri {

	/**
	 * This constant stands for a string identifying the root of the DmTree
	 * (".").
	 * 
	 * @since 2.0
	 */
	public static final String	ROOT_NODE			= ".";

	/**
	 * This constant stands for a char identifying the root of the DmTree ('.').
	 * 
	 * @since 2.0
	 */
	public static final char	ROOT_NODE_CHAR		= '.';

	/**
	 * This constant stands for a string identifying the path separator in the
	 * DmTree ("/").
	 * 
	 * @since 2.0
	 */
	public static final String	PATH_SEPARATOR		= "/";

	/**
	 * This constant stands for a char identifying the path separator in the
	 * DmTree ('/').
	 * 
	 * @since 2.0
	 */
	public static final char	PATH_SEPARATOR_CHAR	= '/';

	// base64 encoding table, modified for use in node name mangling
	private static final char	BASE_64_TABLE[]		= {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c',
			'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '_', // !!!
																																															// this
																																															// differs
																																															// from
																																															// base64
													};

	/**
	 * A private constructor to suppress the default public constructor.
	 */
	private Uri() {
		//
	}

	/**
	 * Returns a node name that is valid for the tree operation methods, based
	 * on the given node name. This transformation is not idempotent, so it must
	 * not be called with a parameter that is the result of a previous
	 * {@code mangle} method call.
	 * <p>
	 * Node name mangling is needed in the following cases:
	 * <ul>
	 * <li>if the name contains '/' or '\' characters</li>
	 * </ul>
	 * <p>
	 * A node name that does not suffer from either of these problems is
	 * guaranteed to remain unchanged by this method. Therefore the client may
	 * skip the mangling if the node name is known to be valid (though it is
	 * always safe to call this method).
	 * <p>
	 * The method returns the normalized {@code nodeName} as described below.
	 * Invalid node names are normalized in different ways, depending on the
	 * cause. If the name contains '/' or '\' characters, then these are simply
	 * escaped by inserting an additional '\' before each occurrence. If the
	 * length of the name does exceed the limit, the following mechanism is used
	 * to normalize it:
	 * <ul>
	 * <li>the SHA 1 digest of the name is calculated</li>
	 * <li>the digest is encoded with the base 64 algorithm</li>
	 * <li>all '/' characters in the encoded digest are replaced with '_'</li>
	 * <li>trailing '=' signs are removed</li>
	 * </ul>
	 * 
	 * @param nodeName the node name to be mangled (if necessary), must not be
	 *        {@code null} or empty
	 * @return the normalized node name that is valid for tree operations
	 * @throws NullPointerException if {@code nodeName} is {@code null}
	 * @throws IllegalArgumentException if {@code nodeName} is empty
	 */
	public static String mangle(String nodeName) {
		return nodeName;
	}

	/**
	 * Construct a URI from the specified URI segments. The segments must
	 * already be mangled.
	 * <p>
	 * If the specified path is an empty array then an empty URI ({@code ""}) is
	 * returned.
	 * 
	 * @param path a possibly empty array of URI segments, must not be
	 *        {@code null}
	 * @return the URI created from the specified segments
	 * @throws NullPointerException if the specified path or any of its segments
	 *         are {@code null}
	 * @throws IllegalArgumentException if the specified path contains too many
	 *         or malformed segments or the resulting URI is too long
	 */
	public static String toUri(String[] path) {
		if (0 == path.length) {
			return "";
		}

		StringBuffer uri = new StringBuffer();
		for (int i = 0; i < path.length; ++i) {
			if (i > 0) {
				uri.append('/');
			}

			// checks if it's not null; otherwise throw NullPointerException.
			if (path[i] == null) {
				throw new NullPointerException("One of its segments is null.");
			}

			uri.append(path[i]);
		}
		return uri.toString();
	}

	/**
	 * This method returns the length of a URI segment. The length of the URI
	 * segment is defined as the number of bytes in the unescaped, UTF-8 encoded
	 * representation of the segment.
	 * <p>
	 * The method verifies that the URI segment is well-formed.
	 * 
	 * @param segment the URI segment
	 * @return URI segment length
	 * @throws NullPointerException if the specified segment is {@code null}
	 * @throws IllegalArgumentException if the specified URI segment is
	 *         malformed
	 */
	private static int getSegmentLength(String segment) {
		if (segment.length() == 0)
			throw new IllegalArgumentException("URI segment is empty.");

		StringBuffer newsegment = new StringBuffer(segment);
		int i = 0;
		while (i < newsegment.length()) { // length can decrease during the
											// loop!
			if (newsegment.charAt(i) == '\\') {
				if (i == newsegment.length() - 1) // last character cannot be a
													// '\'
					throw new IllegalArgumentException("URI segment ends with the escape character.");

				newsegment.deleteCharAt(i); // remove the extra '\'
			} else
				if (newsegment.charAt(i) == '/')
					throw new IllegalArgumentException("URI segment contains an unescaped '/' character.");

			i++;
		}

		if (newsegment.toString().equals(".."))
			throw new IllegalArgumentException("URI segment must not be \"..\".");

		try {
			return newsegment.toString().getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			// This should never happen. All implementations must support
			// UTF-8 encoding;
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Split the specified URI along the path separator '/' characters and
	 * return an array of URI segments. Special characters in the returned
	 * segments are escaped. The returned array may be empty if the specified
	 * URI was empty.
	 * 
	 * @param uri the URI to be split, must not be {@code null}
	 * @return an array of URI segments created by splitting the specified URI
	 * @throws NullPointerException if the specified URI is {@code null}
	 * @throws IllegalArgumentException if the specified URI is malformed
	 */
	public static String[] toPath(String uri) {
		if (uri == null)
			throw new NullPointerException("'uri' parameter is null.");

		if (!isValidUri(uri))
			throw new IllegalArgumentException("Malformed URI: " + uri);

		if (uri.length() == 0)
			return new String[] {};

		List segments = new ArrayList();
		StringBuffer segment = new StringBuffer();

		boolean escape = false;
		for (int i = 0; i < uri.length(); i++) {
			char ch = uri.charAt(i);

			if (escape) {
				if (ch == '/' || ch == '\\')
					segment.append('\\');
				segment.append(ch);
				escape = false;
			} else
				if (ch == '/') {
					segments.add(segment.toString());
					segment = new StringBuffer();
				} else
					if (ch == '\\') {
						escape = true;
					} else
						segment.append(ch);
		}
		if (segment.length() > 0) {
			segments.add(segment.toString());
		}

		return (String[]) segments.toArray(new String[segments.size()]);
	}

	/**
	 * Checks whether the specified URI is an absolute URI. An absolute URI
	 * contains the complete path to a node in the DMT starting from the DMT
	 * root (".").
	 * 
	 * @param uri the URI to be checked, must not be {@code null} and must
	 *        contain a valid URI
	 * @return whether the specified URI is absolute
	 * @throws NullPointerException if the specified URI is {@code null}
	 * @throws IllegalArgumentException if the specified URI is malformed
	 */
	public static boolean isAbsoluteUri(String uri) {
		if (null == uri) {
			throw new NullPointerException("'uri' parameter is null.");
		}
		if (!isValidUri(uri))
			throw new IllegalArgumentException("Malformed URI: " + uri);
		return uri.equals(".") || uri.equals("\\.") || uri.startsWith("./") || uri.startsWith("\\./");
	}

	/**
	 * Encode the node name so that back slash and forward slash are escaped
	 * with a back slash. This method is the reverse of {@link #decode(String)}.
	 * 
	 * @param nodeName the node name to be encoded
	 * @return the encoded node name
	 * @since 2.0
	 */

	public static String encode(String nodeName) {
		// Try not to create an object when it is not necessary
		for (int i = 0; i < nodeName.length(); i++) {
			char c = nodeName.charAt(i);
			if (c == '\\' || c == '/') {
				// We've got an to be escaped character, so now create the
				// string buffer
				StringBuffer sb = new StringBuffer(nodeName);
				for (; i < sb.length(); i++) {
					c = sb.charAt(i);
					if (c == '\\' || c == '/')
						sb.insert(i++, '\\');
				}
				return sb.toString();
			}
		}
		return nodeName;
	}

	/**
	 * Decode the node name so that back slash and forward slash are un-escaped
	 * from a back slash.
	 * 
	 * @param nodeName the node name to be decoded
	 * @return the decoded node name
	 * @since 2.0
	 */

	public static String decode(String nodeName) {
		// Try not to create an object when it is not necessary
		int n = nodeName.indexOf('\\');
		if (n < 0)
			return nodeName;

		StringBuffer sb = new StringBuffer(nodeName);
		while (n >= 0 && n < sb.length()) {
			sb.deleteCharAt(n);
			n++;
			while (n < sb.length() && sb.charAt(n) != '\\')
				n++;
		}
		return sb.toString();
	}

	/**
	 * Checks whether the specified URI is valid. A URI is considered valid if
	 * it meets the following constraints:
	 * <ul>
	 * <li>the URI is not {@code null};</li>
	 * <li>the URI follows the syntax defined for valid DMT URIs;</li>
	 * </ul>
	 * The exact definition of the length of a URI and its segments is given in
	 * the descriptions of the {@code getMaxUriLength()} and
	 * {@code getMaxSegmentNameLength()} methods.
	 * 
	 * @param uri the URI to be validated
	 * @return whether the specified URI is valid
	 */
	public static boolean isValidUri(String uri) {
		if (null == uri)
			return false;

		int paramLen = uri.length();
		if (paramLen == 0)
			return true;
		if (uri.charAt(0) == '/' || uri.charAt(paramLen - 1) == '\\')
			return false;

		int segmentNumber = 0;

		// append a '/' to indicate the end of the last segment (the URI in the
		// parameter must not end with a '/')
		uri += '/';
		paramLen++;

		int start = 0;
		for (int i = 1; i < paramLen; i++) { // first character is not a '/'
			if (uri.charAt(i) == '/' && uri.charAt(i - 1) != '\\') {
				segmentNumber++;

				String segment = uri.substring(start, i);
				if (segmentNumber > 1 && segment.equals("."))
					return false; // the URI contains the "." node name at a
									// position other than the beginning of the
									// URI

				try {
					// also checks that the segment is valid
					getSegmentLength(segment);
				} catch (IllegalArgumentException e) {
					return false;
				}

				// the extra byte is for the separator '/' (will be deducted
				// again for the last segment of the URI)
				start = i + 1;
			}
		}

		return true;
	}

	// Non-public fields and methods

	// package private method for testing purposes
	static String mangle(String nodeName, int limit) {
		if (nodeName == null)
			throw new NullPointerException("The 'nodeName' parameter must not be null.");

		if (nodeName.equals(""))
			throw new IllegalArgumentException("The 'nodeName' parameter must not be empty.");

		if (nodeName.length() > limit)
			// create node name hash
			return getHash(nodeName);

		// escape any '/' and '\' characters in the node name
		StringBuffer nameBuffer = new StringBuffer(nodeName);
		for (int i = 0; i < nameBuffer.length(); i++)
			// 'i' can increase in loop
			if (nameBuffer.charAt(i) == '\\' || nameBuffer.charAt(i) == '/')
				nameBuffer.insert(i++, '\\');

		return nameBuffer.toString();
	}

	private static String getHash(String from) {
		byte[] bytes;
		try {
			bytes = from.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// There's no way UTF-8 encoding is not implemented...
			throw new IllegalStateException("there's no UTF-8 encoder here!");
		}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Can't get an instance of a SHA MessageDigest provider!");
		}
		byte[] digest = md.digest(bytes);

		// very dumb base64 encoder code. There is no need for multiple lines
		// or trailing '='-s....
		// also, we hardcoded the fact that sha-1 digests are 20 bytes long
		StringBuffer sb = new StringBuffer(digest.length * 2);
		for (int i = 0; i < 6; i++) {
			int d0 = digest[i * 3] & 0xff;
			int d1 = digest[i * 3 + 1] & 0xff;
			int d2 = digest[i * 3 + 2] & 0xff;
			sb.append(BASE_64_TABLE[d0 >> 2]);
			sb.append(BASE_64_TABLE[(d0 << 4 | d1 >> 4) & 63]);
			sb.append(BASE_64_TABLE[(d1 << 2 | d2 >> 6) & 63]);
			sb.append(BASE_64_TABLE[d2 & 63]);
		}
		int d0 = digest[18] & 0xff;
		int d1 = digest[19] & 0xff;
		sb.append(BASE_64_TABLE[d0 >> 2]);
		sb.append(BASE_64_TABLE[(d0 << 4 | d1 >> 4) & 63]);
		sb.append(BASE_64_TABLE[(d1 << 2) & 63]);

		return sb.toString();
	}

}
