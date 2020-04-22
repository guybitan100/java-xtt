/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;

public class AutoSaveTestAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private static AutoSaveTestAction action;
	
	private AutoSaveTestAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getAutoSaveButton());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getAutoSaveMenuItem());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_UNCHECK));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_UNCHECK));
		putValue(Action.ACTION_COMMAND_KEY, "auto-save");
	}
	
	public static AutoSaveTestAction getInstance(){
		if (action == null){
			action =  new AutoSaveTestAction();
		}
		return action;
	}
	
	public void actionPerformed(ActionEvent e) {

		try {
			if (TreePanel.autoSaveButton.isSelected())
			{
				putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_CHECK));
				putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_CHECK));
				TreeTestController.test.setAutoSaveEnabled(true);
			}
			else
			{
				putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_UNCHECK));
				putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_UNCHECK));
				TreeTestController.test.setAutoSaveEnabled(false);
			}
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
