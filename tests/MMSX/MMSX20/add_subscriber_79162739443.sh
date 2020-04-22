#!/usr/bin/ksh

querier -y 1 -x 0 -o MMSX -p F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addSubscriber password= attemptedLoginCount=0 loginBlocked=false attemptedResetPasswordCount=0 resetPasswordFirstAttemptTime=0 resetPasswordBlockedTime=0  subscriberId=2 msisdn="79162739443"   user="B Number" region="Moscow" masterSwitchOn=true

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceArchive subscriberId=2  serviceEnabled=true

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceAutoResponder subscriberId=2  serviceEnabled=true serviceConditional=false defaultEnabled=false alreadySent=""

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceForward subscriberId=2  serviceEnabled=false serviceConditional=false forwardingMsisdn="" subjectPrefix="" forwardingAll=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceNickname subscriberId=2  serviceEnabled=true serviceConditional=false nickname="Bnumber"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceSecret subscriberId=2  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceWhiteBlacklist subscriberId=2  serviceEnabled=true serviceConditional=false whitelistMode=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceCalendar subscriberId=2  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceExpressMessage subscriberId=2  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=2  msisdn="0791111113"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=2  msisdn="0791111114"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=2  msisdn="0791111115"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=2  msisdn="0791111116"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=2  msisdn="0791111211"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=2  msisdn="0791111138"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=2  msisdn="0791111114" password="qwerty"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=2  msisdn="0791111125" password="QWERTY"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=2  msisdn="0791111115" password="123456"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=2  msisdn="0791111211" password="poiuyt"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addGroup subscriberId=2  groupId=11 groupName="group11"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=11 msisdn="0791111113"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=11 msisdn="0791111114"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addGroup subscriberId=2  groupId=12 groupName="group12"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=12 msisdn="0791111125"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=12 msisdn="0791111126"

querier -y 1 -x 0 -o MMSX -p F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addSubscriber password= attemptedLoginCount=0 loginBlocked=false attemptedResetPasswordCount=0 resetPasswordFirstAttemptTime=0 resetPasswordBlockedTime=0  subscriberId=3 msisdn="79162739444"   user="B Number" region="Moscow" masterSwitchOn=true

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceArchive subscriberId=3  serviceEnabled=true

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceAutoResponder subscriberId=3  serviceEnabled=true serviceConditional=false defaultEnabled=false alreadySent=""

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceForward subscriberId=3  serviceEnabled=false serviceConditional=false forwardingMsisdn="" subjectPrefix="" forwardingAll=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceNickname subscriberId=3  serviceEnabled=true serviceConditional=false nickname="Bnumber"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceSecret subscriberId=3  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceWhiteBlacklist subscriberId=3  serviceEnabled=true serviceConditional=false whitelistMode=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceCalendar subscriberId=3  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceExpressMessage subscriberId=3  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=3  msisdn="0791111111"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=3  msisdn="0791111112"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=3  msisdn="0791111113"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=3  msisdn="0791111114"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=3  msisdn="0791111212"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=3  msisdn="0791111139"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=3  msisdn="0791111114" password="qwerty"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=3  msisdn="0791111125" password="QWERTY"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=3  msisdn="0791111115" password="123456"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=3  msisdn="0791111211" password="poiuyt"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addGroup subscriberId=3  groupId=13 groupName="group13"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=13 msisdn="0791111113"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=13 msisdn="0791111114"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addGroup subscriberId=3  groupId=14 groupName="group14"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=14 msisdn="0791111125"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=14 msisdn="0791111126"

querier -y 1 -x 0 -o MMSX -p F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addSubscriber password= attemptedLoginCount=0 loginBlocked=false attemptedResetPasswordCount=0 resetPasswordFirstAttemptTime=0 resetPasswordBlockedTime=0  subscriberId=4 msisdn="79162739445"   user="B Number" region="Moscow" masterSwitchOn=true

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceArchive subscriberId=4  serviceEnabled=true

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceAutoResponder subscriberId=4  serviceEnabled=true serviceConditional=false defaultEnabled=false alreadySent=""

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceForward subscriberId=4  serviceEnabled=false serviceConditional=false forwardingMsisdn="" subjectPrefix="" forwardingAll=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceNickname subscriberId=4  serviceEnabled=true serviceConditional=false nickname="Bnumber"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceSecret subscriberId=4  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceWhiteBlacklist subscriberId=4  serviceEnabled=true serviceConditional=false whitelistMode=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceCalendar subscriberId=4  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.MmsxService.addServiceExpressMessage subscriberId=4  serviceEnabled=true serviceConditional=false

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=4  msisdn="0791111111"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=4  msisdn="0791111112"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=4  msisdn="0791111113"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=4  msisdn="0791111114"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=4  msisdn="0791111212"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addWhiteBlackList subscriberId=4  msisdn="0791111139"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=4  msisdn="0791111114" password="qwerty"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=4  msisdn="0791111125" password="QWERTY"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=4  msisdn="0791111115" password="123456"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.setPassword subscriberId=4  msisdn="0791111211" password="poiuyt"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addGroup subscriberId=4  groupId=13 groupName="group13"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=13 msisdn="0791111113"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=13 msisdn="0791111114"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addGroup subscriberId=4  groupId=14 groupName="group14"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=14 msisdn="0791111125"

querier -y 1 -x 0 -o MMSX -p  F.CEE.DB-ACCESS -m com.mobilgw.intf.uds.mor.Mmsx.addRecipientToGroup groupId=14 msisdn="0791111126"
