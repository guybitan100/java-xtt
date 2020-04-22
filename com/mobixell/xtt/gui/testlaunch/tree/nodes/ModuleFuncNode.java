
package com.mobixell.xtt.gui.testlaunch.tree.nodes;
import com.mobixell.xtt.gui.testlaunch.table.ParamDataItem;


public class ModuleFuncNode extends AssetNode{
	private static final long serialVersionUID = -394644410606176446L;
	private String sModule;
    private String sFunction;
   

    public ModuleFuncNode(AssetNode parent ,Object userObject) {
    	super(userObject,NodeType.MOD_FUN);
    	
      }
    
    public void linkMFParamToStep(Boolean use,String sModule,String sFunction,ParamDataItem param) {
    	this.sModule=sModule;
        this.sFunction=sFunction;
    }

    public String getModule() {
		return sModule;
	}
	public void setModule(String sModule) {
		this.sModule = sModule;
	}
	public String getFunction() {
		return sFunction;
	}
	public void setFunction(String sFunction) {
		this.sFunction = sFunction;
	}
	
}
