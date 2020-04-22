
package com.mobixell.xtt.gui.testlaunch.tree.nodes;


public class ThreadNode extends AssetNode {
	private static final long serialVersionUID = 5002535651741330864L;
	public static int id=1;
	
    public ThreadNode(String name) {
    	super(name,NodeType.THREAD);
    	id++;
      }
}
