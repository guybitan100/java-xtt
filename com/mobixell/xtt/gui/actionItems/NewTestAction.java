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

public class NewTestAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static NewTestAction action;
	
	private NewTestAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getChangeTestNameButton());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getChangeTestNameItem());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_SUT_EDIT));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_SUT_EDIT));
		putValue(Action.ACTION_COMMAND_KEY, "change-testname");
	}
	
	public static NewTestAction getInstance(JPanel parent){
		if (action == null){
			action =  new NewTestAction();
		}
		action.parent=parent;
		return action;
	}
	
	public void actionPerformed(ActionEvent e) {
		try {
			((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.NEW_TEST);
			TreeTestController.setTestDirty(true);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
}
