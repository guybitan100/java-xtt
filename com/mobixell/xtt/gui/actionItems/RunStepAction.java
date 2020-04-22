/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;

import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;

public class RunStepAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static RunStepAction action;
	
	private RunStepAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getRunStepItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getRunStepButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_RUN));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_RUN));
		putValue(Action.ACTION_COMMAND_KEY, "run-step");
	}
	
	public static RunStepAction getInstance(JPanel parent){
		if (action == null){
			action =  new RunStepAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.RUN_STEP);
	}
}
