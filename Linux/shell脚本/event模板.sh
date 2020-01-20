#!/bin/bash

event_template_isexit=`curl XGET -i http://es:9200/_template/event_template|grep 200|wc -l`
if [[ $event_template_isexit = '0' ]]; 
then
  echo "########## event_template is not create and  create template##########"
  curl -XPUT "http://es:9200/_template/event_template" -d '{
  "template": "event_*",
  "mappings": {
    "default": {
		"properties":{

			"referer": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"requestmethod": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"srclatitude": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"event_count": {
				"type": "long",
				"index" : "not_analyzed" 
			},
			"event_rule_name": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"filemd5": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"dstcity": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"dstprovince": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"srccountry": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"sourceport": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"dstlongitude": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"event_detail": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"attackmethod": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"cve": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"event_type": {
				"type": "long",
				"index" : "not_analyzed" 
			},
			"sourceip": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"filedir": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"host": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"srccity": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"victim": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"nodeid": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"is_inner": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"srcprovince": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"destip": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"event_rule_level": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"infoid": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"event_rule_id": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"attacktype": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"event_sub_type": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"affectedsystem": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"attacker": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"attackflag": {
				"type": "long",
				"index" : "not_analyzed" 
			},
			"event_rule_result_id": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"opentime": {
				"type": "long",
				"index" : "not_analyzed" 
			},
			"url": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"dstlatitude": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"srclongitude": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"storagetime": {
				"type": "long",
				"index" : "not_analyzed" 
			},
			"event_base_type": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"stage": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"victimtype": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"appid": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"proto": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"reportneip": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"logid": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"destport": {
				"type": "string",
				"index" : "not_analyzed" 
			},
			"dstcountry": {
				"type": "string",
				"index" : "not_analyzed" 
			}

		}
    }
  }
}'
echo "########## event_template create successful##########"
else
echo "########## event_template had exist##########"
fi
exit 0
