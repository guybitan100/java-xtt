/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.images.ImageCenter;

public class AddTestToListAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private static AddTestToListAction action;
	
	private AddTestToListAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getAddTestItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getAddSubTestButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_TEST));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_TEST));
		putValue(Action.ACTION_COMMAND_KEY, "add-test");
	}
	
	public static AddTestToListAction getInstance(){
		if (action == null){
			action =  new AddTestToListAction();
		}
		return action;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		XTTGui.getRunnerTreePanel().addTestToList();
	}
}
