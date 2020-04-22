package com.mobixell.xtt.sis;

import ch.intersys.sis.corba.idl.SubsSelectedInfoAccessServiceModul.SubsSelectedInfoAccessServicePOA;
import ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.AttributValueStruct;
import com.mobixell.xtt.*;

/**<pre><code>
+41796543210                      number gets decoded as follows:
  |||+++++++--------------------- 7 numbers used as bits
  ||| 011111111111111111111111
  ||| ||||||||||||||||||||||||
  ||| ||||||||||||||||||||||++---- getStatus 0-3                            2bit                      11=      3 shift  0
  ||| ||||||||||||||||||++++------ getActiveScCustomer 0-6                  4bit                  111100=     60 shift  2
  ||| ||||||||||||||||++---------- getBarringState 0-2                      2bit                11000000=    192 shift  6
  ||| ||||||||||||||++------------ getUserLanguage 0-5 (only use 0-3)       2bit              1100000000=    768 shift  8
  ||| ||||||||||||++-------------- getComboxType 0-2                        2bit            110000000000=   3072 shift 10
  ||| ||||||||||++---------------- getVasLimitReached 0-2                   2bit          11000000000000=  12288 shift 12
  ||| |||||||+++------------------ getVasBlocked 0-4                        3bit       11100000000000000= 114688 shift 14
  ||| |||||++--------------------- getAdultContent 0-2                      2bit     1100000000000000000= 393216 shift 17
  ||| |||++----------------------- getCustomerMainSegment 0-9(only use 0-3) 2bit   110000000000000000000=1572864 shift 19
  ||| |++------------------------- getBirthdateStatus 0-2                   2bit 11000000000000000000000=6291456 shift 21
  ||| +--------------------------- UNUSABLE, ALWAYS 0!                      1bit 11111111111111111111111=8388607
  |||
  ||+---------------------------- getBRAND
  |+----------------------------- getCustomerAge 0-9
  +------------------------------ getSubscriptionType
  
getPaymentType->getActiveScCustomer                            
getComboxLanguage->getUserLanguage                               
getContractLanguage->getUserLanguage                             
getBirthdate->getCustomerAge                                     
getCustomerFineSegment->getCustomerMainSegment                   
getCreditLimit-> fix 1000
POSTPAID/PREPAID->getActiveScCustomer 2,4 und 6

Numbers starting with 6 are decoded as 41790000009
Number starting with 53 trhow an exception whichs number are the last 2 digits.
Numbers starting with 3 are delayed by the following decoded in miliseconds, the number is decoded as 41790000009 or the override number is used.
</code></pre>
*/
public class SubsSelectedInfoAccessServiceImpl extends SubsSelectedInfoAccessServicePOA
{
    public static final String tantau_sccsid = "@(#)$Id: SubsSelectedInfoAccessServiceImpl.java,v 1.17 2008/04/25 09:30:24 rsoder Exp $";
    //private org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();

    public ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.AttributValueStruct[] getSelectedSubscriptionInfo
    (
        int aNumberType,
        java.lang.String aNumber,
        java.lang.String aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        XTTProperties.printVerbose(this.getClass().getName()+".getSelectedSubscriptionInfo"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    java.lang.String aNumber       :"+aNumber
            +"\n    short aIncludeInfos            :"+aIncludeInfos
            +"\n)"
        );
        synchronized(siskey)
        {
            siscount++;
            siskey.notifyAll();
        }
        AttributValueStruct[] val=getValues(aNumberType,aNumber,aIncludeInfos);
        XTTProperties.printVerbose(this.getClass().getName()+".getSelectedSubscriptionInfo returning 1 AttributValueStruct");
        int wait=0;
        synchronized(siskey)
        {
            siscount++;
            wait=siscount*waittime;
            siskey.notifyAll();
        }
        try
        {
            if(waittime>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSelectedSubscriptionInfo response delay "+waittime);
                Thread.sleep(waittime);
            }
        } catch (Exception e){}
        try
        {
            if(wait>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSelectedSubscriptionInfo per call delay "+wait);
                Thread.sleep(wait);
            }
        } catch (Exception e){}
        return val;
    }

    public ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.AttributValueStruct[][] getSelectedSubscriptionInfoList
    (
        int aNumberType,
        java.lang.String[] aNumberList,
        java.lang.String aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        XTTProperties.printVerbose(this.getClass().getName()+".getSelectedSubscriptionInfoList"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    java.lang.String[] aNumberList :"+aNumberList.length+" entries"
            +"\n    java.lang.String aIncludeInfos :"+aIncludeInfos
            +"\n)"
        );
        AttributValueStruct[][] val=new AttributValueStruct[aNumberList.length][0];
        for(int i=0;i<aNumberList.length;i++)
        {
            val[i]=getValues(aNumberType,aNumberList[i],aIncludeInfos);
        }
        int wait=0;
        synchronized(siskey)
        {
            siscount++;
            wait=siscount*waittime;
            siskey.notifyAll();
        }
        try
        {
            if(waittime>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSelectedSubscriptionInfoList response delay "+waittime);
                Thread.sleep(waittime);
            }
        } catch (Exception e){}
        try
        {
            if(wait>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSelectedSubscriptionInfoList perCall delay "+wait);
                Thread.sleep(wait);
            }
        } catch (Exception e){}
        XTTProperties.printVerbose(this.getClass().getName()+".getSelectedSubscriptionInfoList returning "+val.length+" AttributValueStructs");
        return val;
    }





