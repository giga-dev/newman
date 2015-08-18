#!/bin/bash

# load env if config file exists
source newman-reporter-env.sh

# NEWMAN_SERVER_HOST (default localhost)
# NEWMAN_SERVER_PORT (default 8443)
# NEWMAN_SERVER_REST_USER (default ****)
# NEWMAN_SERVER_REST_PASSWORD (default ****)
# NEWMAN_MAIL_USER (default newman@...)
# NEWMAN_MAIL_PASSWORD (required)
# NEWMAN_MAIL_RECIPIENTS (default rnd_xap@...)
# NEWMAN_MAIL_HTML_TEMPLATE_PATH (default current run dir)
# NEWMAN_MAIL_PREVIOUS_BUILD_ID (default null)

java -Dnewman.server.host=${NEWMAN_SERVER_HOST} -Dnewman.server.port=${NEWMAN_SERVER_PORT} -Dnewman.server.rest.user=${NEWMAN_REST_USERNAME} -Dnewman.server.rest.password=${NEWMAN_REST_PASSWORD} -Dnewman.mail.user=${NEWMAN_MAIL_USER} -Dnewman.mail.password=${NEWMAN_MAIL_PASSWORD} -Dnewman.mail.html.template.path=${NEWMAN_MAIL_HTML_TEMPLATE_PATH} -Dnewman.mail.previous.build.id=${NEWMAN_MAIL_PREVIOUS_BUILD_ID} -jar ../target/newman-reporter-1.0.jar
