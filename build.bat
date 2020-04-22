rem Change paths to fit your environemnt.
echo off

if "%ARCHIVE_CLIENT_HOME%"=="" set ARCHIVE_CLIENT_HOME=C:\Program Files\GNU\cvsnt
if "%CVSROOT%"=="" set CVSROOT=:pserver:cvsbuild@cayman.len.tantau.com:/cvs/CorePlatform/Repository

if %COMPUTERNAME%==VISP set SOFTDRIVE=C
if %COMPUTERNAME%==POSEIDON set SOFTDRIVE=F
if %COMPUTERNAME%==ATLANTIS set SOFTDRIVE=E


if "%SOFTDRIVE%"=="" (
   echo ERROR:
   echo       You need to set SOFTDRIVE to drive where <x>:\Software\apache-ant-1.6.5 and <x>:\Software\jdk1.5.0_06 are installed
   exit /B 1
)


if "%JAVA_HOME%"=="" set JAVA_HOME=%SOFTDRIVE%:\Software\jdk1.5.0_06
if "%ANT_HOME%"=="" set ANT_HOME=%SOFTDRIVE%:\Software\apache-ant-1.6.5

set PATH=%JAVA_HOME%\bin\;%ANT_HOME%\bin\;%ARCHIVE_CLIENT_HOME%;


%ANT_HOME%\bin\ant -lib antlib -propertyfile xtt.mailproperties -logger org.apache.tools.ant.listener.MailLogger Full > log.txt 2>&1


ant -verbose -buildfile build.xml