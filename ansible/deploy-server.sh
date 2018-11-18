#!/usr/bin/env bash
read -r -p "Did you modify the password in group_vars/newmanServer? [y/N] " response
case $response in
    [yY][eE][sS]|[yY])
        sudo sed -i "s/#host_key_checking = False/host_key_checking = False/g" /etc/ansible/ansible.cfg
        ansible-playbook server_deploy.yml -i hosts -u xap
        ;;
    *)
        echo "Please modifiy the password in group_vars/newmanServer and re-run the script"
        ;;
esac
