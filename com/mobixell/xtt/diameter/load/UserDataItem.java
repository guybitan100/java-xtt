package com.mobixell.xtt.diameter.load;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
/**
 * @author Guy Bitan
 *
 */
public class UserDataItem implements Delayed{
	private String sSessionId = "";
	private String msisdn = null;
	private String ipAddress = null;
	private String ruleCat ="";
	private String ruleSubCat ="";
	private String newRuleSubCat ="";
	private String newRuleCat ="";
	private String pushToPerfom = "";
	private long   waitUntil;
	private int    scenario ;

	public final String PUSH_RAR_U = "RE_AUTH_REQUEST-UPDATE";
	public final String PUSH_RAR_T = "RE_AUTH_REQUEST-REMOVE";
	
	
	public UserDataItem  (String sSessionId,String msiISDN,String ipAddress,String [] currentRule,String [] newRule, int scenario)
	{
		setsSessionId(sSessionId);
		setMsisdn(msiISDN);
		setIpAddress(ipAddress);
		setRuleCat(currentRule[0]);
		setRuleSubCat(currentRule[1]);
		setNewRuleCat(newRule[0]);
		setNewRuleSubCat(newRule[1]);
		setScenario(scenario);	 
	}
public UserDataItem  ()
{		
		setsSessionId(null);
		setMsisdn(null);
		setIpAddress(null);
		setRuleCat(null);
		setRuleSubCat(null);
		setNewRuleCat(null);
		setNewRuleSubCat(null);
		setScenario(0);	 
	}
	public String getStrTable(String sessionId)
	{
		String subCat = !getRuleSubCat().equalsIgnoreCase(getRuleCat()) ? "             RuleSubCat      = " + getRuleSubCat() : "";
		String newSubCat = !getNewRuleSubCat().equalsIgnoreCase(getNewRuleCat()) ? "             NewRuleSubCat   = " + getNewRuleSubCat() : "";
		return "S-Id: " + sessionId + "\n" + 
			   "             MsiIsdn         = " + getMsisdn() + "\n"
			 + "             WaitUntil       = " + getWaitUntil() + "\n"
	         + "             IpAddress       = " + getIpAddress() + "\n"
	         + "             RuleCat         = " + getRuleCat() + "\n"
	           + subCat +  "\n" +
	           "             NewRuleCat      = " + getNewRuleCat()+ "\n" 
	           + newSubCat + "\n"
	         + "             PushToPerformed = " + getPushToPerfom() + "\n"
	         + "             Scenario        = " + scenario + "\n"
		     + "             RequestCount    = " + DiameterWorkerServer.requestcount;
	}
	public void copyFromUser (UserDataItem user)
	{
		this.setIpAddress(user.getIpAddress());
		this.setMsisdn(user.getMsisdn());
		this.setNewRuleCat(user.getNewRuleCat());
		this.setNewRuleSubCat(user.getNewRuleSubCat());
		this.setPushToPerfom(user.getPushToPerfom());
		this.setRuleCat(user.getRuleCat());
		this.setRuleSubCat(user.getRuleSubCat());
		this.setScenario(user.getScenario());
		this.setsSessionId(user.getsSessionId());
		this.setWaitUntil(user.getWaitUntil());
	}
	public String getsSessionId() {
		return sSessionId;
	}
	public void setsSessionId(String sSessionId) {
		this.sSessionId = sSessionId;
	}
	public String getRuleSubCat() {
		return this.ruleSubCat;
	}
	public void setRuleSubCat(String ruleSubCat) {
		this.ruleSubCat = ruleSubCat;
	}
	public String getRuleCat() {
		return ruleCat;
	}
	public void setRuleCat(String ruleCat) {
		this.ruleCat = ruleCat;
	}
	public String getNewRuleCat() {
		return newRuleCat;
	}
	public void setNewRuleCat(String newRuleCat) {
		this.newRuleCat = newRuleCat;
	}
	public String getNewRuleSubCat() {
		return newRuleSubCat;
	}
	public void setNewRuleSubCat(String newRuleSubCat) {
		this.newRuleSubCat = newRuleSubCat;
	}
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public int getScenario() {
		return scenario;
	}
	public void setScenario(int scenario) {
		this.scenario = scenario;
	}
	public String getPushToPerfom() {
		return pushToPerfom;
	}

	public void setPushToPerfom(String pushToPerfom) {
		this.pushToPerfom = pushToPerfom;
	}
	public long getWaitUntil() {
		if (waitUntil-System.currentTimeMillis()>0)
			return (waitUntil-System.currentTimeMillis());
		else
			return 0;
	}
	public void setWaitUntil(long waitUntil) {
		this.waitUntil = waitUntil;
	}
	public long getDelay(TimeUnit unit)
	{
		return unit.convert(waitUntil - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}
	public int compareTo(Delayed o)
	{
		return new Long(getDelay(TimeUnit.MILLISECONDS)).compareTo(o.getDelay(TimeUnit.MILLISECONDS));
	}
}