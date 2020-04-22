/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.gui.mapping;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import com.mobixell.xtt.XTTProperties;



/**
 * Mapping Jsystem GUI
 * @author Yaron
 * This class is used a GUI mapping reader tool. it reads properties from the  JSystemMappingProperties.
 * the properties are used by the other clases to work with the TAS GUI objects
 * when one of the GUI objects are changed, the only change that is needed is in the file
 */
public class XttMapping  {

	Properties properties = new Properties();

	File file = new File("JSystemMapping.properties");
	
	static XttMapping mapping;

	public XttMapping() {
		
		InputStream stream =null;
		try {
			stream = 
				getClass().getClassLoader().getResourceAsStream("com/mobixell/xtt/gui/mapping/XttMapping.properties");                
			if (stream == null){
				throw new RuntimeException("Jar file was not found");
			}
			properties.load(stream);
		} catch (Exception e) {
		XTTProperties.printDebug("Xtt Mapping file was not found");
		}finally {
			try{stream.close();}catch(Exception e){};
		}
}
	
	public static XttMapping getInstance(){
		if (mapping == null){
			 mapping = new XttMapping();
		}
		return mapping;
	}
	
	/*
	 * Main Frame
	 */
	public String getExitButton () {return returnStringProperties("EXIT_BUTTON");}
	
	
	/*
	 * T-Run GUI
	 * 
	 */
		
	public String getRemoveItemButton () {return returnStringProperties("REMOVE_ITEM_BUTTON");}
	public String getStopTestButton () {return returnStringProperties("STOP_TEST_BUTTON");}
	public String getRemoveAllItemsButton () {return returnStringProperties("REMOVE_ALL_ITEM_BUTTON");}
	public String getSaveButton () {return returnStringProperties("SAVE_BUTTON");}
	public String getAutoSaveButton () {return returnStringProperties("AUTO_SAVE_BUTTON");}
	public String getSaveAsButton () {return returnStringProperties("SAVE_AS_BUTTON");}
	public String getOpenTestButton(){return returnStringProperties("OPEN_TEST_BUTTON");}
	public String getItemMoveUpButton () {return returnStringProperties("ITEM_MOVE_UP_BUTTON");}
	public String getItemMoveDownButton () {return returnStringProperties("ITEM_MOVE_DOWN_BUTTON");}
	public String getItemMoveToBottomButton () {return returnStringProperties("ITEM_MOVE_TO_BOTTOM_BUTTON");}
	public String getItemMoveToTopButton () {return returnStringProperties("ITEM_MOVE_TO_TOP_BUTTON");}
	public String getShowLogButton () {return returnStringProperties("SHOW_LOG_BUTTON");}
	public String getShowRunnerButton () {return returnStringProperties("SHOW_RUNNER_BUTTON");}
	public String getRunTestButton () {return returnStringProperties("RUN_TEST_BUTTON");}
	public String getSearchButton () {return returnStringProperties("SEARCH_BUTTON");}
	public String getShowXmlButton () {return returnStringProperties("SHOW_XML_BUTTON");}
	
	public String getCollapseTreeButton () {return returnStringProperties("COLLAPSE_BUTTON");}
	public String getExpandTreeButton () {return returnStringProperties("EXPAND_BUTTON");}
	public String getCheckTreeButton () {return returnStringProperties("CHECK_ROOT_BUTTON");}
	public String getUnCheckTreeButton () {return returnStringProperties("UNCHECK_ROOT_BUTTON");}
	public String getEditParamButton () {return returnStringProperties("EDIT_PARAM_BUTTON");}
	public String getCopyStepButton () {return returnStringProperties("COPY_STEP_BUTTON");}
	public String getCopyThreadButton () {return returnStringProperties("COPY_THREAD_BUTTON");}
	public String getCopyLoopButton () {return returnStringProperties("COPY_LOOP_BUTTON");}
	public String getPasteStepButton () {return returnStringProperties("PASTE_STEP_BUTTON");}
	public String getPasteThreadButton () {return returnStringProperties("PASTE_THREAD_BUTTON");}
	public String getPasteLoopButton () {return returnStringProperties("PASTE_LOOP_BUTTON");}
	public String getDuplicateStepButton () {return returnStringProperties("DUPLICATE_STEP_BUTTON");}
	public String getCopyFileButton () {return returnStringProperties("COPY_FILE_BUTTON");}
	public String getRenameFileButton () {return returnStringProperties("RENAME_FILE_BUTTON");}
	public String getCopyFolderButton () {return returnStringProperties("COPY_FOLDER_BUTTON");}
	public String getPasteFileButton () {return returnStringProperties("PASTE_FILEFOLDER_BUTTON");}
	public String getRunStepButton () {return returnStringProperties("RUN_STEP_BUTTON");}
	public String getSuspendParamButton () {return returnStringProperties("SUSPEND_PARAM_BUTTON");}
	public String getUnSuspendParamButton () {return returnStringProperties("UNSUSPEND_PARAM_BUTTON");}
	
