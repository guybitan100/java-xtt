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

public class CollapseTreeAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static CollapseTreeAction action;
	
	private CollapseTreeAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getCollapseTreeItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getCollapseTreeButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_XTT_TEST));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_XTT_TEST));
		putValue(Action.ACTION_COMMAND_KEY, "collapse-tree");
	}
	
	public static CollapseTreeAction getInstance(){
		if (action == null){
			action =  new CollapseTreeAction();
		}
		return action;
	}
	
	public void actionPerformed(ActionEvent e) {
		TreeTestController.handleRootTreeActions(ActionType.COLLAPSE);
	}
}
