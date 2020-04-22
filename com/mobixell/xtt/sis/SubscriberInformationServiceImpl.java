package com.mobixell.xtt.sis;

import ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServicePOA;
import ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.AttributValueStruct;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.ConvertLib;

/**<pre><code>
+41796543210                      number gets decoded as follows:
  |||+++++++--------------------- 7 numbers used as bits
  ||| 011111111111111111111111
  ||| ||||||||||||||||||||||||
  ||| ||||||||||||||||||||||++---- getStatus 0-3                            2bit                      11=      3 shift  0
  ||| ||||||||||||||||||++++------ getActiveScCustomer 0-7 (3,4 not used)   4bit                  111100=     60 shift  2
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
  
getMSISDN->Attribute value fix "MSISDN" for IncludeInfos 1,10
		 ->Attribute value "MSISDN","FxNetline","FxNetlineRangeFrom","FxNetlineRangeTo",
		   "FxHauptNummerIndicator","BluewinPhone","BluewinSerial","Bluewinuser",
		   "BusinessNumber","BusinessVOIPNumber" for IncludeInfos 12.           
getIMSI-> Attribute value fix "IMSI" for IncludeInfos 1,12.  
getPaymentType->getActiveScCustomer (0-2 and 5-7 cases are used)   
getProviderId->getActiveScCustomer (Attribute value = "SwisscomMobile" if first 2 bit are not zero else other other four operators)
getComboxLanguage->getUserLanguage
getContractLanguage->getUserLanguage                             
getBirthdate->getCustomerAge                                     
getCustomerFineSegment->getCustomerMainSegment                   
getCreditLimit-> fix 1000
getGrobsegment->getCustomerMainSegment
getFeinsegment->getCustomerMainSegment
getSubsegment->getCustomerMainSegment
getAgeProfile->getCustomerAge
POSTPAID/PREPAID->getActiveScCustomer 2,6 und 7 
getMODEL->getBRAND
getTAC->getBRAND  
getSNR->getBRAND  
getSVN->getBRAND  
getIMEISV->getBRAND
getCurrentOperator->getActiveScCustomer (Attribute value = "SwisscomMobile" if first 2 bit are not zero else other other four operators)
getRoamingIndicator->getBarringState
getCallWaiting->getBarringState
getState->getStatus
getRadiusNummer->IncludeInfos 10
getHashedMsisdn->IncludeInfos 10
getContentFilterProfile->getCustomerAge,getAdultContent,getCustomerMainSegment
getEmail-> IncludeInfos 10
getFxHauptNummerIndicator->getBarringState

Numbers starting with 6 are decoded as 41790000009
Number starting with 53 trhow an exception whichs number are the last 2 digits.
Numbers starting with 3 are delayed by the following decoded in miliseconds, the number is decoded as 41790000009 or the override number is used.
</code></pre>
*/

