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

public class SuspendParamAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static SuspendParamAction action;
	
	private SuspendParamAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getSuspendParamItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getSuspendParamButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_PAUSE));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_PAUSE));
		putValue(Action.ACTION_COMMAND_KEY, "suspend-parameter");
	}
	
	public static SuspendParamAction getInstance(JPanel parent){
		if (action == null){
			action =  new SuspendParamAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.SUSPEND_PARAM);
	}
}
