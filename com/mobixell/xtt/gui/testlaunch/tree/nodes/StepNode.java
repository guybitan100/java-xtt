
package com.mobixell.xtt.gui.testlaunch.tree.nodes;

public class StepNode extends AssetNode {
	private static final long serialVersionUID = 5002535651741330864L;
    public StepNode(AssetNode parent,Object userObject) 
    	{
    	super(userObject,NodeType.STEP);
    	setSelected(true);
      }
}