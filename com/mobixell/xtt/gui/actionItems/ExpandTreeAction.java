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

public class ExpandTreeAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static ExpandTreeAction action;
	
	private ExpandTreeAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getExpandTreeItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getExpandTreeButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_OPEN_TYPE));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_OPEN_TYPE));
		putValue(Action.ACTION_COMMAND_KEY, "expand-tree");
	}
	
	public static ExpandTreeAction getInstance(){
		if (action == null){
			action =  new ExpandTreeAction();
		}
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		TreeTestController.handleRootTreeActions(ActionType.EXPAND);
	}
}
