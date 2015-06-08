#!/bin/bash

# make sure keys have been generated, if not create for development
if [ ! -d "../keys" ]; then
  ./keysgen.sh
fi

# make sure realm properties exist, if not create a for development
mkdir -p ../config
if [ ! -a "../config/realm.properties" ]; then
   echo "root: root, admin" >> ../config/realm.properties
fi

# run newman server
java -Dnewman.server.realm-config-path=../config/realm.properties -Dnewman.keys-folder-path=../keys/server.keystore -Dnewman.server.web-folder-path=../web -jar ../target/newman-server-1.0.jar
