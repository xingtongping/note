#!/bin/bash
bin/flume-ng agent -n agent -c conf -f conf/adsapi.properties > logs/flume-server.log  2>&1 &
