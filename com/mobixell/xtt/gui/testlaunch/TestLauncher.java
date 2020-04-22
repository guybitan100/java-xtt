package com.mobixell.xtt.gui.testlaunch;

import com.mobixell.xtt.Parser;
import com.mobixell.xtt.XTT;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.log.LogViewer;

/**
 * @author      Guy.Bitan
 * @version     $Id: Test Launcher.java,v 1.7 2011/06/11 13:34:36 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */

public class TestLauncher extends TestLauncherGui implements Runnable
{
	public static int repeatAmount;	
	public static boolean isRunAll = true;
	public TestLauncher(int repeatAmount) throws Exception
	{
		super();
		addWindowListener(this);
		TestLauncher.repeatAmount=repeatAmount;
	}
	private static final long serialVersionUID = 8371605747939716597L;
	public static final String tantau_sccsid = "@(#)$Id: TestLauncher.java,v 1.7 2009/06/11 13:34:36 rsoder Exp $";

	public void runTest()
	{
		if (XTT.isTestRunning())
		{
			getXttgui().showError(this,"Run Error", "Tests are running, can't Test Launcher!");
			return;
		}
		xttlog.clear();
		StringBuffer test = new StringBuffer();
		try
		{
			test = TreeTestController.updateAndGetTestXml(false);
			org.jdom.input.SAXBuilder xmlparser = new org.jdom.input.SAXBuilder();
			xmlparser.setIgnoringElementContentWhitespace(true);
			document = xmlparser.build(new java.io.StringReader(test.toString().trim()));
		}
		catch (Exception e1)
		{
			getXttgui().showError(this,"Test File Error", e1.getClass().getName() + "\n" + e1.getMessage());
			return;
		}
		try
		{
			XTTProperties.getOutputStream().addOutputStream(xttlog);
			Thread t = new Thread(this, "RunTestViaTestLauncher");
			isRunAll=true;
			t.start();
		}
		catch (Exception ex)
		{
			getXttgui().showError(this,"Test File Error", ex.getClass().getName() + "\n" + ex.getMessage());
			return;
		}
	}
    public void runStep() 
	{
		if (XTT.isTestRunning()) 
		{
			getXttgui().showError(this,"Run Error","Tests are running, can't Test Launcher!");
			return;
		}
		xttlog.clear();
		StringBuffer step = new StringBuffer();
		try {
			step = TreeTestController.getBuildXmlStep();
			org.jdom.input.SAXBuilder xmlparser = new org.jdom.input.SAXBuilder();
			xmlparser.setIgnoringElementContentWhitespace(true);
			document = xmlparser.build(new java.io.StringReader(step.toString().trim()));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			try {
				XTTProperties.getOutputStream().addOutputStream(xttlog);
				Thread t = new Thread(this, "RunStepViaTestLauncher");
				isRunAll=false;
				t.start();
			} 
			
			catch (Exception ex) 
			{
				getXttgui().showError(this,"Test File Error", ex.getClass().getName()
						+ "\n" + ex.getMessage());
			}
		}
	public void run()
	{
		SwingUtils.setBusyCursor(this, true);
		getTreePanel().removeAllListeners();
		getTreePanel().progressController.startTest(repeatAmount);	
		getTreePanel().progressController.setRunning(true);
		TreePanel.setEnabledAll(false);
		TreePanel.stopTestButton.setEnabled(true);
		XTTProperties.variableStore.clear();
		LogViewer logViewer = new LogViewer(getXttgui(), xttlog.toString(),false);
				
		for (int i = 0; i < repeatAmount; i++)
		{
			try
			{
				TreePanel.repeatAmountLeft.setText(repeatAmount - (i + 1) + "");
				XTTProperties.setCurrentTestName(TreeTestController.getTestName());
				XTTProperties.setCurrentTestPath(TreeTestController.getTestFileFullPath());
				if (isRunAll)
				XTTProperties.getXTT().runTestFromTestLauncher(document,true,isRunAll);
				else
				{
					if (parser == null) parser = new Parser();
					 parser.runTestNI(document,true,isRunAll);
				}
			}
			catch (Exception ex)
			{
				getXttgui().showError(this,"Test File Error", ex.getClass().getName() + "\n" + ex.getMessage());
				getTreePanel().addAllListeners();
			}
		}
		logViewer.setMessage(XTTProperties.getOutputStream().getStreamVector().get(1).toString());
		XTTProperties.getOutputStream().removeOutputStream(xttlog);
		TreeTestController.setXttLog(xttlog.toString());
		getTreePanel().progressController.endTest();
		getTreePanel().progressController.setRunning(false);
		TreePanel.setEnabledAll(true);
		TreePanel.stopTestButton.setEnabled(false);
		TreePanel.runTestButton.setEnabled(true);
		SwingUtils.setBusyCursor(this, false);
		getTreePanel().addAllListeners();
	}
	 
	public synchronized static void stop(){
		getXttgui().setDoforceabort(true);
		getXttgui().setDoAbort();
		repeatAmount=0;
		while (TreePanel.stopTestButton.isEnabled())
		{
			Thread.yield();
		}
		getXttgui().setDoforceabort(false);
	}
}
