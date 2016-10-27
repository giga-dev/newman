#!/bin/bash
if [ -z "$1" ] || [ -z "$2" ]
  then
    echo "Usage: ./deploy-newman.sh <AWS_ACCESS_ID> <AWS_SECRET_KEY>"
    exit 1
fi
read -r -p "Did you modify the password in group_vars/*? [y/N] " response
case $response in
    [yY][eE][sS]|[yY])
        export AWS_ACCESS_KEY_ID=$1
        export AWS_SECRET_ACCESS_KEY=$2
        ansible-playbook site.yml -i hosts -u xap
        ;;
    *)
        echo "Please modifiy the password in group_vars/* and re-run the script"
        ;;
esac
