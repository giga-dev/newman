#!/bin/bash

cd ../

git pull

mvn clean package -DskipTests

