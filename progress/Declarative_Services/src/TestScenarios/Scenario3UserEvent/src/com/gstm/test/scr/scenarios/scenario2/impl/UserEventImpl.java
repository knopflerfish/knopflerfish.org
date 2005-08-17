/*
 * Created on Aug 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package com.gstm.test.scr.scenarios.scenario2.impl;
import com.gstm.test.scr.scenarios.scenario2.Provider2;
import com.gstm.test.scr.scenarios.scenario2.Provider1;

public class UserEventImpl{
	Provider2 provider2;
	Provider1 provider1;
	
	public UserEventImpl()
	{
		
	}
	public void bindProvider1(Provider1 service){
		provider1 = service;
	}
	public void bindProvider2(Provider2 service){
		provider2 = service;
	}
	public void unbindProvider1(Provider1 service){
		provider1 = null;
	}
	public void unbindProvider2(Provider2 service){
		provider2 = null;
	}
	protected int getProvider1Count(){
		return provider1.getValue();	
	}
	protected int getProvider2Count(){
		return provider2.getValue();
	}
	class userThread extends Thread{

		public void run()
		{
			while(true)
			{
				if((provider1 != null)&& (provider1 != null))
					System.out.println("provider1: " + getProvider1Count() + "  getProvider2:" + getProvider2Count());
				try{
					wait(10000);
				}
				catch(Exception e){
					System.out.println("Exception:" + e);
				}
			}
		}
	}
}


