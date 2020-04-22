package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * NTLMHelper provides NTLM helper functions.
 *
 * @author      Gavin Cattell
 * @version     $Id: NTLMHelper.java,v 1.3 2007/10/05 08:11:13 gcattell Exp $
 */
public class NTLMHelper
{
    private static final long NTLM_VERSION = 1l;

    public static String doNTLMAuth(String headerName, String headerData, String username, String password)
    {
        //String password = "gavin";

        //WWW-Authenticate: NTLM opaque="045CCD95", gssapi-data="TlRMTVNTUAACAAAAAAAAADgAAADzgpjiy3DLrx+k+8kAAAAAAAAAALIAsgA4AAAABQLODgAAAA8CABQAVABBAE4AVABBAFUALQBTAEkAUAABAAwAUwBBAEkAUABBAE4ABAAkAHMAaQBwAC4AbABlAG4ALgB0AGEAbgB0AGEAdQAuAGMAbwBtAAMAMgBTAEEASQBQAEEATgAuAHMAaQBwAC4AbABlAG4ALgB0AGEAbgB0AGEAdQAuAGMAbwBtAAUAJABzAGkAcAAuAGwAZQBuAC4AdABhAG4AdABhAHUALgBjAG8AbQAAAAAA", targetname="SAIPAN.sip.len.tantau.com", realm="SIP Communications Service"
        //Authorization: NTLM qop="auth", opaque="045CCD95", realm="SIP Communications Service", targetname="SAIPAN.sip.len.tantau.com", gssapi-data="TlRMTVNTUAADAAAAGAAYAIgAAAAYABgAoAAAAAAAAABIAAAANgA2AEgAAAAKAAoAfgAAABAAEAC4AAAAVYKQQgUBKAoAAAAPZwBjAGEAdAB0AGUAbABsAEAAcwBpAHAALgBsAGUAbgAuAHQAYQBuAHQAYQB1AC4AYwBvAG0AUABBAFMAVABBALRw1w92jMkJA87XBPkf7autGkbFnXClK8B1TcJIUNhIIwBRJ4esUzRL8WKni67gwGigj8liaOimlZMmXGqeFSc="

        //Server message recieved, create a client reply header
        if (headerName.equals("WWW-Authenticate"))
        {
            Pattern pattern = Pattern.compile("gssapi-data=\"(.+?)\"",Pattern.DOTALL);
            Matcher matcher = pattern.matcher(headerData);
            String gssapi = null;
            if(matcher.find())
            {
                gssapi = matcher.group(1);
                //XTTProperties.printDebug("NTLM: found gssapi: " + gssapi);
            }
            else
            {
                XTTProperties.printFail("NTLM: NTLM Authentication error, can't find gssapi tag in WWW-Authenticate header");
                return null;
            }

            //This should be an NTLM type 2 message now... reply with a type 3
            byte[] ntlmChallenge = ConvertLib.base64Decode(gssapi);
            //XTTProperties.printInfo("NTLM\n: " + ConvertLib.getHexView(ntlmChallenge));
            int messageType = ConvertLib.getIntFromByteArray(ntlmChallenge,8,1);
            //XTTProperties.printDebug("NTLM: message type: " + messageType);

            byte[] nonce = new byte[8];
            ConvertLib.addBytesToArray(nonce,0,ntlmChallenge,24,8);
            //XTTProperties.printInfo("NTLM nonce\n: " + ConvertLib.getHexView(nonce));

            byte[] ntHash = ntHashPassword(password,nonce);
            byte[] lmHash = lmHashPassword(password,nonce);
            //XTTProperties.printInfo("NT Hash\n: " + ConvertLib.getHexView(ntHash));
            //XTTProperties.printInfo("LM Hash\n: " + ConvertLib.getHexView(lmHash));


            //byte[] randomSessionKey = Random.getRandomBytes(16);
            //This should be the secret random key: A2 D5 F8 D2 E7 16 02 C4 76 44 20 AD 2F 21 5B 3F
            //This is the encryted version: 68 a0 8f c9 62 68 e8 a6 95 93 26 5c 6a 9e 15 27
            byte[] randomSessionKey = ConvertLib.getByteArrayFromHexString("A2 D5 F8 D2 E7 16 02 C4 76 44 20 AD 2F 21 5B 3F");
            byte[] encryptedRandomSessionKey = encryptRandomSessionKey(password,randomSessionKey);

            XTTProperties.printInfo("encryptedRandomSessionKey: \n" + ConvertLib.getHexView(encryptedRandomSessionKey));

            String localHostName = null;
            try
            {
                localHostName = java.net.InetAddress.getByName(XTTProperties.getQuietProperty("SYSTEM/IP")).getCanonicalHostName();
                if(localHostName.indexOf(".") > 0)
                {
                    localHostName=localHostName.substring(0,localHostName.indexOf("."));
                    localHostName=localHostName.toUpperCase();
                }
            }
            catch(java.net.UnknownHostException e)
            {
                XTTProperties.printFail("NTLM: Can't resolve host name for '" + XTTProperties.getQuietProperty("SYSTEM/IP") + "'");
                return null;
            }


            byte[] utf16Hostname = ConvertLib.createBytes(localHostName,"UTF-16LE");
            byte[] utf16Username = ConvertLib.createBytes(username,"UTF-16LE");
            byte[] utf16Password = ConvertLib.createBytes(password,"UTF-16LE");

            int usernameOffset = 72;
            int hostnameOffset = utf16Username.length + usernameOffset;
            int lmOffset = utf16Hostname.length + hostnameOffset;
            int ntOffset = 24 + lmOffset;
            int keyOffset = 24 + ntOffset;

            //          Protocol + size + lm hash info + nt resp info + domain info + username info + host info + key info + flags + ...
            int ntlmResponseSize = 8 + 4 + (2 + 2 + 4) + (2 + 2 + 4) + (2 + 2 + 4) + (2 + 2 + 4) + (2 + 2 + 4) + (2 + 2 + 4) + (4) + 8 + utf16Username.length + utf16Hostname.length +  lmHash.length + ntHash.length + encryptedRandomSessionKey.length;
            byte[] ntlmResponse = new byte[ntlmResponseSize];

            ConvertLib.addBytesToArray(ntlmResponse,0,ConvertLib.getByteArrayFromHexString("4E 54 4C 4D 53 53 50 00"));
            //Set Message type to 3
            ntlmResponse[8] = 0x03;

            //Set LM Response info
            ntlmResponse[12] = 0x18; //Size
            ntlmResponse[14] = 0x18; //Max Size
            ntlmResponse[16] = (byte)lmOffset; //Offset

            //Set NT Response info
            ntlmResponse[20] = 0x18; //Size
            ntlmResponse[22] = 0x18; //Max Size
            ntlmResponse[24] = (byte)ntOffset; //Offset

            //Set domain info (Only offset since we're not setting any info)
            ntlmResponse[32] = 0x48; //Offset

            //Set user info
            ntlmResponse[36] = (byte)utf16Username.length; //Size
            ntlmResponse[38] = (byte)utf16Username.length; //Max Size
            ntlmResponse[40] = (byte)usernameOffset; //Offset

            //Set user info
            ntlmResponse[44] = (byte)utf16Hostname.length; //Size
            ntlmResponse[46] = (byte)utf16Hostname.length; //Max Size
            ntlmResponse[48] = (byte)hostnameOffset; //Offset

            //Set session key info
            ntlmResponse[52] = (byte)encryptedRandomSessionKey.length; //Size
            ntlmResponse[54] = (byte)encryptedRandomSessionKey.length; //Max Size
            ntlmResponse[56] = (byte)keyOffset; //Offset

            //Set flags
            ConvertLib.addBytesToArray(ntlmResponse,60,ConvertLib.getByteArrayFromHexString("55 82 90 42"));

            ConvertLib.addBytesToArray(ntlmResponse,usernameOffset,utf16Username);
            ConvertLib.addBytesToArray(ntlmResponse,hostnameOffset,utf16Hostname);
            ConvertLib.addBytesToArray(ntlmResponse,lmOffset,lmHash);
            ConvertLib.addBytesToArray(ntlmResponse,ntOffset,ntHash);
            ConvertLib.addBytesToArray(ntlmResponse,keyOffset,encryptedRandomSessionKey);

            XTTProperties.printDebug("Response\n: " + ConvertLib.getHexView(ntlmResponse));
            String base64NTLMResponse = ConvertLib.base64Encode(ntlmResponse);
            XTTProperties.printDebug("Base64 Response: " + base64NTLMResponse);
        }
        //Client message recieved, create a server reply header
        else if (headerName.equals("Authorization"))
        {
            Pattern pattern = Pattern.compile("gssapi-data=\"(.+?)\"",Pattern.DOTALL);
            Matcher matcher = pattern.matcher(headerData);
            String gssapi = null;
            if(matcher.find())
            {
                gssapi = matcher.group(1);
            }
            else
            {
                XTTProperties.printFail("NTLM: NTLM Authentication error, can't find gssapi tag in Authorization header");
                return null;
            }

            if(gssapi.equals(""))
            {
                //this was a type 1 message, create a type 2 challenge
            }
            else
            {
                //this is a type 3 response, check and reply 200 OK.
            }
        }
        else
        {
            XTTProperties.printFail("NTLM: NTLM Authentication error, unknown header '" + headerName + "'");
            return null;
        }
        return null;
    }
    