public class SubscriberInformationServiceImpl extends SubscriberInformationServicePOA
{
    public AttributValueStruct[] getSelectedStatusSubscriptionInfo(int aNumberType,
                                                                   java.lang.String aNumber,
                                                                   java.lang.String aIncludeInfos)
            throws ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException
    {
        XTTProperties.printVerbose(this.getClass().getName()+".getSelectedStatusSubscriptionInfo"
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
        XTTProperties.printVerbose(this.getClass().getName()+".getSelectedStatusSubscriptionInfo returning 1 AttributValueStruct");
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
                XTTProperties.printVerbose(this.getClass().getName()+".getSelectedStatusSubscriptionInfo response delay "+waittime);
                Thread.sleep(waittime);
            }
        } catch (Exception e){}
        try
        {
            if(wait>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSelectedStatusSubscriptionInfo per call delay "+wait);
                Thread.sleep(wait);
            }
        } catch (Exception e){}
        return val;
    }

    public AttributValueStruct[][] getSelectedStatusSubscriptionInfoList(int aNumberType,
                                                                         java.lang.String[] aNumberList,
                                                                         java.lang.String aIncludeInfos)
            throws ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException
    {
        XTTProperties.printVerbose(this.getClass().getName()+".getSelectedStatusSubscriptionInfoList"
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
                XTTProperties.printVerbose(this.getClass().getName()+".getSelectedStatusSubscriptionInfoList response delay "+waittime);
                Thread.sleep(waittime);
            }
        } catch (Exception e){}
        try
        {
            if(wait>0)
            {
                XTTProperties.printVerbose(this.getClass().getName()+".getSelectedStatusSubscriptionInfoList perCall delay "+wait);
                Thread.sleep(wait);
            }
        } catch (Exception e){}
        XTTProperties.printVerbose(this.getClass().getName()+".getSelectedStatusSubscriptionInfoList returning "+val.length+" AttributValueStructs");
        return val;
    }

    public AttributValueStruct[] getSelectedSubscriptionInfo(int aNumberType,
                                                             java.lang.String aNumber,
                                                             java.lang.String aIncludeInfos)
            throws ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException
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

    public AttributValueStruct[][] getSelectedSubscriptionInfoList(int aNumberType,
                                                                   java.lang.String[] aNumberList,
                                                                   java.lang.String aIncludeInfos)
            throws ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException
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

    /* ************************************************************************************************* *\
 * *************************   Implementation Stuff   ********************************************** *
\* ************************************************************************************************* */


    private AttributValueStruct[] getValues
    (
        int aNumberType,
        java.lang.String aNumber,
        java.lang.String aIncludeInfos
    ) throws ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException
    {
        String[] infos=aIncludeInfos.split(";");

        // bz21044 - change AttributValueStruct array size to accomodate 80 SIS parameters instead of 40
        AttributValueStruct[] values=new AttributValueStruct[80];
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
    ) throws ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException
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
              ||| ||||||||||||||||||++++------ getActiveScCustomer 0-7 (3,4 not used)   4bit                  111100=     60 shift  2
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
                values[0]=getStatus(                 aNumberType,(decodeNumber&3)>>>0);
                values[1]=getMSISDN(          		 aNumberType,aNumber,"MSISDN",aIncludeInfos);
                values[2]=getIMSI(            		 aNumberType,aNumber,"IMSI",aIncludeInfos);
                values[3]=getActiveScCustomer(  	 aNumberType,(decodeNumber&60)>>>2);
                values[4]=getPaymentType     (		 aNumberType,(decodeNumber&60)>>>2);
                values[5]=getSubscriptionType(		 aNumberType,subscriptionType);
                values[6]=getBarringState(    		 aNumberType,(decodeNumber&192)>>>6);
                values[7]=getOppositeNumber(  		 aNumberType,aNumber);
                values[8]=getMasterImsi(      		 aNumberType,aNumber);
                values[9]=getSlaveImsi(       		 aNumberType,aNumber);
                values[10]=getProviderId(     		 aNumberType,(decodeNumber&60)>>>2);
                break;
            case 2:
                values=new AttributValueStruct[4];
                values[0]=getUserLanguage(    		 aNumberType,(decodeNumber&768)>>>8);
                values[1]=getComboxLanguage(  		 aNumberType,(decodeNumber&768)>>>8);
                values[2]=getContractLanguage(		 aNumberType,(decodeNumber&768)>>>8);
                values[3]=getComboxType(      		 aNumberType,(decodeNumber&3072)>>>10);
                break;
            case 3:
                values=new AttributValueStruct[3];
                values[0]=getVasLimitReached(		 aNumberType,(decodeNumber&12288)>>>12);
                values[1]=getVasBlocked(     		 aNumberType,(decodeNumber&114688)>>>14);
                values[2]=getAdultContent(   		 aNumberType,(decodeNumber&393216)>>>17);
                break;
            case 4:
                int pp=((decodeNumber&60)>>>2);
                if(pp==2||pp==6||pp==7)//postpaid/prepaid -> getActiveScCustomer 2,6 and 7
                {
                    values=new AttributValueStruct[16];
                    values[0]=getCustomerAge(        aNumberType,customerage);
                    values[1]=getBirthdate(          aNumberType,customerage);
                    values[2]=getCustomerId(         aNumberType,aNumber);
                    values[3]=getCompanyId(          aNumberType,aNumber);
                    values[4]=getTopLevelCompanyId(  aNumberType,aNumber);
                    values[5]=getCustomerMainSegment(aNumberType,(decodeNumber&1572864)>>>19);
                    values[6]=getCustomerFineSegment(aNumberType,(decodeNumber&1572864)>>>19);
                    values[7]=getCustomerKey(        aNumberType,aNumber);
                    values[8]=getBskNr(              aNumberType,aNumber);
                    values[9]=getContractId(	     aNumberType,aNumber);
                    values[10]=getCreditLimit(       aNumberType,1000);
                    values[11]=getBirthdateStatus(   aNumberType,(decodeNumber&6291456)>>>21);
                    values[12]=getGrobsegment(       aNumberType,(decodeNumber&1572864)>>>19);
                    values[13]=getFeinsegment(       aNumberType,(decodeNumber&1572864)>>>19);
                    values[14]=getSubsegment(        aNumberType,(decodeNumber&1572864)>>>19);
                    values[15]=getAgeProfile(        aNumberType,customerage);
                } else
                {
                    //prepaid
                    values=new AttributValueStruct[5];
                    values[0]=getCustomerAge(        aNumberType,customerage);
                    values[1]=getBirthdate(          aNumberType,customerage);
                    values[2]=getCustomerMainSegment(aNumberType,(decodeNumber&1572864)>>>19);
                    values[3]=getBirthdateStatus(    aNumberType,(decodeNumber&6291456)>>>21);
                    values[4]=getAgeProfile(         aNumberType,customerage);
                }
                break;
            case 5:
                values=new AttributValueStruct[2];
                values[0]=getServiceNumber(    		 aNumberType,aNumber);
                values[1]=getAPNID(    		   		 aNumberType,aNumber);
                break;
            case 6:
                values=new AttributValueStruct[2];
                values[0]=getDUOBILL_MSISDN(   		 aNumberType,aNumber);
                values[1]=getDUOBILL_IMSI(     		 aNumberType,aNumber);
                break;
            case 7:
                values=new AttributValueStruct[6];
                values[0]=getBRAND(   				 aNumberType,brand);
                values[1]=getMODEL(   				 aNumberType,brand);
                values[2]=getTAC(     				 aNumberType,brand);
                values[3]=getSNR(     				 aNumberType,brand);
                values[4]=getSVN(     				 aNumberType,brand);
                values[5]=getIMEISV(  				 aNumberType,brand);
                break;
            case 8:
                values=new AttributValueStruct[3];
                values[0]=getCurrentOperator(   	 aNumberType,(decodeNumber&60)>>>2);
                values[1]=getRoamingIndicator(  	 aNumberType,(decodeNumber&192)>>>6);
                values[2]=getCallWaiting(       	 aNumberType,(decodeNumber&192)>>>6);
                break;
            case 9:
                values=new AttributValueStruct[1];
                values[0]=getState(          		 aNumberType,(decodeNumber&3)>>>0);
                break;
            case 10:
                values=new AttributValueStruct[16];
                values[0]=getRadiusNummer(           aNumberType,aNumber,aIncludeInfos);
                values[1]=getRadiusIpAddress(        aNumberType,aNumber);
                values[2]=getRoamingInfo(            aNumberType,(decodeNumber&60)>>>2);
                values[3]=getIpAddress(          	 aNumberType,aNumber);
                values[4]=getCalledStationId(        aNumberType,aNumber);
                values[5]=getSessionId(              aNumberType,aNumber);
                values[6]=getHashedSessionId(        aNumberType,aNumber);
                values[7]=getTimestamp(          	 aNumberType,aNumber);
                values[8]=getMSISDN(          		 aNumberType,aNumber,"Msisdn",aIncludeInfos);
                values[9]=getBearerInfo(          	 aNumberType,aNumber);
                values[10]=getMaxUplinkBitrate(      aNumberType,aNumber);
                values[11]=getMaxDownlinkBitrate(    aNumberType,aNumber);
                values[12]=getPeakValue(          	 aNumberType,aNumber);
                values[13]=getNasIdentifier(         aNumberType,aNumber);
                values[14]=getAccessChannel(         aNumberType,aNumber);
                values[15]=getHashedMsisdn(          aNumberType,aNumber,aIncludeInfos);
                break;
            case 11:
                values=new AttributValueStruct[1];
                values[0]=getContentFilterProfile(   aNumberType,(decodeNumber&60)>>>2);
                break;
            case 12:
                values=new AttributValueStruct[13];
                values[0]=getSwisscomCustomerNumber( aNumberType,aNumber);
                values[1]=getMSISDN(          		 aNumberType,aNumber,"MSISDN",aIncludeInfos);
                values[2]=getIMSI(            		 aNumberType,aNumber,"IMSI",aIncludeInfos);
                values[3]=getMSISDN(          		 aNumberType,aNumber,"FxNetline",aIncludeInfos);
                values[4]=getMSISDN(          		 aNumberType,aNumber,"FxNetlineRangeFrom",aIncludeInfos);
                values[5]=getMSISDN(          		 aNumberType,aNumber,"FxNetlineRangeTo",aIncludeInfos);
                values[6]=getFxHauptNummerIndicator( aNumberType,(decodeNumber&192)>>>6);
                values[7]=getMSISDN(          		 aNumberType,aNumber,"BluewinPhone",aIncludeInfos);
                values[8]=getEmail(           		 aNumberType,aNumber,aIncludeInfos);
                values[9]=getMSISDN(          		 aNumberType,aNumber,"BluewinSerial",aIncludeInfos);
                values[10]=getMSISDN(         		 aNumberType,aNumber,"Bluewinuser",aIncludeInfos);
                values[11]=getMSISDN(         		 aNumberType,aNumber,"BusinessNumber",aIncludeInfos);
                values[12]=getMSISDN(         		 aNumberType,aNumber,"BusinessVOIPNumber",aIncludeInfos);
                break;
            default:
                throw new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    1012,
                    "Error IncludeInfoParameter value not known");

        }
        return values;
    }


