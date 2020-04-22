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

public class RemoveFromThreadAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static RemoveFromThreadAction action;
	
	private RemoveFromThreadAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getRemoveThreadItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getRemoveThreadButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_FIXTURE_FAILD));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_FIXTURE_FAILD));
		putValue(Action.ACTION_COMMAND_KEY, "remove-from-thread");
	}
	
	public static RemoveFromThreadAction getInstance(JPanel parent){
		if (action == null){
			action =  new RemoveFromThreadAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.REMOVE_FROM_THREAD);
		TreeTestController.setTestDirty(true);
	}
}
