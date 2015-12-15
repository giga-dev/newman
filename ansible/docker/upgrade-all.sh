#!/usr/bin/env bash


cd /newman/ansible/docker
#echo "upgrade-all $pass1 $pass2"
./subs-password.sh $pass1 /newman/ansible/group_vars/newNewmanDockerAgents
./subs-password.sh $pass1 /newman/ansible/group_vars/newmanAnalytics
./subs-password.sh $pass1 /newman/ansible/group_vars/newmanDockerAgents
./subs-password.sh $pass1 /newman/ansible/group_vars/newmanServer
./subs-password.sh $pass1 /newman/ansible/group_vars/newmanSubmitter
./subs-password.sh $pass1 /newman/ansible/group_vars/newmanWindowsAgents

./subs-password.sh $pass2 /newman/ansible/group_vars/newmanTarzanServers

cd /newman/ansible
ansible-playbook site.yml --skip-tags "cron" -i hosts -u xap

