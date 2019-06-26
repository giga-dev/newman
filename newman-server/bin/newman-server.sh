#!/usr/bin/env bash
set -x
DIRNAME=`cd $(dirname ${BASH_SOURCE[0]}) && pwd`
cd $DIRNAME
# load env if config file exists
source newman-server-env.sh

# make sure keys have been generated, if not create for development
if [ ! -d "../keys" ]; then
  ./keysgen.sh
fi

# make sure realm properties exist, if not create a for development
mkdir -p ../config
if [ ! -e "../config/realm.properties" ]; then
cat  << '_EOF' >> ../config/realm.properties
efrat,efrat, user
meshi=meshi, user
ester=ester, user
yuval=yuval, user
inbar=inbar, user
yohana=yohana, user
yechiel=yechiel, user
evgeny=evgeny, user
niv=niv, user
moran=moran, user
barak=barak, user
yael=yael, user
alons=alons, user
ayelet=ayelet, user
root=root, admin
support=support, user
_EOF

fi

# System environment variables
# Mongo host address to connect to from newman server
export NEWMAN_MONGO_DB_HOST=${NEWMAN_MONGO_DB_HOST="mongo-server"}

# Mongo db name to access in database
export NEWMAN_MONGO_DB_NAME=${NEWMAN_MONGO_DB_NAME="newman-db"}

export NEWMAN_SERVER_SPOTINST_TOKEN=${NEWMAN_SERVER_SPOTINST_TOKEN=""}
export NEWMAN_SERVER_SPOTINST_ACCOUNT_ID=${NEWMAN_SERVER_SPOTINST_ACCOUNT_ID=""}
# run newman server
java -Dproduction=true \
    -Dnewman.server.spotinst.token="${NEWMAN_SERVER_SPOTINST_TOKEN}" \
    -Dnewman.server.spotinst.accountId="${NEWMAN_SERVER_SPOTINST_ACCOUNT_ID}" \
    -Dnewman.mongo.db.host=${NEWMAN_MONGO_DB_HOST} \
    -Dnewman.mongo.db.name=${NEWMAN_MONGO_DB_NAME} \
    -Dnewman.server.realm-config-path=../config/realm.properties \
    -Dnewman.keys-folder-path=../keys/server.keystore \
    -Dnewman.server.web-folder-path=../web/elm -jar \
    ../target/newman-server-1.0.jar 2>&1 > /tmp/newman.log
