#!/bin/bash 

clear
read -p "How many runs in this loop ? :" times
for i in `seq 1 $times`
do
echo "Run #$i started at:  `date +%D`  `date +%H:%M:%S`"
/usr/bin/java -Xmx512m -jar lib/xtt.jar -c xtt.conf_10.1.1.36.xml -l /work/XTT_Package_2.0.0410/tests/XMP/XMP_XFW006/IntegrationTestSetup/xtt_tests_xmp431_no_vo_with_bz.list 1>/dev/null
done


##############    The below create a summaries for the above xtt runs    ##############

taarich=`date +%D | tr -s "/" "_"`
mkdir -p /var/www/html/logs/RunSummary/$taarich 2>/dev/null
ls -lrt logs/ | awk '{ print $NF }' | grep '^log' | tail -"$times" > tmp
i=0
while read -r lines
do
((i=i+1))
FILE=`echo $lines`
grep '^Test \#' logs/$FILE | grep FAILED | cut -d" " -f3 > tmp1
while read -r failed; do echo "$failed</br>" >> failed_tmp; done < tmp1
aTime=`grep '^</head>' logs/$FILE | awk -F'>' '{ print $5 }' | awk -F'<' '{ print $1 }'`
echo "
<html>
<body>
`cat failed_tmp`
</body>
</html>" > /var/www/html/logs/RunSummary/$taarich/run"$i"_FAILED_"$taarich".html
a=`grep '^PASSED:.*FAILED:' logs/$FILE`
passed=`echo $a | cut -d";" -f1 | cut -d" " -f2`
failed=`echo $a | cut -d";" -f2 | cut -d" " -f3`
((sum=$passed+$failed))
percentage=`echo "scale=2; $passed/$sum*100" | bc`
echo "Run #$i ~ `grep '^PASSED:.*FAILED:' logs/$FILE`; %$percentage PASSED   ($FILE)"

echo "<tr><td><b>Run #$i </b></td><td>$aTime</td><td style="background-color:#33FF99">PASSED  $passed </td><td style="background-color:#FF0033">FAILED  $failed </td><td style="background-color:#00FF00">Success  %$percentage </td> <td>  <a href=../../../xtt/XTT_Package_2.0.0410/logs/$FILE>log file</a></td>  <td> Click this <a href=run"$i"_FAILED_"$taarich".html>link</a> for the FAILED list</td></tr>" >> table
\rm failed_tmp tmp1
done < tmp

######   creating the html file for the status   ######
echo "
<html>
<body bgcolor="#E0E0E0">
`ssh 10.1.1.36 "/root/versions.sh"`
</br>
<table border=\"2\" style="font-family:Arial">
" > /var/www/html/logs/RunSummary/$taarich/run_SUM_"$taarich".html
cat table >> /var/www/html/logs/RunSummary/$taarich/run_SUM_"$taarich".html
echo "
</table>
</body>
</html>" >> /var/www/html/logs/RunSummary/$taarich/run_SUM_"$taarich".html
\rm tmp table 


