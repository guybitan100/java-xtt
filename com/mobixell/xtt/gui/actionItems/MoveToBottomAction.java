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

/**
 * @author Kobi Gana
 *
 */
public class MoveToBottomAction extends IgnisAction {

	private static final long serialVersionUID = 6645847050708559419L;
	JPanel parent = null;
	private static MoveToBottomAction action;
	
	public MoveToBottomAction() {
		super();
		putValue(Action.NAME, XttMapping.getInstance().getItemMoveToBottomMenuItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getItemMoveToBottomButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_TO_BOTTOM));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_TO_BOTTOM));
		putValue(Action.ACTION_COMMAND_KEY, "move-test-to-bottom");
	}
	
	public static MoveToBottomAction getInstance(JPanel parent){
		if (action == null){
			action =  new MoveToBottomAction();
		}
		action.parent=parent;
		return action;
	}
	
	/**
	 * @see jsystem.treeui.actionItems.IgnisAction#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.TO_BOTTOM);
		TreeTestController.setTestDirty(true);
	}

}
