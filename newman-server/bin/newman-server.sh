#!/bin/bash

if [ ! -d "../keys" ]; then
  ./keysgen.sh
fi

java -Dnewman.server.realm-config-path=../config/realm.properties -Dnewman.keys-folder-path=../keys/server.keystore -Dnewman.server.web-folder-path=../web -jar ../target/newman-server-1.0.jar