/////////// INLCLUDE INFO 1 ///////////////////////////////////////////////////////////////////////////////

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

        return new AttributValueStruct(STATUS_ID_1,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getIMSI(int aNumberType,java.lang.String aNumber,String aAttribute,short aIncludeInfos)
    {
        String mAttribute       =aAttribute;
        String mValue           =aNumber;
        String mValueDescription="";

        // depending on the value of info level and attribute name we assign Id from the standard
        if(aIncludeInfos == 1 && aAttribute.equals("IMSI"))
        	return new AttributValueStruct(IMSI_ID_1,mAttribute,mValue,mValueDescription,0,0);
        if(aIncludeInfos == 12 && aAttribute.equals("IMSI"))
        	return new AttributValueStruct(IMSI_ID_12,mAttribute,mValue,mValueDescription,0,0);

        return null;
    }

    private AttributValueStruct getActiveScCustomer(int aNumberType,int switchNum)
    {
        String mAttribute       ="ActiveScCustomer";
        String mValue           ="";
        String mValueDescription="";

      if((switchNum&3)>0)
      {
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
                  mValueDescription="-";
                  break;
              case 5:
                  mValue="5";
                  mValueDescription="MIGROS_PREPAID";
                  break;
              case 6:
                  mValue="6";
                  mValueDescription="MIGROS";
                  break;
              case 7:
                  mValue="7";
                  mValueDescription="TFL_POSTPAID";
                  break;
          }
      } else
      {
    	  mValue="0";
    	  mValueDescription="Kein aktiver Swisscom Kunde";
      }

        return new AttributValueStruct(ACTIVESCCUSTOMER_ID_1,mAttribute,mValue,mValueDescription,0,0);
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
            case 7:
                mValue="2";
                mValueDescription="Postpaid";
                break;
        }

        return new AttributValueStruct(PAYMENTTYPE_ID_1,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getSubscriptionType(int aNumberType,int switchNum)
    {
        String mAttribute       ="SubscriptionType";
        String mValue           =""+switchNum;
        String mValueDescription="";

        return new AttributValueStruct(SUBSCRIPTIONTYPE_ID_1,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(BARRINGSTATE_ID_1,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(OPPOSITENUMBER_ID_1,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getMasterImsi(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="MasterImsi";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(MASTERIMSI_ID_1,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(SLAVEIMSI_ID_1,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getProviderId(int aNumberType,int switchNum)
    {
        String mAttribute       ="ProviderId";
        String mValue           ="";
        String mValueDescription="";

        if((switchNum&3)>0)
        {
                    mValue="98093";
                    mValueDescription="SwisscomMobile";
            
        } else
        {
        	switch(switchNum>>>2)
            {
                default:                
                case 0:
                    mValue="98094";
                    mValueDescription="Orange";
                    break;
                case 1:
                    mValue="98092";
                    mValueDescription="Sunrise";
                    break;
                case 2:
                    mValue="98091";
                    mValueDescription="Tele2";
                    break;
                case 3:
                    mValue="98024";
                    mValueDescription="InAndPhone";
                    break;
            }
        }
        return new AttributValueStruct(PROVIDERID_ID_1,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(USERLANGUAGE_ID_2,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(COMBOXLANGUAGE_ID_2,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(CONTRACTLANGUAGE_ID_2,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(COMBOXTYPE_ID_2,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(VASLIMITREACHED_ID_3,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(VASBLOCKED_ID_3,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(ADULTCONTENT_ID_3,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(CUSTOMERAGE_ID_4,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(BIRTHDATE_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getCustomerId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="CustomerId";
        String mValue           ="1_"+aNumber;
        String mValueDescription="";

        return new AttributValueStruct(CUSTOMERID_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getCompanyId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="CompanyId";
        String mValue           ="1_"+aNumber;
        String mValueDescription="";

        return new AttributValueStruct(COMPANYID_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getTopLevelCompanyId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="TopLevelCompanyId";
        String mValue           ="2_"+aNumber;
        String mValueDescription="";

        return new AttributValueStruct(TOPLEVELCOMPANYID_ID_4,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(CUSTOMERMAINSEGMENT_ID_4,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(CUSTOMERFINESEGMENT_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getCustomerKey(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="CustomerKey";
        String mValue           =aNumber;
        String mValueDescription="";


        return new AttributValueStruct(CUSTOMERKEY_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getBskNr(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="BskNr";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(BSKNR_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }


    private AttributValueStruct getContractId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="ContractId";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(CONTRACTID_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getCreditLimit(int aNumberType,int switchNum)
    {
        String mAttribute       ="CreditLimit";
        String mValue           =""+(switchNum*100);
        String mValueDescription="";

        return new AttributValueStruct(CREDITLIMIT_ID_4,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(BIRTHDATESTATUS_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getGrobsegment(int aNumberType,int switchNum)
    {
        String mAttribute       ="Grobsegment";
        String mValue           ="";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="RES";
                break;
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
                mValue="SME";
                break;
            case 2:
            case 4:
            case 6:
            case 8:
                mValue="CBU";
                break;
        }
        return new AttributValueStruct(GROBSEGMENT_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getFeinsegment(int aNumberType,int switchNum)
    {
        String mAttribute       ="Feinsegment";
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
        return new AttributValueStruct(FEINSEGMENT_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getSubsegment(int aNumberType,int switchNum)
    {
        String mAttribute       ="Subsegment";
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
        return new AttributValueStruct(SUBSEGMENT_ID_4,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getAgeProfile(int aNumberType,int switchNum)
     {
        String mAttribute       ="AgeProfile";
        String mValue           ="0";
        String mValueDescription="";

        AttributValueStruct customerAgeAttributValueStruct = this.getCustomerAge(aNumberType,switchNum);

        if(Integer.parseInt(customerAgeAttributValueStruct.value)>0 &&
        		Integer.parseInt(customerAgeAttributValueStruct.value)<16)
        {
        	mValue = "10";
        } else if(Integer.parseInt(customerAgeAttributValueStruct.value)>15 &&
        		Integer.parseInt(customerAgeAttributValueStruct.value)<18)
        {
        	mValue = "20";
        } else if(Integer.parseInt(customerAgeAttributValueStruct.value) >= 18)
        {
        	mValue = "30";
        }

        return new AttributValueStruct(AGEPROFILE_ID_4,mAttribute,mValue,mValueDescription,0,0);
     }

/////////// INLCLUDE INFO 5 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getServiceNumber(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="ServiceNumber";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(SERVICENUMBER_ID_5,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getAPNID(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="ApnId";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(APNID_ID_5,mAttribute,mValue,mValueDescription,0,0);
    }

/////////// INLCLUDE INFO 6 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getDUOBILL_MSISDN(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="DuobillMsisdn";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(DUOBILLMSISDN_ID_6,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getDUOBILL_IMSI(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="DuobillImsi";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(DUOBILLIMSI_ID_6,mAttribute,mValue,mValueDescription,0,0);
    }

/////////// INLCLUDE INFO 7 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getBRAND(int aNumberType,int switchNum)
    {
        String mAttribute       ="Brand";
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
        return new AttributValueStruct(BRAND_ID_7,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getMODEL(int aNumberType,int switchNum)
    {
        String mAttribute       ="Model";
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
        return new AttributValueStruct(MODEL_ID_7,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(TAC_ID_7,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(SNR_ID_7,mAttribute,mValue,mValueDescription,0,0);
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
        return new AttributValueStruct(SVN_ID_7,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getIMEISV(int aNumberType,int switchNum){
    	String mAttribute       ="Imeisv";
        String mValue           ="";
        String mValueDescription="";
        String svnAttributValueStructOutPut = "";

    	AttributValueStruct tacAttributValueStruct = this.getTAC(aNumberType,switchNum);
    	AttributValueStruct facAttributValueStruct = this.getFAC(aNumberType,switchNum);
    	AttributValueStruct snrAttributValueStruct = this.getSNR(aNumberType,switchNum);
    	AttributValueStruct svnAttributValueStruct = this.getSVN(aNumberType,switchNum);

    	mValue = tacAttributValueStruct.value + facAttributValueStruct.value + snrAttributValueStruct.value;

    	for (int i = 0; i < svnAttributValueStruct.value.length(); i ++) {
    	    if (svnAttributValueStruct.value.charAt(i) != '.')
    	    {
    	    	svnAttributValueStructOutPut += svnAttributValueStruct.value.charAt(i);
    	    }
    	}

    	mValue = mValue + svnAttributValueStructOutPut;
    	return new AttributValueStruct(IMEISV_ID_7,mAttribute,mValue,mValueDescription,0,0);
    }

    // add FAC - Final Assembly Code
    private AttributValueStruct getFAC(int aNumberType,int switchNum)
    {
        String mAttribute       ="FAC";
        String mValue           ="00";
        String mValueDescription="";

        //there isn't any FAC SIS parameter hence we dont any need ID for it
        return new AttributValueStruct(0,mAttribute,mValue,mValueDescription,0,0);
    }
/////////// INLCLUDE INFO 8 ///////////////////////////////////////////////////////////////////////////////

    private AttributValueStruct getCurrentOperator(int aNumberType,int switchNum)
    {
        String mAttribute       ="CurrentOperator";
        String mValue           ="";
        String mValueDescription="";

        if((switchNum&3)>0)
        {
        	mValue="228-01";
            mValueDescription="SwisscomMobile";
            
        } else 
        {
        	switch(switchNum>>>2)
            {
                default:
                case 0:
                    mValue="228-03";
                    mValueDescription="Orange";
                    break;
                case 1:
                    mValue="228-02";
                    mValueDescription="Sunrise";
                    break;
                case 2:
                    mValue="228-08";
                    mValueDescription="Tele2";
                    break;
                case 3:
                    mValue="228-07";
                    mValueDescription="InAndPhone";
                    break;
            }
        }
        return new AttributValueStruct(CURRENTOPERATOR_ID_8,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getRoamingIndicator(int aNumberType,int switchNum)
    {
        String mAttribute       ="RoamingIndicator";
        String mValue           ="0";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="0";
                mValueDescription="Undefined";
                break;
            case 1:
                mValue="1";
                mValueDescription="HomeOperator";
                break;
            case 2:
                mValue="2";
                mValueDescription="RoamingOperator";
                break;
        }

        return new AttributValueStruct(ROAMINGINDICATOR_ID_8,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getCallWaiting(int aNumberType,int switchNum)
    {
        String mAttribute       ="CallWaiting";
        String mValue           ="0";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="0";
                break;
            case 1:
                mValue="1";
                break;
            case 2:
                mValue="2";
                break;
        }

        return new AttributValueStruct(CALLWAITING_ID_8,mAttribute,mValue,mValueDescription,0,0);
    }
/////////// INLCLUDE INFO 9 ///////////////////////////////////////////////////////////////////////////////
    private AttributValueStruct getState(int aNumberType,int switchNum)
    {
        String mAttribute       ="State";
        String mValue           =""+switchNum;
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="IDLE";
                break;
            case 1:
                mValue="BUSY";
                break;
            case 2:
                mValue="DET";
                break;
            case 3:
                mValue="IDET";
                break;
        }

        return new AttributValueStruct(STATE_ID_9,mAttribute,mValue,mValueDescription,0,0);
    }

/////////// INLCLUDE INFO 10 ///////////////////////////////////////////////////////////////////////////////

    private AttributValueStruct getRadiusNummer(int aNumberType,java.lang.String aNumber,short aIncludeInfos)
    {
        String mAttribute       ="RadiusNummer";
        String mValue           ="";
        String mValueDescription="";

        AttributValueStruct radiusNummerAttributValueStruct = this.getMSISDN(aNumberType,aNumber,"Msisdn",aIncludeInfos);

        mValue = radiusNummerAttributValueStruct.value;

        return new AttributValueStruct(RADIUSNUMMER_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getRadiusIpAddress(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="RadiusIpAddress";
        String mValue           ="";
        String mValueDescription="";

        return new AttributValueStruct(RADIUSIPADDRESS_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getRoamingInfo(int aNumberType,int switchNum)
    {
        String mAttribute       ="RoamingInfo";
        String mValue           ="";
        String mValueDescription="";

        AttributValueStruct roamingInfoAttributValueStruct = this.getCurrentOperator(aNumberType,switchNum);

        mValue = roamingInfoAttributValueStruct.value;

        return new AttributValueStruct(ROAMINGINFO_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getIpAddress(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="IpAddress";
        String mValue           ="";
        String mValueDescription="";

        return new AttributValueStruct(IPADDRESS_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getCalledStationId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="CalledStationId";
        String mValue           ="";
        String mValueDescription="";

        return new AttributValueStruct(CALLEDSTATIONID_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getSessionId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="SessionId";
        String mValue           ="";
        String mValueDescription="";

        mValue = String.valueOf(System.currentTimeMillis());

        return new AttributValueStruct(SESSIONID_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getHashedSessionId(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="HashedSessionId";
        String mValue           ="";
        String mValueDescription="";

        AttributValueStruct hashedSessionIdAttributValueStruct = this.getSessionId(aNumberType,aNumber);

        mValue = ConvertLib.getHexMD5Hash(hashedSessionIdAttributValueStruct.value);

        return new AttributValueStruct(HASHEDSESSIONID_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getTimestamp(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="Timestamp";
        String mValue           ="";
        String mValueDescription="";

        java.util.Date adate = new java.util.Date(System.currentTimeMillis());
  		String DATE_FORMAT = "dd.MM.yyyy hh:mm:ss";
  		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);

        mValue = sdf.format(adate);

        return new AttributValueStruct(TIMESTAMP_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getBearerInfo(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="BearerInfo";
        String mValue           ="3G";
        String mValueDescription="";

        return new AttributValueStruct(BEARERINFO_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getMaxUplinkBitrate(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="MaxUplinkBitrate";
        String mValue           ="";
        String mValueDescription="";

        return new AttributValueStruct(MAXUPLINKBITRATE_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getMaxDownlinkBitrate(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="MaxDownlinkBitrate";
        String mValue           ="";
        String mValueDescription="";

        return new AttributValueStruct(MAXDOWNLINKBITRATE_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getPeakValue(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="PeakValue";
        String mValue           ="";
        String mValueDescription="";

        return new AttributValueStruct(PEAKVALUE_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getNasIdentifier(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="NasIdentifier";
        String mValue           ="";
        String mValueDescription="";

        return new AttributValueStruct(NASIDENTIFIER_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getAccessChannel(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="AccessChannel";
        String mValue           ="GPRS";
        String mValueDescription="";

        return new AttributValueStruct(ACCESSCHANNEL_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getHashedMsisdn(int aNumberType,java.lang.String aNumber,short aIncludeInfos)
    {
        String mAttribute       ="HashedMsisdn";
        String mValue           ="";
        String mValueDescription="";

        AttributValueStruct hashedMsisdnAttributValueStruct = this.getMSISDN(aNumberType,aNumber,"Msisdn",aIncludeInfos);

        mValue = ConvertLib.getHexMD5Hash(hashedMsisdnAttributValueStruct.value);

        return new AttributValueStruct(HASHEDMSISDN_ID_10,mAttribute,mValue,mValueDescription,0,0);
    }

/////////// INLCLUDE INFO 11 ///////////////////////////////////////////////////////////////////////////////

    private AttributValueStruct getContentFilterProfile(int aNumberType,int switchNum)
    {
        String mAttribute       ="ContentFilterProfile";
        String mValue           ="";
        String mValueDescription="";

        AttributValueStruct customerAgeAttributValueStruct = this.getCustomerAge(aNumberType,switchNum);
        AttributValueStruct adultContentAttributValueStruct = this.getAdultContent(aNumberType,switchNum);
        AttributValueStruct customerMainSegmentAttributValueStruct = this.getCustomerMainSegment(aNumberType,switchNum);

        if(getInt(customerAgeAttributValueStruct.value) == 0)
        {
        	mValue = "0";
        	mValueDescription = "Undefined";
        } else if(getInt(customerAgeAttributValueStruct.value) < 16)
        {
        	mValue = "1";
        	mValueDescription = "Child";
        } else if((getInt(customerAgeAttributValueStruct.value) > 15) ||
        		(getInt(customerAgeAttributValueStruct.value) < 18))
        {
        	mValue = "2";
        	mValueDescription = "Teen";
        } else if((getInt(customerAgeAttributValueStruct.value) >= 18) ||
        		(getInt(customerMainSegmentAttributValueStruct.value) == 2))
        {
        	mValue = "3";
        	mValueDescription = "Adult";
        } else if(((getInt(customerAgeAttributValueStruct.value) >= 18) ||
        		(getInt(customerMainSegmentAttributValueStruct.value) == 2)) &&
        		(getInt(adultContentAttributValueStruct.value) > 0))
        {
        	mValue = "4";
        	mValueDescription = "AdultRestricted";
        }

        return new AttributValueStruct(CONTENTFILTERPROFILE_ID_11,mAttribute,mValue,mValueDescription,0,0);
    }
/////////// INLCLUDE INFO 12 ///////////////////////////////////////////////////////////////////////////////

    private AttributValueStruct getSwisscomCustomerNumber(int aNumberType,java.lang.String aNumber)
    {
        String mAttribute       ="SwisscomCustomerNumber";
        String mValue           =aNumber;
        String mValueDescription="";

        return new AttributValueStruct(SWISSCOMCUSTOMERNUMBER_ID_12,mAttribute,mValue,mValueDescription,0,0);
    }
    
    private AttributValueStruct getFxHauptNummerIndicator(int aNumberType,int switchNum)
    {
        String mAttribute       ="FxHauptNummerIndicator";
        String mValue           ="0";
        String mValueDescription="";

        switch(switchNum)
        {
            default:
            case 0:
                mValue="0";
                mValueDescription="Undefined";
                break;
            case 1:
                mValue="1";
                mValueDescription="nicht Hauptnummer";
                break;
            case 2:
                mValue="2";
                mValueDescription="Hauptnummer";
                break;
        }

        return new AttributValueStruct(FXHAUPTNUMMERINDICATOR_ID_12,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getEmail(int aNumberType,String aNumber,short aIncludeInfos)
    {
        String mAttribute       ="Email";
        String mValue           =this.getMSISDN(aNumberType,aNumber,"MSISDN",aIncludeInfos).value + "@mobixell.com";
        String mValueDescription="";
        return new AttributValueStruct(EMAIL_ID_12,mAttribute,mValue,mValueDescription,0,0);
    }

    private AttributValueStruct getMSISDN(int aNumberType,java.lang.String aNumber,String aAttribute,short aIncludeInfos)
    {
        String mAttribute       =aAttribute;
        String mValue           =aNumber;
        String mValueDescription="";

        // depending on the value of info level and attribute name we assign Id from the standard
        if(aIncludeInfos == 1 && aAttribute.equals("MSISDN"))
            	return new AttributValueStruct(MSISDN_ID_1,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 10 && aAttribute.equals("Msisdn"))
        		return new AttributValueStruct(MSISDN_ID_10,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 12 && aAttribute.equals("MSISDN"))
                return new AttributValueStruct(MSISDN_ID_12,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 12 && aAttribute.equals("FxNetline"))
        	return new AttributValueStruct(FXNETLINE_ID_12,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 12 && aAttribute.equals("FxNetlineRangeFrom"))
            	return new AttributValueStruct(FXNETLINERANGEFROM_ID_12,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 12 && aAttribute.equals("FxNetlineRangeTo"))
                return new AttributValueStruct(FXNETLINERANGETO_ID_12,mAttribute,mValue,mValueDescription,0,0);        
        else if(aIncludeInfos == 12 && aAttribute.equals("BluewinPhone"))
                return new AttributValueStruct(BLUEWINPHONE_ID_12,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 12 && aAttribute.equals("BluewinSerial"))
                return new AttributValueStruct(BLUEWINSERIAL_ID_12,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 12 && aAttribute.equals("Bluewinuser"))
                return new AttributValueStruct(BLUEWINUSER_ID_12,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 12 && aAttribute.equals("BusinessNumber"))
                return new AttributValueStruct(BUSINESSNUMBER_ID_12,mAttribute,mValue,mValueDescription,0,0);
        else if(aIncludeInfos == 12 && aAttribute.equals("BusinessVOIPNumber"))
                return new AttributValueStruct(BUSINESSVOIPNUMBER_ID_12,mAttribute,mValue,mValueDescription,0,0);

        return null;
    }

/////////// INLCLUDE INFO END ///////////////////////////////////////////////////////////////////////////////
    private void throwException(int switchNum)
        throws ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException
    {
        ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException exRet=null;
        switch(switchNum)
        {
            case 0:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error in function GrantService!"
                );
            case 1:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error in function GetSubscriptionInfo!"
                );
            case 2:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error in function GetAttributeValueDescription!"
                );
            case 3:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "SIS Server is not initialized!"
                );
            case 4:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Unknown Customer!"
                );
            case 5:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error not a valid NumberTyp!"
                );
            case 6:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error not a valid MSISDN Number!"
                );
            case 7:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error not a valid IMSI Number!"
                );
            case 8:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Database Date has not a valid format"
                );
            case 9:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "CORBA Method not yet Implemented"
                );
            case 10:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "This Method is no more supported"
                );
            case 11:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error a parameter not set"
                );
            case 12:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error IncludeInfoParameter value not known"
                );
            case 13:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error in Configuration"
                );
            case 14:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
                    switchNum,
                    "Error not supported action"
                );
            case 15:
                exRet=new ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.SubscriberInformationServiceException(
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
            SubscriberInformationServiceImpl.waittime=waittimems;
        }
    }
    public static void setPerCallDelay(int waittimems)
    {
        synchronized(siskey)
        {
            SubscriberInformationServiceImpl.waittimeperCall=waittimems;
        }
    }
    public static void setOverrideNumber(String overrideNumber)
    {
        synchronized(siskey)
        {
            SubscriberInformationServiceImpl.overrideNumber=overrideNumber;
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
                XTTProperties.printInfo("SubscriberInformationServiceImpl.waitForSISCalls: "+siscount+"/"+number);
                if(wait>0)
                {
                    prevcount=siscount;
                    siskey.wait(wait);
                    if(siscount==prevcount)
                    {
                        XTTProperties.printFail("SubscriberInformationServiceImpl.waitForSISCalls: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    siskey.wait();
                }
            }
            XTTProperties.printInfo("SubscriberInformationServiceImpl.waitForSISCalls: "+siscount+"/"+number);
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
                XTTProperties.printInfo("SubscriberInformationServiceImpl.waitForTimeoutSISCalls: "+siscount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=siscount;
                siskey.wait(wait);
                if(siscount==prevcount)
                {
                    XTTProperties.printInfo("SubscriberInformationServiceImpl.waitForTimeoutSISCalls: timed out with no SIS calls!");
                    return;
                }
            }
            XTTProperties.printFail("SubscriberInformationServiceImpl.waitForTimeoutSISCalls: SIS call received: "+siscount+"/"+number+"");
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
                XTTProperties.printInfo("SubscriberInformationServiceImpl.waitForSISLookups: "+numcount+"/"+number);
                if(wait>0)
                {
                    prevcount=numcount;
                    numkey.wait(wait);
                    if(numcount==prevcount)
                    {
                        XTTProperties.printFail("SubscriberInformationServiceImpl.waitForSISLookups: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    numkey.wait();
                }
            }
            XTTProperties.printInfo("SubscriberInformationServiceImpl.waitForSISLookups: "+numcount+"/"+number);
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
                XTTProperties.printInfo("SubscriberInformationServiceImpl.waitForTimeoutSISLookups: "+numcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=numcount;
                numkey.wait(wait);
                if(numcount==prevcount)
                {
                    XTTProperties.printInfo("SubscriberInformationServiceImpl.waitForTimeoutSISLookups: timed out with no SIS lookups!");
                    return;
                }
            }
            XTTProperties.printFail("SubscriberInformationServiceImpl.waitForTimeoutSISLookups: SIS lookup received: "+numcount+"/"+number+"");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

   private int getInt(String stringVal)
   {
	   return Integer.parseInt(stringVal);
   }

    /**
     * Following constants are SIS parameter ID constants from the standard.
     * These values are used for the first field of the new definition of
     * ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.AttributValueStruct
     *
     * New fields of ch.intersys.sis.corba.idl.SubscriberInformationServiceModul.AttributValueStruct class:
     *
     * - public int id;
     * - public java.lang.String attrName;
     * - public java.lang.String value;
     * - public java.lang.String description;
     * - public int info;
     * - public int infoIndex; 
     */

/////////// INLCLUDE INFO 1 ///////
       private static final int STATUS_ID_1 				 = 101;
       private static final int MSISDN_ID_1 				 = 102;
       private static final int IMSI_ID_1    				 = 103;
       private static final int ACTIVESCCUSTOMER_ID_1 		 = 104;
       private static final int PAYMENTTYPE_ID_1 			 = 105;
       private static final int SUBSCRIPTIONTYPE_ID_1 		 = 106;
       private static final int BARRINGSTATE_ID_1 			 = 107;
       private static final int OPPOSITENUMBER_ID_1 		 = 108;
       private static final int MASTERIMSI_ID_1 			 = 109;
       private static final int SLAVEIMSI_ID_1 				 = 110;
       private static final int PROVIDERID_ID_1 			 = 111;

/////////// INLCLUDE INFO 2 ///////
       private static final int USERLANGUAGE_ID_2 			 = 201;
       private static final int COMBOXLANGUAGE_ID_2 		 = 202;
       private static final int CONTRACTLANGUAGE_ID_2  		 = 203;
       private static final int COMBOXTYPE_ID_2 			 = 204;

/////////// INLCLUDE INFO 3 ///////
       private static final int VASLIMITREACHED_ID_3 		 = 301;
       private static final int VASBLOCKED_ID_3 			 = 302;
       private static final int ADULTCONTENT_ID_3 			 = 303;

/////////// INLCLUDE INFO 4 ///////
       private static final int CUSTOMERAGE_ID_4 			 = 401;
       private static final int BIRTHDATE_ID_4          	 = 402;
       private static final int CUSTOMERID_ID_4         	 = 403;
       private static final int COMPANYID_ID_4     			 = 404;
       private static final int TOPLEVELCOMPANYID_ID_4   	 = 405;
       private static final int CUSTOMERMAINSEGMENT_ID_4	 = 406;
       private static final int CUSTOMERFINESEGMENT_ID_4	 = 407;
       private static final int CUSTOMERKEY_ID_4 			 = 408;
       private static final int BSKNR_ID_4 					 = 409;
       private static final int CONTRACTID_ID_4 			 = 410;
       private static final int CREDITLIMIT_ID_4 			 = 411;
       private static final int BIRTHDATESTATUS_ID_4 		 = 412;
       private static final int GROBSEGMENT_ID_4 			 = 413;
       private static final int FEINSEGMENT_ID_4 			 = 414;
       private static final int SUBSEGMENT_ID_4 			 = 415;
       private static final int AGEPROFILE_ID_4 			 = 416;

/////////// INLCLUDE INFO 5 ///////
       private static final int SERVICENUMBER_ID_5 			 = 501;
       private static final int APNID_ID_5 					 = 502;

/////////// INLCLUDE INFO 6 ///////
       private static final int DUOBILLMSISDN_ID_6 			 = 601;
       private static final int DUOBILLIMSI_ID_6 			 = 602;

/////////// INLCLUDE INFO 7 ///////
       private static final int BRAND_ID_7 					 = 701;
       private static final int MODEL_ID_7 					 = 702;
       private static final int TAC_ID_7 					 = 703;
       private static final int SNR_ID_7 					 = 704;
       private static final int SVN_ID_7 					 = 705;
       private static final int IMEISV_ID_7 				 = 706;

/////////// INLCLUDE INFO 8 ///////
       private static final int CURRENTOPERATOR_ID_8 		 = 801;
       private static final int ROAMINGINDICATOR_ID_8 		 = 802;
       private static final int CALLWAITING_ID_8 		  	 = 803;

/////////// INLCLUDE INFO 9 ///////
       private static final int STATE_ID_9 					 = 901;

/////////// INLCLUDE INFO 10 ///////
       private static final int RADIUSNUMMER_ID_10 			 = 1001;
       private static final int RADIUSIPADDRESS_ID_10 		 = 1002;
       private static final int ROAMINGINFO_ID_10 			 = 1003;
       private static final int IPADDRESS_ID_10 			 = 1004;
       private static final int CALLEDSTATIONID_ID_10 		 = 1005;
       private static final int SESSIONID_ID_10 			 = 1006;
       private static final int HASHEDSESSIONID_ID_10 		 = 1007;
       private static final int TIMESTAMP_ID_10 			 = 1008;
       private static final int MSISDN_ID_10 		 		 = 1009;
       private static final int BEARERINFO_ID_10 			 = 1010;
       private static final int MAXUPLINKBITRATE_ID_10 		 = 1011;
       private static final int MAXDOWNLINKBITRATE_ID_10	 = 1012;
       private static final int PEAKVALUE_ID_10 			 = 1013;
       private static final int NASIDENTIFIER_ID_10 		 = 1014;
       private static final int ACCESSCHANNEL_ID_10 		 = 1015;
       private static final int HASHEDMSISDN_ID_10	 		 = 1016;

/////////// INLCLUDE INFO 11 ///////
       private static final int CONTENTFILTERPROFILE_ID_11	 = 1101;

/////////// INLCLUDE INFO 12 ///////
       private static final int SWISSCOMCUSTOMERNUMBER_ID_12 = 1201;
       private static final int MSISDN_ID_12 			 	 = 1202;
       private static final int IMSI_ID_12 					 = 1203;
       private static final int FXNETLINE_ID_12				 = 1204;
       private static final int FXNETLINERANGEFROM_ID_12	 = 1205;
       private static final int FXNETLINERANGETO_ID_12		 = 1206;
       private static final int FXHAUPTNUMMERINDICATOR_ID_12 = 1207;
       private static final int BLUEWINPHONE_ID_12			 = 1208;
       private static final int EMAIL_ID_12					 = 1209;
       private static final int BLUEWINSERIAL_ID_12			 = 1210;
       private static final int BLUEWINUSER_ID_12			 = 1211;
       private static final int BUSINESSNUMBER_ID_12		 = 1212;
       private static final int BUSINESSVOIPNUMBER_ID_12	 = 1213;
}