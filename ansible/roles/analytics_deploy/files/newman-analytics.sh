#!/bin/bash

export NEWMAN_SERVER_HOST=xap-newman
export NEWMAN_SERVER_PORT=8443
export NEWMAN_SERVER_REST_USER=root
export NEWMAN_SERVER_REST_PASSWORD=root

java -Dnewman.server.host=${NEWMAN_SERVER_HOST} -Dnewman.server.port=${NEWMAN_SERVER_PORT} -Dnewman.server.rest.user=${NEWMAN_SERVER_REST_USER} -Dnewman.server.rest.password=${NEWMAN_SERVER_REST_PASSWORD} -jar ../newman-analytics-1.0.jar $@
