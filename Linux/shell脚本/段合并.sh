#!/bin/bash

#只是合并前一天的数据

echo ""
echo "####################### syslog , genlog, event index of ES start segment merge #######################"

#这里指定es的主机名为:es
es_host=es
#index name
yesterday_date=$(date +"%Y%m%d" -d "-1day")
index_syslog=syslog_$yesterday_date
index_genlog=genlog_$yesterday_date
index_event=event_$yesterday_date

index_array=(
$index_syslog 
$index_genlog 
$index_event
)

for index_name in "${index_array[@]}";do
    #is exist
    index_cnt=`curl -XHEAD -i http://$es_host:9200/$index_name/$index_name|grep 200|wc -l`
    echo "$index_name index_cnt = $index_cnt ####################################"
    [ $index_cnt -eq 0 ]&&continue
    
    #if segments <10 ,do nothing
    segments_cnt=`curl http://$es_host:9200/_cat/segments/$index_name|wc -l`
    echo "$index_name segments_cnt = $segments_cnt ####################################"
    [ $segments_cnt -le 10 ]&&continue
    #segments merge
    #echo "http://$es_host:9200/$index_name/_forcemerge?max_num_segments=1"
   	curl -XPOST http://$es_host:9200/$index_name/_forcemerge?max_num_segments=1
    
    #5min
    sleep 300
done


echo "####################### syslog , genlog, event index of ES end segment merge #######################"
echo ""
