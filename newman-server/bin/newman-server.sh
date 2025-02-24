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

cat  << '_EOF' >> ../config/realm.properties
ester=ester, user
yuval=yuval, user
evgeny=evgeny, user
moran=moran, user
root=root, admin
support=support, user
mishel=mishel, user
sagiv=sagiv, user
yonatan=yonatan, user
michael=michael, user
irena=irena, user
tomer=tomer, user
noi=noi, user
anton=anton, user
oleksii=oleksii, user
alesia=alesia, user
olha=olha, user
michaelg=michaelg, user
sapir=sapir, user
shai=shai, user
davyd=davyd, user
esubotin=esubotin, user
inbal=inbal, user
tolik=tolik, user
rami=rami, user
ariel=ariel, user
chani=chani, user
krystyna=krystyna, user
user=user, user
_EOF


# System environment variables
# Mongo host address to connect to from newman server
# remote mongo db - xap-builder.gspaces.com:27017

export NEWMAN_SERVER_SPOTINST_TOKEN=${NEWMAN_SERVER_SPOTINST_TOKEN=""}
export NEWMAN_SERVER_SPOTINST_ACCOUNT_ID=${NEWMAN_SERVER_SPOTINST_ACCOUNT_ID=""}
# run newman server
# to debug, add java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \

# java --add-opens java.base/java.lang=ALL-UNNAMED \
#    --add-opens java.base/java.net=ALL-UNNAMED \
#    --add-opens java.base/java.nio=ALL-UNNAMED \
#    --add-opens java.base/java.util=ALL-UNNAMED \
#    --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
#    --add-opens java.base/java.time=ALL-UNNAMED \
java -Dproduction=true \
    -Dnewman.server.spotinst.token="${NEWMAN_SERVER_SPOTINST_TOKEN}" \
    -Dnewman.server.spotinst.accountId="${NEWMAN_SERVER_SPOTINST_ACCOUNT_ID}" \
    -Dnewman.mongo.db.host=${MONGODB_HOST} \
    -Dnewman.mongo.db.name=${MONGODB_DATABASE} \
    -Dnewman.mongo.db.username=${MONGODB_USERNAME} \
    -Dnewman.mongo.db.password=${MONGODB_PASSWORD} \
    -Dnewman.server.realm-config-path=../config/realm.properties \
    -Dnewman.keys-folder-path=../keys/server.keystore \
    -Dnewman.certificate=../certs/keystore.p12 \
    -Dnewman.server.web-folder-path=../web/vue -jar \
    ../target/newman-server-1.0.jar 2>&1 > /tmp/newman.log
