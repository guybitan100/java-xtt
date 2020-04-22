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

public class SearchAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private static SearchAction action;
	private JPanel parent = null;
	
	private SearchAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getSearchButton());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getSearchItem());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_SEARCH));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_SEARCH));
		putValue(Action.ACTION_COMMAND_KEY, "search");
	}
	
	public static SearchAction getInstance(JPanel parent){
		if (action == null){
			action =  new SearchAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.SEARCH);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
}
