package com.mobixell.xtt.diameter.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.util.DateTimeUtils;

public class CsvLog
{
	public static final int  FILE_SIZE_80MG = 83886080;

	public static enum LogType
	{
		TPS, SUM, TRANS
	};

	private String sFileFullPath = "";
	private String sFileName = "";
	private FileWriter writer;
	LogType logType;

	public CsvLog(LogType logtype)
	{
		switch (logtype)
		{
			case SUM:
				sFileName = "diam_sum";
				break;
			case TPS:
				sFileName = "diam_tps";
				break;
			case TRANS:
				sFileName = "diam_trans";
				break;
		}
		
		logType = logtype;
		openNewFile();
	}

	public void closeFile()
	{
		try
		{
			this.writer.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static int cntDWDReq = 0;
	public static int cntCERReq = 0;

	public static int cntCCRIRes = 0;
	public static int cntCCRTRes = 0;
	public static int cntCCRURes = 0;
	public static int cntDWDRes = 0;
	public static int cntRART = 0;
	public static int cntCERRes = 0;

	public void genLineToCsvFile(String cmdtime, String server_client,String srcIp,String destIp, String srcPort, String destPort, String sessionId,
			String cmdName, String cmdSubName, String cmdType, String userScenario, String userIpAddr,
			String userMsiIsdn, String userRuleCat, String userRuleSubCat) throws IOException
	{
		checkFileSize();
		
		try
		{
			writer.append(cmdtime);
			writer.append(',');
			writer.append(server_client);
			writer.append(',');
			writer.append(srcIp);
			writer.append(',');
			writer.append(destIp);
			writer.append(',');
			writer.append(srcPort);
			writer.append(',');
			writer.append(destPort);
			writer.append(',');
			writer.append(sessionId);
			writer.append(',');
			writer.append(cmdName);
			writer.append(',');
			writer.append(cmdSubName);
			writer.append(',');
			writer.append(cmdType);
			writer.append(',');
			writer.append(userScenario);
			writer.append(',');
			writer.append(userIpAddr);
			writer.append(',');
			writer.append(userMsiIsdn);
			writer.append(',');
			writer.append(userRuleCat);
			writer.append(',');
			writer.append(userRuleSubCat);
			writer.append(',');
			writer.append('\n');
			writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void genLineToCsvFile() throws IOException
	{
		checkFileSize();

		switch (logType)
		{
			case SUM:
				long totalReq = CmdDataItem.totalCCRIReq + CmdDataItem.totalCCRTReq + CmdDataItem.totalCCRUReq
						+ CmdDataItem.totalDWDReq + CmdDataItem.totalCERReq;

				long totalRes = CmdDataItem.totalCCRIRes + CmdDataItem.totalCCRTRes + CmdDataItem.totalCCRURes
						+ CmdDataItem.totalDWDRes + CmdDataItem.totalCERRes;
				try
				{
					writer.append(DateTimeUtils.getTime("HH:mm:ss(SSS)"));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.totalCCRIReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.totalCCRIRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.totalCCRTReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.totalCCRTRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.totalCCRUReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.totalCCRURes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.totalRARUReq));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.totalRARTReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.totalRARRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.totalDWDReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.totalDWDRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.totalCERReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.totalCERRes));
					writer.append(',');

					writer.append(Long.toString(totalReq));
					writer.append(',');
					writer.append(Long.toString(totalRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.totalUsers));
					writer.append(',');
					writer.append('\n');
					writer.flush();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				break;

			case TPS:
				long totalTpsReq = CmdDataItem.tpsCCRIReq + CmdDataItem.tpsCCRTReq + CmdDataItem.tpsCCRUReq
						+ CmdDataItem.tpsDWDReq + CmdDataItem.tpsCERReq;

				long totalTpsRes = CmdDataItem.tpsCCRIRes + CmdDataItem.tpsCCRTRes + CmdDataItem.tpsCCRURes
						+ CmdDataItem.tpsDWDRes + CmdDataItem.tpsCERRes;

				long totalMaxTpsReq = CmdDataItem.maxTpsCCRIReq + CmdDataItem.maxTpsCCRTReq + CmdDataItem.maxTpsCCRUReq
						+ CmdDataItem.maxTpsDWDReq + CmdDataItem.maxTpsCERReq;

				long totalMaxTpsRes = CmdDataItem.maxTpsCCRIRes + CmdDataItem.maxTpsCCRTRes + CmdDataItem.maxTpsCCRURes
						+ CmdDataItem.maxTpsDWDRes + CmdDataItem.maxTpsCERRes;
				
				 long totalTpsAvgReq =    CmdDataItem.tpsAvgCCRIReq + 
						   CmdDataItem.tpsAvgCCRTReq + 
				    	   CmdDataItem.tpsAvgCCRUReq +
				    	   CmdDataItem.tpsAvgDWDReq  + 
				    	   CmdDataItem.tpsAvgCERReq ;

				 long totalTpsAvgRes =    CmdDataItem.tpsAvgCCRIRes + 
						   CmdDataItem.tpsAvgCCRTRes + 
				    	   CmdDataItem.tpsAvgCCRURes +
				    	   CmdDataItem.tpsAvgDWDRes  + 
				    	   CmdDataItem.tpsAvgCERRes ;
				try
				{
					writer.append(DateTimeUtils.getTime("HH:mm:ss(SSS)"));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsCCRIReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsCCRIRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgCCRIReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgCCRIRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsCCRIReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsCCRIRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.tpsCCRTReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsCCRTRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgCCRTReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgCCRTRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsCCRTReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsCCRTRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.tpsCCRUReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsCCRURes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgCCRUReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgCCRURes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsCCRUReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsCCRURes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.tpsRARUReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgRARUReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsRARUReq));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.tpsRARTReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgRARTReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsRARTReq));
					writer.append(',');
					
