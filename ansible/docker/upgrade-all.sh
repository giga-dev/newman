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

if [ "$module" == "all" ]
then
  echo "deploying whole environment"
  ansible-playbook site.yml --skip-tags "${skipTags}" -i hosts -u xap
elif [ "$module" == "agents" ]
then
    if [ -z "$cmdToDo" ]
    then
    	echo "deploying only agents"
    	ansible-playbook agents_deploy.yml --skip-tags "${skipTags}" -i hosts -u xap
    else
    	echo "executing $cmdToDo on $module"
    	ansible newmanDockerAgents -s -m shell -a "$cmdToDo" -i hosts -u xap
    fi
elif [ "$module" == "server" ]
then
    if [ -z "$cmdToDo" ]
    then
        echo "deploying only server"
        ansible-playbook server_deploy.yml --skip-tags "${skipTags}" -i hosts -u xap
    else
        echo "executing $cmdToDo on $module"
        ansible newmanServer -m shell -a "$cmdToDo" -i hosts -u xap
    fi
elif [ "$module" == "submitter" ]
then
    if [ -z "$cmdToDo" ]
    then
        echo "deploying only submitter"
        ansible-playbook submitter_deploy.yml --skip-tags "${skipTags}" -i hosts -u xap
    else
        echo "executing $cmdToDo on $module"
        ansible newmanSubmitter -m shell -a "$cmdToDo" -i hosts -u xap
    fi
elif [ "$module" == "analytics" ]
then
    if [ -z "$cmdToDo" ]
    then
        echo "deploying only analytics"
        ansible-playbook analytics_deploy.yml --skip-tags "${skipTags}" -i hosts -u xap
    else
        echo "executing $cmdToDo on $module"
        ansible newmanAnalytics -m shell -a "$cmdToDo" -i hosts -u xap
    fi
else
  echo "invalid module option, please choose module from the following set: {all/agents/server/submitter/analytics}"
  exit 1
fi

