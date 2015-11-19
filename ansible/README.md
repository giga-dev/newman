# Instructions for using Ansible to deploy Newman

## Prerequisites

* Install Ansible on the control machine [Docs](http://docs.ansible.com/ansible/intro_installation.html#installing-the-control-machine)
* Modify <NewmanSources>/ansible/group_vars/* with the correct credentials (will use pem files later on)

## Usage and Examples

* Deploy/redeploy Newman by running `ansible-playbook site.yml -i hosts -u xap`
* Deploy/redeploy Newman without installing cron tabs by running `ansible-playbook site.yml --skip-tags "cron" -i hosts -u xap`
* Deploy/redeploy a single module (e.g only agents) by running: `ansible-playbook agents_deploy.yml -i hosts -u xap`
* Send remote commands to all agents (or some of them) e.g: `ansible newmanDockerAgents -a "/bin/echo hello" -i hosts -u xap`