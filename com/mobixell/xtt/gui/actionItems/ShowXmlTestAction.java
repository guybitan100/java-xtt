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

public class ShowXmlTestAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static ShowXmlTestAction action;
	
	private ShowXmlTestAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getShowXmlItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getShowXmlButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_XML));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_XML));
		putValue(Action.ACTION_COMMAND_KEY, "show-xml");
	}
	
	public static ShowXmlTestAction getInstance(JPanel parent){
		if (action == null){
			action =  new ShowXmlTestAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.SHOW_XML);
	}
}