    public static String getClientSignatureBuffer(LinkedHashMap<String,Vector<String>> headers)
    {
        
        return null;
    }
    
    public static String getServerSignatureBuffer(LinkedHashMap<String,Vector<String>> headers, int responseCode)
    {
        /*
        1.	Authorization Method (e.g. “NTLM” or “Kerberos”)
        2.	srand (Authentication-Info header)
        3.	snum (Authentication-Info header)
        4.	Realm (Authentication-Info header)
        5.	Targetname (Authentication-Info header)
        6.	Call-ID (Call-ID header)
        7.	CSeq # (CSeq header)
        8.	CSeq method (CSeq header)
        9.	From URI (From header)
        10.	From tag (From header)
        11.	To tag (To header)
        12.	Expires (if present, otherwise use <>)  (Expires header)
        13.	Response Code (responses only)
        */

        Pattern pattern = null;
        Matcher matcher = null;
        StringBuffer signatureBuffer = new StringBuffer();
        signatureBuffer.append("<NTLM>");
        
        //FIND srand
        pattern = Pattern.compile("srand=\"(.+?)\"",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("authentication-info").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find srand in Authentication-Info header");
            return null;
        }       
        
        //FIND snum 
        pattern = Pattern.compile("snum=\"(.+?)\"",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("authentication-info").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find srand in Authentication-Info header");
            return null;
        }
        
        //FIND realm 
        pattern = Pattern.compile("realm=\"(.+?)\"",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("authentication-info").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find srand in Authentication-Info header");
            return null;
        } 
        //FIND targetname 
        pattern = Pattern.compile("targetname=\"(.+?)\"",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("authentication-info").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find targetname in Authentication-Info header");
            return null;
        }
        //FIND Call-ID 
        signatureBuffer.append("<" + headers.get("call-id").firstElement() + ">");
        //FIND CSeq # 
        pattern = Pattern.compile(".?([0-9]+?)",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("cseq").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find targetname in Authentication-Info header");
            return null;
        }
        //FIND CSeq method 
        pattern = Pattern.compile(".?[0-9]+? (.*)",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("cseq").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find targetname in Authentication-Info header");
            return null;
        } 
        //FIND From URI 
        pattern = Pattern.compile("<(.+?)>",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("from").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find targetname in Authentication-Info header");
            return null;
        }
        //FIND From tag 
        pattern = Pattern.compile("tag=(.+?);",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("from").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find targetname in Authentication-Info header");
            return null;
        } 
        //FIND To tag 
        pattern = Pattern.compile("tag=(.+?)\\z",Pattern.DOTALL);
        matcher = pattern.matcher(headers.get("to").firstElement());
        if(matcher.find())
        {
            signatureBuffer.append("<" + matcher.group(1) + ">");
        }
        else
        {
            XTTProperties.printFail("NTLM: can't find targetname in Authentication-Info header");
            return null;
        }        
        //FIND Expires
        try
        {
            signatureBuffer.append("<" + headers.get("expires").firstElement() + ">");                                              
        }
        catch(Exception e)
        {
            signatureBuffer.append("<>");  
        }
        //FIND response
        if(responseCode >= 0)
        {
            signatureBuffer.append("<"+responseCode+">");  
        }        
        return signatureBuffer.toString();    
    }

