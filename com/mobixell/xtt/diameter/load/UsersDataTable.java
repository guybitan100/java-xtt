package com.mobixell.xtt.diameter.load;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.DelayQueue;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.diameter.DiameterManager;
import com.mobixell.xtt.diameter.server.DiameterServer;
import com.mobixell.xtt.diameter.statistics.CmdDataItem;

public class UsersDataTable {
	public static int scenario=1;
	long now ;
	public Hashtable<String, UserDataItem> users = new Hashtable<String, UserDataItem>();
	public static DelayQueue<UserDataItem> queue = new DelayQueue<UserDataItem>();
	
	public synchronized void addUser(String ssSessionId,String ipAddress) 
	{
		UserDataItem udi1 = null ;
		String [] msiIsdn = ipAddress.split("\\.");	
		int totalUsersIn = (int) CmdDataItem.totalCCRIReq;
		
	if	(DiameterManager.IS_SCENARIO_CHANGE)
	{
		if (totalUsersIn!=0 && totalUsersIn % DiameterManager.SCENARIO_CHANGE_EVERY==0) 
		{
			if (scenario==1) scenario= 2 ;
			else if (scenario==2) scenario= 3;
			else if (scenario==3) scenario= 1;
		}	
	}	
			String sMsiIsdn = msiIsdn[0]+ "9" + msiIsdn[1] + "9" + msiIsdn[2] + "9" + msiIsdn[3];
			
			if (Integer.parseInt(msiIsdn[3])>=1 && Integer.parseInt(msiIsdn[3])<=4)
			{
				udi1 = new UserDataItem( ssSessionId,
										sMsiIsdn,
										ipAddress,
										DiameterServer.ruleManager.generateVideoOptRule(),
										DiameterServer.ruleManager.generateVideoOptRule(),
										scenario);
				users.put(ssSessionId, udi1);
			}
			else
			{			
				udi1 = new UserDataItem( ssSessionId, 
									    sMsiIsdn,
						  			    ipAddress,
						  			    DiameterServer.ruleManager.generateWebOptRule(),
						  			    DiameterServer.ruleManager.generateWebOptRule(),
						  			    scenario);
			   users.put(ssSessionId, udi1);
			}
		
			 users.put(ssSessionId,udi1);	
			 switch(scenario)
	          {
	              case 1  :
	            	       now =System.currentTimeMillis();
	            	       udi1.setWaitUntil(now + DiameterManager.RART_TIME_OUT_SEC * 1000);
	            	  	   udi1.setPushToPerfom(udi1.PUSH_RAR_T);
	            	       queue.add(udi1);  
	              		   break;
	              case 2  :
	            	  	   now =System.currentTimeMillis();
	            	  	   udi1.setWaitUntil(now + DiameterManager.RARU_TIME_OUT_SEC * 1000);
	            	  	   udi1.setPushToPerfom(udi1.PUSH_RAR_U);
	              		   queue.add(udi1);  
	              		   
	              		   UserDataItem udi2 = new UserDataItem();
	              		   udi2.copyFromUser(udi1);
	              		   now =System.currentTimeMillis();
	              		   udi2.setWaitUntil(now + DiameterManager.RART_TIME_OUT_SEC * 1000);
	              		   udi2.setPushToPerfom(udi1.PUSH_RAR_T);
	            	  	   queue.add(udi2);
	              		   break;
	              default :
	            	  	  now =System.currentTimeMillis();
	            	  	  udi1.setWaitUntil(now + DiameterManager.RART_TIME_OUT_SEC * 1000);
	            	  	  udi1.setPushToPerfom(udi1.PUSH_RAR_T);
	            	  	  queue.add(udi1);   
	              		  break;
	          }		
		 XTTProperties.printDebug("User:" +  ssSessionId + " Add");
	}
	
	public synchronized void removeUser(String ssSessionId) 
	{
		XTTProperties.printDebug("User:" +  ssSessionId + " Removed");
		users.remove(ssSessionId);
	}
	public void updateRule(String ssSessionId, String[] rule) 
	{
		 UserDataItem user = users.get(ssSessionId);
		 user.setRuleCat(rule[0]);
		 user.setRuleSubCat(rule[1]);
		 users.remove(ssSessionId);
		 users.put(ssSessionId,user);
	}
	public void setRuleCat(String sSessionId,String newRule) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
		 user.setRuleCat(newRule);
	}
	public void setRuleSubCat(String sSessionId,String newRule) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
		 user.setRuleSubCat(newRule);
	}
	public void setNewRuleCat(String sSessionId,String newRule) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
		 user.setNewRuleCat(newRule);
	}
	public void setNewRuleSubCat(String sSessionId,String newRule) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
		 user.setNewRuleSubCat(newRule);
	}
	public String getRuleCat(String sSessionId) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
			 return user.getRuleCat();
		 else
		 return sSessionId + " didn't get CCRI";	
	}
	public String getRuleSubCat(String sSessionId) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
			 return user.getRuleSubCat();
		 else
		 return sSessionId + " didn't get CCRI";	
	}
	public String getNewRuleCat(String sSessionId) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
			 return user.getNewRuleCat();
		 else
		 return sSessionId + " didn't get CCRI";	 		 
	}
	public String getNewRuleSubCat(String sSessionId) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
			 return user.getNewRuleSubCat();
		 else
		 return sSessionId + " didn't get CCRI";	 		 
	}
	public String getMsisdn(String sSessionId) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
		 return user.getMsisdn();
		 else
		 return sSessionId + " didn't get CCRI";	 
	}
	public int getScenario(String sSessionId) 
	{
		 UserDataItem user = users.get(sSessionId);
		 if (user!=null)
		 return user.getScenario();
		 else
		 return 0;	
	}
	public Hashtable<String, UserDataItem> getUsers() 
	{
		return users;
	}
	public int getUsersCount() 
	{
		return users.size();
	}
	public UserDataItem getUser (String sSessionId) 
	{
		 UserDataItem user = users.get(sSessionId);
		 return  user;
	}
	public synchronized String getPrintUsers() 
	{
		String retStr ="";
		Enumeration<String> names; 
		String key;
		names = users.keys(); 
		XTTProperties.printDebug("Current Users In The DB");
		XTTProperties.printDebug("-----------------------");
		while(names.hasMoreElements()) 
		{ 
			key = (String) names.nextElement();
			retStr += users.get(key).getStrTable(key)+"\t";
			retStr += "-----------------------\t";
			XTTProperties.printDebug(users.get(key).getStrTable(key));
			XTTProperties.printDebug("-----------------------");
			
		}
		retStr +="Total Users: (" + users.size() + ")";
		retStr +=" -----------------------";
		XTTProperties.printDebug("Total Users: (" + users.size() + ")");
		XTTProperties.printDebug("-----------------------");
		
		return  retStr;
	}
	
}
