#!/bin/bash
./keysgen.sh
mvn install && (cd example; mvn install)