	public String getConfParamButton () {return returnStringProperties("CONF_PARAM_BUTTON");}
	public String getAddSubTestButton () {return returnStringProperties("SUB_TEST_BUTTON");}
	public String getAddTestButton () {return returnStringProperties("TEST_BUTTON");}
	public String getChangeSubTestButton () {return returnStringProperties("CHANGE_SUB_TEST_BUTTON");}
	public String getAddThreadButton () {return returnStringProperties("THREAD_BUTTON");}
	public String getAddLoopButton () {return returnStringProperties("LOOP_BUTTON");}
	public String getRemoveThreadButton () {return returnStringProperties("REMOVE_THREAD_BUTTON");}
	public String getRemoveLoopButton () {return returnStringProperties("REMOVE_LOOP_BUTTON");}
	public String getAddToThreadButton () {return returnStringProperties("TO_THREAD_BUTTON");}
	public String getAddToLoopButton () {return returnStringProperties("TO_LOOP_BUTTON");}
	public String getConfParam(){return returnStringProperties("CONF_PARAM_LIST");}
	public String getChangeTestNameButton () {return returnStringProperties("CHANGE_TEST_NAME_BUTTON");}
	public String getChangeStepNameButton () {return returnStringProperties("CHANGE_STEP_NAME_BUTTON");}
	public String getCreateFolderButton () {return returnStringProperties("CREATE_FOLDER_BUTTON");}
	public String getDeleteFolderButton () {return returnStringProperties("DELETE_FOLDER_BUTTON");}
	public String getDeleteFileButton () {return returnStringProperties("DELETE_FILE_BUTTON");}
	
	public String getItemRemoveAllMenuItem(){return returnStringProperties("ITEM_MENU_REMOVE_ALL");}
	public String getItemRemoveMenuItem(){return returnStringProperties("ITEM_MENU_REMOVE");}
	public String getIStopTestMenuItem(){return returnStringProperties("ITEM_MENU_STOP_TEST");}
	public String getSaveMenuItem(){return returnStringProperties("ITEM_MENU_SAVE");}
	public String getAutoSaveMenuItem () {return returnStringProperties("AUTO_SAVE_ITEM");}
	public String getSaveAsMenuItem(){return returnStringProperties("ITEM_MENU_SAVE_AS");}
	public String getOpenTestMenuItem(){return returnStringProperties("ITEM_MENU_OPEN_TEST");}
	public String getCreateFolderMenuItem () {return returnStringProperties("CREATE_FOLDER");}
	public String getDeleteFolderMenuItem () {return returnStringProperties("DELETE_FOLDER");}
	public String getDeleteFileMenuItem () {return returnStringProperties("DELETE_FILE");}
	
