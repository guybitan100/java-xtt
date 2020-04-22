/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.TestLauncher;
import com.mobixell.xtt.gui.testlaunch.WaitDialog;
import com.mobixell.xtt.images.ImageCenter;

public class StopTestAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	JPanel parent = null;
	private static StopTestAction action;
	
	private StopTestAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getIStopTestMenuItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getStopTestButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_STOP));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_STOP));
		putValue(Action.ACTION_COMMAND_KEY, "remove-item");
	}
	
	public static StopTestAction getInstance(JPanel parent){
		if (action == null){
			action =  new StopTestAction();
		}
		return action;
	}
	public void actionPerformed(ActionEvent e) {
		(new Thread() {
			public void run() {
				try {
					try {
						WaitDialog.launchWaitDialog("Stoping test ...", null);
						TestLauncher.stop();
					} catch (Exception e1) {
						XTTProperties.printFail("Fail to stop test");
					}
				} finally {
					if(WaitDialog.isRunning())
					WaitDialog.endWaitDialog();
				}
			}
		}).start();
	}

}
