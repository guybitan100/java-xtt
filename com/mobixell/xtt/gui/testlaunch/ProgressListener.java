/*
 * Created on Jul 1, 2005
 * 
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.testlaunch;

/**
 * @author guy.bitan
 * 
 */
public interface ProgressListener {
	public void setTestMaxTime(long time);
	public void updateTimes(long testTime);
}
