#!/bin/bash

echo ""
echo "####################### netflow , http index of ES start delete #######################"
index_dt=$(date +"%Y%m%d" -d "-1day")
echo $index_dt

#delete netflow
netflow_index=netflow_$index_dt
index_cnt=`/usr/bin/curl -XHEAD -i http://es:9200/$netflow_index/$netflow_index|grep 200|wc -l`
echo "####################### $index_name index_cnt = $index_cnt ###index_cnt=1 exist, othersize unexist"
[ $index_cnt -eq 1 ]&&curl  -XDELETE es:9200/$netflow_index
#[ $index_cnt -eq 1 ]&& curl  -XDELETE es:9200/$netflow_index
echo "####################### $netflow_index delete complete."

#delete http
http_index=http_$index_dt
index_cnt=`/usr/bin/curl -XHEAD -i http://es:9200/$http_index/$http_index|grep 200|wc -l`
echo "####################### $index_name index_cnt = $index_cnt ###index_cnt=1 exist, othersize unexist"
[ $index_cnt -eq 1 ]&&curl  -XDELETE es:9200/$http_index
echo "####################### $http_index delete complete."


echo "####################### netflow , http index of ES end delete #######################"
echo ""

exit 0
