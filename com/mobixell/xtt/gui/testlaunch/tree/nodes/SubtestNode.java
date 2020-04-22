
package com.mobixell.xtt.gui.testlaunch.tree.nodes;
public class SubtestNode extends AssetNode {
		private static final long serialVersionUID = -5392395068390507837L;

	public SubtestNode(AssetNode parent,Object userObject) {
    	super(userObject,NodeType.SUBTEST);
    	setSelected(true);
      }
}
