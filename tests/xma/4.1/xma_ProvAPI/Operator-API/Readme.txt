
Steps to run Communities-API test cases.

Prerequisites:
----------------
XMA should properly up with clean database.

1. For http:// protocol:

	Need XMA tag in config file as described here:
	
	<XMA>
		<IP>172.21.5.6</IP>     <---- SYSTEM IP
		<PORT>8080</PORT>	 <---- HTTP PORT
		<protocol>http://</protocol>
	</XMA>

2. Run xtt test cases for http protocol.

-------------------------------------------------------------

Prerequisites:
----------------
XMA should properly up with clean database.

1. For https:// protocol:

	Need XMA tag in config file as described here:
	
	<XMA>
		<IP>172.21.5.6</IP>     <---- SYSTEM IP
		<PORT>8081</PORT>	 <---- HTTPs PORT
		<protocol>https://</protocol>
	</XMA>

2. Run xtt test cases for https protocol.

---------------------------------------------------------------