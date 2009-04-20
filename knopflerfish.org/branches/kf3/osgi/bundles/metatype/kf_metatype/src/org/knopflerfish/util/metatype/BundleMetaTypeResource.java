/*
 * Copyright (c) 2006, KNOPFLERFISH project
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

import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Dictionary;

import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.service.metatype.AttributeDefinition;

import java.lang.reflect.*;

//TODO lots of optimization

public class BundleMetaTypeResource implements MetaTypeInformation{
	
	private Bundle bundle;
	
	private Vector metaDatas = new Vector();
	
	//id -> MetaData
	private Hashtable pids = new Hashtable();
	private Hashtable factoryPids = new Hashtable();
	
	private String[] locales;
	
	
	public BundleMetaTypeResource(Bundle bundle){
		this.bundle = bundle;
	}

	public Bundle getBundle() {
		return bundle;
	}
	
	public void addMetaData(MetaData md){
		metaDatas.add(md);
	}

	public String[] getFactoryPids() {
		Vector factoryPidsV = new Vector();
		factoryPidsV.addAll(factoryPids.keySet());
		return  (String[]) factoryPidsV.toArray(new String[factoryPidsV.size()]);  
	}

	public String[] getPids() {
		Vector pidsV = new Vector();
		pidsV.addAll(pids.keySet());
		return (String[]) pidsV.toArray(new String[pidsV.size()]);
	}
		
	public void prepare(){
		Enumeration enume = metaDatas.elements();
		while(enume.hasMoreElements()){
			MetaData md = (MetaData) enume.nextElement();
		    Iterator it = md.getPids().iterator();
		    while(it.hasNext()){
		    	pids.put((String) it.next(), md);
		    }
			it = md.getFactoryPids().iterator();
			while(it.hasNext()){
				factoryPids.put((String) it.next(), md);
			}
			if(locales != null){
				String[] newLocales = md.getLocales();
				String[] temp = new String[locales.length + newLocales.length];
				System.arraycopy(locales, 0, temp, 0, locales.length);
				System.arraycopy(newLocales, 0, temp, locales.length, newLocales.length);
				locales = temp;
			}
			else{
				locales = md.getLocales();
			}
		}
	}

	public String[] getLocales() {
		return locales;
	}

	public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
		MetaData md;
		md = (MetaData) pids.get(id);
		if(md == null){
			md = (MetaData) factoryPids.get(id);
		}
		if(md == null){
			throw new IllegalArgumentException("no information available for id " + id);
		}
		if(locale == null){
			locale = Locale.getDefault().toString();
		}
		
		//TODO validate locale
		
		return md.getOCD(id, locale);		
	}
	
	void mergeWith(BundleMetaTypeResource other){
		if(other == null){
			return;
		}
		  
		if(bundle != other.bundle){
			return;
		}
		  
		Enumeration enume = other.metaDatas.elements();
		while(enume.hasMoreElements()){
			metaDatas.add(enume.nextElement());
		}
	}
	
}

//TODO localization specs not definite yet

class MetaData {
	
	private String localizationFileBaseName;
	
	private Hashtable pids = new Hashtable();
	private Hashtable factoryPids = new Hashtable();
	
	private Hashtable OCDs = new Hashtable();
	
	private String[] locales;
	
	private Bundle bundle;

        final static private String locBaseDir =
           Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14);

	//TODO check for Attributes manifest's Constants.BUNDLE_LOCALIZATION
	  
	public MetaData(String localizationFile, Bundle bundle){
            this.localizationFileBaseName =  localizationFile;
	    this.bundle = bundle;
	}	
	
	public MetaData(Bundle bundle){
          this.localizationFileBaseName = "bundle";
            this.bundle = bundle;	
	}	
	
	public void prepare(){
		loadLocales();
	}
	
	public String[] getLocales(){
		return locales;
	}
	
	public Set getFactoryPids() {
		return factoryPids.keySet();
	}

	public Set getPids() {
		return pids.keySet();
	}
	
	public void addOCD(ObjectClassDefinition ocd){
		OCDs.put(ocd.getID(), ocd);
	}
	
	//TODO locale finding rules not definite
	
	ObjectClassDefinition getOCD(String id, String locale){
		OCD ocd = (OCD) pids.get(id);
		if(ocd == null){
			ocd = (OCD) factoryPids.get(id);
		}
		if(ocd == null) return null;
		
		Enumeration url;
		int underscore;
	
		url = bundle.findEntries(locBaseDir, localizationFileBaseName + "_" + locale + ".properties", false);
		if(url == null){
			underscore = locale.lastIndexOf('_');
			if(underscore > 0){
				locale = locale.substring(0, underscore - 1);
			}
			url = bundle.findEntries(locBaseDir, localizationFileBaseName + "_" + locale + ".properties", false);
			if(url == null){
				underscore = locale.lastIndexOf('_');
				if(underscore > 0){
					locale = locale.substring(0, underscore - 1);
				}
				url = bundle.findEntries(locBaseDir, localizationFileBaseName + "_" + locale + ".properties", false);
			}
			locale = Locale.getDefault().toString();
			url = bundle.findEntries(locBaseDir, localizationFileBaseName + "_" + locale + ".properties", false);
			if(url == null){
				underscore = locale.lastIndexOf('_');
				if(underscore > 0){
					locale = locale.substring(0, underscore - 1);
				}
				url = bundle.findEntries(locBaseDir, localizationFileBaseName + "_" + locale + ".properties", false);
				if(url == null){
					underscore = locale.lastIndexOf('_');
					if(underscore > 0){
						locale = locale.substring(0, underscore - 1);
					}
					url = bundle.findEntries(locBaseDir, localizationFileBaseName + "_" + locale + ".properties", false);
				}
				//lastly
				if(url == null){
					url = bundle.findEntries(locBaseDir, localizationFileBaseName + ".properties", false);
				}
			}	
		}
		
                if (url != null) {
                    ocd.localize(loadLocaleEntries((URL)url.nextElement()));
                }
		
		return ocd;
	}
	
	public void designate(String factoryPid, String pid, String ocdref, 
			              Configuration conf, Vector currentAttributes){
		
		ObjectClassDefinition ocd;
		ocd = (ObjectClassDefinition) OCDs.get(ocdref);
		
		if(ocd != null){ 
			if(conf != null && currentAttributes.size() > 0){
				AttributeDefinition[] attrDefs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
				Hashtable ADs = new Hashtable(); //id is key
				for(int i = 0; i < attrDefs.length; i++){
					AttributeDefinition ad = attrDefs[i];
					ADs.put(ad.getID(), ad);
				}
				
				Dictionary props = conf.getProperties();
				
				Enumeration attrsAssigns = currentAttributes.elements();
				while(attrsAssigns.hasMoreElements()){
					AE ae = (AE) attrsAssigns.nextElement();
					AttributeDefinition ad = (AttributeDefinition) ADs.get(ae.adref);
				
					if(ad != null){
						Object value = null;
						int card = ad.getCardinality();
						
						switch(ad.getType()){
            				case AttributeDefinition.STRING:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(values.nextElement());
            						}
            					}
            					else if(card > 0){
            						value = new String[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((String[])value)[i] = (String) values.nextElement();
            						}
            					}
            					else{ 
            						value = ae.values.elementAt(0);
            					}
            					break;
            				case AttributeDefinition.BOOLEAN:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(Boolean.valueOf((String)values.nextElement()));
            						}
            					}
            					else if(card > 0){
            						value = new Boolean[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((Boolean[])value)[i] = Boolean.valueOf((String) values.nextElement());
            						}
            					}
            					else{ 
            						value = Boolean.valueOf((String) ae.values.elementAt(0));
            					}
            					break;						
            				case AttributeDefinition.BYTE:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(Byte.valueOf((String)values.nextElement()));
            						}
            					}
            					else if(card > 0){
            						value = new Byte[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((Byte[])value)[i] = Byte.valueOf((String) values.nextElement());
            						}
            					}
            					else{ 
            						value = Byte.valueOf((String) ae.values.elementAt(0));
            					}
            					break;	
            				case AttributeDefinition.DOUBLE:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(Double.valueOf((String)values.nextElement()));
            						}
            					}
            					else if(card > 0){
            						value = new Double[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((Double[])value)[i] = Double.valueOf((String) values.nextElement());
            						}
            					}
            					else{ 
            						value = Double.valueOf((String) ae.values.elementAt(0));
            					}
            					break;	
            				case AttributeDefinition.FLOAT:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(Float.valueOf((String)values.nextElement()));
            						}
            					}
            					else if(card > 0){
            						value = new Float[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((Float[])value)[i] = Float.valueOf((String) values.nextElement());
            						}
            					}
            					else{ 
            						value = Float.valueOf((String) ae.values.elementAt(0));
            					}
            					break;	
            				case AttributeDefinition.INTEGER:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(Integer.valueOf((String)values.nextElement()));
            						}
            					}
            					else if(card > 0){
            						value = new Integer[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((Integer[])value)[i] = Integer.valueOf((String) values.nextElement());
            						}
            					}
            					else{ 
            						value = Integer.valueOf((String) ae.values.elementAt(0));
            					}
            					break;	
            				case AttributeDefinition.LONG:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(Long.valueOf((String)values.nextElement()));
            						}
            					}
            					else if(card > 0){
            						value = new Long[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((Long[])value)[i] = Long.valueOf((String) values.nextElement());
            						}
            					}
            					else{ 
            						value = Long.valueOf((String) ae.values.elementAt(0));
            					}
            					break;	
            				case AttributeDefinition.SHORT:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(Short.valueOf((String)values.nextElement()));
            						}
            					}
            					else if(card > 0){
            						value = new Integer[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((Short[])value)[i] = Short.valueOf((String) values.nextElement());
            						}
            					}
            					else{ 
            						value = Short.valueOf((String) ae.values.elementAt(0));
            					}
            					break;	
            				case AttributeDefinition.CHARACTER:
            					if(card < 0){
            						value = new Vector(-1 * card);
            						Enumeration values = ae.values.elements();
            						while(values.hasMoreElements()){
            							((Vector)value).add(new Character(((String)values.nextElement()).charAt(0)));
            						}
            					}
            					else if(card > 0){
            						value = new Character[card];
            						Enumeration values = ae.values.elements();
            						for(int i = 0; values.hasMoreElements(); i++){
            							((Character[])value)[i] = new Character(((String)values.nextElement()).charAt(0));
            						}
            					}
            					else{ 
            						value = new Character(((String) ae.values.elementAt(0)).charAt(0));
            					}
            					break;								
						}
            	
					/*TODO  code along these lines would be less repetitive
					Class clazz = null;
						
					switch(ad.getType()){
        				case AttributeDefinition.STRING:
        					clazz = null;
        					break;
        				case AttributeDefinition.BOOLEAN:
        					clazz = Boolean.class;
        					break;						
        				case AttributeDefinition.BYTE:
        					clazz = Byte.class;
        					break;	
        				case AttributeDefinition.DOUBLE:
        					clazz = Double.class;
        					break;	
        				case AttributeDefinition.FLOAT:
        					clazz = Float.class;
        					break;	
        				case AttributeDefinition.INTEGER:
        					clazz = Integer.class;
        					break;	
        				case AttributeDefinition.LONG:
        					clazz = Long.class;
        					break;	
        				case AttributeDefinition.SHORT:
        					clazz = Short.class;
        					break;	
        				case AttributeDefinition.CHARACTER:
        					clazz = Character.class;
        					
        					break;
        					
					}	
					
					Method method = null;
					try{
						method = clazz.getMethod("valueOf", new Class[]{String.class});
					}
					catch(NoSuchMethodException e){
						
					}
					
					
					
					
					Boolean val = null;
					
					try{
						val = (Boolean) method.invoke(null, new Object[]{"true"});
					}
					catch(IllegalAccessException e){
						
					}
			        catch(InvocationTargetException e){
						
					}	
        			*/		
						if(value != null){
							props.put(ad.getName(), value);
						}
					}
            	
				} //while
			
				try{
					conf.update(props);
				}
				catch(IOException ioe){}
				
			} //if
				
			
			if(factoryPid != null){
				factoryPids.put(factoryPid, ocd);
			}
			else{
				pids.put(pid, ocd);
			}
		}
		
		//TODO enforce bundleLocation
	}
	
	private void loadLocales(){
            String x = Locale.getDefault().toString();
            Vector localesV = new Vector();
	    
            Enumeration localizationFiles = bundle.findEntries(locBaseDir, localizationFileBaseName + "*.properties", false);
            if(localizationFiles != null){
	    	while(localizationFiles.hasMoreElements()){ 
	    	    URL url = (URL)localizationFiles.nextElement();
                    String fileName = url.getFile().substring(15);
                    if(fileName.length() == (localizationFileBaseName + ".properties").length()){
		       	continue;
                    }
                    else {
                        int dot = fileName.lastIndexOf('.');
                        fileName= fileName.substring(0, dot);
                        int underscore = fileName.indexOf('_');
                        fileName = fileName.substring(underscore + 1);
                        localesV.add(fileName);
                    }
	    	}	
	    	locales = (String[]) localesV.toArray(new String[localesV.size()]);
            }
	}

	private Properties loadLocaleEntries(URL url){
		Properties entries = new Properties();
		
		try{
			InputStream is = url.openStream();
			  
			entries.load(is);
		}
		catch(IOException e){ 
			return entries;
		}
		return entries;
	}
	
}

