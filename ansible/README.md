# Instructions for using Ansible to deploy Newman

## Prerequisites

* Linux machine
* [Docker](https://www.docker.com) installed
* Newman source code cloned locally

## Building the docker image

1. CD to `<Newman>/ansible/docker folder`
2. Execute `build.sh` script to build the docker container image

## Upgrading the system

* Execute `run.sh <pw1> <pw2>` script to upgrade the whole system (pw1 and pw2 are passwords for accessing the agents hosts)
* To skip some unwanted roles, run `export skipTags=...` before running `run.sh` script (when not specifying any skipTags the default skipped tags are `cron` and `local`)
* To upgrade a single module, run `export module=...` before running `run.sh` script, the available modules are: `all/agents/server/submitter/analytics` (when not specifying any module `all` is used)
* If a DNS resolution is needed to access the hosts from the `hosts` file, provide the ip of the DNS server that will be used by the docker container by running `export dns=...` before running `run.sh` script

## Examples of interaction with all newman agents

* CD to `<Newman>/ansible` folder
* Execute command to all agents: `ansible newmanDockerAgents -a "/bin/echo hello" -i hosts -u xap`
* Execute ansible `shell` module execution on all agents: `ansible newmanDockerAgents -s -m shell -a "rm -rf /home/xap/xap-newman-agent/job-*" -i hosts -u xap`
* Check the [ansible modules](http://docs.ansible.com/ansible/list_of_all_modules.html) list for for more information.
