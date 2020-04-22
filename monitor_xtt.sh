#!/bin/bash

DATE=`date +%D_%H:%M:%S | tr -s "/|:" "-"`
ps auxw | head -1 | tr -s " " "," > cpu_mem_$DATE.csv
while true
do
	pgrep -f XTT 1>/dev/null || break
	ps auxw | grep XTT | grep -v grep | tr -s " " "," >> cpu_mem_$DATE.csv
	sleep 5
done

