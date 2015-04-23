#!/bin/bash
(cd newman-server/web; bower update)
./keysgen.sh
mvn clean install
