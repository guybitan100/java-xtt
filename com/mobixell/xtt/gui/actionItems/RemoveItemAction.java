/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JPanel;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.WaitDialog;
import com.mobixell.xtt.gui.testlaunch.panels.TreePanel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController.ActionType;
import com.mobixell.xtt.images.ImageCenter;

public class RemoveItemAction extends IgnisAction {
	
	private static final long serialVersionUID = 1L;
	JPanel parent = null;
	private static RemoveItemAction action;
	
	private RemoveItemAction(){
		super();
		putValue(Action.NAME, XttMapping.getInstance().getItemRemoveMenuItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getRemoveItemButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_DELETE_ITEM));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_DELETE_ITEM));
		putValue(Action.ACTION_COMMAND_KEY, "remove-item");
	}
	
	public static RemoveItemAction getInstance(JPanel parent){
		if (action == null){
			action =  new RemoveItemAction();
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
						((TreePanel)(parent)).treeTestController.handleTreeActions(ActionType.DELETE);
						WaitDialog.launchWaitDialog("Delete item ...", null);
						TreeTestController.setTestDirty(true);
					} catch (Exception e1) {
						XTTProperties.printFail("Fail to delete item");
					}
				} finally {
					WaitDialog.endWaitDialog();
				}
			}
		}).start();
	}

}
