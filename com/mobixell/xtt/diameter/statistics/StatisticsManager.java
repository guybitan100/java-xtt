package com.mobixell.xtt.diameter.statistics;

import java.io.IOException;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.diameter.DiameterManager;
import com.mobixell.xtt.diameter.statistics.CsvLog.LogType;

public class StatisticsManager extends Thread
{
	public CsvLog csvSumStatLog;
	public CsvLog csvTpsStatLog;
	public long startTime = 0;
    public int interval = DiameterManager.TPS_INTERVAL;
    public long  intCnt = 1;
    public int sec = 1000;
    
    public long befTotalCCRIReq; 
    public long befTotalCCRTReq;
    public long befTotalCCRUReq; 
    public long befTotalDWDReq;
    public long befTotalCERReq;
    public long befTotalRARTReq;
    public long befTotalRARUReq;
    
    public long befTotalCCRIRes; 
    public long befTotalCCRTRes;
    public long befTotalCCRURes;
    public long befTotalDWDRes;
    public long befTotalCERRes;
    public long befTotalRARRes;
    
    public long totalReq;
    public long totalRes;
    
	public StatisticsManager()
	{
		csvSumStatLog = new CsvLog(LogType.SUM);
		csvTpsStatLog = new CsvLog(LogType.TPS);
		
		befTotalCCRIReq = CmdDataItem.totalCCRIReq; 
		befTotalCCRTReq = CmdDataItem.totalCCRTReq;
		befTotalCCRUReq = CmdDataItem.totalCCRUReq; 
		befTotalDWDReq =  CmdDataItem.totalDWDReq;
		befTotalCERReq =  CmdDataItem.totalCERReq;
		
		befTotalCCRIRes = CmdDataItem.totalCCRIRes; 
		befTotalCCRTRes = CmdDataItem.totalCCRTRes;
		befTotalCCRURes = CmdDataItem.totalCCRURes; 
		befTotalDWDRes =  CmdDataItem.totalDWDRes;
		befTotalCERRes =  CmdDataItem.totalCERRes;
		
		CmdDataItem.maxTpsCCRIReq   =  CmdDataItem.tpsCCRIReq;
		CmdDataItem.maxTpsCCRTReq   =  CmdDataItem.tpsCCRTReq;
		CmdDataItem.maxTpsCCRUReq   =  CmdDataItem.tpsCCRUReq;
		CmdDataItem.maxTpsDWDReq    =  CmdDataItem.tpsDWDReq;
		CmdDataItem.maxTpsCERReq    =  CmdDataItem.tpsCERReq;
		CmdDataItem.maxTpsRARTReq   =  CmdDataItem.tpsRARTReq;
		CmdDataItem.maxTpsRARUReq   =  CmdDataItem.tpsRARUReq;

		CmdDataItem.maxTpsCCRIRes   =  CmdDataItem.tpsCCRIRes;
		CmdDataItem.maxTpsCCRTRes   =  CmdDataItem.tpsCCRTRes;
		CmdDataItem.maxTpsCCRURes   =  CmdDataItem.tpsCCRURes;
		CmdDataItem.maxTpsDWDRes    =  CmdDataItem.tpsDWDRes;
		CmdDataItem.maxTpsCERRes    =  CmdDataItem.tpsCERRes;
		CmdDataItem.maxTpsRARRes    =  CmdDataItem.tpsRARRes;
	}

	public void run()
	{
		while (true)
		{
			readCounters();
		}
	}
	
