#!/bin/bash
read -r -p "Did you modify the passwords in group_vars/newmanDockerAgents and in group_vars/newmanInsightEdgeDockerAgents? [y/N] " response
case $response in
    [yY][eE][sS]|[yY])
        ansible-playbook agents_deploy.yml -i hosts -u xap
        ;;
    *)
        echo "Please modifiy the password in group_vars/newmanDockerAgents and in group_vars/newmanInsightEdgeDockerAgents and re-run the script"
        ;;
esac