    public static String createSignature(String signatureBuffer, byte[] randomSessionKey, long sequenceNumber)
    {
        ARC4 arc4 = new ARC4();
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        
        byte[] unencrypted = new byte[12];
        byte[] signature = new byte[16];
        
        crc.update(ConvertLib.createBytes(signatureBuffer));
        long crcResult = crc.getValue();
        byte[] byteCrc = ConvertLib.getByteArrayFromLong(crcResult,4);
        byte[] byteVersion = ConvertLib.getByteArrayFromLong(NTLM_VERSION,4);
        byte[] byteSeqNum = ConvertLib.getByteArrayFromLong(sequenceNumber,4);
        
        XTTProperties.printInfo("CRC: " + crcResult +"\n"+ ConvertLib.getHexView(byteCrc));
        
        signature[0]=byteVersion[3];
        signature[1]=byteVersion[2];
        signature[2]=byteVersion[1];
        signature[3]=byteVersion[0];
        
        unencrypted[4]=byteCrc[3];
        unencrypted[5]=byteCrc[2];
        unencrypted[6]=byteCrc[1];
        unencrypted[7]=byteCrc[0];
        
        unencrypted[8]=byteSeqNum[3];
        unencrypted[9]=byteSeqNum[2];
        unencrypted[10]=byteSeqNum[1];
        unencrypted[11]=byteSeqNum[0];
        
        XTTProperties.printInfo("Unencrypted: " + ConvertLib.getHexView(unencrypted));
        
        arc4.setKey(randomSessionKey);
        
        byte[] encrypted = arc4.encrypt(unencrypted);
        
        ConvertLib.addBytesToArray(signature,4,encrypted);
        
        return ConvertLib.getHexStringFromByteArray(signature);        
    }

