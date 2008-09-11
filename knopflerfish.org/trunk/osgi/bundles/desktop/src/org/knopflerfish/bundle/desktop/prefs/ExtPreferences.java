/*
 * Copyright (c) 2008, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.knopflerfish.bundle.desktop.prefs;

import java.util.prefs.*;
import java.util.Map;

/**
 * Extension interface to java.util.Preferences
 *
 * <p>
 * Since the standard Preferences API does not support any 
 * metadata, this interface can be implemented to add 
 * information as type, description and default value.
 * </p>
 */
public interface ExtPreferences {

  public static final String TYPE_INT     = Integer.TYPE.getName();
  public static final String TYPE_LONG    = Long.TYPE.getName();
  public static final String TYPE_BOOLEAN = Boolean.TYPE.getName();
  public static final String TYPE_DOUBLE  = Double.TYPE.getName();
  public static final String TYPE_STRING  = "String";
  public static final String TYPE_COLOR   = "Color";

  /**
   * Property name for description information.
   *
   * <p>
   * <tt>getProperty(key, PROP_DESCRIPTION, null)</tt> returns
   * a textual description, if defined.
   * </p>
   * <p>
   * Value is "desc"
   * </p>
   */
  public static final String PROP_DESC = "desc";

  /**
   * Property name for type information.
   *
   * <p>
   * <tt>getProperty(key, PROP_TYPE, null)</tt> returns
   * one of the <tt>TYPE_xx</tt> types, if defined.
   * </p>
   * <p>
   * Value is "type"
   * </p>
   */
  public static final String PROP_TYPE        = "type";

  public static final String PROP_RESOLVED    = "resolved";

  /**
   * Returns an extended property of a preference node's key
   *
   * @param key node key
   * @param propName name of extended property for the key
   *
   * @param defValue of extended property or <tt>defValue</tt> if not defined.
   */
  public String  getProperty(String key, String propName, String defValue);

  /**
   * Set an extentended property of a prefererence node's key
   *
   * @param key node key
   * @param propName name of extended property for the key
   * @param val new value for the propName extended property
   */
  public void    setProperty(String key, String propName, String val);

  /**
   * Get array of property names.
   */
  public String[] getExtPropNames(String key);
}
