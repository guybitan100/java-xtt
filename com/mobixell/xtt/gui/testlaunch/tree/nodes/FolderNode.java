
package com.mobixell.xtt.gui.testlaunch.tree.nodes;


public class FolderNode extends AssetNode {
	private static final long serialVersionUID = 5002535651741330864L;
	 private String path; 
	 
    public FolderNode(Object userObject,String path) {
    	super(userObject,NodeType.FOLDER);
    	this.path=path;
      }

	public String getFolderPath()
	{
		return path;
	}
}
