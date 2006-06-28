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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class MultivalueMap implements Map {
	private Map m_map = null;

	public MultivalueMap() {
		m_map = new HashMap();
	}

	public MultivalueMap(Map map) {
		m_map = map;
	}

	/**
	 * @see java.util.Map#size()
	 */
	public int size() {
		return m_map.size();
	}

	/**
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		m_map.clear();
	}

	/**
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return m_map.isEmpty();
	}

	/**
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object arg0) {
		return m_map.containsKey(arg0);
	}

	/**
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object arg0) {
		return false;
	}

	/**
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		return null;
	}

	/**
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map arg0) {
	}

	/**
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		return m_map.entrySet();
	}

	/**
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		return m_map.keySet();
	}

	/**
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
		return m_map.get(key);
	}

	/**
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object arg0) {
		return m_map.remove(arg0);
	}

	/**
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
		Object prev = m_map.get(key);
		if (prev == null) {
			m_map.put(key, value);
			return value;
		} else {
			// TODO if element have the same type ?
			if (prev instanceof List) {
				((List) prev).add(value);
				return prev;
			} else {
				List list = new ArrayList();
				list.add(prev);
				list.add(value);
				m_map.put(key, list);
				return list;
			}
		}
	}

	public String toString() {
		StringBuffer sb=new StringBuffer();
		sb.append("[MultivalueMap:");
		if(m_map.isEmpty()) {
			sb.append("empty");
		} else {
			Set keys=m_map.keySet();
			Iterator iter=keys.iterator();
			while(iter.hasNext()){
				String key=(String)iter.next();
				sb.append("\n\"").append(key).append("\":");
				sb.append(m_map.get(key).toString());		
			}
			sb.append('\n');
		}
		sb.append(']');
		return sb.toString();
	}
}