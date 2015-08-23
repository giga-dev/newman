#!/bin/bash

# load env if config file exists
source newman-analytics-env.sh

# NEWMAN_SERVER_HOST (default localhost)
# NEWMAN_SERVER_PORT (default 8443)
# NEWMAN_SERVER_REST_USER (default ****)
# NEWMAN_SERVER_REST_PASSWORD (default ****)

java -Dnewman.server.host=${NEWMAN_SERVER_HOST} -Dnewman.server.port=${NEWMAN_SERVER_PORT} -Dnewman.server.rest.user=${NEWMAN_REST_USERNAME} -Dnewman.server.rest.password=${NEWMAN_REST_PASSWORD} -jar ../target/newman-analytics-1.0.jar $@