					writer.append(Long.toString(CmdDataItem.tpsRARRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgRARRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsRARRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.tpsDWDReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsDWDRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgDWDReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgDWDRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsDWDReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsDWDRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.tpsCERReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsCERRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgCERReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.tpsAvgCERRes));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsCERReq));
					writer.append(',');
					writer.append(Long.toString(CmdDataItem.maxTpsCERRes));
					writer.append(',');

					writer.append(Long.toString(totalTpsReq));
					writer.append(',');
					writer.append(Long.toString(totalTpsRes));
					writer.append(',');
					writer.append(Long.toString(totalTpsAvgReq));
					writer.append(',');
					writer.append(Long.toString(totalTpsAvgRes));
					writer.append(',');
					writer.append(Long.toString(totalMaxTpsReq));
					writer.append(',');
					writer.append(Long.toString(totalMaxTpsRes));
					writer.append(',');

					writer.append(Long.toString(CmdDataItem.totalUsers));
					writer.append(',');
					writer.append('\n');
					writer.flush();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				break;
		}
	}

	public void checkFileSize() throws IOException
	{
		File file = new File(sFileFullPath);

		if (!file.exists() || !file.isFile())
		{
			XTTProperties.printDebug("File: " + sFileFullPath + " doesn\'t exist");
			return;
		}
		// Here we get the actual size
		if (file.length() >= FILE_SIZE_80MG)
		{
			zipFile();
			openNewFile();	
			file.delete();
		}
	}
