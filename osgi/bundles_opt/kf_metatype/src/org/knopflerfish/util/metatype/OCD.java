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

public class OCD implements ObjectClassDefinition {

  /**
   * Value for <tt>getDescription</tt> representing services.
   * <br>
   * Value is "service"
   */
  public static final String SERVICE = "service";

  /**
   * Value for <tt>getDescription</tt> representing factories
   * <br>
   * Value is "factory"
   */
  public static final String FACTORY = "factory";

  String id;
  String name;
  String desc;
  List optAttrs;
  List reqAttrs;

  /**
   * @param desc To able to differentiate between
   *             services and factories, description must be either
   *             OCD.SERVICE or OCD.FACTORY
   */
  public OCD(String id, 
	     String name, 
	     String desc) {

    if(id == null || "".equals(id)) {
      throw new IllegalArgumentException("Id must not be null or empty");
    }

    if(!(SERVICE.equals(desc) || FACTORY.equals(desc))) {
      throw new IllegalArgumentException("Description must be either '" + 
					 SERVICE + "' or '"  +
					 FACTORY);
    }

    this.id    = id;
    this.name  = name;
    this.desc  = desc;
    this.optAttrs = new ArrayList();
    this.reqAttrs = new ArrayList();
  }

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
   * Get description of OCD. To able to differentiate between
   * services and factories, this value should be either
   * OCD.SERVICE or OCD.FACTORY
   */
  public String getDescription() {
    return desc;
  }
  
  public InputStream getIcon(int size) {
    return null;
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
