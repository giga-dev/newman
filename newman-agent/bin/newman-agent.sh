#!/bin/bash
# root at newman-agent/bin

# if not set, defaults are used
NEWMAN_USERNAME=
NEWMAN_PASSWORD=
# default is localhost
NEWMAN_SERVER_HOST=
# default is 8443
NEWMAN_SERVER_PORT=

java -jar ../target/newman-agent-1.0.jar -Dnewman.agent.home=../ -Dnewman.agent.hostname=`hostname`  -Dnewman.agent.server-host=${NEWMAN_SERVER_HOST} -Dnewman.agent.server-port=${NEWMAN_SERVER_PORT} -Dnewman.agent.server-rest-user=${NEWMAN_USERNAME} -Dnewman.agent.server-rest-pw=${NEWMAN_PASSWORD}

#!/bin/bash
# root at newman-agent/bin

NEWMAN_USERNAME=
NEWMAN_PASSWORD=

NEWMAN_SERVER_HOST=
NEWMAN_SERVER_PORT=

java -jar ../target/newman-agent-1.0.jar -Dnewman.agent.home=../ -Dnewman.agent.hostname=`hostname`  -Dnewman.agent.server-host=${NEWMAN_SERVER_HOST} -Dnewman.agent.server-port=${NEWMAN_SERVER_PORT} -Dnewman.agent.server-rest-user=${NEWMAN_USERNAME} -Dnewman.agent.server-rest-pw=${NEWMAN_PASSWORD}