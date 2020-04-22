
Steps to run Provisioning Core API test cases.

Prerequisites:
----------------
XMP should properly up with clean database.

1. For http:// protocol:

	Need XMP tag in config file as described here:
	
	<XMP>
		<IP>172.21.5.13</IP>     <---- SYSTEM IP
		<PORT>9998</PORT>	 <---- HTTP PORT
		<protocol>http://</protocol>
	</XMP>

2. Run xtt test cases for http protocol.

-------------------------------------------------------------

Prerequisites:
----------------
XMP should properly up with clean database.

1. For https:// protocol:

	Need XMP tag in config file as described here:
	
	<XMP>
		<IP>172.21.5.13</IP>     <---- SYSTEM IP
		<PORT>9999</PORT>	 <---- HTTPs PORT
		<protocol>https://</protocol>
	</XMP>

2. Run xtt test cases for https protocol.

---------------------------------------------------------------