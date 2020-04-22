Test steps to execute Diameter Integration testcase "DiameterSupport.TC.001" and further testcases :



1. Copy the vfnz-mmsc.jar file in the <XMA_INSTALL_DIR>/lib directory.

2. Update the host name,XMA installed path and Java installed path in the xupdate_diameter.xml file.

	Ex: ONLINE-BILLING.lggwsl15.1	
	    <machine-name>lggwsl15</machine-name>
	    <value>/home/xma/4.1/lib/vfnz-mmsc.jar</value>
	    <string>-Dcom.mobilgw.jdk.home=/usr/local/java</string>

3. Modify the <transportPath> in the xupdate_diameter.xml with the IP Address and Port number of Diameter server .

	EX: <transportPath>172.20.14.56:1344</transportPath>
	(172.20.14.56 : Is the IP address of Windows machine where XTT is running since we are starting DIAMETER server using xtt) 

4. Generate cv file from the pv file with xupdate_diameter.xml xupdate file.

	Command:xms generate configuration <pvfile name> <cvfile name>

5. Configure the Diameter Server port number in the xtt config file.

	EX:  <DiameterServer>
		<!-- the udp/tcp listening port of the internal DiameterServer -->
			<Port>1344</Port>
		<!-- the listening port of the internal secure-DiameterServer -->
			<SecurePort>1345</SecurePort>
		<!-- timeout on client connections to the DiameterServer -->
				<Timeout>30000</Timeout>
		<!-- time to wait on a "wait" function before continuing -->
				<waitTimeout>30000</waitTimeout>
		 </DiameterServer>

6. Run the XTT in the Windows machine.Execute the xttDiameterTC001.xml to start the DIAMETER server.

7. Don't stop the DIAMETER server since the ONLINE-BILLING.lggwsl15.1 process connects to the DIAMETER server.

8. Start the XMA system using generated cv file.

	command: xms start system -clean <cvfile name>