public void openNewFile()
{
	sFileFullPath = sFileName + "_" + DateTimeUtils.getTime("dd_MM_yy_HH_mm_ss")+"_log.csv" ;
	
	try
	{
		this.writer = new FileWriter(sFileFullPath);
		printCsvHeader();
	}
	catch (IOException e)
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
	
	public String getsFileName()
	{
		return sFileFullPath;
	}

	public void setsFileName(String sFileName)
	{
		this.sFileFullPath = sFileName;
	}

	public void printCsvHeader() throws IOException
	{
		switch (logType)
		{
			case TPS:
				writer.append("Time");
				writer.append(',');
				writer.append("CCRI-TPS-REQ");
				writer.append(',');
				writer.append("CCRI-TPS-RES");
				writer.append(',');
				writer.append("CCRI-AVG-TPS-REQ");
				writer.append(',');
				writer.append("CCRI-AVG-TPS-RES");
				writer.append(',');
				writer.append("CCRI-MAX-TPS-REQ");
				writer.append(',');
				writer.append("CCRI-MAX-TPS-RES");
				writer.append(',');

				writer.append("CCRT-TPS-REQ");
				writer.append(',');
				writer.append("CCRT-TPS-RES");
				writer.append(',');
				writer.append("CCRT-AVG-TPS-REQ");
				writer.append(',');
				writer.append("CCRT-AVG-TPS-RES");
				writer.append(',');
				writer.append("CCRT-MAX-TPS-REQ");
				writer.append(',');
				writer.append("CCRT-MAX-TPS-RES");
				writer.append(',');

				writer.append("CCRU-TPS-REQ");
				writer.append(',');
				writer.append("CCRU-TPS-RES");
				writer.append(',');
				writer.append("CCRU-AVG-TPS-REQ");
				writer.append(',');
				writer.append("CCRU-AVG-TPS-RES");
				writer.append(',');
				writer.append("CCRU-MAX-TPS-REQ");
				writer.append(',');
				writer.append("CCRU-MAX-TPS-RES");
				writer.append(',');

				writer.append("RARU-TPS-REQ");
				writer.append(',');
				writer.append("RARU-AVG-TPS-REQ");
				writer.append(',');
				writer.append("RARU-MAX-TPS-REQ");
				writer.append(',');

				writer.append("RART-TPS-REQ");
				writer.append(',');
				writer.append("RART-AVG-TPS-REQ");
				writer.append(',');
				writer.append("RART-MAX-TPS-REQ");
				writer.append(',');				
				
				writer.append("RAR-TPS-RES");
				writer.append(',');
				writer.append("RAR-AVG-TPS-RES");
				writer.append(',');
				writer.append("RAR-MAX-TPS-RES");
				writer.append(',');
				
				writer.append("DWD-TPS-REQ");
				writer.append(',');
				writer.append("DWD-TPS-RES");
				writer.append(',');
				writer.append("DWD-AVG-TPS-REQ");
				writer.append(',');
				writer.append("DWD-AVG-TPS-RES");
				writer.append(',');
				writer.append("DWD-MAX-TPS-REQ");
				writer.append(',');
				writer.append("DWD-MAX-TPS-RES");
				writer.append(',');

				writer.append("CER-TPS-REQ");
				writer.append(',');
				writer.append("CER-TPS-RES");
				writer.append(',');
				writer.append("CER-AVG-TPS-REQ");
				writer.append(',');
				writer.append("CER-AVG-TPS-RES");
				writer.append(',');
				writer.append("CER-MAX-TPS-REQ");
				writer.append(',');
				writer.append("CER-MAX-TPS-RES");
				writer.append(',');

				writer.append("TOTAL-TPS-REQ");
				writer.append(',');
				writer.append("TOTAL-TPS-RES");
				writer.append(',');

				writer.append("TOTAL-AVG-TPS-REQ");
				writer.append(',');
				writer.append("TOTAL-AVG-TPS-RES");
				writer.append(',');
				
				writer.append("TOTAL-MAX-TPS-REQ");
				writer.append(',');
				writer.append("TOTAL-MAX-TPS-RES");
				writer.append(',');

				writer.append("ACTIVE-USERS");
				writer.append(',');
				writer.append('\n');
				writer.flush();

				break;

			case SUM:
				writer.append("Time");
				writer.append(',');
				writer.append("CCRI-SUM-REQ");
				writer.append(',');
				writer.append("CCRI-SUM-RES");
				writer.append(',');

				writer.append("CCRT-SUM-REQ");
				writer.append(',');
				writer.append("CCRT-SUM-RES");
				writer.append(',');

				writer.append("CCRU-SUM-REQ");
				writer.append(',');
				writer.append("CCRU-SUM-RES");
				writer.append(',');

				writer.append("RARU-SUM-REQ");
				writer.append(',');

				writer.append("RART-SUM-REQ");
				writer.append(',');
				writer.append("RAR-SUM-RES");
				writer.append(',');

				writer.append("DWD-SUM-REQ");
				writer.append(',');
				writer.append("DWD-SUM-RES");
				writer.append(',');

				writer.append("CER-SUM-REQ");
				writer.append(',');
				writer.append("CER-SUM-RES");
				writer.append(',');

				writer.append("TOTAL-REQ");
				writer.append(',');
				writer.append("TOTAL-RES");
				writer.append(',');

				writer.append("ACTIVE-USERS");
				writer.append(',');
				writer.append('\n');
				writer.flush();
				break;

			case TRANS:
				writer.append("TIME");
				writer.append(',');
				writer.append("PCRF S/C");
				writer.append(',');
				writer.append("SOURCE-IP");
				writer.append(',');
				writer.append("DEST-IP");
				writer.append(',');
				writer.append("SOURCE-PORT");
				writer.append(',');
				writer.append("DEST-PORT");
				writer.append(',');
				writer.append("SESSION-ID");
				writer.append(',');
				writer.append("CMD-NAME");
				writer.append(',');
				writer.append("REQUEST-TYPE");
				writer.append(',');
				writer.append("CMD-TYPE");
				writer.append(',');
				writer.append("SCENARIO");
				writer.append(',');
				writer.append("IP-ADDRES");
				writer.append(',');
				writer.append("MSIISDN");
				writer.append(',');
				writer.append("RULE-CATEGORY");
				writer.append(',');
				writer.append("RULE-SUBCATEGORY");
				writer.append(',');
				writer.append('\n');
				writer.flush();
				break;

		}
	}

	public LogType getType()
	{
		return logType;
	}

	public void setType(LogType type)
	{
		this.logType = type;
	}

	public void zipFile()
	{
	    	// Create a buffer for reading the files
	    	byte[] buf = new byte[FILE_SIZE_80MG];

	    	try {
	    		String outFilename = sFileName + "_" + DateTimeUtils.getTime("dd_MM_yy_HH_mm_ss")+"_log.zip" ;
	    	    // Create the ZIP file
	    	    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));

	    	    // Compress the files
	    	        FileInputStream in = new FileInputStream(sFileFullPath);

	    	        // Add ZIP entry to output stream.
	    	        out.putNextEntry(new ZipEntry(sFileFullPath));

	    	        // Transfer bytes from the file to the ZIP file
	    	        int len;
	    	        while ((len = in.read(buf)) > 0) {
	    	            out.write(buf, 0, len);
	    	        }

	    	        // Complete the entry
	    	        out.closeEntry();
	    	        in.close();
	    	    // Complete the ZIP file
	    	    out.close();
	    	} catch (IOException e) {
	    	}
	}
}