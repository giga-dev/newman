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

java -jar ../target/newman-agent-1.0.jar -Dnewman.agent.home=../ -Dnewman.agent.hostname=`hostname`  -Dnewman.agent.server-host=${NEWMAN_SERVER_HOST} -Dnewman.agent.server-port=${NEWMAN_SERVER_PORT} -Dnewman.agent.server-rest-user=${NEWMAN_USERNAME} -Dnewman.agent.server-rest-pw=${NEWMAN_PASSWORD}