    public static long getSequenceNumberFromSignature(String rspauth, byte[] randomSessionKey)
    {
        ARC4 arc4 = new ARC4();
        byte[] byteRspauth = ConvertLib.getByteArrayFromHexString(rspauth);
        byte[] encrypted = new byte[12];
        ConvertLib.addBytesToArray(encrypted,0,byteRspauth,4,12);
        
        XTTProperties.printInfo("Full: " + ConvertLib.getHexView(byteRspauth));
        XTTProperties.printInfo("Encrypted: " + ConvertLib.getHexView(encrypted));
        
        arc4.setKey(randomSessionKey);
        byte[] decrypted = arc4.encrypt(encrypted);
        
        XTTProperties.printInfo("Decrypted: " + ConvertLib.getHexView(decrypted));
        
        byte[] byteSeq = {decrypted[11],decrypted[10],decrypted[9],decrypted[8]};
        byte[] byteCrc = {decrypted[7],decrypted[6],decrypted[5],decrypted[4]};
        //long seqNum = Long.parseLong(new String(decrypted,8,4));
        //XTTProperties.printInfo("Seq Number: " + seqNum);
        XTTProperties.printInfo("Crc: " + ConvertLib.getHexView(byteCrc));
        XTTProperties.printInfo("Seq: " + ConvertLib.getHexView(byteSeq));
        long seqNum = ConvertLib.getLongFromByteArray(byteSeq,0,4);
        XTTProperties.printInfo("Seq: " + seqNum);
        
        return 0l;
    }

    /*private long reverseLong(long x)
    {
        long answer = ((((x) >> 24) & 0x00ff) | 
                      (((x) >> 8) & 0xff00)   | 
                      (((x) & 0xff00) << 8)   | 
                      (((x) & 0x00ff) << 24));
         return answer;
    }*/
    
