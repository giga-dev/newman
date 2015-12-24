#!/usr/bin/env bash
echo "creating directory /home/xap/newman-agent/supervisord_logs"
mkdir -p /home/xap/newman-agent/supervisord_logs

echo "remove all files of supervisord_logs"
rm -f /home/xap/newman-agent/supervisord_logs/*

echo "create file /home/xap/newman-agent/supervisord_logs/err.log"
touch /home/xap/newman-agent/supervisord_logs/err.log

echo "create file /home/xap/newman-agent/supervisord_logs/out.log"
touch /home/xap/newman-agent/supervisord_logs/out.log

echo "start supervisord and newman agent"
supervisord -c /home/xap/newman-agent/supervisord.conf
