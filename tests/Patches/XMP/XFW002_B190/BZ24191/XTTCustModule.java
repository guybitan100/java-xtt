/*
javac -cp /home/erkans/xmp/lib/mig.jar XTTCustModule.java
jar -cvf XTTCustModule.jar XTTCustModule.class
*/

import com.mobixell.cee.Trace;
import com.mobixell.intf.cust.CustModuleIntf;
import com.mobixell.intf.snmp.Cust;
import java.util.Map;
import java.util.List;
import com.mobixell.mig.cust.Alert;

public class XTTCustModule implements CustModuleIntf
{	private Alert alert;
	
	public XTTCustModule(Map config)
	{	alert = new Alert(XTTCustModule.class.getName());
		alert.startupComplete("XTTCustModule");
	}

	public void updateContexts(Map context)
	{	
		context.remove("HTA_ReqHdr_clientheaderremovethis");
		context.put("HTA_ReqHdr_clientheaderchangethis", "def");
		context.put("HTA_ReqHdr_clientheaderaddthis", "ghi");
		context.put("REQMOD_Items", (String)context.get("REQMOD_MatchedPattern") + (String)context.get("REQMOD_MatchedData") + (String)context.get("REQMOD_CurrentProtocol") + (String)context.get("REQMOD_CurrentDomainName") + (String)context.get("REQMOD_CurrentPort") + (String)context.get("REQMOD_CurrentPath") + (String)context.get("REQMOD_CurrentSearchString"));
		context.put("$0", context.get("$0") + "_01");
		context.put("$1", context.get("$1") + "_02");
		context.put("$2", context.get("$2") + "_03");
		context.put("$3", context.get("$3") + "_04");
		Trace.info("XTTCustModule manipulates some of context variables!");
		
		alert.custInformationTrap("Wrong_OID","this is my custome trap");
		Trace.info("CUST MODULE...   XTTCustModule");
	}

	public Cust.PollResult pollForStatistics()
	{	return new Cust.PollResult();
	}

	public void resetStatistics()
	{	return;
	}
}