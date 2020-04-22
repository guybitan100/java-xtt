#
# $Id: makefile.mak,v 1.2 2006/07/21 17:04:22 cvsbuild Exp $
#

bin:
        sh ant -verbose -buildfile build.xml 


clean:
        sh ant -verbose -buildfile build.xml clean
        @rm -f make.log
