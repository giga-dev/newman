[![Build Status](https://travis-ci.org/giga-dev/newman.svg?branch=master)](https://travis-ci.org/giga-dev/newman) 


# Newman Scripts

All scripts refer to HOSTS_FILE with a generic name newman-hosts which is a lisy of all newman agents hosts,
note that if you want to run on all agents you should change newman-hosts.all to newman-hosts,
similarly:

* newman-hosts.intel refer only to agents that run on the intel machines
* newman-hosts.imc-srv01 refer only to agents that run on the imc-srv01
* newman-hosts.lab refer only to agents that run in the lab

## New Host

* _update_etc_hosts.sh_: copies newman-scripts/new_host/etc_hosts/hosts to the /etc/hosts file in the agent machine
* _update_static_ip.sh_: changes hosts static ip according to the mapping in newman-scripts/new_host/static_ip/newman-hosts-mapping
* _update_host_name.sh_: changes hosts host-name according to the mapping in newman-scripts/new_host/update_hostname/newman-hosts-mapping
* _install_java.sh_: installs java to specified kpath (experimental)

## Update Agents

* _update_agent.sh_: updates newman-agent-1.0.jar on all agents
* _update_agent_script.sh_: updates newman-agent.sh on all agents

both scripts call killall -9 java after the update to trigger restart nby supervisor

**NOTE:**

 intel agents need to have NEWMAN_AGENT_CAPABILITIES="DOCKER,LINUX" in the newman-agent.sh script,

 the lab agents need to have NEWMAN_AGENT_CAPABILITIES="DOCKER,LINUX,MVN".

## Utils
 * _check_hosts_ssh.sh_: tries to establish ssh connection to every host and report ok / failure
 * _clearDockerImages.sh_: deletes the docker images cached in the agents to free space
 * _killJavaAndSupervisor_: run kill -9 java and kill supervisor on all hosts
 * _run_command.sh_: takes the command you wish to run as an argument and runs it on all hosts
 
### Examples
 ##### Newman Agents restarting
 ###### For restarting all newman agents following actions should be done:
    1. Create newman-hosts file, copy there content of both files: newman-hosts.imc-srv01 and newman-hosts.imc-srv02
    2. Go to Newman web, select Manage Newman, press on "Suspend"
    3. Run follwoing script with command as a parameter: ./run_command.sh "killall java"  
    4. Wait untill all agents appear in Newman web 
    5. Go to Newman web, select Manage Newman, press on "Unuspend"