    /*public static void signedUnsignedTest()
    {
        long a = 0xFFFF;
        XTTProperties.printInfo("" + );
    }*/
    
       
    public static byte[] encryptRandomSessionKey(String password, byte[] randomSessionKey)
    {
        //UTF-16LE bytes of password.
        byte[] pass = new byte[password.length() * 2];//ConvertLib.createBytes(password,"UTF-16LE");
        for(int i=0,index=0;i<password.length();i++,index+=2)
        {
            pass[index]=(byte)password.charAt(i);
        }
        byte[] sessionEncryptionKey = new byte[0];
        //byte[] passwordHash1 = new byte[0];
        try
        {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD4", "XTTCrypto");
            //Take MD4 of password to create NTLM hash
            byte[] passwordHash1 = md.digest(pass);
            //Take MD4 of NTLM hash to create NTLM User Session Key
            sessionEncryptionKey = md.digest(passwordHash1);
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }
        XTTProperties.printInfo("Pass: \n" + ConvertLib.getHexView(pass));
        //XTTProperties.printInfo("PassHash1: \n" + ConvertLib.getHexView(passwordHash1));
        //XTTProperties.printInfo("SessionKey: \n" + ConvertLib.getHexView(sessionEncryptionKey));
        

        ARC4 arc4 = new ARC4();
        arc4.setKey(sessionEncryptionKey);
        byte[] encryptedRandomSessionKey = arc4.encrypt(randomSessionKey);
        //XTTProperties.printInfo("EncryptedRandomKey: \n" + ConvertLib.getHexView(encryptedRandomSessionKey));
        return encryptedRandomSessionKey;
    }

/*    public static byte[] encryptRandomSessionKey(String password, byte[] randomSessionKey)
    {
        //UTF-16LE bytes of password.
        byte[] pass = ConvertLib.createBytes(password,"UTF-16LE");
        byte[] sessionEncryptionKey = new byte[0];
        try
        {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD4", "XTTCrypto");
            //Take MD4 of password to create NTLM hash
            byte[] passwordHash1 = md.digest(pass);
            //Take MD4 of NTLM hash to create NTLM User Session Key
            sessionEncryptionKey = md.digest(passwordHash1);
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }

        ARC4 arc4 = new ARC4();
        arc4.setKey(sessionEncryptionKey);
        byte[] encryptedRandomSessionKey = arc4.encrypt(randomSessionKey);
        return encryptedRandomSessionKey;
    }*/

    public static byte[] arc4Test(byte[] key, byte[] data)
    {
        ARC4 arc4 = new ARC4();
        //ARC4 arc3 = new ARC4();
        arc4.setKey(key);
        //arc3.setKey(ConvertLib.createBytes(key));
        byte[] decrypted = arc4.encrypt(data);
        //byte[] sessionKey2 = arc3.decrypt(ConvertLib.createBytes(data));
        return decrypted;
    }


    public static void crcTest(String data)
    {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(ConvertLib.createBytes(data));
        long crcResults = crc.getValue();
        XTTProperties.printInfo("CRC: " + crcResults);
    }


    /*
     * Turns a 56 bit key into the 64 bit, odd parity key.
     */
    private static byte[] setupKey(byte[] key56)
    {
        byte[] key64 = new byte[8];
        key64[0] = (byte) ((key56[0] >> 1) & 0xff);
        key64[1] = (byte) ((((key56[0] & 0x01) << 6) | (((key56[1] & 0xff) >> 2) & 0xff)) & 0xff);
        key64[2] = (byte) ((((key56[1] & 0x03) << 5) | (((key56[2] & 0xff) >> 3) & 0xff)) & 0xff);
        key64[3] = (byte) ((((key56[2] & 0x07) << 4) | (((key56[3] & 0xff) >> 4) & 0xff)) & 0xff);
        key64[4] = (byte) ((((key56[3] & 0x0f) << 3) | (((key56[4] & 0xff) >> 5) & 0xff)) & 0xff);
        key64[5] = (byte) ((((key56[4] & 0x1f) << 2) | (((key56[5] & 0xff) >> 6) & 0xff)) & 0xff);
        key64[6] = (byte) ((((key56[5] & 0x3f) << 1) | (((key56[6] & 0xff) >> 7) & 0xff)) & 0xff);
        key64[7] = (byte) (key56[6] & 0x7f);

        for (int i = 0; i < key64.length; i++)
        {
            key64[i] = (byte) (key64[i] << 1);
        }
        return key64;
    }

