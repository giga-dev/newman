#!/bin/bash
# root at newman-agent/bin

# if not set, defaults are used
NEWMAN_USERNAME=
NEWMAN_PASSWORD=
# default is localhost
NEWMAN_SERVER_HOST=
# default is 8443
NEWMAN_SERVER_PORT=
# db name default is account user name who started newman server
if [ -z "${NEWMAN_DB_NAME}" ]; then
    NEWMAN_DB_NAME=${USER}
fi

java -jar ../target/newman-agent-1.0.jar -Dnewman.mongo.db.name=${NEWMAN_DB_NAME} -Dnewman.agent.home=../ -Dnewman.agent.hostname=`hostname`  -Dnewman.agent.server-host=${NEWMAN_SERVER_HOST} -Dnewman.agent.server-port=${NEWMAN_SERVER_PORT} -Dnewman.agent.server-rest-user=${NEWMAN_USERNAME} -Dnewman.agent.server-rest-pw=${NEWMAN_PASSWORD}