	public String getItemMoveDownMenuItem(){return returnStringProperties("ITEM_MENU_MOVE_DOWN");}
	public String getItemMoveUpMenuItem(){return returnStringProperties("ITEM_MENU_MOVE_UP");}
	public String getItemMoveToBottomMenuItem(){return returnStringProperties("ITEM_MENU_MOVE_TO_BOTTOM");}
	public String getItemMoveToTopMenuItem(){return returnStringProperties("ITEM_MENU_MOVE_TO_TOP");}
	public String getAddSubTestItem () {return returnStringProperties("ITEM_ADD_SUB_TEST");}
	public String getAddTestItem () {return returnStringProperties("ITEM_ADD_TEST");}
	public String getChangeSubTestItem () {return returnStringProperties("ITEM_CHANGE_SUB_TEST");}
	public String getAddThreadItem () {return returnStringProperties("ITEM_ADD_THREAD");}
	public String getAddLoopItem () {return returnStringProperties("ITEM_ADD_LOOP");}
	public String getRemoveThreadItem () {return returnStringProperties("ITEM_REMOVE_THREAD");}
	public String getRemoveLoopItem () {return returnStringProperties("ITEM_REMOVE_LOOP");}
	public String getAddToThreadItem () {return returnStringProperties("ITEM_ADD_TO_THREAD");}
	public String getAddToLoopItem () {return returnStringProperties("ITEM_ADD_TO_LOOP");}
	public String getChangeTestNameItem () {return returnStringProperties("CHANGE_TEST_NAME");}
	public String getSearchItem () {return returnStringProperties("SEARCH");}
	public String getChangeStepNameItem () {return returnStringProperties("CHANGE_STEP_NAME");}
	public String getCollapseTreeItem () {return returnStringProperties("COLLAPSE");}
	public String getExpandTreeItem () {return returnStringProperties("EXPAND");}
	public String getCheckTreeItem () {return returnStringProperties("CHECK_ROOT");}
	public String getUnCheckTreeItem () {return returnStringProperties("UNCHECK_ROOT");}
	public String getEditParamItem () {return returnStringProperties("EDIT_PARAM");}
	public String getCopyStepItem () {return returnStringProperties("COPY_STEP");}
	public String getCopyThreadItem () {return returnStringProperties("COPY_THREAD");}
	public String getCopyLoopItem () {return returnStringProperties("COPY_LOOP");}
	public String getPasteStepItem () {return returnStringProperties("PASTE_STEP");}
	public String getPasteThreadItem () {return returnStringProperties("PASTE_THREAD");}
	public String getPasteLoopItem () {return returnStringProperties("PASTE_LOOP");}
	public String getDuplicateStepItem () {return returnStringProperties("DUPLICATE_STEP");}
	public String getCopyFileItem () {return returnStringProperties("ITEM_COPY_FILE");}
	public String getRenameFileItem () {return returnStringProperties("ITEM_RENAME_FILE");}
	public String getCopyFolderItem () {return returnStringProperties("ITEM_COPY_FOLDER");}
	public String getPasteFileItem () {return returnStringProperties("ITEM_PASTE_FILEFOLDER");}
	public String getRunStepItem () {return returnStringProperties("RUN_STEP");}
	public String getSuspendParamItem () {return returnStringProperties("SUSPEND_PARAM");}
	public String getUnSuspendParamItem () {return returnStringProperties("UNSUSPEND_PARAM");}
	public String getShowLogItem () {return returnStringProperties("SHOW_LOG");}
	public String getShowRunnerItem () {return returnStringProperties("SHOW_RUNNER");}
	public String getRunTestItem () {return returnStringProperties("RUN_TEST");}
	public String getShowXmlItem () {return returnStringProperties("SHOW_XML");}
	/**
	 * This method is used to return properties from the properties file
	 * formated as trimmed strings
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public String returnStringProperties(String propertyName){
			
			try{
				properties.getProperty(propertyName).trim();
			}catch(Exception e){
				XTTProperties.printDebug("Property "+propertyName+" was not found in the XttMappingFile");
			}

     return properties.getProperty(propertyName).trim();
	}
	/**
	 * This method is used to retrun properties from the properties file
	 * formated as integers 
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	
	public int returnIntProperty(String propertyName){
			
			try{
				properties.getProperty(propertyName).trim();
			}catch(Exception e){
			XTTProperties.printDebug("Property "+propertyName+" was not found in the XttMappingFile");
			}

	  return Integer.parseInt(properties.getProperty(propertyName).trim());
	}
	
}