   /* public static byte[] sign(byte[] nonce)
    {
        try
        {
            String password="gavin";
            java.security.Provider xtt_provider = new XTTCrypto();
            java.security.Security.addProvider(xtt_provider);

            //UTF-16LE bytes of password.
            byte[] pass = new byte[0];
            try
            {
                pass = password.getBytes("UTF-16LE");
            } catch (java.io.UnsupportedEncodingException uee)
            {
                XTTProperties.printWarn("Unsupported charset: UTF-16LE");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(uee);
                }
            }

            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD4", "XTTCrypto");
            //Take MD4 of password to create NTLM hash
            byte[] NTLMhash = md.digest(pass);
            //Take MD4 of NTLM hash to create NTLM User Session Key
            byte[] NTLMUserSessionKey = md.digest(NTLMhash);

            RC4.setKey(NTLMUserSessionKey);

            byte[] sessionKey = RC4.decrypt(ConvertLib.getByteArrayFromHexString("68 A0 8F C9 62 68 E8 A6 95 93 26 5C 6A 9E 15 27"));

            XTTProperties.printInfo("Negotiated Key\n: " + ConvertLib.getHexView(sessionKey));


            RC4.setKey(sessionKey);

            long seqNum = 0l;

            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(ConvertLib.createBytes("Content-Length: 0"));
            long crcResults = crc.getValue();

            XTTProperties.printInfo("Crc result: " + crcResults);

            byte[] concated = new byte[12];
            byte[] start = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            ConvertLib.addBytesToArray(concated,0,start);
            ConvertLib.addBytesToArray(concated,4,java.math.BigInteger.valueOf(crcResults).toByteArray());
            ConvertLib.addBytesToArray(concated,8,java.math.BigInteger.valueOf(seqNum).toByteArray());

            XTTProperties.printInfo("concated Key\n: " + ConvertLib.getHexView(concated));

            byte[] signed = RC4.encrypt(concated);

            XTTProperties.printInfo("Signed Key\n: " + ConvertLib.getHexView(signed));*/


            /*byte[] sessionKey = RC4.decrypt(ConvertLib.getByteArrayFromHexString("68 A0 8F C9 62 68 E8 A6 95 93 26 5C 6A 9E 15 27"));

            XTTProperties.printInfo("key2: " + ConvertLib.getHexView(sessionKey));

            RC4.setKey(sessionKey);


            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(new byte[0]);
            long crcResults = crc.getValue();

            XTTProperties.printInfo("Crc result: " + crcResults);



            byte[] interim = new byte[12];
            byte[] start = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            ConvertLib.addBytesToArray(interim,0,start);
            ConvertLib.addBytesToArray(interim,4,java.math.BigInteger.valueOf(crcResults).toByteArray());
            ConvertLib.addBytesToArray(interim,8,java.math.BigInteger.valueOf(seqNum).toByteArray());

            XTTProperties.printInfo("Key: " + ConvertLib.getHexView(interim));

            byte[] signResult = RC4.encrypt(interim);

            XTTProperties.printInfo("Signature: " + ConvertLib.getHexView(signResult));

            seqNum = 1l;

            interim = new byte[12];
            ConvertLib.addBytesToArray(interim,0,start);
            ConvertLib.addBytesToArray(interim,4,java.math.BigInteger.valueOf(crcResults).toByteArray());
            ConvertLib.addBytesToArray(interim,8,java.math.BigInteger.valueOf(seqNum).toByteArray());

            XTTProperties.printInfo("Key: " + ConvertLib.getHexView(interim));

            signResult = RC4.encrypt(interim);

            XTTProperties.printInfo("Signature: " + ConvertLib.getHexView(signResult));     */

       /* }
        catch(Exception e) //
        {
            XTTProperties.printException(e);
        }
        return new byte[0];
    }*/

