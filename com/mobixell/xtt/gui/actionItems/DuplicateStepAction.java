/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;

public class DuplicateStepAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static DuplicateStepAction action;
	
	private DuplicateStepAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getDuplicateStepItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getDuplicateStepButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.MENU_ICON_DUPLICATE_STEP));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.MENU_ICON_DUPLICATE_STEP));
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
		putValue(Action.ACTION_COMMAND_KEY, "duplicate-step");
	}
	
	public static DuplicateStepAction getInstance(JPanel parent){
		if (action == null){
			action =  new DuplicateStepAction();
		}
		action.parent=parent;
		return action;
	}
	
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.Duplicate_STEP);
		TreeTestController.setTestDirty(true);
	}
}
