#!/bin/bash
#file should be in the following format: user@host on each line (one per line)
HOSTS_TO_DEPLOY_FILE=$1
AGENT_FOLDER="/home/xap/newman-agent"
echo "killing all old agents"
for dest in $(<${HOSTS_TO_DEPLOY_FILE}); do
  sshpass -p 'password' ssh ${dest} '/home/xap/newman-agent/stop-newman-agent.sh'
  sshpass -p 'password' ssh ${dest} 'pkill -9 java'
  sshpass -p 'password' ssh ${dest} 'docker rm -f $(docker ps -a -q)'
done
echo "deploying new agent jar"
for dest in $(<${HOSTS_TO_DEPLOY_FILE}); do
  sshpass -p 'password' scp ../target/newman-agent-1.0.jar ${dest}:${AGENT_FOLDER}
done
echo "starting new agents"
for dest in $(<${HOSTS_TO_DEPLOY_FILE}); do
  sshpass -p 'password' ssh ${dest} 'cd /home/xap/newman-agent && ./newman-agent.sh'
done
