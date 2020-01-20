#!/bin/bash

syslog_template_isexit=`curl XGET -i http://es:9200/_template/syslog_template|grep 200|wc -l`
if [[ $syslog_template_isexit = '0' ]]; 
then
  echo "########## syslog_template is not create and  create template##########"
  curl -XPUT "http://es:9200/_template/syslog_template" -d '{
  "template": "syslog_*",
  "mappings": {
    "default": {
		"properties": {
				"recordid": {
						"type": "string",
						"index" : "not_analyzed" 
				},
				"reportip": {
						"type": "string",
						"index" : "not_analyzed" 
				},
				"ismatch": {
						"type": "long",
						"index" : "not_analyzed" 
				},
				"storagetime": {
						"type": "long",
						"index" : "not_analyzed" 
				},
				"recvtime": {
						"type": "long",
						"index" : "not_analyzed" 
				},
				"logcontent": {
						"type": "string",
						"index" : "not_analyzed"
				},
				"nodeid": {
						"type": "long",
						"index" : "not_analyzed" 
				},
				"reportapp": {
						"type": "string",
						"index" : "not_analyzed" 
				},
				"dublecount": {
						"type": "long",
						"index" : "not_analyzed" 
				}
		}
    }
  }
}'
echo "########## syslog_template create successful##########"
else
echo "########## syslog_template had exist##########"
fi
exit 0