    public ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.GrantDecisionStruct grantService
    (
        int aNumberType,
        java.lang.String aNumber,
        java.lang.String aService,
        java.lang.String aServiceAction,
        java.lang.String aLanguage
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        XTTProperties.printVerbose(this.getClass().getName()+".grantService"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    java.lang.String aNumber       :"+aNumber
            +"\n    java.lang.String aService      :"+aService
            +"\n    java.lang.String aServiceAction:"+aServiceAction
            +"\n    java.lang.String aLanguage     :"+aLanguage
            +"\n)"
        );
        int wait=0;
        synchronized(siskey)
        {
            siscount++;
            wait=siscount*waittime;
            siskey.notifyAll();
        }
        try
        {
            if(waittime>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".grantService response delay "+waittime);
                Thread.sleep(waittime);
            }
        } catch (Exception e){}
        try
        {
            if(wait>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".grantService perCall delay "+wait);
                Thread.sleep(wait);
            }
        } catch (Exception e){}
        return null;
    }

    public ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.AttributValueStruct[] getSubscriptionInfo
    (
        int aNumberType,
        java.lang.String aNumber,
        short aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        XTTProperties.printVerbose(this.getClass().getName()+".getSubscriptionInfo"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    java.lang.String aNumber       :"+aNumber
            +"\n    short aIncludeInfos            :"+aIncludeInfos
            +"\n)"
        );
        AttributValueStruct[] val=getValues(aNumberType,aNumber,aIncludeInfos+"");
        XTTProperties.printVerbose(this.getClass().getName()+".getSubscriptionInfo returning 1 AttributValueStruct");
        int wait=0;
        synchronized(siskey)
        {
            siscount++;
            wait=siscount*waittime;
            siskey.notifyAll();
        }
        try
        {
            if(waittime>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSubscriptionInfo response delay "+waittime);
                Thread.sleep(waittime);
            }
        } catch (Exception e){}
        try
        {
            if(wait>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSubscriptionInfo perCall delay "+wait);
                Thread.sleep(wait);
            }
        } catch (Exception e){}
        return val;
    }

    public ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.AttributValueStruct[][] getSubscriptionInfoList
    (
        int aNumberType,
        java.lang.String[] aNumberList,
        short aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        XTTProperties.printVerbose(this.getClass().getName()+".getSubscriptionInfoList"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    java.lang.String[] aNumberList :"+aNumberList.length+" entries"
            +"\n    short aIncludeInfos            :"+aIncludeInfos
            +"\n)"
        );
        //String[] infos=aIncludeInfos.split(";");
        AttributValueStruct[][] val=new AttributValueStruct[aNumberList.length][0];
        for(int i=0;i<aNumberList.length;i++)
        {
            val[i]=getValues(aNumberType,aNumberList[i],aIncludeInfos+"");//Short.parseShort(infos[i]));
        }
        XTTProperties.printVerbose(this.getClass().getName()+".getSubscriptionInfoList returning "+val.length+" AttributValueStructs");
        int wait=0;
        synchronized(siskey)
        {
            siscount++;
            wait=siscount*waittime;
            siskey.notifyAll();
        }
        try
        {
            if(waittime>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSubscriptionInfoList response delay "+waittime);
                Thread.sleep(waittime);
            }
        } catch (Exception e){}
        try
        {
            if(wait>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSubscriptionInfoList perCall delay "+wait);
                Thread.sleep(wait);
            }
        } catch (Exception e){}
        return val;
    }

    public java.lang.String getAttributeValueDescriptions()
    {
        XTTProperties.printVerbose(this.getClass().getName()+".getAttributeValueDescriptions()");
        int wait=0;
        synchronized(siskey)
        {
            siscount++;
            wait=siscount*waittime;
            siskey.notifyAll();
        }
        try
        {
            if(waittime>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getAttributeValueDescriptions response delay "+waittime);
                Thread.sleep(waittime);
            }
        } catch (Exception e){}
        try
        {
            if(wait>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getAttributeValueDescriptions perCall delay "+wait);
                Thread.sleep(wait);
            }
        } catch (Exception e){}
        return "getAttributeValueDescriptions";
    }

