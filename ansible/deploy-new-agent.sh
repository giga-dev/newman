#!/bin/bash
echo "*********************************************"
echo "*********************************************"
echo "Don't forget to modify the password in group_vars/newNewmanDockerAgents"
echo "*********************************************"
echo "*********************************************"
ansible-playbook new_agents_deploy.yml -i hosts -u xap