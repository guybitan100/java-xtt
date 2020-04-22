package com.mobixell.xtt.diameter.statistics;

/**
 * @author guyb
 *
 */
public class CmdDataItem {
		
	private String sessionId=null;
	private String cmdName=null;
	public  static enum  Type { REQUEST,RESPONSE};
	private Type cmdType=null;
	private String cmdDesc=null;
	
	public static long totalCCRIReq =0;
	public static long totalCCRTReq =0;
	public static long totalCCRUReq =0;
	public static long totalDWDReq=0;
	public static long totalCERReq=0;
	public static long totalRARTReq = 0;
	public static long totalRARUReq = 0;
	
	
	public static long totalCCRIRes = 0;
	public static long totalCCRTRes = 0;
	public static long totalCCRURes = 0;
	public static long totalDWDRes  = 0;
	public static long totalCERRes  = 0;
	public static long totalRARRes = 0;

	public static long totalUsers = 0;
	
	public static long tpsCCRIReq = 0;
	public static long tpsCCRTReq = 0;
	public static long tpsCCRUReq = 0;
	public static long tpsDWDReq = 0;
	public static long tpsCERReq = 0;
	public static long tpsRARTReq = 0;
	public static long tpsRARUReq = 0;
	
	public static long totalTpsCCRIReq = 0;
	public static long totalTpsCCRTReq = 0;
	public static long totalTpsCCRUReq = 0;
	public static long totalTpsDWDReq = 0;
	public static long totalTpsCERReq = 0;
	public static long totalTpsRARTReq = 0;
	public static long totalTpsRARUReq = 0;
	
	public static long tpsAvgCCRIReq = 0;
	public static long tpsAvgCCRTReq = 0;
	public static long tpsAvgCCRUReq = 0;
	public static long tpsAvgDWDReq = 0;
	public static long tpsAvgCERReq = 0;
	public static long tpsAvgRARTReq = 0;
	public static long tpsAvgRARUReq = 0;
	
	public static long tpsCCRIRes = 0;
	public static long tpsCCRTRes = 0;
	public static long tpsCCRURes = 0;
	public static long tpsDWDRes = 0;
	public static long tpsCERRes = 0;
	public static long tpsRARRes = 0;
	
	public static long totalTpsCCRIRes = 0;
	public static long totalTpsCCRTRes = 0;
	public static long totalTpsCCRURes = 0;
	public static long totalTpsDWDRes = 0;
	public static long totalTpsCERRes = 0;
	public static long totalTpsRARRes = 0;
	
	public static long tpsAvgCCRIRes = 0;
	public static long tpsAvgCCRTRes = 0;
	public static long tpsAvgCCRURes = 0;
	public static long tpsAvgDWDRes = 0;
	public static long tpsAvgCERRes = 0;
	public static long tpsAvgRARRes = 0;
	
	public static long maxTpsCCRIReq = 0;
	public static long maxTpsCCRTReq = 0;
	public static long maxTpsCCRUReq = 0;
	public static long maxTpsDWDReq = 0;
	public static long maxTpsCERReq = 0;
	public static long maxTpsRARTReq = 0;
	public static long maxTpsRARUReq = 0;
	
	public static long maxTpsCCRIRes = 0;
	public static long maxTpsCCRTRes = 0;
	public static long maxTpsCCRURes = 0;
	public static long maxTpsDWDRes = 0;
	public static long maxTpsCERRes = 0;
	public static long maxTpsRARRes = 0;

	public static long totalReq;	 		    
	public static long totalRes;	           
	public static long totalTpsReq;        		
	public static long totalTpsRes;        		
	public static long totalMaxTpsReq;    		
	public static long totalMaxTpsRes;    		
	public static long totalTpsAvgReq;    		
	public static long totalTpsAvgRes;          
	
	
	
	
	
	
	
	private String cmdTime;
	
	public CmdDataItem (String cmdTime,String cmdName, Type cmdType,String cmdDesc) 
	{
		this.cmdTime = cmdTime;
		this.cmdName = cmdName;
		this.cmdType = cmdType;
		this.cmdDesc = cmdDesc;
	}
	public String getSessionId() 
	{
		return sessionId;
	}
	public void setSessionId(String sessionId) 
	{
		this.sessionId = sessionId;
	}
	public String getCmdName() 
	{
		return cmdName;
	}
	public void setCmdName(String cmdName) 
	{
		this.cmdName = cmdName;
	}
	public Type getCmdType() 
	{
		return cmdType;
	}
	public void setCmdType(Type cmdType) 
	{
		this.cmdType = cmdType;
	}
	public String getCmdDesc() {
		return cmdDesc;
	}
	public void setCmdDesc(String cmdDesc) 
	{
		this.cmdDesc = cmdDesc;
	}
	public String getCmdTime() 
	{
		return cmdTime;
	}
	public void setCmdTime(String cmdTime) 
	{
		this.cmdTime = cmdTime;
	}	
	
}