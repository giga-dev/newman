#!/usr/bin/env bash

echo "creating directory /home/ec2-user/newman-agent/supervisord_logs"
mkdir -p /home/ec2-user/newman-agent/supervisord_logs
echo "remove all files of supervisord_logs"
rm -f /home/ec2-user/newman-agent/supervisord_logs/*
echo "create file /home/ec2-user/newman-agent/supervisord_logs/err.log"
touch /home/ec2-user/newman-agent/supervisord_logs/err.log
echo "create file /home/ec2-user/newman-agent/supervisord_logs/out.log"
touch /home/ec2-user/newman-agent/supervisord_logs/out.log
echo "start supervisord and newman agent"
supervisord -c /home/ec2-user/newman-agent/supervisord.conf
