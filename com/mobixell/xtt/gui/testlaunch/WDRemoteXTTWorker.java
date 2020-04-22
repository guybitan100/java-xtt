package com.mobixell.xtt.gui.testlaunch;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.main.XTTGui;
public class WDRemoteXTTWorker extends Thread
{
	/** How long to sleep between tries */
	public static final int SECOND = 1000;
	public static final int MIN = 60 * SECOND;
	private static int WAIT = SECOND *10 ;
	private boolean isWantCheck = false;
	XTTGui xttgui = null;
	public WDRemoteXTTWorker(XTTGui xttgui,boolean isWantCheck)
	{
		super("RemoteXTTCheckStatusWorker");
		this.xttgui=xttgui;
		this.isWantCheck = isWantCheck;
		 // Depending on configuration
		setDaemon(true); // so we don't keep the main app alive
        	WAIT = 5*SECOND;
	}

	public static class Application
	{
		public static final Object LOCK = new Object();
	}  
	
	public void run()
	{
		while (true)
		{ 
			synchronized (Application.LOCK)
			{
				if (isWantCheck()) 
					setStatus();
			}
			try
			{
				sleep(WAIT);
			}
			catch (InterruptedException e)
			{
				// do nothing with it
			}
		}
	}
	/**
	 * Save the current model's data in fn. If fn == null, use current fname or
	 * prompt for a filename if null.
	 */
	public void setStatus()
	{
		try
		{
				  if(XTTProperties.isRemoteXTTRunning())
				  {
					  WAIT = 1 * MIN; 
				  }  
				  else
				  {
					  WAIT = 1*SECOND;
				  }
				  
				  xttgui.refreshConfigurations();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean isWantCheck()
	{
		return isWantCheck;
	}
	public void setWantCheck(boolean isWantCheck)
	{
		this.isWantCheck = isWantCheck;
	}
}

	
