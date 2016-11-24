#!/bin/bash

export AWS_ACCESS_KEY_ID="<enter here your AWS_ACCESS_KEY_ID>"
export AWS_SECRET_ACCESS_KEY="<enter here your AWS_SECRET_ACCESS_KEY>"
export PEM_LOCATION="<enter here full path to your pem key including the name of the file >"

export AWS_DEFAULT_REGION="eu-central-1b"

read -r -p "Did you modify the password in group_vars/newNewmanDockerAgents? [y/N] " response
case $response in
    [yY][eE][sS]|[yY])
        sudo sed -i "s/#host_key_checking = False/host_key_checking = False/g" /etc/ansible/ansible.cfg
        ansible-playbook new_agents_deployTEST.yml -i hosts -u xap --private-key ${PEM_LOCATION}
        ;;
    *)
        echo "Please modifiy the password in group_vars/newNewmanDockerAgents and re-run the script"
        ;;
esac
