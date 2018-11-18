#!/usr/bin/env bash
pkill -9 java
docker rm -f $(docker ps -a -q)