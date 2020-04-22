/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import javax.swing.Action;
import javax.swing.JPanel;


import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.images.ImageCenter;

public class MoveToTopAction extends IgnisAction {

	private static final long serialVersionUID = 42831365889855674L;
	private static MoveToTopAction action;
	JPanel parent = null;
	
	public MoveToTopAction() {
		super();
		putValue(Action.NAME, XttMapping.getInstance().getItemMoveToTopMenuItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getItemMoveToTopButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_TO_TOP));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_TO_TOP));
		putValue(Action.ACTION_COMMAND_KEY, "move-test-to-top");
	}
	
	public static MoveToTopAction getInstance(JPanel parent){
		if (action == null){
			action =  new MoveToTopAction();
		}
		action.parent=parent;
		return action;
	}
	/**
	 * @see jsystem.treeui.actionItems.IgnisAction#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.TO_TOP);
		TreeTestController.setTestDirty(true);
	}

}
