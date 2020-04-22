
package com.mobixell.xtt.gui.testlaunch.tree.nodes;


public class ParamNode extends AssetNode {
	private static final long serialVersionUID = 5002535651741330864L;
	private boolean isInGroup;
    public ParamNode(AssetNode parent ,Object userObject) {
    	super(userObject,NodeType.PARMS);
    	isInGroup = false;
      }
	public boolean isInGroup()
	{
		return isInGroup;
	}
	public void setInGroup(boolean isInGroup)
	{
		this.isInGroup = isInGroup;
	}
}
