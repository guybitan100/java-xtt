package com.mobixell.xtt.gui.testlaunch.tree;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;

public class AutoSaveWorker extends Thread
{
	/** How long to sleep between tries */
	public static final int SECOND = 1000;
	private static int SECONDSTOWAIT = SECOND *10 ;
	private boolean isWantAutoSave = false;

	public AutoSaveWorker()
	{
		super("AutoSave Thread");
		 // Depending on configuration
		setDaemon(true); // so we don't keep the main app alive
		Integer autosave = XTTProperties.getIntProperty("GUI/LAUNCHER/AUTOSAVE");
        if(autosave.equals("null"))
        {
        	isWantAutoSave = false;
        	TreePanel.autoSaveButton.setEnabled(false);
        } 
        else if  (autosave<=0)
        {
        	isWantAutoSave = false;
        	TreePanel.autoSaveButton.setEnabled(false);
        }
        else 
        {
        	TreePanel.autoSaveButton.setEnabled(true);
        	SECONDSTOWAIT = autosave * SECOND;
        }
	}

	public static class Application
	{
		public static final Object LOCK = new Object();
	}  
	
	public void run()
	{
		while (true)
		{ // entire run method runs forever.
			try
			{
				sleep(SECONDSTOWAIT);
			}
			catch (InterruptedException e)
			{
				// do nothing with it
			}
			synchronized (Application.LOCK)
			{
				if (isWantAutoSave() && hasUnsavedChanges()) 
					saveFile();
			}
		}
	}
	/** Ask the model if it has any unsaved changes, don't save otherwise */
	public boolean hasUnsavedChanges()
	{
		return TreeTestController.isTestDirty();
	}

	/**
	 * Save the current model's data in fn. If fn == null, use current fname or
	 * prompt for a filename if null.
	 */
	public void saveFile()
	{
		try
		{
				XTTProperties.printDebug("AutoSave: START");
				TreeTestController.test.save();
				XTTProperties.printDebug("AutoSave: STOP");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean isWantAutoSave()
	{
		return isWantAutoSave;
	}
	public void setWantAutoSave(boolean isWantAutoSave)
	{
		this.isWantAutoSave = isWantAutoSave;
	}
}

	
