#!/usr/bin/env bash
DIRNAME=`cd $(dirname ${BASH_SOURCE[0]}) && pwd`
cd $DIRNAME
source env.sh
# System environment variables
# username and password to connect to newman server
NEWMAN_USERNAME=${NEWMAN_USERNAME=root}
NEWMAN_PASSWORD=${NEWMAN_PASSWORD=root}
# newman server host address
NEWMAN_SERVER_HOST=${NEWMAN_SERVER_HOST=xap-newman.gspaces.com}
# newman server port
NEWMAN_SERVER_PORT=${NEWMAN_SERVER_PORT=8443}
# newman agent home directory
NEWMAN_AGENT_HOME=${NEWMAN_AGENT_HOME=${HOME}/xap-newman-agent}
#newman agent capabilities
NEWMAN_AGENT_CAPABILITIES=${NEWMAN_AGENT_CAPABILITIES="DOCKER,LINUX,MVN"}

NEWMAN_AGENT_WORKERS=${NEWMAN_AGENT_WORKERS=3}

NEWMAN_AGENT_GROUPNAME=${NEWMAN_AGENT_GROUPNAME="Undefined"}

echo "starting agent, date is `date`"
echo "JAVA_HOME=${JAVA_HOME}"
echo "env -> " 
env
java -Dnewman.agent.workers=${NEWMAN_AGENT_WORKERS} -Dnewman.agent.groupName="${NEWMAN_AGENT_GROUPNAME}" -Dnewman.agent.home=${NEWMAN_AGENT_HOME} -Dnewman.agent.hostname=`hostname`  -Dnewman.agent.server-host=${NEWMAN_SERVER_HOST} -Dnewman.agent.server-port=${NEWMAN_SERVER_PORT} -Dnewman.agent.server-rest-user=${NEWMAN_USERNAME} -Dnewman.agent.server-rest-pw=${NEWMAN_PASSWORD} -Dnewman.agent.capabilities=${NEWMAN_AGENT_CAPABILITIES} -jar ../target/newman-agent-1.0.jar >> "nohup_agent1.out"
echo ""
