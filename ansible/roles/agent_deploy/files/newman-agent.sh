#!/bin/bash
# System environment variables
# username and password to connect to newman server
NEWMAN_USERNAME=root
NEWMAN_PASSWORD=root
# newman server host address
NEWMAN_SERVER_HOST=xap-newman
# newman server port
NEWMAN_SERVER_PORT=8443
# newman agent home directory
NEMAN_AGENT_HOME=/home/xap/xap-newman-agent
#newman agent capabilities
NEWMAN_AGENT_CAPABILITIES="DOCKER,LINUX"
# newman agent workers, if not set default to 1
if [ -z "${NEWMAN_AGENT_WORKERS}" ]; then
   NEWMAN_AGENT_WORKERS=3
fi
echo "starting agent, date is `date`"
echo "JAVA_HOME=${JAVA_HOME}"
echo "env: "
env
java -Dnewman.agent.workers=${NEWMAN_AGENT_WORKERS} -Dnewman.agent.home=${NEMAN_AGENT_HOME} -Dnewman.agent.hostname=`hostname`  -Dnewman.agent.server-host=${NEWMAN_SERVER_HOST} -Dnewman.agent.server-port=${NEWMAN_SERVER_PORT} -Dnewman.agent.server-rest-user=${NEWMAN_USERNAME} -Dnewman.agent.server-rest-pw=${NEWMAN_PASSWORD} -Dnewman.agent.capabilities=${NEWMAN_AGENT_CAPABILITIES} -jar newman-agent-1.0.jar > "nohup_agent1.out"
echo ""