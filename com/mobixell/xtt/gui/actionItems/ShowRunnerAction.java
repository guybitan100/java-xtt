/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JFrame;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;

public class ShowRunnerAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private static ShowRunnerAction action;
	
	private ShowRunnerAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getShowRunnerItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getShowRunnerButton());
		putValue(Action.ACTION_COMMAND_KEY, "show-Runner");
	}
	
	public static ShowRunnerAction getInstance(){
		if (action == null){
			action =  new ShowRunnerAction();
		}
		return action;
	}
	
	public void actionPerformed(ActionEvent e) {
		TestLauncherGui.getXttgui().setVisible(true);
	}
}
