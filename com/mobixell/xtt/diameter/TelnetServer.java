package com.mobixell.xtt.diameter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import com.mobixell.xtt.util.OSUtils;

public class TelnetServer extends Thread
{
	public static ServerSocket s = null;
	public static Socket incoming = null;
	public Scanner in;
	PrintWriter out;
	String PROMPT = "Diameter>";
	boolean done = false;
	public static int port = 4444;
	public static String ip = null;
	public int id =0;
	public ServerSocket ss;
	TelnetServer(String localIpAddr) throws IOException
	{
		super("TelnetServer(IP: "+localIpAddr+" Port: " + port +")");
		ip=localIpAddr;
		init();
	}

	public void init() throws IOException
	{
		// establish server socket
		
		ss=new ServerSocket();
		
		if (ip=="" || ip==null) 
        {
        	ip= OSUtils.getIpAddr();
        	ss.bind(new java.net.InetSocketAddress(port));
        	
        }
        else
        {
        	ss.bind(new java.net.InetSocketAddress(ip,port));
        }
	}

	public void run()
	{
		while (true)
		{
			try
			{
				incoming = s.accept();
				TelnetWorkerServer tws = new TelnetWorkerServer(++id,incoming);
				tws.start();
			}
			catch (IOException e)
			{
			}
			catch (Exception e)
			{
			}
		}
	}
}
