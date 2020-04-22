
package com.mobixell.xtt.gui.testlaunch.tree.nodes;


public class FileNode extends AssetNode {
	private static final long serialVersionUID = 5002535651741330864L;
    private String fullPath;
    private String path; 
    private String fileType = "xml";
    boolean isTest = true;
    public boolean isTest()
	{
		return isTest;
	}
	public void setTest(boolean isTest)
	{
		this.isTest = isTest;
	}
	public FileNode(Object userObject,String fullPath,String path) {
    	super(userObject,NodeType.FILE);
    	this.fullPath=fullPath;
    	this.path=path;
      }
	public String getFileFullPath() {
		return fullPath;
	}
	public String getFilePath() {
		return path;
	}
	public String getFullPath() {
		return fullPath;
	}
	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
}
