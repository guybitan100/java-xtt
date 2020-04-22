package com.mobixell.xtt.diameter.client;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.diameter.DiameterManager;
import java.util.Hashtable;
/**
 * <p>Handle the socket poll for diameter client</P>
 * @version $Id: DiameterClient.java,v 1.6 2011/03/08 10:50:32 guybitan Exp $
 * @author Roger Soder & Guy Bitan
 */
public class SocketWheel
{
	public static int numOfTcpCon = 0;
	private final LinkedBlockingQueue<SocketBucket> socketsQueue = new LinkedBlockingQueue<SocketBucket>();
	public Hashtable<Integer, Socket> socketsTable = new Hashtable<Integer, Socket>();
	
	public SocketWheel(String ip, int port)
	{
		for (int i = numOfTcpCon; i < DiameterManager.NUM_OF_TCP_CON; i++, numOfTcpCon++)
		{
			try
			{
				Socket socket = new Socket(ip, port);
				addSocket(socket);
				socketsTable.put(i+1, socket);
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				XTTProperties.printDebug("DiameterWorkerPushClient Can Not Open Socket to PCEF Server: " + ip);
			}
		}
	}

	public synchronized void addSocket(Socket socket) throws Exception
	{
			socketsQueue.add(new SocketBucket(socket));
	}
	public synchronized void closeSockets() throws IOException
	{
			while (!socketsQueue.isEmpty())
			{
				socketsQueue.poll().getSocket().close();
				socketsQueue.notifyAll();
			}
	}

	public synchronized SocketBucket getPollBucket() throws IOException, InterruptedException
	{
		
			SocketBucket socketBucket = socketsQueue.poll(20 * 1000, TimeUnit.MILLISECONDS);
			return socketBucket;
	}

	public Hashtable<Integer, Socket> getSocketsTable()
	{
		return socketsTable;
	}
}