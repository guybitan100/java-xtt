package com.mobixell.xtt.diameter.client;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
/**
 * <p>Handle the socket bucket for diameter client</P>
 * @version $Id: DiameterClient.java,v 1.6 2011/03/08 10:50:32 guybitan Exp $
 * @author Roger Soder & Guy Bitan
 */
public class SocketBucket 
  {
    private final Socket socket;
    private final BufferedOutputStream out;
    private BufferedInputStream in;
    int timeout = 100000;
    public SocketBucket(Socket socket) throws IOException 
    {
    	 this.socket = socket;
         out = new BufferedOutputStream(socket.getOutputStream(), 65536);
         socket.setSoTimeout(timeout);
     	 socket.setTcpNoDelay(true);
     	 socket.setReuseAddress(true);
    }
	public Socket getSocket()
	{
		return socket;
	}

	public BufferedOutputStream getOut()
	{
		return out;
	}

	public BufferedInputStream getIn(boolean isReadInput) throws IOException
	{
		if(isReadInput)
		in = new BufferedInputStream(socket.getInputStream(),65536);
		
		return in;
	}
  }