/* ************************************************************************************************* *\
 * *************************   Implementation Stuff   ********************************************** *
\* ************************************************************************************************* */


    private AttributValueStruct[] getValues
    (
        int aNumberType,
        java.lang.String aNumber,
        java.lang.String aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        String[] infos=aIncludeInfos.split(";");

        AttributValueStruct[] values=new AttributValueStruct[40];
        AttributValueStruct[] tempv =null;
        int length=0;

        for(int i=0;i<infos.length;i++)
        {
            tempv=getSingleValues(aNumberType,aNumber,Short.parseShort(infos[i]));
            for(int j=length;j<length+tempv.length;j++)
            {
                values[j]=tempv[j-length];
            }
            length=length+tempv.length;
        }
        AttributValueStruct[] returnvalues=new AttributValueStruct[length];
        for(int i=0;i<length;i++)
        {
            returnvalues[i]=values[i];
        }
        synchronized(numkey)
        {
            numcount++;
            numkey.notifyAll();
        }
        return returnvalues;
    }

    // Do not call this function directly, use getValues
    private AttributValueStruct[] getSingleValues
    (
        int aNumberType,
        java.lang.String aNumberReceived,
        short aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {

        java.lang.String aNumber=aNumberReceived;
        try
        {
            if(aNumber.startsWith("+"))
            {
                aNumber=aNumber.substring(1,aNumber.length());
            }
            aNumber=aNumber.split("/")[0];
        } catch(Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
        
        // We only take the last 7 chars for the decodeNumber or we get an integer overflow to easily.
        // The other numbers can be decoded from the string anyway.
        int decodeNumber=Integer.parseInt(aNumber.substring(aNumber.length()-7,aNumber.length()));
        String dn=aNumber;
        if(overrideNumber!=null)
        {
            decodeNumber=Integer.parseInt(overrideNumber.substring(overrideNumber.length()-7,overrideNumber.length()));
            dn=overrideNumber;
        } else if(aNumber.startsWith("53"))
        {
            XTTProperties.printDebug(this.getClass().getName()+".getSingleValues"
                +"\n("
                +"\n    int aNumberType                :"+aNumberType
                +"\n    String aNumberReceived         :"+aNumberReceived+" -> "+aNumber
                +"\n    short aIncludeInfos            :"+aIncludeInfos
                +"\n)"
                +"\ndecodedNumber as: throwException"
            );
            throwException(Integer.parseInt(aNumber.substring(aNumber.length()-2,aNumber.length()-0)));
        } else if(aNumber.startsWith("6"))
        {
            decodeNumber=Integer.parseInt("0000009");
            dn="41790000009";
        } else if(aNumber.startsWith("3"))
        {
            int delay=Integer.parseInt(dn.substring(dn.length()-10,dn.length()));
            XTTProperties.printDebug(this.getClass().getName()+".getSingleValues: delaying by "+delay+"ms");
            try
            {
                Thread.sleep(delay);
            } catch(Exception ex){}
            if(overrideNumber!=null)
            {
                decodeNumber=Integer.parseInt(overrideNumber.substring(overrideNumber.length()-7,overrideNumber.length()));
                dn=overrideNumber;
            } else
            {
                decodeNumber=Integer.parseInt("0000009");
                dn="41790000009";
            }
        }
        int subscriptionType=Integer.parseInt(dn.substring(dn.length()-10,dn.length()-9));
        int customerage=Integer.parseInt(dn.substring(dn.length()-9,dn.length()-8));
        int brand=Integer.parseInt(dn.substring(dn.length()-8,dn.length()-7));

        XTTProperties.printDebug(this.getClass().getName()+".getSingleValues"
            +"\n("
            +"\n    int aNumberType                :"+aNumberType
            +"\n    String aNumberReceived         :"+aNumberReceived+" -> "+aNumber
            +"\n    short aIncludeInfos            :"+aIncludeInfos
            +"\n)"
            +"\ndecodedNumber as: "+dn
        );

        AttributValueStruct[] values=null;
        switch(aIncludeInfos)
        {
            /*
            +41796543210                      number gets decoded as follows:
              |||+++++++--------------------- 7 numbers used as bits
              ||| 011111111111111111111111
              ||| ||||||||||||||||||||||||
              ||| ||||||||||||||||||||||++---- getStatus 0-3                            2bit                      11=      3 shift  0
              ||| ||||||||||||||||||++++------ getActiveScCustomer 0-6                  4bit                  111100=     60 shift  2
              ||| ||||||||||||||||++---------- getBarringState 0-2                      2bit                11000000=    192 shift  6
              ||| ||||||||||||||++------------ getUserLanguage 0-5 (only use 0-3)       2bit              1100000000=    768 shift  8
              ||| ||||||||||||++-------------- getComboxType 0-2                        2bit            110000000000=   3072 shift 10
              ||| ||||||||||++---------------- getVasLimitReached 0-2                   2bit          11000000000000=  12288 shift 12
              ||| |||||||+++------------------ getVasBlocked 0-4                        3bit       11100000000000000= 114688 shift 14
              ||| |||||++--------------------- getAdultContent 0-2                      2bit     1100000000000000000= 393216 shift 17
              ||| |||++----------------------- getCustomerMainSegment 0-9(only use 0-3) 2bit   110000000000000000000=1572864 shift 19
              ||| |++------------------------- getBirthdateStatus 0-2                   2bit 11000000000000000000000=6291456 shift 21
              ||| +--------------------------- UNUSABLE, ALWAYS 0!                      1bit 11111111111111111111111=8388607
              |||
              ||+---------------------------- getBRAND
              |+----------------------------- getCustomerAge 0-9
              +------------------------------ getSubscriptionType
            */
            case 1:
                values=new AttributValueStruct[11];
                values[0]=getStatus(          aNumberType,(decodeNumber&3)>>>0);
                values[1]=getMSISDN(          aNumberType,aNumber);
                values[2]=getIMSI(            aNumberType,aNumber);
                values[3]=getActiveScCustomer(aNumberType,(decodeNumber&60)>>>2);
                values[4]=getPaymentType     (aNumberType,(decodeNumber&60)>>>2);
                values[5]=getSubscriptionType(aNumberType,subscriptionType);
                values[6]=getBarringState(    aNumberType,(decodeNumber&192)>>>6);
                values[7]=getOppositeNumber(  aNumberType,aNumber);
                values[8]=getMasterImsi(      aNumberType,aNumber);
                values[9]=getSlaveImsi(       aNumberType,aNumber);
                values[10]=getProviderId(     aNumberType,0);
                break;
            case 2:
                values=new AttributValueStruct[4];
                values[0]=getUserLanguage(    aNumberType,(decodeNumber&768)>>>8);
                values[1]=getComboxLanguage(  aNumberType,(decodeNumber&768)>>>8);
                values[2]=getContractLanguage(aNumberType,(decodeNumber&768)>>>8);
                values[3]=getComboxType(      aNumberType,(decodeNumber&3072)>>>10);
                break;
            case 3:
                values=new AttributValueStruct[3];
                values[0]=getVasLimitReached(aNumberType,(decodeNumber&12288)>>>12);
                values[1]=getVasBlocked(     aNumberType,(decodeNumber&114688)>>>14);
                values[2]=getAdultContent(   aNumberType,(decodeNumber&393216)>>>17);
                break;
            case 4:
                int pp=((decodeNumber&60)>>>2);
                if(pp==2||pp==4||pp==6)//postpaid/prepaid -> getActiveScCustomer 2,4 und 6
                {
                    values=new AttributValueStruct[11];
                    values[0]=getCustomerAge(        aNumberType,customerage);
                    values[1]=getBirthdate(          aNumberType,customerage);
                    values[2]=getCustomerId(         aNumberType,aNumber);
                    values[3]=getCompanyId(          aNumberType,aNumber);
                    values[4]=getTopLevelCompanyId(  aNumberType,aNumber);
                    values[5]=getCustomerMainSegment(aNumberType,(decodeNumber&1572864)>>>19);
                    values[6]=getCustomerFineSegment(aNumberType,(decodeNumber&1572864)>>>19);
                    values[7]=getCustomerKey(        aNumberType,aNumber);
                    values[8]=getBskNr(              aNumberType,aNumber);
                    values[9]=getCreditLimit(        aNumberType,1000);
                    values[10]=getBirthdateStatus(   aNumberType,(decodeNumber&6291456)>>>21);
                } else
                {
                    values=new AttributValueStruct[4];
                    values[0]=getCustomerAge(        aNumberType,customerage);
                    values[1]=getBirthdate(          aNumberType,customerage);
                    values[2]=getCustomerMainSegment(aNumberType,(decodeNumber&1572864)>>>19);
                    values[3]=getBirthdateStatus(    aNumberType,(decodeNumber&6291456)>>>21);
                }
                break;
            case 5:
                values=new AttributValueStruct[1];
                values[0]=getServiceNumber(    aNumberType,aNumber);
                break;
            case 6:
                values=new AttributValueStruct[2];
                values[0]=getDUOBILL_MSISDN(     aNumberType,aNumber);
                values[1]=getDUOBILL_IMSI(       aNumberType,aNumber);
                break;
            case 7:
                values=new AttributValueStruct[5];
                values[0]=getBRAND(   aNumberType,brand);
                values[1]=getMODEL(   aNumberType,brand);
                values[2]=getTAC(     aNumberType,brand);
                values[3]=getSNR(     aNumberType,brand);
                values[4]=getSVN(     aNumberType,brand);
                break;
            default:
                throw new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    1012,
                    "Error IncludeInfoParameter value not known");

        }
        return values;
    }

    private AttributValueStruct getStatus(int aNumberType,int switchNum)
    {
        String mAttribute       ="Status";
        String mValue           =""+switchNum;
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="0";
                mValueDescription="Undefiniert";
                break;
            case 1:
                mValue="1";
                mValueDescription="Active";
                break;
            case 2:
                mValue="2";
                mValueDescription="Inactive";
                break;
            case 3:
                mValue="3";
                mValueDescription="Suspended(nur fuer Postpaid verfuegbar)";
                break;
        }

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }
/////////// INLCLUDE INFO 1 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getMSISDN(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="MSISDN";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getIMSI(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="IMSI";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getActiveScCustomer(int aNumberType,int switchNum)
    {
        String mAttribute       ="ActiveScCustomer";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="0";
                mValueDescription="Kein aktiver Swisscom Kunde";
                break;
            case 1:
                mValue="1";
                mValueDescription="SCM_PREPAID";
                break;
            case 2:
                mValue="2";
                mValueDescription="SCM_POSTPAID";
                break;
            case 3:
                mValue="3";
                mValueDescription="-";
                break;
            case 4:
                mValue="4";
                mValueDescription="TFL_POSTPAID";
                break;
            case 5:
                mValue="5";
                mValueDescription="MIGROS_PREPAID";
                break;
            case 6:
                mValue="5";
                mValueDescription="MIGROS";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getPaymentType(int aNumberType,int switchNum)
    {
        String mAttribute       ="PaymentType";
        String mValue           =""+switchNum;
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
            case 6:
                mValue="0";
                mValueDescription="Undefiniert";
                break;
            case 1:
            case 5:
                mValue="1";
                mValueDescription="Prepaid";
                break;
            case 2:
            case 4:
                mValue="2";
                mValueDescription="Postpaid";
                break;
        }

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getSubscriptionType(int aNumberType,int switchNum)
    {
        String mAttribute       ="SubscriptionType";
        String mValue           =""+switchNum;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getBarringState(int aNumberType,int switchNum)
    {
        String mAttribute       ="BarringState";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            case 0:
                mValue="0";
                mValueDescription="Keine Kassensperre";
                break;
            case 1:
                mValue="1";
                mValueDescription="Kreditlimite erreicht";
                break;
            case 2:
            default:
                mValue="2";
                mValueDescription="Kassensperre aktiv oder unbekannte Nummer mit Postpaid Vertrag";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getOppositeNumber(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="OppositeNumber";
        String mValue           ="";
        String mValueDescription="";

        StringBuffer b=new StringBuffer("");

        int x=0;
        for(int i=aNumber.length();i>x;i--)
        {
            b.append(aNumber.substring(i-1,i));
        }
        mValue=b.toString();
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getMasterImsi(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="MasterImsi";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getSlaveImsi(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="SlaveImsi";
        String mValue           ="";
        String mValueDescription="";

        StringBuffer b=new StringBuffer("");

        int x=0;
        for(int i=aNumber.length();i>x;i--)
        {
            b.append(aNumber.substring(i-1,i));
        }
        mValue=b.toString();
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getProviderId(int aNumberType,int switchNum)
    {
        String mAttribute       ="ProviderId";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="98093";
                mValueDescription="SwisscomMobile";
                break;
            case 1:
                mValue="98094";
                mValueDescription="Orange";
                break;
            case 2:
                mValue="98092";
                mValueDescription="Sunrise";
                break;
            case 3:
                mValue="98091";
                mValueDescription="Tele2";
                break;
            case 4:
                mValue="98024";
                mValueDescription="InAndPhone";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }
/////////// INLCLUDE INFO 2 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getUserLanguage(int aNumberType,int switchNum)
    {
        String mAttribute       ="UserLanguage";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            case 1:
                mValue="de";
                mValueDescription="Deutsch";
                break;
            case 2:
                mValue="fr";
                mValueDescription="Französich";
                break;
            case 3:
                mValue="en";
                mValueDescription="Englisch";
                break;
            case 4:
                mValue="it";
                mValueDescription="Italienisch";
                break;
            case 5:
                mValue="sp";
                mValueDescription="Spanisch";
                break;
            case 0:
            default:
                mValue="?";
                mValueDescription="unbekannt";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }
    private AttributValueStruct getComboxLanguage(int aNumberType,int switchNum)
    {
        String mAttribute       ="ComboxLanguage";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            case 1:
                mValue="de";
                mValueDescription="Deutsch";
                break;
            case 2:
                mValue="fr";
                mValueDescription="Franzoesich";
                break;
            case 3:
                mValue="en";
                mValueDescription="Englisch";
                break;
            case 4:
                mValue="it";
                mValueDescription="Italienisch";
                break;
            case 5:
                mValue="sp";
                mValueDescription="Spanisch";
                break;
            case 0:
            default:
                mValue="?";
                mValueDescription="unbekannt";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }
    private AttributValueStruct getContractLanguage(int aNumberType,int switchNum)
    {
        String mAttribute       ="ContractLanguage";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            case 1:
                mValue="de";
                mValueDescription="Deutsch";
                break;
            case 2:
                mValue="fr";
                mValueDescription="Franzoesich";
                break;
            case 3:
                mValue="en";
                mValueDescription="Englisch";
                break;
            case 4:
                mValue="it";
                mValueDescription="Italienisch";
                break;
            case 5:
                mValue="sp";
                mValueDescription="Spanisch";
                break;
            case 0:
            default:
                mValue="?";
                mValueDescription="unbekannt";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }
    private AttributValueStruct getComboxType(int aNumberType,int switchNum)
    {
        String mAttribute       ="ComboxType";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
                mValue="b";
                mValueDescription="Basic";
                break;
            case 2:
            case 4:
            case 6:
            case 8:
                mValue="P";
                mValueDescription="Pro";
                break;
            case 0:
            default:
                mValue="?";
                mValueDescription="unbekannt";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

/////////// INLCLUDE INFO 3 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getVasLimitReached(int aNumberType,int switchNum)
    {
        String mAttribute       ="VasLimitReached";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            case 0:
                mValue="0";
                mValueDescription="Kreditlimite nicht erreicht";
                break;
            case 1:
            default:
                mValue="1";
                mValueDescription="Kreditlimite erreicht (gesetzt durch Swisscom)";
                break;
            case 2:
                mValue="2";
                mValueDescription="Kreditlimite erreicht (gesetzt durch Kunde)";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getVasBlocked(int aNumberType,int switchNum)
    {
        String mAttribute       ="VasBlocked";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            case 0:
                mValue="0";
                mValueDescription="kein Premium Content blockiert";
                break;
            case 1:
            default:
                mValue="1";
                mValueDescription="All Premium Content (gesetzt durch Swisscom)";
                break;
            case 2:
                mValue="2";
                mValueDescription="All Premium Content (gesetzt durch Kunde)";
                break;
            case 3:
                mValue="3";
                mValueDescription="Adult Content blockiert (gesetzt durch Swisscom) -> deprecated";
                break;
            case 4:
                mValue="4";
                mValueDescription="Adult Content blockiert (gesetzt durch Kunde) -> deprecated";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getAdultContent(int aNumberType,int switchNum)
    {
        String mAttribute       ="AdultContent";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            case 0:
                mValue="0";
                mValueDescription="kein Premium Content blockiert";
                break;
            default:
            case 1:
                mValue="1";
                mValueDescription="Adult Content blockiert (gesetzt durch Swisscom)";
                break;
            case 2:
                mValue="2";
                mValueDescription="Adult Content blockiert (gesetzt durch Kunde)";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }


/////////// INLCLUDE INFO 4 POST/PRE ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getCustomerAge(int aNumberType,int switchNum)
    {
        String mAttribute       ="CustomerAge";
        String mValue           =switchNum+"";
        String mValueDescription="";
        switch(switchNum)
        {
            default:
            case 0:
                mValue="0";
                break;
            case 1:
                mValue="11";
                break;
            case 2:
                mValue="17";
                break;
            case 3:
                mValue="18";
                break;
            case 4:
                mValue="19";
                break;
            case 5:
                mValue="21";
                break;
            case 6:
                mValue="30";
                break;
            case 7:
                mValue="41";
                break;
            case 8:
                mValue="52";
                break;
            case 9:
                mValue="72";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }
    private AttributValueStruct getBirthdate(int aNumberType,int switchNum)
    {
        String mAttribute       ="Birthdate";
        String mValue           =switchNum+"";
        String mValueDescription="";
        java.util.GregorianCalendar c=new java.util.GregorianCalendar();
        
        switch(switchNum)
        {
            default:
            case 0:
                mValue="01.01.1970";
                break;
            case 1:
                mValue="01.01."+(c.get(c.YEAR)-11);
                break;
            case 2:
                mValue="02.01."+(c.get(c.YEAR)-17);
                break;
            case 3:
                mValue="03.01."+(c.get(c.YEAR)-18);
                break;
            case 4:
                mValue="04.01."+(c.get(c.YEAR)-19);
                break;
            case 5:
                mValue="05.01."+(c.get(c.YEAR)-21);
                break;
            case 6:
                mValue="06.01."+(c.get(c.YEAR)-30);
                break;
            case 7:
                mValue="07.01."+(c.get(c.YEAR)-41);
                break;
            case 8:
                mValue="08.01."+(c.get(c.YEAR)-52);
                break;
            case 9:
                mValue="09.01."+(c.get(c.YEAR)-72);
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getCustomerId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="CustomerId";
        String mValue           ="1_"+aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getCompanyId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="CompanyId";
        String mValue           ="1_"+aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getTopLevelCompanyId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="TopLevelCompanyId";
        String mValue           ="2_"+aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getCustomerMainSegment(int aNumberType,int switchNum)
    {
        String mAttribute       ="CustomerMainSegment";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="0";
                mValueDescription="Undefined";
                break;
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
                mValue="1";
                mValueDescription="RESIDENTIAL (Privater Kunde)";
                break;
            case 2:
            case 4:
            case 6:
            case 8:
                mValue="2";
                mValueDescription="CORPORATE (Firmenkunde)";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getCustomerFineSegment(int aNumberType,int switchNum)
    {
        String mAttribute       ="CustomerFineSegment";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="";
                break;
            case 1:
                mValue="RES";
                break;
            case 3:
                mValue="RESID / YOUTH";
                break;
            case 5:
                mValue="RESID / MAINSTREAM";
                break;
            case 7:
                mValue="RESID / MIGROS";
                break;
            case 9:
                mValue="RES";
                break;
            case 2:
                mValue="BUSI / SMALLBUSINES";
                break;
            case 4:
                mValue="BUSI / CORP-SME";
                break;
            case 6:
                mValue="BUSI / CORP-LAC";
                break;
            case 8:
                mValue="BUSI / CORP-GAC";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getCustomerKey(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="CustomerKey";
        String mValue           =aNumber;
        String mValueDescription="";


        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getBskNr(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="BskNr";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getContractId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="ContractId";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getCreditLimit(int aNumberType,int switchNum)
    {
        String mAttribute       ="CreditLimit";
        String mValue           =""+(switchNum*100);
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getBirthdateStatus(int aNumberType,int switchNum)
    {
        String mAttribute       ="BirthdateStatus";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="0";
                mValueDescription="not verified";
                break;
            case 1:
                mValue="1";
                mValueDescription="verified";
                break;
            case 2:
                mValue="2";
                mValueDescription="verification failed";
                break;
        }
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

/////////// INLCLUDE INFO 5 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getServiceNumber(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="ServiceNumber";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

/////////// INLCLUDE INFO 6 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getDUOBILL_MSISDN(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="DUOBILL_MSISDN";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getDUOBILL_IMSI(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="DUOBILL_IMSI";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

/////////// INLCLUDE INFO 7 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getBRAND(int aNumberType,int switchNum)
    {
        String mAttribute       ="BRAND";
        String mValue           ="";
        String mValueDescription="";
        switch (switchNum) 
        {
            case 0:
                mValue = new String("Nokia Mobile Phones");
                break;
            case 1:
                mValue = new String("Nokia Mobile Phones");
                break;
            case 2:
                mValue = new String("Nokia Mobile Phones");
                break;
            default:
            case 3:
                mValue = new String("Nokia Mobile Phones");
                break;
            case 4:
                mValue = new String("Nokia Mobile Phones");
                break;
            case 5:
                mValue = new String("Sony Ericsson");
                break;
            case 6:
                mValue = new String("Sony Ericsson");
                break;
            case 7:
                mValue = new String("Moterola");
                break;
        }
        mValueDescription=mValue;
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getMODEL(int aNumberType,int switchNum)
    {
        String mAttribute       ="MODEL";
        String mValue           ="";
        String mValueDescription="";
        switch (switchNum) 
        {
            case 0:
                mValue = new String("3100");
                break;
            case 1:
                mValue = new String("5110");
                break;
            case 2:
                mValue = new String("6010");
                break;
            default:
            case 3:
                mValue = new String("6110");
                break;
            case 4:
                mValue = new String("6310");
                break;
            case 5:
                mValue = new String("T68i");
                break;
            case 6:
                mValue = new String("P900");
                break;
            case 7:
                mValue = new String("V700");
                break;
        }
        mValueDescription=mValue;
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getTAC(int aNumberType,int switchNum)
    {
        String mAttribute       ="TAC";
        String mValue           ="";
        String mValueDescription="";

        switch (switchNum) 
        {
            case 0:
                mValue = new String("335044");
                break;
            case 1:
                mValue = new String("490551");
                break;
            case 2:
                mValue = new String("449147");
                break;
            default:
            case 3:
                mValue = new String("490518");
                break;
            case 4:
                mValue = new String("350841");
                break;
            case 5:
                mValue = new String("350372");
                break;
            case 6:
                mValue = new String("351965");
                break;
            case 7:
                mValue = new String("399965");
                break;
        }
        mValueDescription=mValue;
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getSNR(int aNumberType,int switchNum)
    {
        String mAttribute       ="SNR";
        String mValue           ="";
        String mValueDescription="";

        switch (switchNum) 
        {
            case 0:
                mValue = new String("999999");
                break;
            case 1:
                mValue = new String("111111");
                break;
            case 2:
                mValue = new String("222222");
                break;
            default:
            case 3:
                mValue = new String("333333");
                break;
            case 4:
                mValue = new String("444444");
                break;
            case 5:
                mValue = new String("555555");
                break;
            case 6:
                mValue = new String("666666");
                break;
            case 7:
                mValue = new String("777777");
                break;
        }
        mValueDescription=mValue;
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

    private AttributValueStruct getSVN(int aNumberType,int switchNum)
    {
        String mAttribute       ="SVN";
        String mValue           ="";
        String mValueDescription="";

        switch (switchNum) 
        {
            case 0:
                mValue = new String("1.3");
                break;
            case 1:
                mValue = new String("2.3");
                break;
            case 2:
                mValue = new String("3.3");
                break;
            default:
            case 3:
                mValue = new String("4.3");
                break;
            case 4:
                mValue = new String("2.3");
                break;
            case 5:
                mValue = new String("3.3");
                break;
            case 6:
                mValue = new String("4.3");
                break;
            case 7:
                mValue = new String("4.3");
                break;
        }
        mValueDescription=mValue;
        return new AttributValueStruct(mAttribute,mValue,mValueDescription);
    }

/////////// INLCLUDE INFO END ///////////////////////////////////////////////////////////////////////////////
    private void throwException(int switchNum)
        throws ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException
    {
        ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException exRet=null;
        switch(switchNum)
        {
            case 0:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error in function GrantService!"
                );
            case 1:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error in function GetSubscriptionInfo!"
                );
            case 2:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error in function GetAttributeValueDescription!"
                );
            case 3:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "SIS Server is not initialized!"
                );
            case 4:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Unknown Customer!"
                );
            case 5:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error not a valid NumberTyp!"
                );
            case 6:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error not a valid MSISDN Number!"
                );
            case 7:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error not a valid IMSI Number!"
                );
            case 8:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Database Date has not a valid format"
                );
            case 9:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "CORBA Method not yet Implemented"
                );
            case 10:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "This Method is no more supported"
                );
            case 11:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error a parameter not set"
                );
            case 12:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error IncludeInfoParameter value not known"
                );
            case 13:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error in Configuration"
                );
            case 14:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error not supported action"
                );
            case 15:
                exRet=new ch.intersys.sis.corba.idl.SubsInfoAccessServiceModul.SubsInfoAccessServiceException(
                    switchNum,
                    "Error in function GetSubscriptionInfoList!"
                );
        }
        XTTProperties.printDebug(this.getClass().getName()+".throwException switchNum:"+switchNum+"\n"+exRet);
        synchronized(siskey)
        {
            siscount++;
            siskey.notifyAll();
        }
        throw exRet;
    }

/* ************************************************************************************************* *\
 * *************************   XTT Specific Stuff   ************************************************ *
\* ************************************************************************************************* */

    private static Object siskey=new Object();
    private static Object numkey=new Object();
    private static int siscount=0;
    private static int numcount=0;
    private static int waittime=0;
    private static int waittimeperCall=0;
    private static String overrideNumber=null;


    public static void initialize()
    {
        synchronized(siskey)
        {
            siscount=0;
            waittime=0;
            waittimeperCall=0;
        }
        synchronized(numkey)
        {
            numcount=0;
        }
    }
    
    public static void setResponseDelay(int waittimems)
    {
        synchronized(siskey)
        {
            SubsSelectedInfoAccessServiceImpl.waittime=waittimems;
        }
    }
    public static void setPerCallDelay(int waittimems)
    {
        synchronized(siskey)
        {
            SubsSelectedInfoAccessServiceImpl.waittimeperCall=waittimems;
        }
    }
    public static void setOverrideNumber(String overrideNumber)
    {
        synchronized(siskey)
        {
            SubsSelectedInfoAccessServiceImpl.overrideNumber=overrideNumber;
        }
    }
   /**
     * Wait for a number of wsp messages
     */
    public static void waitForSISCalls(int number) throws java.lang.InterruptedException
    {
        /*
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("SMPPWorker.waitForWSPMessages: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        */
        int wait=XTTProperties.getIntProperty("SIS/WAITTIMEOUT");
        int prevcount=0;

        synchronized(siskey)
        {
            while(siscount<number)
            {
                XTTProperties.printInfo("SubsSelectedInfoAccessServiceImpl.waitForSISCalls: "+siscount+"/"+number);
                if(wait>0)
                {
                    prevcount=siscount;
                    siskey.wait(wait);
                    if(siscount==prevcount)
                    {
                        XTTProperties.printFail("SubsSelectedInfoAccessServiceImpl.waitForSISCalls: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    siskey.wait();
                }
            }
            XTTProperties.printInfo("SubsSelectedInfoAccessServiceImpl.waitForSISCalls: "+siscount+"/"+number);
        }
    }
    public static void waitForTimeoutSISCalls(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
    
        synchronized(siskey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=siscount+1;
            }
            while(siscount<number)
            {
                XTTProperties.printInfo("SubsSelectedInfoAccessServiceImpl.waitForTimeoutSISCalls: "+siscount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=siscount;
                siskey.wait(wait);
                if(siscount==prevcount)
                {
                    XTTProperties.printInfo("SubsSelectedInfoAccessServiceImpl.waitForTimeoutSISCalls: timed out with no SIS calls!");
                    return;
                }
            }
            XTTProperties.printFail("SubsSelectedInfoAccessServiceImpl.waitForTimeoutSISCalls: SIS call received: "+siscount+"/"+number+"");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }


   /**
     * Wait
     */
    public static void waitForSISLookups(int number) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("SIS/WAITTIMEOUT");
        int prevcount=0;

        synchronized(numkey)
        {
            while(numcount<number)
            {
                XTTProperties.printInfo("SubsSelectedInfoAccessServiceImpl.waitForSISLookups: "+numcount+"/"+number);
                if(wait>0)
                {
                    prevcount=numcount;
                    numkey.wait(wait);
                    if(numcount==prevcount)
                    {
                        XTTProperties.printFail("SubsSelectedInfoAccessServiceImpl.waitForSISLookups: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    numkey.wait();
                }
            }
            XTTProperties.printInfo("SubsSelectedInfoAccessServiceImpl.waitForSISLookups: "+numcount+"/"+number);
        }
    }
    public static void waitForTimeoutSISLookups(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
    
        synchronized(numkey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=numcount+1;
            }
            while(numcount<number)
            {
                XTTProperties.printInfo("SubsSelectedInfoAccessServiceImpl.waitForTimeoutSISLookups: "+numcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=numcount;
                numkey.wait(wait);
                if(numcount==prevcount)
                {
                    XTTProperties.printInfo("SubsSelectedInfoAccessServiceImpl.waitForTimeoutSISLookups: timed out with no SIS lookups!");
                    return;
                }
            }
            XTTProperties.printFail("SubsSelectedInfoAccessServiceImpl.waitForTimeoutSISLookups: SIS lookup received: "+numcount+"/"+number+"");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

}