    public static byte[] ntHashPassword(String password, byte[] nonce)
    {
        try
        {
            StringBuffer ntPassword = new StringBuffer();
            for (int i=0; i<password.length(); i++)
            {
                ntPassword.append(password.charAt(i));
                ntPassword.append((char)0);
            }
            byte[] pass = ConvertLib.createBytes(ntPassword.toString());
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD4", "XTTCrypto");
            byte[] ntpass = md.digest(pass);
            byte[] keyPass = new byte[21];
            ConvertLib.addBytesToArray(keyPass,0,ntpass);
            return hashPassword(keyPass,nonce);
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }
        return new byte[0];

    }
    public static byte[] lmHashPassword(String password, byte[] nonce)
    {
        byte[] pass = ConvertLib.createBytes(password.toUpperCase());
        byte[] lmPw1 = new byte[7];
        byte[] lmPw2 = new byte[7];

        int len = pass.length;
        if (len > 7) { len = 7; }

        int i=0;
        for (i = 0; i < len; i++)
        {
            lmPw1[i] = pass[i];
        }
        for (; i < 7; i++)
        {
            lmPw1[i] = (byte) 0;
        }

        len = pass.length;
        if (len > 14) { len = 14; }

        for (i = 7; i < len; i++)
        {
            lmPw2[i - 7] = pass[i];
        }
        for (; i < 14; i++)
        {
            lmPw2[i - 7] = (byte) 0;
        }

        byte[] magic = {(byte) 0x4B, (byte) 0x47, (byte) 0x53, (byte) 0x21, (byte) 0x40, (byte) 0x23, (byte) 0x24, (byte) 0x25};

        byte[] lmHpw1 = encrypt(lmPw1, magic);
        byte[] lmHpw2 = encrypt(lmPw2, magic);

        byte[] lmHpw = new byte[21];

        for (i = 0; i < lmHpw1.length; i++)
        {
            lmHpw[i] = lmHpw1[i];
        }
        for (i = 0; i < lmHpw2.length; i++)
        {
            lmHpw[i + 8] = lmHpw2[i];
        }
        for (i = 0; i < 5; i++)
        {
            lmHpw[i + 16] = (byte) 0;
        }
        return hashPassword(lmHpw,nonce);

    }

    private static byte[] hashPassword(byte[] keys, byte[] data)
    {
        byte[] lmResp = new byte[24];

        byte[] keys1 = new byte[7];
        byte[] keys2 = new byte[7];
        byte[] keys3 = new byte[7];
        int i=0;
        for (i = 0; i < 7; i++)
        {
            keys1[i] = keys[i];
        }
        for (i = 0; i < 7; i++)
        {
            keys2[i] = keys[i + 7];
        }
        for (i = 0; i < 7; i++)
        {
            keys3[i] = keys[i + 14];
        }
        byte[] results1 = encrypt(keys1, data);
        byte[] results2 = encrypt(keys2, data);
        byte[] results3 = encrypt(keys3, data);

        for (i = 0; i < 8; i++)
        {
            lmResp[i] = results1[i];
        }
        for (i = 0; i < 8; i++)
        {
            lmResp[i + 8] = results2[i];
        }
        for (i = 0; i < 8; i++)
        {
            lmResp[i + 16] = results3[i];
        }

        return lmResp;
    }

