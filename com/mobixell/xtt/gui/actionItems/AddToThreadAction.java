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

public class AddToThreadAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static AddToThreadAction action;
	
	private AddToThreadAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getAddToThreadItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getAddToThreadButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_FIXTURE_RUNNING));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_FIXTURE_RUNNING));
		putValue(Action.ACTION_COMMAND_KEY, "add-to-thread");
	}
	
	public static AddToThreadAction getInstance(JPanel parent){
		if (action == null){
			action =  new AddToThreadAction();
		}
		action.parent=parent;
		return action;
	}
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.ADD_TO_THREAD);
		TreeTestController.setTestDirty(true);
	}
}
