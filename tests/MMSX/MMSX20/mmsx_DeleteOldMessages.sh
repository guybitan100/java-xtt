#!/bin/bash
#
##########################################################################
#
# Mobixell 724 Solutions Inc.
#
# MMSX script to delete old archived messages from the database.
#
# Command: 
#
# mmsx_DeleteOldMessages <maxAge (in days)>
# 
# Example:
#
# mmsx_DeleteOldMessages 10
#
# Note:
# - The default value for maxAge is 365 days.  
# - Execute 'xms run command bash' to setup the environment(PATH, LD_LIBRARY_PATH and CLASSPATH) 
# 
# $Id: mmsx_DeleteOldMessages.sh,v 1.1 2010/07/16 11:23:25 mbhopale Exp $
#
##########################################################################


##########################################################################
# Calculate the delete timestamp for maxAge
##########################################################################

if [ "$1" -ge 0 ]; then
   maxAgeSeconds=$((  $1 * 86400))     
else
   maxAgeSeconds=$(( 365 * 86400)) 
fi

deleteTimeSeconds=$(( $(date +%s) - $maxAgeSeconds ))
deleteTimestamp=`date -d @$deleteTimeSeconds "+%Y-%m-%d %T"`

##########################################################################
# Delete the old messages  
##########################################################################
echo "...All messages older than $deleteTimestamp are going to be deleted." 

exitSts=0;
$( querier -y 1 -x 0 -p DB-ACCESS -o MmsxArchive -m com.mobilgw.intf.uds.mor.MmsxArchive.deleteOldMessages timestamp="$deleteTimestamp" )
exitSts=$?

if [ $exitStatus != 0 ]; then
  echo "...mmsx_DeleteOldMessages failed at $(date)."
else 
  echo "...mmsx_DeleteOldMessages completed successfully at $(date)." 
fi

exit $exitStatus;