	public void printDataOut()
	{
		XTTProperties.printInfo("\n\nNAME      TYPE           TPS       TPS-AVG       MAX-TPS        TOTAL  \n"
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%13s%10d%12d%14d%14d\n", "CCRI", "REQUEST", CmdDataItem.tpsCCRIReq,
						        CmdDataItem.tpsAvgCCRIReq, CmdDataItem.maxTpsCCRIReq, CmdDataItem.totalCCRIReq)
				+ String.format("%1s%14s%9d%12d%14d%14d\n", "CCRI", "RESPONSE", CmdDataItem.tpsCCRIRes,
								CmdDataItem.tpsAvgCCRIRes, CmdDataItem.maxTpsCCRIRes, CmdDataItem.totalCCRIRes)
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%13s%10d%12d%14d%14d\n", "RARU", "REQUEST", CmdDataItem.tpsRARUReq,
								CmdDataItem.tpsAvgRARUReq, CmdDataItem.maxTpsRARUReq, CmdDataItem.totalRARUReq)
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%13s%10d%12d%14d%14d\n", "RART", "REQUEST", CmdDataItem.tpsRARTReq,
								CmdDataItem.tpsAvgRARTReq, CmdDataItem.maxTpsRARTReq, CmdDataItem.totalRARTReq)
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%13s%9d%12d%14d%14d\n", "RARUT", "RESPONSE", CmdDataItem.tpsRARRes,
								CmdDataItem.tpsAvgRARRes, CmdDataItem.maxTpsRARRes, CmdDataItem.totalRARRes)
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%13s%10d%12d%14d%14d\n", "CCRT", "REQUEST", CmdDataItem.tpsCCRTReq,
								CmdDataItem.tpsAvgCCRTReq, CmdDataItem.maxTpsCCRTReq, CmdDataItem.totalCCRTReq)
				+ String.format("%1s%14s%9d%12d%14d%14d\n", "CCRT", "RESPONSE", CmdDataItem.tpsCCRTRes,
								CmdDataItem.tpsAvgCCRTRes, CmdDataItem.maxTpsCCRTRes, CmdDataItem.totalCCRTRes)
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%13s%10d%12d%14d%14d\n", "CCRU", "REQUEST", CmdDataItem.tpsCCRUReq,
								CmdDataItem.tpsAvgCCRUReq, CmdDataItem.maxTpsCCRUReq, CmdDataItem.totalCCRUReq)
				+ String.format("%1s%14s%9d%12d%14d%14d\n", "CCRU", "RESPONSE", CmdDataItem.tpsCCRURes,
								CmdDataItem.tpsAvgCCRURes, CmdDataItem.maxTpsCCRURes, CmdDataItem.totalCCRURes)
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%14s%10d%12d%14d%14d\n", "DWD", "REQUEST", CmdDataItem.tpsDWDReq,
								CmdDataItem.tpsAvgDWDReq, CmdDataItem.maxTpsDWDReq, CmdDataItem.totalDWDReq)
				+ String.format("%1s%15s%9d%12d%14d%14d\n", "DWD", "RESPONSE", CmdDataItem.tpsDWDRes,
								CmdDataItem.tpsAvgDWDRes, CmdDataItem.maxTpsDWDRes, CmdDataItem.totalDWDRes)
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%14s%10d%12d%14d%14d\n", "CER", "REQUEST", CmdDataItem.tpsCERReq,
								CmdDataItem.tpsAvgCERReq, CmdDataItem.maxTpsCERReq, CmdDataItem.totalCERReq)
				+ String.format("%1s%15s%9d%12d%14d%14d\n", "CER", "RESPONSE", CmdDataItem.tpsCERRes,
								CmdDataItem.tpsAvgCERRes, CmdDataItem.maxTpsCERRes, CmdDataItem.totalCERRes)
				+ "---------------------------------------------------------------------\n"
				+ String.format("%1s%12s%10d%12d%14d%14d\n", "TOTAL", "REQUEST",
						CmdDataItem.totalTpsReq, CmdDataItem.totalTpsAvgReq, CmdDataItem.totalMaxTpsReq,CmdDataItem.totalReq)
				+ String.format("%1s%13s%9d%12d%14d%14d\n", "TOTAL", "RESPONSE",
						CmdDataItem.totalTpsRes, CmdDataItem.totalTpsAvgRes, CmdDataItem.totalMaxTpsRes,CmdDataItem.totalRes)
				+ "---------------------------------------------------------------------\n\n" 
		        + "PCRF ACTIVE USERS: " + CmdDataItem.totalUsers + "\n\n\n\n\n");
								
		try
		{
			csvSumStatLog.genLineToCsvFile();
			csvTpsStatLog.genLineToCsvFile();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	
	public void readCounters()
	{
		try
		{
			CmdDataItem.tpsCCRIReq  = (CmdDataItem.totalCCRIReq   -  befTotalCCRIReq)  / interval ;
			CmdDataItem.tpsCCRTReq  = (CmdDataItem.totalCCRTReq   -  befTotalCCRTReq)  / interval ;
			CmdDataItem.tpsCCRUReq  = (CmdDataItem.totalCCRUReq   -  befTotalCCRUReq)  / interval ;
			CmdDataItem.tpsDWDReq   = (CmdDataItem.totalDWDReq    -  befTotalDWDReq)   / interval ;
			CmdDataItem.tpsCERReq   = (CmdDataItem.totalCERReq    -  befTotalCERReq)   / interval ;
			CmdDataItem.tpsRARTReq  = (CmdDataItem.totalRARTReq   -  befTotalRARTReq)  / interval ;
			CmdDataItem.tpsRARUReq  = (CmdDataItem.totalRARUReq   -  befTotalRARUReq)  / interval ;
			
			CmdDataItem.tpsCCRIRes =  (CmdDataItem.totalCCRIRes    -  befTotalCCRIRes) / interval ;
			CmdDataItem.tpsCCRTRes =  (CmdDataItem.totalCCRTRes    -  befTotalCCRTRes) / interval ;
			CmdDataItem.tpsCCRURes =  (CmdDataItem.totalCCRURes    -  befTotalCCRURes) / interval ;
			CmdDataItem.tpsDWDRes  =  (CmdDataItem.totalDWDRes     -  befTotalDWDRes)  / interval ;
			CmdDataItem.tpsCERRes  =  (CmdDataItem.totalCERRes     -  befTotalCERRes)  / interval ;
			CmdDataItem.tpsRARRes  =  (CmdDataItem.totalRARRes     -  befTotalRARRes)  / interval ;
			
			
			CmdDataItem.totalTpsCCRIReq += CmdDataItem.tpsCCRIReq;
			CmdDataItem.totalTpsCCRTReq += CmdDataItem.tpsCCRTReq;
			CmdDataItem.totalTpsCCRUReq += CmdDataItem.tpsCCRUReq;
			CmdDataItem.totalTpsDWDReq  += CmdDataItem.tpsDWDReq;
			CmdDataItem.totalTpsCERReq  += CmdDataItem.tpsCERReq;
			CmdDataItem.totalTpsRARTReq += CmdDataItem.tpsRARTReq;
			CmdDataItem.totalTpsRARUReq += CmdDataItem.tpsRARUReq;
			
			CmdDataItem.totalTpsCCRIRes += CmdDataItem.tpsCCRIRes;
			CmdDataItem.totalTpsCCRTRes += CmdDataItem.tpsCCRTRes;
			CmdDataItem.totalTpsCCRURes += CmdDataItem.tpsCCRURes;
			CmdDataItem.totalTpsDWDRes  += CmdDataItem.tpsDWDRes;
			CmdDataItem.totalTpsCERRes  += CmdDataItem.tpsCERRes;
			CmdDataItem.totalTpsRARRes  += CmdDataItem.tpsRARRes;
			
			
			CmdDataItem.tpsAvgCCRIReq = CmdDataItem.totalTpsCCRIReq / intCnt;
			CmdDataItem.tpsAvgCCRTReq = CmdDataItem.totalTpsCCRTReq / intCnt;
			CmdDataItem.tpsAvgCCRUReq = CmdDataItem.totalTpsCCRUReq / intCnt;
			CmdDataItem.tpsAvgDWDReq  = CmdDataItem.totalTpsDWDReq  / intCnt;
			CmdDataItem.tpsAvgCERReq  = CmdDataItem.totalTpsCERReq  / intCnt;
			CmdDataItem.tpsAvgRARTReq = CmdDataItem.totalTpsRARTReq / intCnt;
			CmdDataItem.tpsAvgRARUReq = CmdDataItem.totalTpsRARUReq / intCnt;
			
			CmdDataItem.tpsAvgCCRIRes = CmdDataItem.totalTpsCCRIRes / intCnt;
			CmdDataItem.tpsAvgCCRTRes = CmdDataItem.totalTpsCCRTRes / intCnt;
			CmdDataItem.tpsAvgCCRURes = CmdDataItem.totalTpsCCRURes / intCnt;
			CmdDataItem.tpsAvgDWDRes  = CmdDataItem.totalTpsDWDRes  / intCnt;
			CmdDataItem.tpsAvgCERRes  = CmdDataItem.totalTpsCERRes  / intCnt;
			CmdDataItem.tpsAvgRARRes  = CmdDataItem.totalTpsRARRes   / intCnt;
			
            CmdDataItem.totalReq      =	 CmdDataItem.totalCCRIReq + 
				        			     CmdDataItem.totalCCRTReq +
				        			     CmdDataItem.totalCCRUReq + 
				        			     CmdDataItem.totalDWDReq  +
				        			     CmdDataItem.totalCERReq;

			CmdDataItem.totalRes = 		 CmdDataItem.totalCCRIRes + 
						    			 CmdDataItem.totalCCRTRes + 
						    			 CmdDataItem.totalCCRURes + 
						    			 CmdDataItem.totalDWDRes  + 
						    			 CmdDataItem.totalCERRes;
			
			CmdDataItem.totalTpsReq =    CmdDataItem.tpsCCRIReq + 
							    		 CmdDataItem.tpsCCRTReq + 
							    		 CmdDataItem.tpsCCRUReq +
							    		 CmdDataItem.tpsDWDReq  + 
							    		 CmdDataItem.tpsCERReq ;
			
			CmdDataItem.totalTpsRes =    CmdDataItem.tpsCCRIRes + 
							    		 CmdDataItem.tpsCCRTRes + 
							    		 CmdDataItem.tpsCCRURes +
							    		 CmdDataItem.tpsDWDRes  + 
						    		     CmdDataItem.tpsCERRes ;
			
			CmdDataItem.totalMaxTpsReq = CmdDataItem.maxTpsCCRIReq + 
			    					     CmdDataItem.maxTpsCCRTReq + 
							    	     CmdDataItem.maxTpsCCRUReq +
							    	     CmdDataItem.maxTpsDWDReq  + 
							    	     CmdDataItem.maxTpsCERReq ;
			
			
			CmdDataItem.totalMaxTpsRes = CmdDataItem.maxTpsCCRIRes + 
							    	     CmdDataItem.maxTpsCCRTRes + 
							    	     CmdDataItem.maxTpsCCRURes +
							    	     CmdDataItem.maxTpsDWDRes  + 
							    	     CmdDataItem.maxTpsCERRes ;
		  
			CmdDataItem.totalTpsAvgReq = CmdDataItem.tpsAvgCCRIReq + 
									     CmdDataItem.tpsAvgCCRTReq + 
							    	     CmdDataItem.tpsAvgCCRUReq +
							    	     CmdDataItem.tpsAvgDWDReq  + 
							    	     CmdDataItem.tpsAvgCERReq ;
		  
			CmdDataItem.totalTpsAvgRes = CmdDataItem.tpsAvgCCRIRes + 
									     CmdDataItem.tpsAvgCCRTRes + 
							    	     CmdDataItem.tpsAvgCCRURes +
							    	     CmdDataItem.tpsAvgDWDRes  + 
							    	     CmdDataItem.tpsAvgCERRes ;
			
			printDataOut();
			resetCntAndWait();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void resetCntAndWait() throws InterruptedException
	{
		if (CmdDataItem.maxTpsCCRIReq  < CmdDataItem.tpsCCRIReq) CmdDataItem.maxTpsCCRIReq = CmdDataItem.tpsCCRIReq;
		if (CmdDataItem.maxTpsCCRTReq  < CmdDataItem.tpsCCRTReq) CmdDataItem.maxTpsCCRTReq = CmdDataItem.tpsCCRTReq;
		if (CmdDataItem.maxTpsCCRUReq  < CmdDataItem.tpsCCRUReq) CmdDataItem.maxTpsCCRUReq = CmdDataItem.tpsCCRUReq;
		if (CmdDataItem.maxTpsDWDReq   < CmdDataItem.tpsDWDReq)  CmdDataItem.maxTpsDWDReq =  CmdDataItem.tpsDWDReq;
		if (CmdDataItem.maxTpsCERReq   < CmdDataItem.tpsCERReq)  CmdDataItem.maxTpsCERReq =  CmdDataItem.tpsCERReq;
		if (CmdDataItem.maxTpsRARTReq  < CmdDataItem.tpsRARTReq)  CmdDataItem.maxTpsRARTReq =  CmdDataItem.tpsRARTReq;
		if (CmdDataItem.maxTpsRARUReq  < CmdDataItem.tpsRARUReq)  CmdDataItem.maxTpsRARUReq =  CmdDataItem.tpsRARUReq;

		if (CmdDataItem.maxTpsCCRIRes  < CmdDataItem.tpsCCRIRes) CmdDataItem.maxTpsCCRIRes = CmdDataItem.tpsCCRIRes;
		if (CmdDataItem.maxTpsCCRTRes  < CmdDataItem.tpsCCRTRes) CmdDataItem.maxTpsCCRTRes = CmdDataItem.tpsCCRTRes;
		if (CmdDataItem.maxTpsCCRURes  < CmdDataItem.tpsCCRURes) CmdDataItem.maxTpsCCRURes = CmdDataItem.tpsCCRURes;
		if (CmdDataItem.maxTpsDWDRes   < CmdDataItem.tpsDWDRes)  CmdDataItem.maxTpsDWDRes =  CmdDataItem.tpsDWDRes;
		if (CmdDataItem.maxTpsCERRes   < CmdDataItem.tpsCERRes)  CmdDataItem.maxTpsCERRes =  CmdDataItem.tpsCERRes;
		if (CmdDataItem.maxTpsRARRes  < CmdDataItem.tpsRARRes)  CmdDataItem.maxTpsRARRes =  CmdDataItem.tpsRARRes;

		befTotalCCRIReq = CmdDataItem.totalCCRIReq;
		befTotalCCRTReq = CmdDataItem.totalCCRTReq;
		befTotalCCRUReq = CmdDataItem.totalCCRUReq;
		befTotalDWDReq = CmdDataItem.totalDWDReq;
		befTotalCERReq = CmdDataItem.totalCERReq;
		befTotalRARTReq = CmdDataItem.totalRARTReq;
		befTotalRARUReq = CmdDataItem.totalRARUReq;

		befTotalCCRIRes = CmdDataItem.totalCCRIRes;
		befTotalCCRTRes = CmdDataItem.totalCCRTRes;
		befTotalCCRURes = CmdDataItem.totalCCRURes;
		befTotalCERRes = CmdDataItem.totalCERRes;
		befTotalDWDRes = CmdDataItem.totalDWDRes;
		befTotalCERRes = CmdDataItem.totalCERRes;
		befTotalRARRes = CmdDataItem.totalRARRes;
		
		CmdDataItem.tpsCCRIReq = 0;
		CmdDataItem.tpsCCRTReq = 0;
		CmdDataItem.tpsCCRUReq = 0;
		CmdDataItem.tpsDWDReq = 0;
		CmdDataItem.tpsCERReq = 0;
		CmdDataItem.tpsRARTReq = 0;
		CmdDataItem.tpsRARUReq = 0;

		CmdDataItem.tpsCCRIRes = 0;
		CmdDataItem.tpsCCRTRes = 0;
		CmdDataItem.tpsCCRURes = 0;
		CmdDataItem.tpsDWDRes  = 0;
		CmdDataItem.tpsCERRes  = 0;
		CmdDataItem.tpsRARRes  = 0;

		Thread.sleep(interval * sec);
		intCnt++;

	}
}