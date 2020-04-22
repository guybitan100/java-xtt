/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.actionItems;

import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.Action;
import com.mobixell.xtt.gui.mapping.XttMapping;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.table.ParamDataItem;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;

public class EditConfParamAction extends IgnisAction {

	private static final long serialVersionUID = 1L;
	private static EditConfParamAction action;
	
	private EditConfParamAction()
	{
		super();
		putValue(Action.NAME, XttMapping.getInstance().getEditParamItem());
		putValue(Action.SHORT_DESCRIPTION, XttMapping.getInstance().getEditParamButton());
		putValue(Action.SMALL_ICON, ImageCenter.getInstance().getImage(ImageCenter.ICON_CURRENT_FIXTURE));
		putValue(Action.LARGE_ICON_KEY, ImageCenter.getInstance().getImage(ImageCenter.ICON_CURRENT_FIXTURE));
		putValue(Action.ACTION_COMMAND_KEY, "edit-conf-param");
	}
	
	public static EditConfParamAction getInstance(){
		if (action == null){
			action =  new EditConfParamAction();
		}
		return action;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
   	    Vector<ParamDataItem> vec_items = null;
   	    try {
   	    	String sModFunc [] = TreeTestController.getModFuncName().split("/");
   	    	String sModule = sModFunc[0];
   	    	String sFunction =  sModFunc[1];
   	    	boolean isMandatory = TreeTestController.isModFuncMandatory();
			vec_items = TreeTestController.getModFunParam(null);
			TestLauncherGui.parameterDialog.edit(isMandatory,sModule,sFunction,vec_items);
			TreeTestController.setTestDirty(true);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
   	    
		
	}
}
