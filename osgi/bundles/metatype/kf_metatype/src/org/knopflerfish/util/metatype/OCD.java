/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Implementation of the {@link ObjectClassDefinition} interface.
 */
public class OCD
  implements ObjectClassDefinition
{

  URL sourceURL;
  String id;
  String name;
  String localized_name;
  String desc;
  String localized_desc;
  List<AD> optAttrs;
  List<AD> reqAttrs;
  List<AD> localized_optAttrs;
  List<AD> localized_reqAttrs;
  Hashtable<Integer, String> icons = new Hashtable<>();
  Hashtable<Integer, String> localized_icons = null;
  int maxInstances = 1;

  /**
   * Create a new, empty {@link ObjectClassDefinition}.
   *
   * @param id
   *          unique ID of the definition.
   * @param name
   *          human-readable name of the definition. If set to <tt>null</tt>,
   *          use <i>id</i> as name.
   * @param desc
   *          human-readable description of the definition
   * @throws IllegalArgumentException
   *           if <i>id</i> is <null> or empty
   */
  public OCD(String id, String name, String desc, URL sourceURL)
  {

    if (id == null || "".equals(id)) {
      throw new IllegalArgumentException("Id must not be null or empty");
    }

    this.id = id;
    this.name = name != null ? name : id;
    this.desc = desc;
    this.optAttrs = new ArrayList<>();
    this.reqAttrs = new ArrayList<>();
    this.sourceURL = sourceURL;
  }

  /**
   * Creates an OCD with attribute definitions from an existing dictionary.
   *
   * @param id
   *          unique ID of the definition.
   * @param name
   *          human-readable name of the definition. If set to <tt>null</tt>,
   *          use <i>id</i> as name.
   * @param desc
   *          human-readable description of the definition
   * @param props
   *          set of key value pairs used for attribute definitions. all entries
   *          in <i>props</i> will be set as REQUIRED attributes.
   * @throws IllegalArgumentException
   *           if <i>id</i> is <null> or empty
   *
   */
  public OCD(String id, String name, String desc, Dictionary<String, ?> props)
  {
    this(id, name, desc, (URL) null);

    // System.out.println("OCD " + id + ", props=" + props);
    for (final Enumeration<String> e = props.keys(); e.hasMoreElements();) {
      final String key = e.nextElement();
      if ("service.pid".equals(key.toLowerCase())) {
        continue;
      }
      if ("service.factorypid".equals(key.toLowerCase())) {
        continue;
      }
      final Object val = props.get(key);

      int card = 0;
      final int type = AD.getType(val);

      if (val instanceof Vector) {
        card = Integer.MIN_VALUE;
      } else if (val.getClass().isArray()) {
        card = Integer.MAX_VALUE;
      }

      final AD ad =
        new AD(key, type, card, key, card == 0
          ? new String[] { AD.toString(val) }
          : null);

      // System.out.println(" add " + ad);
      add(ad, REQUIRED);
    }

  }

  /**
   * Add an attribute definition
   *
   * @param attr
   *          definition to add
   * @param filter
   *          either OPTIONAL or REQUIRED
   * @throws IllegalArgumentException
   *           if filter is not OPTIONAL or REQUIRED
   */
  public void add(AD attr, int filter)
  {
    switch (filter) {
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

  public AttributeDefinition[] getAttributeDefinitions(int filter)
  {
    switch (filter) {
    case ALL: {
      final ArrayList<AD> all = new ArrayList<>();
      if (localized_optAttrs == null) {
        all.addAll(reqAttrs);
        all.addAll(optAttrs);
      } else {
        all.addAll(localized_reqAttrs);
        all.addAll(localized_optAttrs);
      }
      final AttributeDefinition[] ads = new AttributeDefinition[all.size()];
      all.toArray(ads);
      return ads;
    }
    case REQUIRED: {
      final AttributeDefinition[] ads =
        new AttributeDefinition[reqAttrs.size()];
      if (localized_reqAttrs == null) {
        reqAttrs.toArray(ads);
      } else {
        localized_reqAttrs.toArray(ads);
      }
      return ads;
    }
    case OPTIONAL: {
      final AttributeDefinition[] ads =
        new AttributeDefinition[optAttrs.size()];
      if (localized_optAttrs == null) {
        optAttrs.toArray(ads);
      } else {
        localized_optAttrs.toArray(ads);
      }
      return ads;
    }
    default:
      throw new IllegalArgumentException("Unsupported filter=" + filter);
    }
  }

  /**
   * Get description of OCD.
   */
  public String getDescription()
  {
    if (localized_desc != null) {
      return localized_desc;
    } else {
      return desc;
    }
  }

  /**
   * This code handles multiple icon sizes but the specification currently only
   * allows on size.
   *
   * @param size
   *          Size of icon requested, if size is 0 return largest icon.
   */
  String getIconURL(int size)
  {
    final Hashtable<?, ?> itab = (localized_icons != null) ? localized_icons : icons;
    if (size == 0) {
      for (final Enumeration<?> keys = itab.keys(); keys.hasMoreElements();) {
        final int i = (Integer) keys.nextElement();
        if (size < i) {
          size = i;
        }
      }
    }
    return (String) itab.get(size);
  }

  /**
   * This code handles multiple icon sizes but the specification currently only
   * allows on size.
   *
   * @param size
   *          Size of icon requested, if size is 0 return largest icon.
   */
  public InputStream getIcon(int size)
      throws IOException
  {
    String url = getIconURL(size);
    if (url == null) {
      url = getIconURL(0);
    }
    if (url != null) {
      if (sourceURL != null) {
        return new URL(new URL(sourceURL, "/"), url).openStream();
      } else {
        return new URL(url).openStream();
      }
    }
    return null;
  }

  /**
   * Get maximum number of instances. Services return 1, factories &gt; 1.
   */
  public int getMaxInstances()
  {
    return maxInstances;
  }

  /**
   * Set URL to icon
   */
  public void setIconURL(String url)
  {
    icons.put(Integer.MAX_VALUE, url);
  }

  public void addIcon(int size, String url)
  {
    icons.put(size, url);
  }

  public String getID()
  {
    return id;
  }

  public String getName()
  {
    if (localized_name != null) {
      return localized_name;
    } else {
      return name;
    }
  }

  void localize(Properties properties)
  {
    if (properties != null) {
      localized_name = localizeName(name, properties);
      localized_desc = localizeName(desc, properties);

      localized_icons = new Hashtable<>();
      for (final Entry<Integer, String> entry : icons.entrySet()) {
        localized_icons.put(entry.getKey(),
                            localizeName(entry.getValue(), properties));
      }

      localized_reqAttrs = new ArrayList<>();
      for (final AD attr : reqAttrs) {
        localized_reqAttrs.add(attr.localize(properties));
      }

      localized_optAttrs = new ArrayList<>();
      for (final AD attr : optAttrs) {
        localized_optAttrs.add(attr.localize(properties));
      }

    }
  }

  String localizeName(String name, Dictionary<?, ?> dict)
  {
    if (name.startsWith("%")) {
      String sub;
      if ((sub = (String) dict.get(name.substring(1))) != null) {
        return sub;
      } else {
        return name;
      }
    }
    return name;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();

    sb.append("OCD[id=").append(id);
    sb.append("\nname=").append(name);
    sb.append("\ndesc=").append(desc);
    sb.append("\nregAttrs=");
    for (final Iterator<AD> it = reqAttrs.iterator(); it.hasNext();) {
      final AttributeDefinition attr = it.next();
      sb.append(attr);
      if (it.hasNext()) {
        sb.append("\n");
      }
    }
    sb.append("\noptAttrs=");
    for (final Iterator<AD> it = optAttrs.iterator(); it.hasNext();) {
      final AttributeDefinition attr = it.next();
      sb.append(attr);
      if (it.hasNext()) {
        sb.append("\n");
      }
    }
    sb.append("\n/OCD]");
    return sb.toString();
  }
}
