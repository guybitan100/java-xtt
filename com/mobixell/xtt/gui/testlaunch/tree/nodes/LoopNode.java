
package com.mobixell.xtt.gui.testlaunch.tree.nodes;


public class LoopNode extends AssetNode {
	private static final long serialVersionUID = 5002535651741330864L;
	public static int id=1;
	public String start = "0";
	public String stop = "1";
	public String step = "1";
    public LoopNode(String name,String start,String stop,String step) {
    	super(name,NodeType.LOOP);
    	this.start=start;
    	this.stop=stop;
    	this.step=step;
    	id++;
      }
	public String getStart() {
		return start;
	}
	public void setStart(String start) {
		this.start = start;
	}
	public String getStop() {
		return stop;
	}
	public void setStop(String stop) {
		this.stop = stop;
	}
	public String getStep() {
		return step;
	}
	public void setStep(String step) {
		this.step = step;
	}
}
