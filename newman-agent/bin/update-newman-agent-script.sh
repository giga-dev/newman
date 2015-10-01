#!/bin/bash
#file should be in the following format: user@host on each line (one per line)
HOSTS_TO_DEPLOY_FILE=$1
AGENT_FOLDER="/home/xap/newman-agent"
echo "updating newman-agent.sh script to all agents"
for dest in $(<${HOSTS_TO_DEPLOY_FILE}); do
  sshpass -p 'password' scp newman-agent.sh ${dest}:${AGENT_FOLDER}
  sshpass -p 'password' ssh ${dest} 'cd /home/xap/newman-agent && chmod +x newman-agent.sh'
done
