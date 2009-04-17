/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.util.metatype;

import org.osgi.framework.*;
import org.osgi.service.metatype.*;

import org.osgi.service.metatype.*;
import java.io.*;
import java.util.*;
import java.net.URL;

/**
 * Implementation of the ObjectClassDefinition interface.
 *
 */
public class OCD implements ObjectClassDefinition {


  String id;
  String name;
  String desc;
  List optAttrs;
  List reqAttrs;

  String      iconURL = null;

  int  maxInstances = 1;

  /**
   * Create a new, empty ObjectClassDefinition.
   *
   * @param id unique ID of the definition.
   * @param name human-readable name of the definition. If set to 
   *         <tt>null</tt>,
   *        use <i>id</i> as name.
   * @param desc human-readable description of the definition
   * @throws IllegalArgumentException if <i>id</i> is <null> or empty
   */
  public OCD(String id, 
	     String name, 
	     String desc) {

    if(id == null || "".equals(id)) {
      throw new IllegalArgumentException("Id must not be null or empty");
    }

    this.id    = id;
    this.name  = name != null ? name : id;
    this.desc  = desc;
    this.optAttrs = new ArrayList();
    this.reqAttrs = new ArrayList();
  }

  /**
   * Creates an OCD with attribute definitions from an existing 
   * dictionary.
   *
   * @param id unique ID of the definition.
   * @param name human-readable name of the definition. If set to <tt>null</tt>,
   *       use <i>id</i> as name.
   * @param desc human-readable description of the definition
   * @param props set of key value pairs used for attribute definitions.
   *        all entries in <i>props</i> will be set as REQUIRED
   *        atttributes.
   * @throws IllegalArgumentException if <i>id</i> is <null> or empty
   *
   */
  public OCD(String id, 
	     String name, 
	     String desc,
	     Dictionary props) {
    this(id, name, desc);

    //    System.out.println("OCD " + id + ", props=" + props);
    for(Enumeration e = props.keys(); e.hasMoreElements();) {
      String key = (String)e.nextElement();
      if("service.pid".equals(key.toLowerCase())) {
	continue;
      }
      if("service.factorypid".equals(key.toLowerCase())) {
	continue;
      }
      Object val = props.get(key);

      int card = 0;
      int type = AD.getType(val);

      if(val instanceof Vector) {
	card = Integer.MIN_VALUE;
      } else if(val.getClass().isArray()) {
	card = Integer.MAX_VALUE;
      }

      AD ad = new AD(key, type, card, key, 
		     card == 0
		     ?  new String[] { AD.toString(val) }
		     : null);
      

      //      System.out.println(" add " + ad);
      add(ad, REQUIRED);
    }

  }

  /**
   * Add an attribute definition
   *
   * @param attr definition to add
   * @param filter either OPTIONAL or REQUIRED
   * @throws Illegalargumentexception if filter is not OPTIONAL or REQUIRED
   */
  public void add(AttributeDefinition attr, int filter) {
    switch(filter) {
    case OPTIONAL:
      optAttrs.add(attr);
      break;
    case REQUIRED:
      reqAttrs.add(attr);
      break;
    default:
      throw new IllegalArgumentException("Unsupported filter=" + filter);
    }
  }

  public AttributeDefinition[] getAttributeDefinitions(int filter) {
    AttributeDefinition[] attrs = null;
    switch(filter) {
    case ALL: {
      ArrayList all = new ArrayList();
      all.addAll(reqAttrs);
      all.addAll(optAttrs);
      AttributeDefinition[] ads = new AttributeDefinition[all.size()];
      all.toArray(ads);
      return ads;
    }
    case REQUIRED: {
      AttributeDefinition[] ads = new AttributeDefinition[reqAttrs.size()];
      reqAttrs.toArray(ads);
      return ads;
    }
    case OPTIONAL: {
      AttributeDefinition[] ads = new AttributeDefinition[optAttrs.size()];
      optAttrs.toArray(ads);
      return ads;
    }
    default:
      throw new IllegalArgumentException("Unsupported filter=" + filter);
    }
  }

  /**
   * Get description of OCD. 
   */
  public String getDescription() {
    return desc;
  }
  
  /**
   * Get icon stream using the <code>getIconURL</code> URL.
   *
   * @param size icon size hint is ignored.
   */
  public InputStream getIcon(int size) throws IOException {
    if(iconURL != null) {
      URL url = new URL(iconURL);
      return url.openStream();
    } else {
      return null;
    }

  }

  /**
   * Get maximum number of instances. Services return 1, factories
   * &gt; 1.
   */
  public int getMaxInstances() {
    return maxInstances;
  }
  /**
   * Set URL to icon
   */
  public void setIconURL(String url) {
    iconURL = url;
  }

  /**
   * Get URL to icon data
   */
  public String getIconURL() {
    return iconURL;
  }


  public String getID() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("OCD[id=" + id);
    sb.append("\nname=" + name);
    sb.append("\ndesc=" + desc);
    sb.append("\nregAttrs=");
    for(Iterator it = reqAttrs.iterator(); it.hasNext(); ) {
      AttributeDefinition attr = (AttributeDefinition)it.next();
      sb.append(attr);
      if(it.hasNext()) {
	sb.append("\n");
      }
    }
    sb.append("\noptAttrs=");
    for(Iterator it = optAttrs.iterator(); it.hasNext(); ) {
      AttributeDefinition attr = (AttributeDefinition)it.next();
      sb.append(attr);
      if(it.hasNext()) {
	sb.append("\n");
      }
    }
    sb.append("\n/OCD]");
    return sb.toString();
  }
}
