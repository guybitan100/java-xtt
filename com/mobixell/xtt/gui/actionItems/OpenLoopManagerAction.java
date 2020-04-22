/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;

import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;

public class OpenLoopManagerAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static OpenLoopManagerAction action;
	
	private OpenLoopManagerAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getAddLoopItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getAddLoopButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_FOR_LOOP));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_FOR_LOOP));
		putValue(Action.ACTION_COMMAND_KEY, "loop-managment");
	}
	
	public static OpenLoopManagerAction getInstance(JPanel parent){
		if (action == null){
			action =  new OpenLoopManagerAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.ADD_LOOP);
	}
}
