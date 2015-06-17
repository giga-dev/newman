#!/bin/bash
# root at newman-agent/bin

# System environment variables
# username and password to connect to newman server
#NEWMAN_USERNAME=
#NEWMAN_PASSWORD=

# newman server host address
#NEWMAN_SERVER_HOST=localhost

# newman server port
#NEWMAN_SERVER_PORT=8443

# newman agent home directory
#NEMAN_AGENT_HOME=${USER}/newman-agent

# newman agent workers, if not set default to 5
if [ -z "${JAVA_HOME}" ]; then
   NEWMAN_AGENT_WORKERS=5
fi

java -Dnewman.agent.workers=${NEWMAN_AGENT_WORKERS} -Dnewman.agent.home=${NEMAN_AGENT_HOME} -Dnewman.agent.hostname=`hostname`  -Dnewman.agent.server-host=${NEWMAN_SERVER_HOST} -Dnewman.agent.server-port=${NEWMAN_SERVER_PORT} -Dnewman.agent.server-rest-user=${NEWMAN_USERNAME} -Dnewman.agent.server-rest-pw=${NEWMAN_PASSWORD} -jar ../target/newman-agent-1.0.jar
