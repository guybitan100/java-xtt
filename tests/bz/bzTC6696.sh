#!/bin/bash

function docmu
{
until [ "$CMUVAL" == "Partial" ]
do
CMUVAL=`$XMSCOMMAND cmu -r $SERVICE 2>&1|grep Partial|awk '{print $1}'`
done;
CMUVAL=""
}

SERVICE=$1
AMOUNT=$2
XMSCOMMAND="$3 $4 $5 $6 $7 $8"
CMUVAL="cmu:"
for (( LOOPER=$AMOUNT ; $LOOPER ; LOOPER=$LOOPER-1 )) ; 
do
docmu
pkill -f "cee -n $SERVICE"
done;
