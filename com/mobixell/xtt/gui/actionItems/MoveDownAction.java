/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;

import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.images.ImageCenter;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;

public class MoveDownAction extends IgnisAction {

	private static final long serialVersionUID = 1L;
	JPanel parent = null;
	private static MoveDownAction action;
	
	private MoveDownAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getItemMoveDownMenuItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getItemMoveDownButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_DOWN));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_DOWN));
		putValue(Action.ACTION_COMMAND_KEY, "move-test-down");
	}
	
	public static MoveDownAction getInstance(JPanel parent){
		if (action == null){
			action =  new MoveDownAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.DOWN);
		TreeTestController.setTestDirty(true);
	}
}
