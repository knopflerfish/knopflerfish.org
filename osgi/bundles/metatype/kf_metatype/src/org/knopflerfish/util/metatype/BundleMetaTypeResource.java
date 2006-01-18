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

package org.knopflerfish.util.metatype;

import java.io.*;
import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

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
		String[] fpids;
		Vector factoryPidsV = new Vector();
		factoryPidsV.addAll(factoryPids.keySet());
		fpids =  (String[]) factoryPidsV.toArray(new String[factoryPidsV.size()]);  
		if(fpids.length == 0){
	    	return null;
	    }
	    else{
	    	return fpids;
	    }
	}

	public String[] getPids() {
	    String[] rpids;
		Vector pidsV = new Vector();
		pidsV.addAll(pids.keySet());
		rpids = (String[]) pidsV.toArray(new String[pidsV.size()]);
		if(rpids.length == 0){
	    	return null;
	    }
	    else{
	    	return rpids;
	    }
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
			//TODO get/set up locales
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
			throw new IllegalArgumentException("no information available for id");
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
	
	private String localizationFile;
	
	private Hashtable pids = new Hashtable();
	private Hashtable factoryPids = new Hashtable();
	
	private Hashtable OCDs = new Hashtable();
	
	private Hashtable locales = new Hashtable();
	
	public MetaData(String localizationFile){
		this.localizationFile = localizationFile;
	}	
	
	public MetaData(){
		this.localizationFile = "bundle";
	}	
	
	public void prepare(){
		loadLocales();
	}
	
	public String[] getLocales(){
		Enumeration enume = locales.keys();
		Vector localesV = new Vector();
		while(enume.hasMoreElements()){
			localesV.add(enume.nextElement());
		}
		return (String[]) localesV.toArray(new String[localesV.size()]);
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
	
	ObjectClassDefinition getOCD(String id, String locale){
		OCD ocd = (OCD) pids.get(id);
		if(ocd == null){
			ocd = (OCD) factoryPids.get(id);
		}
		if(ocd == null) return null;
		ocd.localize((Properties) locales.get(locale));
		return ocd;
	}
	
	public void designate(String factoryPid, String pid, String ocdref, Configuration conf){
		ObjectClassDefinition ocd;
		ocd = (ObjectClassDefinition) OCDs.get(ocdref);
		
		if(ocd != null){
			if(conf != null){
				Dictionary props = conf.getProperties();
				AttributeDefinition[] attrDefs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
				for(int i = 0; i < attrDefs.length; i++){
					AttributeDefinition ad = attrDefs[i];
					Object value;
					int card = ad.getCardinality();
					if(card == Integer.MIN_VALUE || card < 0){
						value = new Vector();
					}
					else if(card == Integer.MAX_VALUE){
						//array?
						value = new Vector();
					}
					else {
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
            	
				} //for
			
				try{
					conf.update(props);
				}
				catch(IOException ioe){}
			}
			
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
		String fileName = localizationFile;
	    /*
    	localization_entries = loadLocaleEntries(fileName + LOCALIZATION_FILE_SUFFIX, localization_entries);
    	
    	if(!usingDefault){ //since otherwise will redo it right after
    		StringTokenizer std = new StringTokenizer(Locale.getDefault().toString(), "_");
  
    		while(std.hasMoreTokens()){
        		fileName += "_" + std.nextToken();
        		localization_entries = loadLocaleEntries(fileName + LOCALIZATION_FILE_SUFFIX, localization_entries);
        	}
  
    		fileName = localizationFilesLocation;
    	}
    	
    	StringTokenizer st = new StringTokenizer(locale, "_");
    	while(st.hasMoreTokens()){
    		fileName += "_" + st.nextToken();
    		localization_entries = loadLocaleEntries(fileName + LOCALIZATION_FILE_SUFFIX, localization_entries);
    	}*/
		//locales.put(locale, loadLocaleEntries());
	}
	
	
//	TODO should this be done just before uninstalling? -> does DefaultLocale change?
	  private Properties loadLocaleEntries(String fileName){
		  Properties entries = new Properties();
		  
		  try{
			  InputStream is = new FileInputStream(new File(fileName));
			  //Properties locale_entries = new Properties();
			
			  /*locale_*/entries.load(is);
			  /*Iterator it = locale_entries.keySet().iterator();
			  while(it.hasNext()){
				  Object o = it.next();
				  entries.put(o, locale_entries.get(o));
			  }*/
		  }
		  catch(IOException e){ //includes FileNotFoundException
			  return entries;
		  }
		  return entries;
	  }
	
}

