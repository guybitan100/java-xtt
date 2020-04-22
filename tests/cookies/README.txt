//
// How to run the Cookie-Tests:
//

1. User "xttCookieUser" must be provisioned in the xmg/oam database


2. Define XTT_CLASSPATH

   e.g. export XTT_CLASSPATH=".;u:/source/tests/xtt/xtt.jar;u:/source/exports/lib/jdom.jar;u:/source/exports/lib/radclient3.jar;u:/source/exports/lib/jwap-1.1.jar"


3. Run a single test or a list of tests

   e.g. Single test:   
   cd u:/source/tests/xtt
   java -cp $XTT_CLASSPATH com.tantau.xtt.XTT -c tests/cookies/Cookie_Config.xml -s tests/cookies/Cookies_HTTP_Test0001_Sub0001.xml	

   e.g. Test List (run all cookie tests using a WAP1 or HTTP connection):
   cd u:/source/tests/xtt
   java -cp $XTT_CLASSPATH com.tantau.xtt.XTT -c tests/cookies/Cookie_Config.xml -t tests/cookies/XMG2-6-GA-T0000-WAP1.list

   java -cp $XTT_CLASSPATH com.tantau.xtt.XTT -c tests/cookies/Cookie_Config.xml -t tests/cookies/XMG2-6-GA-T0000-HTTP.list
   
   
