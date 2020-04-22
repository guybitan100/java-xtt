PV file and experiment file should be adapted.

FOllowing files are required to run load gen exepriment:
lg_pcrf_experiment.CCR_WebServiceCall_d60.xml - experiment file
lg_pcrf_sessions.CCR_WebServiceCall.xml - session description file
lg_pcrf_userIps.txt - IP addresses
lg_pcrf_users.txt - users
lg_pcrf_update_rules.data - soap request data 



#LoadGen configuartion used for the test is available on sugarloaf (172.20.2.8) in:
/home/lukasz/loadgen_xfw003/cfg

#Set environment
/opt/loadgen/bin/xms -loadgen r c bash

#generate cv
#start system
xms start sys -clean cv-pv_loadgen_XFWB154_20091127.xml
xms load da /home/lukasz/cfg/license-loadgen-sugarloaf.xml
xms link work /opt/loadgen/cfg/loadgen.xpdl
xms load da loadgen.cxpdl

#Load Dynamic Data
xms load fi lg_pcrf_users.txt
xms load fi lg_pcrf_userIps.txt

#Start experiment
lg_pcrf_experiment.CCR_WebServiceCall_d24h.xml

