XFW004 brach is used for XMA4.1.4 testing

XMA4.1.4 setup techniques..

1. Get latest PV and XTT config file from following folder:

	tests\xma\XFW_004\IntegrationTestSetup

2. For Push Proxy Gateway test cases need to change PV and XTT config file as follows:

	A. Comment following tag in PV file:

	<!-- <out messageStoreName="One" messageStorePartitionNumber="1" protocol="MM1">
		<mm1 channelType="SMS" mmscHost="UPDATE_XFW_HOST_NM" mmscPort="4001" mmscProtocol="http">
		<sms channel="TestChannel"/>
		</mm1>
	</out> -->
	
	B. Add following tag in PV file:

	<out messageStoreName="One" messageStorePartitionNumber="1" protocol="MM1">
		<mm1 channelType="PAP" mmscHost="UPDATE_XFW_HOST_NM" mmscPort="4001" mmscProtocol="http">
		<pap ppgHost="UPDATE_XFW_HOST_IP" ppgDomain="XMA" ppgPort="3775"/>
		</mm1>
	</out>

	C. Update XTT config file as follows:

	<pap>
		<ppgHost>UPDATE_XFW_HOST_IP</ppgHost>
		<ppgPort>3775</ppgPort>
	</pap>

	D. Run PAP test cases from following folder:
	
	\tests\xma\XFW_004\MM1-PAP-interface

3. Check the process logs and activity logs while executing test cases.
