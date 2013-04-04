/*
 * Copyright (c) 2006-2013, KNOPFLERFISH project
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

/**
 * @author Philippe Laporte
 */

//TODO lots of optimization to be done, both in speed and storage

package org.knopflerfish.util.metatype;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

//TODO lots of optimization

public class BundleMetaTypeResource
  implements MetaTypeInformation
{

  private final Bundle bundle;

  private final Vector<MetaData> metaDatas = new Vector<MetaData>();

  // id -> MetaData
  private final Hashtable<String, MetaData> pids = new Hashtable<String, MetaData>();
  private final Hashtable<String, MetaData> factoryPids = new Hashtable<String, MetaData>();

  private String[] locales;

  public BundleMetaTypeResource(Bundle bundle)
  {
    this.bundle = bundle;
  }

  public Bundle getBundle()
  {
    return bundle;
  }

  public void addMetaData(MetaData md)
  {
    metaDatas.add(md);
  }

  public String[] getFactoryPids()
  {
    final Vector<String> factoryPidsV = new Vector<String>();
    factoryPidsV.addAll(factoryPids.keySet());
    return factoryPidsV.toArray(new String[factoryPidsV.size()]);
  }

  public String[] getPids()
  {
    final Vector<String> pidsV = new Vector<String>();
    pidsV.addAll(pids.keySet());
    return pidsV.toArray(new String[pidsV.size()]);
  }

  public void prepare()
  {
    final Set<String> locales = new HashSet<String>();
    for (final MetaData md : metaDatas) {
      for (final String pid : md.getPids()) {
        pids.put(pid, md);
      }
      for (final String fpid : md.getFactoryPids()) {
        factoryPids.put(fpid, md);
      }
      final String[] newLocales = md.getLocales();
      if (newLocales!=null) {
        for (final String locale : newLocales) {
          locales.add(locale);
        }
      }
    }
    this.locales = locales.toArray(new String[locales.size()]);
  }

  public String[] getLocales()
  {
    return locales;
  }

  public ObjectClassDefinition getObjectClassDefinition(String id, String locale)
  {
    MetaData md;
    md = pids.get(id);
    if (md == null) {
      md = factoryPids.get(id);
    }
    if (md == null) {
      final Set<String> allIDs = new TreeSet<String>();
      allIDs.addAll(pids.keySet());
      allIDs.addAll(factoryPids.keySet());
      throw new IllegalArgumentException("no information available for id "
                                         + id +". Available ids: " +allIDs);
    }
    if (locale == null) {
      locale = Locale.getDefault().toString();
    }

    return md.getOCD(id, locale);
  }

  void mergeWith(BundleMetaTypeResource other)
  {
    if (other == null) {
      return;
    }

    if (bundle != other.bundle) {
      return;
    }

    final Enumeration<MetaData> enume = other.metaDatas.elements();
    while (enume.hasMoreElements()) {
      metaDatas.add(enume.nextElement());
    }
  }

}

// TODO localization specs not definite yet

class MetaData
{

  private final String localizationFileBaseName;

  private final Hashtable<String, ObjectClassDefinition> pids = new Hashtable<String, ObjectClassDefinition>();
  private final Hashtable<String, ObjectClassDefinition> factoryPids = new Hashtable<String, ObjectClassDefinition>();

  private final Hashtable<String, ObjectClassDefinition> OCDs = new Hashtable<String, ObjectClassDefinition>();

  private String[] locales;

  private final Bundle bundle;

