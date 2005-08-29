package com.gstm.test.eventadmin.scenarios.util;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class Util{
	public static Bundle installBundle(BundleContext context,String resource){
			try{
			
			/* get the class loader */
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();	
			/* get the resource */
			URL url=classLoader.getResource(resource);
			/* print the url */
			System.out.println("URL:" + url);
			/* install the bundle */
			Bundle bundle = context.installBundle(url.toString());
			return bundle;
			
			}catch(Exception e){
				/* print the error */
				System.out.println("ERROR in Util installBundle()" + e);
				return null;
			}
			
	}
	
}