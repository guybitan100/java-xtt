package com.mobixell.xtt.diameter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import com.mobixell.xtt.FunctionModule;
import com.mobixell.xtt.diameter.server.DiameterServer;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
import com.mobixell.xtt.diameter.statistics.CmdDataItem;
/**
 * <p>Handle the Diameter Telnet </P>
 * @version $Id: TelnetWorkerServer.java,v 1.0 2011/03/08 10:50:32 Exp $
 * @author Guy Bitan
 */
public class TelnetWorkerServer extends Thread
{
	public Socket incoming = null;
	public Scanner in;
	PrintWriter out;
	String PROMPT = "Diameter>";
	boolean done = false;
	InputStream inps;
	OutputStream outs;
	
	TelnetWorkerServer(int id,Socket soket) throws IOException
	{
		super("TelnetWorkerServer("+id+")");
		incoming = soket;
		incoming.setSoTimeout(200);
		init();
	}
	public void init() throws IOException
	{
		incoming.setSoTimeout(60000);
		InputStream inps = incoming.getInputStream();
		OutputStream outs = incoming.getOutputStream();
		in = new Scanner(inps);
		out = new PrintWriter(outs, true);
		done = false;
		out.print(PROMPT);
		out.flush();
	}
	public void run()
	{
		while (true)
		{
			try
			{
				while (!done && in.hasNextLine())
				{
					String line = in.nextLine();
					if (line.trim().indexOf("exit")>=0)
					{
						closeInputCon();
						closeConnection();
						init();
					}
					else if (line.trim().indexOf("stat")>=0)
					{
						out.println("");
						out.println("eXtreme Test Tool (XTT) - Diameter Manager Statistics");
						out.println("");
						out.println("NAME      TYPE           TPS       TPS-AVG       MAX-TPS        TOTAL");
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%13s%10d%12d%14d%14d", "CCRI", "REQUEST", CmdDataItem.tpsCCRIReq,
								CmdDataItem.tpsAvgCCRIReq, CmdDataItem.maxTpsCCRIReq, CmdDataItem.totalCCRIReq));
						out.println(String.format("%1s%14s%9d%12d%14d%14d", "CCRI", "RESPONSE", CmdDataItem.tpsCCRIRes,
								CmdDataItem.tpsAvgCCRIRes, CmdDataItem.maxTpsCCRIRes, CmdDataItem.totalCCRIRes));
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%13s%10d%12d%14d%14d", "RARU", "REQUEST", CmdDataItem.tpsRARUReq,
								CmdDataItem.tpsAvgRARUReq, CmdDataItem.maxTpsRARUReq, CmdDataItem.totalRARUReq));
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%13s%10d%12d%14d%14d", "RART", "REQUEST", CmdDataItem.tpsRARTReq,
								CmdDataItem.tpsAvgRARTReq, CmdDataItem.maxTpsRARTReq, CmdDataItem.totalRARTReq));
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%13s%9d%12d%14d%14d", "RARUT", "RESPONSE", CmdDataItem.tpsRARRes,
								CmdDataItem.tpsAvgRARRes, CmdDataItem.maxTpsRARRes, CmdDataItem.totalRARRes));
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%13s%10d%12d%14d%14d", "CCRT", "REQUEST", CmdDataItem.tpsCCRTReq,
								CmdDataItem.tpsAvgCCRTReq, CmdDataItem.maxTpsCCRTReq, CmdDataItem.totalCCRTReq));
						out.println(String.format("%1s%14s%9d%12d%14d%14d", "CCRT", "RESPONSE", CmdDataItem.tpsCCRTRes,
								CmdDataItem.tpsAvgCCRTRes, CmdDataItem.maxTpsCCRTRes, CmdDataItem.totalCCRTRes));
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%13s%10d%12d%14d%14d", "CCRU", "REQUEST", CmdDataItem.tpsCCRUReq,
								CmdDataItem.tpsAvgCCRUReq, CmdDataItem.maxTpsCCRUReq, CmdDataItem.totalCCRUReq));
						out.println(String.format("%1s%14s%9d%12d%14d%14d", "CCRU", "RESPONSE", CmdDataItem.tpsCCRURes,
								CmdDataItem.tpsAvgCCRURes, CmdDataItem.maxTpsCCRURes, CmdDataItem.totalCCRURes));
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%14s%10d%12d%14d%14d", "DWD", "REQUEST", CmdDataItem.tpsDWDReq,
								CmdDataItem.tpsAvgDWDReq, CmdDataItem.maxTpsDWDReq, CmdDataItem.totalDWDReq));
						out.println(String.format("%1s%15s%9d%12d%14d%14d", "DWD", "RESPONSE", CmdDataItem.tpsDWDRes,
								CmdDataItem.tpsAvgDWDRes, CmdDataItem.maxTpsDWDRes, CmdDataItem.totalDWDRes));
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%14s%10d%12d%14d%14d", "CER", "REQUEST", CmdDataItem.tpsCERReq,
								CmdDataItem.tpsAvgCERReq, CmdDataItem.maxTpsCERReq, CmdDataItem.totalCERReq));
						out.println(String.format("%1s%15s%9d%12d%14d%14d", "CER", "RESPONSE", CmdDataItem.tpsCERRes,
								CmdDataItem.tpsAvgCERRes, CmdDataItem.maxTpsCERRes, CmdDataItem.totalCERRes));
						out.println("---------------------------------------------------------------------");
						out.println(String.format("%1s%12s%10d%12d%14d%14d", "TOTAL", "REQUEST",
								CmdDataItem.totalTpsReq, CmdDataItem.totalTpsAvgReq, CmdDataItem.totalMaxTpsReq,
								CmdDataItem.totalReq));
						out.println(String.format("%1s%13s%9d%12d%14d%14d", "TOTAL", "RESPONSE",
								CmdDataItem.totalTpsRes, CmdDataItem.totalTpsAvgRes, CmdDataItem.totalMaxTpsRes,
								CmdDataItem.totalRes));
						out.println("---------------------------------------------------------------------\n");
						out.println("PCRF ACTIVE USERS: " + CmdDataItem.totalUsers + "\n\n\n\n");
						done = false;
						out.print(PROMPT);
						out.flush();
					}
					else if (line.trim().indexOf("conf")>=0)
					{
						out.println("");
						out.println("eXtreme Test Tool (XTT) - Diameter Manager Configuration");
						out.println("");
						out.println("\tCusomer:             " + DiameterManager.customer);
						out.println("\tRunLoad:             " + DiameterManager.runLoad);
						out.println("\tLocalIpAddress:      " + DiameterManager.localIpAddr);
						out.println("\tRemoteIpAddress:     " + DiameterManager.remoteIpAddr);
						out.println("\tPort:                " + DiameterManager.localPort);
						out.println("\tResponseDocument:    " + DiameterManager.respDocumentS);
						out.println("\tNumOfTcpCon:         " + DiameterManager.NUM_OF_TCP_CON);
						out.println("\tRART-time-out:       " + DiameterManager.RART_TIME_OUT_SEC);
						out.println("\tRARU-time-out:       " + DiameterManager.RARU_TIME_OUT_SEC);
						out.println("\tChangeScenarioEvery: " + DiameterManager.SCENARIO_CHANGE_EVERY);
						out.println("\tTpsInterval:         " + DiameterManager.TPS_INTERVAL);
						out.println("");
						done = false;
						out.print(PROMPT);
						out.flush();
					}
					else if (line.trim().indexOf("users")>=0)
					{
						out.println("");
						if (DiameterManager.usersTab.getUsersCount()>0)
							out.println(DiameterManager.usersTab.getPrintUsers());
						else
							out.println("\tTotal-Users = 0");	
						out.println("");
						done = false;
						out.print(PROMPT);
						out.flush();
					}
					else
					{
						 done = false;
						 out.println("");
						 out.println("eXtreme Test Tool (XTT) - Diameter Manager Telnet");
					     out.println("Version: DiameterServer: "+FunctionModule.parseVersion(DiameterServer.tantau_sccsid)+" DiameterWorker: "+FunctionModule.parseVersion(DiameterWorkerServer.tantau_sccsid));
					     out.println("");
					     out.println("Usage:");
					     out.println("\tshow stat");
					     out.println("\t\tReturn The Diameter Server (PCRF) Statistics");
					     out.println("");
					     out.println("\tshow conf");
					     out.println("\t\tReturn the Diameter Server (PCRF) Configuration");
					     out.println("");
					     out.println("\tshow users");
					     out.println("\t\tReturn the Diameter Server (PCRF) Configuration");
					     out.println("");
						 out.print(PROMPT);
						 out.flush();
					}
				}
			}
			catch (IOException e)
			{
				System.out.println("TelnetWorkerServer: Connection closed.");
			}
			catch (Exception e)
			{
				System.out.println("TelnetWorkerServer: Connection closed.");
			}
			closeInputCon();
			closeConnection();
		}
	}
	public void closeInputCon()
	{
		try
		{
			incoming.close();
			incoming = null;
		}
		catch (IOException e)
		{
			System.out.println("TelnetWorkerServer: Connection closed.");
		}
	}
	public void closeConnection()
	{
		try
		{
			incoming.close();
			incoming = null;
		}
		catch (IOException e)
		{
			System.out.println("TelnetWorkerServer: Connection closed.");

		}
	}
}
