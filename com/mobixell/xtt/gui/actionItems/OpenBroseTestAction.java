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
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;

public class OpenBroseTestAction extends IgnisAction {

	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static OpenBroseTestAction action;
	
	private OpenBroseTestAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getOpenTestMenuItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getOpenTestButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_EDIT));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_EDIT));
		putValue(Action.ACTION_COMMAND_KEY, "open-test");
	}
	
	public static OpenBroseTestAction getInstance(JPanel parent){
		if (action == null){
			action =  new OpenBroseTestAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.OPEN_TEST);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
