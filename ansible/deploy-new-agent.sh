#!/bin/bash
read -r -p "Did you modify the password in group_vars/newNewmanDockerAgents? [y/N] " response
case $response in
    [yY][eE][sS]|[yY])
        ansible-playbook new_agents_deploy.yml -i hosts -u xap
        ;;
    *)
        echo "Please modifiy the password in group_vars/newNewmanDockerAgents and re-run the script"
        ;;
esac
