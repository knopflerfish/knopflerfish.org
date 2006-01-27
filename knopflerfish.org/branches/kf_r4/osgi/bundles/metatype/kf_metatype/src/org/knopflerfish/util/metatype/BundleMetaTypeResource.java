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


class MetaData {
	
	//private String NO_LOCALE = "none";
	
	private String localizationFileBaseName;
	
	private Hashtable pids = new Hashtable();
	private Hashtable factoryPids = new Hashtable();
	
	private Hashtable OCDs = new Hashtable();
	
	private String[] locales;
	
	private Bundle bundle;
	
	
	//TODO check for Attributes manifes's Constants.BUNDLE_LOCALIZATION
	  
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
		
		URL url;
		int underscore;
	
		url = bundle.getResource(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14) + localizationFileBaseName + "_" + locale + ".properties");
		if(url == null){
			underscore = locale.lastIndexOf('_');
			if(underscore > 0){
				locale = locale.substring(0, underscore - 1);
			}
			url = bundle.getResource(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14) + localizationFileBaseName + "_" + locale + ".properties");
			if(url == null){
				underscore = locale.lastIndexOf('_');
				if(underscore > 0){
					locale = locale.substring(0, underscore - 1);
				}
				url = bundle.getResource(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14) + localizationFileBaseName + "_" + locale + ".properties");
			}
			locale = Locale.getDefault().toString();
			url = bundle.getResource(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14) + localizationFileBaseName + "_" + locale + ".properties");
			if(url == null){
				underscore = locale.lastIndexOf('_');
				if(underscore > 0){
					locale = locale.substring(0, underscore - 1);
				}
				url = bundle.getResource(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14) + localizationFileBaseName + "_" + locale + ".properties");
				if(url == null){
					underscore = locale.lastIndexOf('_');
					if(underscore > 0){
						locale = locale.substring(0, underscore - 1);
					}
					url = bundle.getResource(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14) + localizationFileBaseName + "_" + locale + ".properties");
				}
				//lastly
				if(url == null){
					url = bundle.getResource(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14) + localizationFileBaseName + ".properties");
				}
			}	
		}
		
		ocd.localize(loadLocaleEntries(url));
		
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
					AttributeDefinition ad = (AttributeDefinition) ADs.get(((AE) attrsAssigns.nextElement()).adref);
				
					Object value;
					int card = ad.getCardinality();
					if(card == Integer.MIN_VALUE || card < 0){
						value = new Vector();
					}
					else if(card == Integer.MAX_VALUE){
						//TODO array?
						value = new Vector();
					}
					else {
						//TODO finish up
						switch(ad.getType()){
            				case AttributeDefinition.STRING:
            											if(card == 0){
            												value = new String();
            											}
            											else{
            												value = new String[card];
            											}
            											break;
            				case AttributeDefinition.BOOLEAN:
														if(card == 0){
															//TODO what to put?
															value = new Object();
														}
														else{
															value = new boolean[card];
														}
														break;
            				case AttributeDefinition.BYTE:
														if(card == 0){
															//TODO what to put?
															value = new Object();
														}
														else{
															value = new byte[card];
														}
														break;
            				default:
														if(card == 0){
															//TODO what to put?
															value = new Object();
														}
														else{
															value = new Object[card];
														}
														break;								
						}
					}
            	
					props.put(ad.getName(), value);
            	
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
	    
		Enumeration localizationFiles = bundle.getEntryPaths(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME.substring(0, 14));
		if(localizationFiles != null){
	    	while(localizationFiles.hasMoreElements()){ 
	    		URL url = bundle.getResource((String)localizationFiles.nextElement());
		        String fileName = url.getFile().substring(15);
		        if(!fileName.endsWith(".properties")){
		        	continue;
		        }
		        else if(!fileName.startsWith(localizationFileBaseName)){
		        	continue;
		        }
		        else if(fileName.length() == (localizationFileBaseName + ".properties").length()){
		        	continue;
		        }
		        else{
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
		
		if(url == null){
			return null;
		}
		  
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

