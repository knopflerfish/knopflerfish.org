/*
 * $Header: /home/wistrand/cvs/knopflerfish.org/osgi/bundles/wireadmin/src/org/osgi/service/wireadmin/BasicEnvelope.java,v 1.1.1.1 2004/03/05 20:35:17 wistrand Exp $
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
package org.osgi.service.wireadmin;

/**
 * <tt>BasicEnvelope</tt> is an implementation of the {@link Envelope} interface
 *
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */
public class BasicEnvelope implements Envelope {
	Object 			value;
	Object 			identification;
	String 			scope;

	/**
	 * Constructor.
	 *
	 * @param value Content of this envelope, may be <tt>null</tt>.
	 * @param identifying Identifying object for this <tt>Envelope</tt> object, must not be <tt>null</tt>
	 * @param scope Scope name for this object, must not be <tt>null</tt>
	 * @see Envelope
	 */
	public BasicEnvelope(Object value, Object identification, String scope) {
		this.value = value;
		this.identification = identification;
		this.scope = scope;
	}
	
		
	/**
	 * @see org.osgi.service.wireadmin.Envelope#getValue()
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @see org.osgi.service.wireadmin.Envelope#getIdentification()
	 */
	public Object getIdentification() {
		return identification;
	}

	/**
	 * @see org.osgi.service.wireadmin.Envelope#getScope()
	 */
	public String getScope() {
		return scope;
	}


}
