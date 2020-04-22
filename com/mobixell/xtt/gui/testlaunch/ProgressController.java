package com.mobixell.xtt.gui.testlaunch;

import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;

/**
 * @author guy.bitan
 * 
 */
public class ProgressController extends Thread  {

	ProgressListener view;
	long testStartTime = 0;
	boolean running = false;
	long testMaxTime = 0;
	long suiteStartTime = 0;
	int totalSteps =0;

	public ProgressController(ProgressListener view) {
		super("ProgressController");
		this.view = view;
	}
	public void run() 
	{
		while (true) 
		{
			while (running) {
				if (testStartTime > 0) 
				{
					long runTime = System.currentTimeMillis() - testStartTime;
					if (runTime > testMaxTime) 
					{
						view.setTestMaxTime(testMaxTime);
					}
					view.updateTimes(runTime);
				}
				try 
				{
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized void setRunning(boolean running) {
		this.running = running;
		notify();
	}

	public void endTest() {
		//long runTime = System.currentTimeMillis() - testStartTime;
		view.setTestMaxTime(testMaxTime);
	}
	public void startTest(int repeatAmount) {
		totalSteps = TreeTestController.getNumOfSteps();
		testStartTime = System.currentTimeMillis();
		testMaxTime =totalSteps*repeatAmount;
		view.setTestMaxTime(testMaxTime);
	}

}
