/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.SubtestDialog;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;

public class AddSubTestAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	SubtestDialog subTestDialog;
	private static AddSubTestAction action;
	
	private AddSubTestAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getAddSubTestItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getAddSubTestButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_SUBTEST));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_SUBTEST));
		putValue(Action.ACTION_COMMAND_KEY, "add-sabetest");
	}
	
	public static AddSubTestAction getInstance(){
		if (action == null){
			action =  new AddSubTestAction();
		}
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try
		{
			SubtestDialog.showDialog(TreeTestController.getTestLauncherGui(),null);
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
