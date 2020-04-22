set CLASSPATH=lib\xtt.jar;lib\xercesImpl.jar;lib\sis2_client-1.1.2.jar;lib\logkit-1.2.jar;lib\avalon-framework-4.1.5.jar;lib\jacorb.jar;lib\jdmkrt.jar;lib\jdmktk.jar;lib\jsnmpapi.jar;lib\jaxen-jdom.jar;lib\jaxen-core.jar;lib\saxpath.jar;lib\jdom.jar;lib\radclient3.jar;lib\jWAP.jar;lib\;%classpath%
java -Xmx1024m -jar y:\xtt\lib\xtt.jar -c XTT-config-xma40-lanta.xml -s tests/bz/BZ20458/BZ20458-0.xml
:loop
java -Xmx1024m -jar y:\xtt\lib\xtt.jar -c XTT-config-xma40-lanta.xml -s tests/bz/BZ20458/BZ20458.xml
goto loop

