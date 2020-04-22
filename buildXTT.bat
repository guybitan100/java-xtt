
rem "@(#)$Id: buildXTT.bat,v 1.5 2010/03/17 09:03:21 cvsbuild Exp $";

rem Change paths to fit your environemnt.

set ANT_HOME=c:\Program Files\java\apache-ant-1.8.2
set JAVA_HOME=c:\Program Files\Java\jdk1.6.0_26
set PATH=c:\Program Files\Java\jdk1.6.0_26\bin;c:\Program Files\java\apache-ant-1.8.2\bin;

%ANT_HOME%\bin\ant -lib antlib Full > log.txt 2>&1

ant -verbose -buildfile build.xml > log.txt