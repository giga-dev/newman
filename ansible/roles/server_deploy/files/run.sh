#!/usr/bin/env bash
#/bin/bash
nohup java -Dnewman.mongo.db.name=newman-db -Dnewman.server.realm-config-path=/home/xap/newman-server/config/realm.properties -Dnewman.server.address=192.168.50.66 -Dnewman.server.enabledBuildCache=false -jar newman-server-1.0.jar &> server_nohup.log &