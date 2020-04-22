package com.mobixell.xtt.sis;

import ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServicePOA;
import com.mobixell.xtt.*;

public class SubsInfoAccessServiceImpl extends SubsInfoAccessServicePOA
{
    public static final String tantau_sccsid = "@(#)$Id: SubsInfoAccessServiceImpl.java,v 1.2 2006/07/21 17:04:33 cvsbuild Exp $";
    //private org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();


    public ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.GrantDecisionStruct grantService
    (
        int aNumberType,
        java.lang.String aNumber,
        java.lang.String aService,
        java.lang.String aServiceAction,
        java.lang.String aLanguage
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        XTTProperties.printInfo(this.getClass().getName()+".grantService"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    java.lang.String aNumber       :"+aNumber
            +"\n    java.lang.String aService      :"+aService
            +"\n    java.lang.String aServiceAction:"+aServiceAction
            +"\n    java.lang.String aLanguage     :"+aLanguage
            +"\n)"
        );
        return null;
    }



    public ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.AttributValueStruct[] getSubscriptionInfo
    (
        int aNumberType,
        java.lang.String aNumber,
        short aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        XTTProperties.printInfo(this.getClass().getName()+".getSubscriptionInfo"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    java.lang.String aNumber       :"+aNumber
            +"\n    short aIncludeInfos            :"+aIncludeInfos
            +"\n)"
        );
        return null;
    }

    public ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.AttributValueStruct[][] getSubscriptionInfoList
    (
        int aNumberType,
        java.lang.String[] aNumberList,
        short aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        XTTProperties.printInfo(this.getClass().getName()+".getSubscriptionInfoList"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    java.lang.String[] aNumberList :"+aNumberList
            +"\n    short aIncludeInfos            :"+aIncludeInfos
            +"\n)"
        );
        return null;
    }

    public java.lang.String getAttributeValueDescriptions()
    {
        XTTProperties.printInfo(this.getClass().getName()+".getSubscriptionInfoList()");
        return null;
    }


}