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

public class ChangeSubTestAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	
	private static ChangeSubTestAction action;
	
	private ChangeSubTestAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getChangeSubTestItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getChangeSubTestButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_IMPORT_WIZ));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_IMPORT_WIZ));
		putValue(Action.ACTION_COMMAND_KEY, "change-sabetest");
	}
	
	public static ChangeSubTestAction getInstance(){
		if (action == null){
			action =  new ChangeSubTestAction();
		}
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			SubtestDialog.showDialog(TreeTestController.getTestLauncherGui(), TreeTestController.getSubTestNode());
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
