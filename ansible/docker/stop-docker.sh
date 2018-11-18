#!/usr/bin/env bash
docker stop $(docker ps -q --filter ancestor=newman/anisble)