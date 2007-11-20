/*
 * Generic Metadata XML Parser
 * Copyright (c) 2004, Didier Donsez
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Didier Donsez <didier.donsez@imag.fr>
 * Contributor(s):
 *
**/
package fr.imag.adele.metadataparser;

/**
 * This class provides methods to process class name
 */

public class ClassUtility {

	/**
	 * This method capitalizes the first character in the provided string.
	 * @return resulted string
	 */
	public static String capitalize(String name) {

		int len=name.length();
		StringBuffer sb=new StringBuffer(len);
		boolean setCap=true;
		for(int i=0; i<len; i++){
			char c=name.charAt(i);
			if(c=='-' || c=='_') {
				setCap=true;			
			} else {
				if(setCap){
					sb.append(Character.toUpperCase(c));
					setCap=false;
				} else {
					sb.append(c);
				}
			}
		} 
 
		return sb.toString();
	}

	/**
	 * This method capitalizes all characters in the provided string.
	 * @return resulted string
	 */
	public static String finalstaticOf(String membername) {
		int len=membername.length();
		StringBuffer sb=new StringBuffer(len+2);
		for(int i=0; i<len; i++){
			char c=membername.charAt(i);
			if(Character.isLowerCase(c) ) {
				sb.append(Character.toUpperCase(c));
			} else if(Character.isUpperCase(c) ) {
				sb.append('_').append(c);
			} else {
				sb.append(c);				
			}
		} 
 
		return sb.toString();
	}
	
	/**
	 * This method returns the package name in a full class name
	 * @return resulted string
	 */
	public static String packageOf(String fullclassname) {
		int index=fullclassname.lastIndexOf(".");
		if(index>0) {
			return fullclassname.substring(0,index);
		} else {
			return "";	
		}
	}

	/**
	 * This method returns the package name in a full class name
	 * @return resulted string
	 */
	public static String classOf(String fullclassname) {
		int index=fullclassname.lastIndexOf(".");
		if(index>0) {
			return fullclassname.substring(index+1);
		} else {
			return fullclassname;	
		}
	}
}