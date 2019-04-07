REM System environment variables
REM username and password to connect to newman server
set NEWMAN_USERNAME=root
set NEWMAN_PASSWORD=root
REM newman server host address
set NEWMAN_SERVER_HOST=18.224.235.227
REM newman server port
set NEWMAN_SERVER_PORT=8443
REM newman agent home directory
set NEWMAN_AGENT_HOME=C:\automation\work
set NEWMAN_AGENT_CAPABILITIES="WINDOWS,DOTNET"
set NEWMAN_AGENT_WORKERS=3

set NEWMAN_AGENT_GROUPNAME=DotNet


start /B java -Dnewman.agent.workers=%NEWMAN_AGENT_WORKERS% -Dnewman.agent.groupName=%NEWMAN_AGENT_GROUPNAME% -Dnewman.agent.home=%NEWMAN_AGENT_HOME% -Dnewman.agent.server-host=%NEWMAN_SERVER_HOST% -Dnewman.agent.server-port=%NEWMAN_SERVER_PORT% -Dnewman.agent.server-rest-user=%NEWMAN_USERNAME% -Dnewman.agent.server-rest-pw=%NEWMAN_PASSWORD% -Dnewman.agent.capabilities=%NEWMAN_AGENT_CAPABILITIES% -jar ..\target\newman-agent-1.0.jar > agent_output.log