    private static byte[] encrypt(byte[] key, byte[] data)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            key = setupKey(key);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
            byte[] encryptedData = cipher.doFinal(data);
            return encryptedData;
        }
        catch (java.security.NoSuchAlgorithmException nsae)
        {
            XTTProperties.printFail("DES encryption is not available.");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null)) XTTProperties.printException(nsae);
        }
        catch (java.security.InvalidKeyException ike)
        {
            XTTProperties.printFail("Invalid key for DES encryption.");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null)) XTTProperties.printException(ike);
        }
        catch (javax.crypto.NoSuchPaddingException nspe)
        {
            XTTProperties.printFail("NoPadding option for DES is not available.");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null)) XTTProperties.printException(nspe);
        }
        catch (javax.crypto.IllegalBlockSizeException ibse)
        {
            XTTProperties.printFail("Invalid block size for DES encryption.");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null)) XTTProperties.printException(ibse);
        }
        catch (javax.crypto.BadPaddingException bpe)
        {
            XTTProperties.printFail("Data not padded correctly for DES encryption.");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null)) XTTProperties.printException(bpe);
        }
        return new byte[0];
    }


    public static final String tantau_sccsid = "@(#)$Id: NTLMHelper.java,v 1.3 2007/10/05 08:11:13 gcattell Exp $";


    /* NTLM2 Session Security

                String password="gavin";
            java.security.Provider xtt_provider = new XTTCrypto();
            java.security.Security.addProvider(xtt_provider);

            //UTF-16LE bytes of password.
            byte[] pass = new byte[0];
            try
            {
                pass = password.getBytes("UTF-16LE");
            } catch (java.io.UnsupportedEncodingException uee)
            {
                XTTProperties.printWarn("Unsupported charset: UTF-16LE");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(uee);
                }
            }

            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD4", "XTTCrypto");
            //Take MD4 of password to create NTLM hash
            byte[] NTLMhash = md.digest(pass);
            //Take MD4 of NTLM hash to create NTLM User Session Key
            byte[] NTLMUserSessionKey = md.digest(NTLMhash);


            XTTProperties.printInfo("NTLMUserSessionKey: " + ConvertLib.getHexView(NTLMUserSessionKey));

            //Create NTLM Session Response User Session Key

            javax.crypto.spec.SecretKeySpec keyspec = new javax.crypto.spec.SecretKeySpec(NTLMUserSessionKey, "HmacMD5");

            // Get instance of Mac object implementing HMAC-MD5, and
            // initialize it with the above secret key
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacMD5");
            mac.init(keyspec);
            byte[] NTLM2SessionResponseUserSessionKey = mac.doFinal(nonce);

            XTTProperties.printInfo("NTLM2SessionResponseUserSessionKey: " + ConvertLib.getHexView(NTLM2SessionResponseUserSessionKey));
                                                                     //0x73657373696f6e206b657920746f20636c69656e742d746f2d736572766572207365616c696e67206b6579206d6167696320636f6e7374616e7400
            byte[] magicConstant = ConvertLib.getByteArrayFromHexString("73657373696f6e206b657920746f20636c69656e742d746f2d736572766572207369676e696e67206b6579206d6167696320636f6e7374616e7400");

            byte[] temp = new byte[NTLM2SessionResponseUserSessionKey.length + magicConstant.length];
            ConvertLib.addBytesToArray(temp,0,NTLM2SessionResponseUserSessionKey);
            ConvertLib.addBytesToArray(temp,NTLM2SessionResponseUserSessionKey.length,magicConstant);

            byte[] ClientSigningKey = new byte[0];
            try
            {
                java.security.MessageDigest md5Hash = java.security.MessageDigest.getInstance("MD5");
                ClientSigningKey = md5Hash.digest(temp);
            }
            catch (Exception e)
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }

            XTTProperties.printInfo("ClientSigningKey: " + ConvertLib.getHexView(ClientSigningKey));

            temp = new byte[5 + magicConstant.length];
            ConvertLib.addBytesToArray(temp,0,NTLM2SessionResponseUserSessionKey,0,5);
            ConvertLib.addBytesToArray(temp,5,magicConstant);

            byte[] ClientSealingKey = new byte[0];
            try
            {
                java.security.MessageDigest md5Hash = java.security.MessageDigest.getInstance("MD5");
                ClientSealingKey = md5Hash.digest(temp);
            }
            catch (Exception e)
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }

            XTTProperties.printInfo("ClientSigningKey: " + ConvertLib.getHexView(ClientSealingKey));

            byte[] message= ConvertLib.createBytes("");
            long seqNum = 0l;
            byte[] seq = java.math.BigInteger.valueOf(seqNum).toByteArray();
            temp = new byte[message.length+seq.length];
            ConvertLib.addBytesToArray(temp,0,seq);
            ConvertLib.addBytesToArray(temp,seq.length,message);

            keyspec = new javax.crypto.spec.SecretKeySpec(ClientSigningKey, "HmacMD5");
            mac = javax.crypto.Mac.getInstance("HmacMD5");
            mac.init(keyspec);
            byte[] signed = mac.doFinal(temp);

            XTTProperties.printInfo("Pre-RC4 message: " + ConvertLib.getHexView(signed));

            RC4.setKey(ClientSealingKey);

            temp = new byte[8];
            ConvertLib.addBytesToArray(temp,0,signed,0,8);

            byte[] sealed = RC4.encrypt(temp);

            XTTProperties.printInfo("Sealed: " + ConvertLib.getHexView(sealed));

            */
}