  final static private String locBaseDir =
    Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14);

  // TODO check for Attributes manifest's Constants.BUNDLE_LOCALIZATION

  public MetaData(String localizationFile, Bundle bundle)
  {
    this.localizationFileBaseName = localizationFile;
    this.bundle = bundle;
  }

  public MetaData(Bundle bundle)
  {
    this.localizationFileBaseName = "bundle";
    this.bundle = bundle;
  }

  public void prepare()
  {
    loadLocales();
  }

  public String[] getLocales()
  {
    return locales;
  }

  public Set<String> getFactoryPids()
  {
    return factoryPids.keySet();
  }

  public Set<String> getPids()
  {
    return pids.keySet();
  }

  public void addOCD(ObjectClassDefinition ocd)
  {
    OCDs.put(ocd.getID(), ocd);
  }

  // TODO locale finding rules not definite

  ObjectClassDefinition getOCD(String id, String locale)
  {
    OCD ocd = (OCD) pids.get(id);
    if (ocd == null) {
      ocd = (OCD) factoryPids.get(id);
    }
    if (ocd == null) {
      return null;
    }

    Enumeration<URL> url;
    int underscore;

    if (locale == null) {
      locale = Locale.getDefault().toString();
    }

    url =
      findEntries(locBaseDir, localizationFileBaseName + "_" + locale
                              + ".properties");
    if (url == null) {
      underscore = locale.lastIndexOf('_');
      if (underscore > 0) {
        locale = locale.substring(0, underscore - 1);
      }
      url =
        findEntries(locBaseDir, localizationFileBaseName + "_" + locale
                                + ".properties");
      if (url == null) {
        underscore = locale.lastIndexOf('_');
        if (underscore > 0) {
          locale = locale.substring(0, underscore - 1);
        }
        url =
          findEntries(locBaseDir, localizationFileBaseName + "_" + locale
                                  + ".properties");
      }
      // lastly
      if (url == null) {
        url = findEntries(locBaseDir, localizationFileBaseName + ".properties");
      }
    }

    if (url != null) {
      ocd.localize(loadLocaleEntries(url.nextElement()));
    }

    return ocd;
  }

  public void designate(String factoryPid,
                        String pid,
                        String ocdref,
                        Configuration conf,
                        Vector<?> currentAttributes)
  {

    ObjectClassDefinition ocd;
    ocd = OCDs.get(ocdref);

    if (ocd != null) {
      if (conf != null && currentAttributes.size() > 0) {
        final AttributeDefinition[] attrDefs =
          ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        final Hashtable<String, AttributeDefinition> ADs = new Hashtable<String, AttributeDefinition>(); // id is key
        for (final AttributeDefinition ad : attrDefs) {
          ADs.put(ad.getID(), ad);
        }

        final Dictionary<String, Object> props = conf.getProperties();

        final Enumeration<?> attrsAssigns = currentAttributes.elements();
        while (attrsAssigns.hasMoreElements()) {
          final AE ae = (AE) attrsAssigns.nextElement();
          final AttributeDefinition ad =
            ADs.get(ae.adref);

          if (ad != null) {
            Object value = null;
            final int card = ad.getCardinality();

            switch (ad.getType()) {
            case AttributeDefinition.STRING:
              if (card < 0) {
                final Vector<String> res = new Vector<String>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add((String)values.nextElement());
                }
                value = res;
              } else if (card > 0) {
                final String[] res = new String[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] = (String) values.nextElement();
                }
                value = res;
              } else {
                value = ae.values.elementAt(0);
              }
              break;
            case AttributeDefinition.BOOLEAN:
              if (card < 0) {
                final Vector<Boolean> res = new Vector<Boolean>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add(Boolean.valueOf((String) values.nextElement()));
                }
                value = res;
              } else if (card > 0) {
                final Boolean[] res = new Boolean[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] = Boolean.valueOf((String) values.nextElement());
                }
                value = res;
              } else {
                value = Boolean.valueOf(ae.values.elementAt(0));
              }
              break;
            case AttributeDefinition.BYTE:
              if (card < 0) {
                final Vector<Byte> res = new Vector<Byte>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add(Byte.valueOf((String) values.nextElement()));
                }
                value = res;
              } else if (card > 0) {
                final Byte[] res = new Byte[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] = Byte.valueOf((String) values.nextElement());
                }
                value = res;
              } else {
                value = Byte.valueOf(ae.values.elementAt(0));
              }
              break;
            case AttributeDefinition.DOUBLE:
              if (card < 0) {
                final Vector<Double> res = new Vector<Double>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add(Double.valueOf((String) values.nextElement()));
                }
                value = res;
              } else if (card > 0) {
                final Double[] res = new Double[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] = Double.valueOf((String) values.nextElement());
                }
                value = res;
              } else {
                value = Double.valueOf(ae.values.elementAt(0));
              }
              break;
            case AttributeDefinition.FLOAT:
              if (card < 0) {
                final Vector<Float> res = new Vector<Float>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add(Float.valueOf((String) values.nextElement()));
                }
                value = res;
              } else if (card > 0) {
                final Float[] res = new Float[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] = Float.valueOf((String) values.nextElement());
                }
                value = res;
              } else {
                value = Float.valueOf(ae.values.elementAt(0));
              }
              break;
            case AttributeDefinition.INTEGER:
              if (card < 0) {
                final Vector<Integer> res = new Vector<Integer>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add(Integer.valueOf((String) values.nextElement()));
                }
                value = res;
              } else if (card > 0) {
                final Integer[] res = new Integer[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] = Integer.valueOf((String) values.nextElement());
                }
                value = res;
              } else {
                value = Integer.valueOf(ae.values.elementAt(0));
              }
              break;
            case AttributeDefinition.LONG:
              if (card < 0) {
                final Vector<Long> res = new Vector<Long>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add(Long.valueOf((String) values.nextElement()));
                }
                value = res;
              } else if (card > 0) {
                final Long[] res = new Long[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] = Long.valueOf((String) values.nextElement());
                }
                value = res;
              } else {
                value = Long.valueOf(ae.values.elementAt(0));
              }
              break;
            case AttributeDefinition.SHORT:
              if (card < 0) {
                final Vector<Short> res = new Vector<Short>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add(Short.valueOf((String) values.nextElement()));
                }
                value = res;
              } else if (card > 0) {
                final Short[] res = new Short[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] = Short.valueOf((String) values.nextElement());
                }
                value = res;
              } else {
                value = Short.valueOf(ae.values.elementAt(0));
              }
              break;
            case AttributeDefinition.CHARACTER:
              if (card < 0) {
                final Vector<Character> res = new Vector<Character>(-1 * card);
                final Enumeration<?> values = ae.values.elements();
                while (values.hasMoreElements()) {
                  res.add(new Character(((String) values.nextElement())
                      .charAt(0)));
                }
                value = res;
              } else if (card > 0) {
                final Character[] res = new Character[card];
                final Enumeration<?> values = ae.values.elements();
                for (int i = 0; values.hasMoreElements(); i++) {
                  res[i] =
                    new Character(((String) values.nextElement()).charAt(0));
                }
                value = res;
              } else {
                value =
                  new Character(ae.values.elementAt(0).charAt(0));
              }
              break;
            }

            /*
             * TODO code along these lines would be less repetitive Class clazz
             * = null;
             *
             * switch(ad.getType()){ case AttributeDefinition.STRING: clazz =
             * null; break; case AttributeDefinition.BOOLEAN: clazz =
             * Boolean.class; break; case AttributeDefinition.BYTE: clazz =
             * Byte.class; break; case AttributeDefinition.DOUBLE: clazz =
             * Double.class; break; case AttributeDefinition.FLOAT: clazz =
             * Float.class; break; case AttributeDefinition.INTEGER: clazz =
             * Integer.class; break; case AttributeDefinition.LONG: clazz =
             * Long.class; break; case AttributeDefinition.SHORT: clazz =
             * Short.class; break; case AttributeDefinition.CHARACTER: clazz =
             * Character.class;
             *
             * break;
             *
             * }
             *
             * Method method = null; try{ method = clazz.getMethod("valueOf",
             * new Class[]{String.class}); } catch(NoSuchMethodException e){
             *
             * }
             *
             *
             *
             *
             * Boolean val = null;
             *
             * try{ val = (Boolean) method.invoke(null, new Object[]{"true"}); }
             * catch(IllegalAccessException e){
             *
             * } catch(InvocationTargetException e){
             *
             * }
             */
            if (value != null) {
              props.put(ad.getName(), value);
            }
          }

        } // while

        try {
          conf.update(props);
        } catch (final IOException ioe) {
        }

      } // if

      if (factoryPid != null) {
        factoryPids.put(factoryPid, ocd);
      } else {
        pids.put(pid, ocd);
      }
    }

    // TODO enforce bundleLocation
  }

  private void loadLocales()
  {
    //final String x = Locale.getDefault().toString();
    final Vector<String> localesV = new Vector<String>();

    final Enumeration<URL> localizationFiles =
      findEntries(locBaseDir, localizationFileBaseName, ".properties");

    if (localizationFiles != null) {
      while (localizationFiles.hasMoreElements()) {
        final URL url = localizationFiles.nextElement();
        String fileName = url.getFile().substring(15);
        if (fileName.length() == (localizationFileBaseName + ".properties")
            .length()) {
          continue;
        } else {
          final int dot = fileName.lastIndexOf('.');
          fileName = fileName.substring(0, dot);
          final int underscore = fileName.indexOf('_');
          fileName = fileName.substring(underscore + 1);
          localesV.add(fileName);
        }
      }
      locales = localesV.toArray(new String[localesV.size()]);
    }
  }

  // Find files in the bundle without triggering resolve of it.
  private Enumeration<URL> findEntries(String baseDir, String fileName) {
    Enumeration<URL> res;
    if (bundle.getState() == Bundle.INSTALLED) {
      final Enumeration<String> p = bundle.getEntryPaths(baseDir);
      if (p != null) {
        final Vector<URL> tmp = new Vector<URL>();
        while (p.hasMoreElements()) {
          final String path = p.nextElement();
          final int lastSlash = path.lastIndexOf('/');
          if (lastSlash > 0) {
            final String name = path.substring(lastSlash) + 1;
            if (name.equals(fileName)) {
              tmp.addElement(bundle.getEntry(path));
            }
          }
        }
        res = tmp.elements();
      } else {
        res = null;
      }
    } else {
      res =
        bundle.findEntries(locBaseDir, fileName, false);
    }
    return res;
  }

  // Find files in the bundle without triggering resolve of it.
  private Enumeration<URL> findEntries(String baseDir,
                                       String fileNamePrefix,
                                       String fileNameSuffix)
  {
    Enumeration<URL> res;
    if (bundle.getState() == Bundle.INSTALLED) {
      final Enumeration<String> p = bundle.getEntryPaths(baseDir);
      if (p != null) {
        final Vector<URL> tmp = new Vector<URL>();
        while (p.hasMoreElements()) {
          final String path = p.nextElement();
          final int lastSlash = path.lastIndexOf('/');
          if (lastSlash > 0) {
            final String name = path.substring(lastSlash) + 1;
            if (name.startsWith(fileNamePrefix)
                && name.endsWith(fileNameSuffix)) {
              tmp.addElement(bundle.getEntry(path));
            }
          }
        }
        res = tmp.elements();
      } else {
        res = null;
      }
    } else {
      res =
        bundle.findEntries(locBaseDir, fileNamePrefix + "*" + fileNameSuffix,
                           false);
    }
    return res;
  }

  private Properties loadLocaleEntries(URL url)
  {
    final Properties entries = new Properties();

    try {
      final InputStream is = url.openStream();

      entries.load(is);
    } catch (final IOException e) {
      return entries;
    }
    return entries;
  }

}
