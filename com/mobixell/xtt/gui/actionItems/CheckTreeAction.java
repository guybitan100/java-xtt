/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;

public class CheckTreeAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static CheckTreeAction action;
	
	private CheckTreeAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getCheckTreeItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getCheckTreeButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_CHECK));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_CHECK));
		putValue(Action.ACTION_COMMAND_KEY, "check-tree");
	}
	
	public static CheckTreeAction getInstance(){
		if (action == null){
			action =  new CheckTreeAction();
		}
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		TreeTestController.handleRootTreeActions(ActionType.CHECK);
	}
}
