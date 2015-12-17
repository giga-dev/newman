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

#Generating ssh keys.
(cd /newman/newman-server/bin; ./keysgen.sh)

#Install UI dependencies.
echo "Installing UI dependencies."
#(cd /newman/newman-server/web; export CI=true; bower --allow-root install)
(cd /newman/newman-server/web;bower --allow-root install)

#Compile newman
(cd /newman; mvn install)

#Run ansible
cd /newman/ansible
sed -i "s/#host_key_checking = False/host_key_checking = False/g" /etc/ansible/ansible.cfg
ansible-playbook site.yml --skip-tags "cron,local" -i hosts -u xap

