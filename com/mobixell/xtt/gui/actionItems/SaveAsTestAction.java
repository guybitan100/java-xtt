/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;

public class SaveAsTestAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	private JPanel parent = null;
	private static SaveAsTestAction action;
	
	private SaveAsTestAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getSaveAsMenuItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getSaveAsButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_SAVE_AS));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_SAVE_AS));
		putValue(Action.ACTION_COMMAND_KEY, "saveAs-test");
	}
	
	public static SaveAsTestAction getInstance(JPanel parent){
		if (action == null){
			action =  new SaveAsTestAction();
		}
		action.parent=parent;
		return action;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		(new Thread() {
			public void run() {
				try {
					try {
						((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.SAVEAS_TEST);
					} catch (Exception e1) {
						XTTProperties.printFail("Fail to save test");
					}
				} finally {
				}
			}
		}).start();
	}

}
