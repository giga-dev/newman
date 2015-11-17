Instructions for using Ansible to interact with newman agents:

Install Ansible on the client machine:
 * Ubuntu: 1. sudo apt-get install software-properties-common
           2. sudo apt-add-repository ppa:ansible/ansible
           3. sudo apt-get update
           4. sudo apt-get install ansible
 * For the rest of the distros check http://docs.ansible.com/ansible/intro_installation.html#installing-the-control-machine

Ping all agents and see their operating system details:
 * Ping: cd to <NewmanSources>/newman-agent/ansible and execute: `ansible newmanDockerAgents -m ping -u xap -i hosts`
 * See OS details: cd to <NewmanSources>/newman-agent/ansible and execute: `ansible newmanDockerAgents -m setup -u xap -i hosts`

Inject cron jobs to all newman agents:
 * CD to <NewmanSources>/newman-agent/ansible and execute: `ansible-playbook agents-crontabs.yml -i hosts -u xap`

Update agents' jar & redeploy:
 * CD to <NewmanSources>/newman-agent/ansible and execute: `ansible-playbook agents-redeploy.yml -i hosts -u xap`
 * Note: you can pass custom maven_repo location where the newman artifacts are stored or customize the local mvn command by running the following command:
 `ansible-playbook --extra-vars '{"maven_repo":"<local_m2_repo>","mvn_cmd":"<local_mvn_cmd>"}' agents-redeploy.yml -i hosts -u xap`
