package com.mobixell.xtt.diameter.statistics;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.diameter.load.UserDataItem;
import com.mobixell.xtt.util.DateTimeUtils;

public class CmdDataTable 
{
	public static int numOfcmd=0;
	public CsvLog csvTranlog ;
	public CmdDataItem cmdDi;
	
	public CmdDataTable(CsvLog csvlog) 
	{
		this.csvTranlog = csvlog;
	}

	
	public synchronized void addCmd(String sc,String srcIp,String destIp,String srcPort,String destPort,String sessionId,String cmdName,CmdDataItem.Type cmdType,String cmdSubName,UserDataItem user) throws Exception 
	{
		String now =DateTimeUtils.getTime("HH:mm:ss(SSS)");		
		
		try {					
			if (user !=null)
			{
			csvTranlog.genLineToCsvFile(now,
										sc,
										srcIp,
										destIp,
										srcPort,
										destPort,
										sessionId, 
										cmdName, 
										cmdSubName,
										cmdType.toString(),
										Integer.toString(user.getScenario())    ,
										user.getIpAddress()						,
										user.getMsisdn()						,
										user.getRuleCat()						,
										user.getRuleSubCat())					;
			}
			else
			{
				csvTranlog.genLineToCsvFile( now,
											 sc,
											 srcIp,
											 destIp,
											 srcPort,
											 destPort,
											 "--", 
											 cmdName, 
											 cmdSubName,
											 cmdType.toString(),
											 "--",
											 "--",
											 "--",
											 "--",
											 "--");
			}
			
		} catch (Exception e)
		{
			 XTTProperties.printDebugException(e);
		}
		
	}
}
