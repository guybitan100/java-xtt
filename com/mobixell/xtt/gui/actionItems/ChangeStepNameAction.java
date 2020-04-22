/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;

public class ChangeStepNameAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static ChangeStepNameAction action;
	
	private ChangeStepNameAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getChangeStepNameButton());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getChangeStepNameItem());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_SUT_EDIT));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_SUT_EDIT));
		putValue(Action.ACTION_COMMAND_KEY, "change-stepname");
	}
	
	public static ChangeStepNameAction getInstance(JPanel parent){
		if (action == null){
			action =  new ChangeStepNameAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {

		try {
			((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.CHANGE_STEP_NAME);
			TreeTestController.setTestDirty(true);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
