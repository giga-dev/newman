# Instructions for using Ansible to deploy Newman

## Prerequisites

* Install Ansible on the control machine [Docs](http://docs.ansible.com/ansible/intro_installation.html#installing-the-control-machine)
* Modify <NewmanSources>/ansible/group_vars/* with the correct credentials (will use pem files later on)

## Usage and Examples

* Test the whole deployment flow without changing the hosts by running `ansible-playbook site.yml -i hosts -u xap --check`
* Deploy/redeploy Newman by running `ansible-playbook site.yml -i hosts -u xap`
* Deploy/redeploy Newman without installing cron tabs by running `ansible-playbook site.yml --skip-tags "cron" -i hosts -u xap`
* Deploy/redeploy a single module (e.g only agents) by running: `ansible-playbook agents_deploy.yml -i hosts -u xap`
* Send remote commands to all agents (or some of them) e.g:
1. `ansible newmanDockerAgents -a "/bin/echo hello" -i hosts -u xap`
2. `ansible newmanDockerAgents -s -m shell -a "rm -rf /home/xap/xap-newman-agent/job-*" -i hosts -u xap`


## supervisord commends
* To connect to supervisor use 'supervisorctl -c /home/xap/newman-agent/supervisord.conf' (it is important to use specific .conf file)
* Logs of supervisor use - 'vi /tmp/supervisord.log' (*** /home/xap/newman-agent/supervisord_logs is directory for newman_agent logs ***)
* The .conf file of newman_agent appended to the end of /home/xap/newman-agent/supervisord.conf
