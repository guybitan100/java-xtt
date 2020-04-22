/*
 * $RCSfile: LDAPConstants.java,v $
 * $Revision: 1.1 $
 * $Date: 2006/11/17 12:33:36 $
 */

package com.mobixell.xtt.ldap;

/**
 * LDAPConstants
 * <p>
 * Holds values used by the LDAPWorker and LDAPEntry classes
 *
 * @author Roger Soder
 * @version 1.0
 */
public interface LDAPConstants
{
    // LDAP Commands
	static public final int BINDREQUEST       = 0x00;
	static public final int BINDRESPONSE	  = 0x01;
	static public final int UNBINDREQUEST	  = 0x02;
	static public final int SEARCHREQUEST	  = 0x03;
	static public final int SEARCHRESENTRY	  = 0x04;
	static public final int SEARCHRESDONE	  = 0x05;
	static public final int SEARCHRESREF	  = 0x13;
	static public final int MODIFYREQUEST	  = 0x06;
	static public final int MODIFYRESPONSE	  = 0x07;
	static public final int ADDREQUEST	      = 0x08;
	static public final int ADDRESPONSE       = 0x09;
	static public final int DELREQUEST	      = 0x0A;
	static public final int DELRESPONSE       = 0x0B;
	static public final int MODDNREQUEST	  = 0x0C;
	static public final int MODDNRESPONSE	  = 0x0D;
	static public final int COMPAREREQUEST	  = 0x0E;
	static public final int COMPARERESPONSE   = 0x0F;
	static public final int ABANDONREQUEST	  = 0x10;
	static public final int EXTENDEDREQ       = 0x17;
	static public final int EXTENDEDRESP	  = 0x18;
                                              
    // CLASS                                  
    public static final int MASK_CLASS        = 0xC0;
    public static final int UNIVERSAL         = 0x00;
    public static final int APPLICATION       = 0x40;
    public static final int CONTEXT           = 0x80;
    public static final int PRIVATE           = 0xC0;
                                              
    // DATA TYPE                              
    public static final int MASK_DATATYPE     = 0x20;
    public static final int PRIMITIVE         = 0x00;
    public static final int CONSTRUCTED       = 0x20;
                                              
    // TAG                                    
    public static final int MASK_TAG          = 0x1F;
    public static final int BOOLEAN           = 0x01;
    public static final int INTEGER           = 0x02;
    public static final int BIT_STRING        = 0x03;
    public static final int OCTET_STRING      = 0x04;
    public static final int NULL              = 0x05;
    public static final int ENUMERATED        = 0x0A;
    public static final int SEQUENCE          = 0x10;
    public static final int SET               = 0x11;
    //Not in RFC
    public static final int ATTRNAME          = 0x12;
    // less used
    public static final int OBJECT_IDENTIFIER = 0x06;
    public static final int OBJECT_DESCRIPTOR = 0x07;
    public static final int EXTERNAL          = 0x08;
    public static final int REAL              = 0x09;
    public static final int EMBEDDED_PDV      = 0x0B;
    public static final int UTF8STRING        = 0x0C;
    // mostly not used
    public static final int EOC               = 0x00;
    public static final int NUMERICSTRING     = 18;
    public static final int PRINTABLESTRING   = 19;
    public static final int T61STRING         = 20;
    public static final int TELETEXSTRING     = 20;
    public static final int VIDEOTEXSTRING    = 21;
    public static final int IA5STRING         = 22;
    public static final int UTCTIME           = 23;
    public static final int GENERALIZEDTIME   = 24;
    public static final int GRAPHICSTRING     = 25;
    public static final int VISIBLESTRING     = 26;
    public static final int GENERALSTRING     = 27;
    public static final int UNIVERSALSTRING   = 28;
    public static final int BMPSTRING         = 30;

    public static final int MAX_TAG_NUMBER  =536870911;

    public static final int AUTHENTICATIONCHOICE_SIMPLE = 0x00;
    public static final int AUTHENTICATIONCHOICE_SASL   = 0x03;



    // resultCode 
    public static final int SUCCESS                      =0;
    public static final int OPERATIONSERROR              =1;
    public static final int PROTOCOLERROR                =2;
    public static final int TIMELIMITEXCEEDED            =3;
    public static final int SIZELIMITEXCEEDED            =4;
    public static final int COMPAREFALSE                 =5;
    public static final int COMPARETRUE                  =6;
    public static final int AUTHMETHODNOTSUPPORTED       =7;
    public static final int STRONGERAUTHREQUIRED         =8;
    public static final int REFERRAL                     =10;
    public static final int ADMINLIMITEXCEEDED           =11;
    public static final int UNAVAILABLECRITICALEXTENSION =12;
    public static final int CONFIDENTIALITYREQUIRED      =13;
    public static final int SASLBINDINPROGRESS           =14;
    public static final int NOSUCHATTRIBUTE              =16;
    public static final int UNDEFINEDATTRIBUTETYPE       =17;
    public static final int INAPPROPRIATEMATCHING        =18;
    public static final int CONSTRAINTVIOLATION          =19;
    public static final int ATTRIBUTEORVALUEEXISTS       =20;
    public static final int INVALIDATTRIBUTESYNTAX       =21;
    public static final int NOSUCHOBJECT                 =32;
    public static final int ALIASPROBLEM                 =33;
    public static final int INVALIDDNSYNTAX              =34;
    public static final int ALIASDEREFERENCINGPROBLEM    =36;
    public static final int INAPPROPRIATEAUTHENTICATION  =48;
    public static final int INVALIDCREDENTIALS           =49;
    public static final int INSUFFICIENTACCESSRIGHTS     =50;
    public static final int BUSY                         =51;
    public static final int UNAVAILABLE                  =52;
    public static final int UNWILLINGTOPERFORM           =53;
    public static final int LOOPDETECT                   =54;
    public static final int NAMINGVIOLATION              =64;
    public static final int OBJECTCLASSVIOLATION         =65;
    public static final int NOTALLOWEDONNONLEAF          =66;
    public static final int NOTALLOWEDONRDN              =67;
    public static final int ENTRYALREADYEXISTS           =68;
    public static final int OBJECTCLASSMODSPROHIBITED    =69;
    public static final int AFFECTSMULTIPLEDSAS          =71;
    public static final int OTHER                        =80;

    public static final int SCOPE_BASEOBJECT    =0;
    public static final int SCOPE_SINGLELEVEL   =1;
    public static final int SCOPE_WHOLESUBTREE  =2;

    public static final int DEREFERE_NEVER          =0;
    public static final int DEREFERE_INSEARCHING    =1;
    public static final int DEREFERE_FINDINGBASEOBJ =2;
    public static final int DEREFERE_ALWAYS         =3;

    public static final int FILTER_AND              =0;
    public static final int FILTER_OR               =1;
    public static final int FILTER_NOT              =2;
    public static final int FILTER_EQUALITYMATCH    =3;
    public static final int FILTER_SUBSTRINGS       =4;
    public static final int FILTER_GREATEROREQUAL   =5;
    public static final int FILTER_LESSOREQUAL      =6;
    public static final int FILTER_PRESENT          =7;
    public static final int FILTER_APPROXMATCH      =8;
    public static final int FILTER_EXTENSIBLEMATCH  =9;

}