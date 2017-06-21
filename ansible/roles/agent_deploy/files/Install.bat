Set PR_PATH=%CD%
SET PR_SERVICE_NAME=newman-agent-1.0
SET PR_JAR=newman-agent-1.0.jar
 
SET START_CLASS=com.gigaspaces.newman.NewmanAgent
SET START_METHOD=main
SET STOP_CLASS=java.lang.System
SET STOP_METHOD=serviceStop
rem ; separated values
SET STOP_PARAMS=0
rem ; separated values

SET JVM_OPTIONS1=-Dapp.home=%PR_PATH%       
Set JVM_OPTIONS2=-Dnewman.agent.workers=3
rem set NEWMAN_SERVER_HOST=192.168.11.141
Set JVM_OPTIONS3=-Dnewman.agent.home=C:\newman\work
Set JVM_OPTIONS4=-Dnewman.agent.server-host=xap-newman
Set JVM_OPTIONS5=-Dnewman.agent.server-port=8443 
Set JVM_OPTIONS6=-Dnewman.agent.server-rest-user=root
Set JVM_OPTIONS7=-Dnewman.agent.server-rest-pw=root
Set JVM_OPTIONS8=-Dnewman.agent.capabilities="WINDOWS,DOTNET"
 
prunsrv.exe //IS//%PR_SERVICE_NAME% --Install="%PR_PATH%\prunsrv.exe" --StartPath=C:\newman --Classpath=C:\newman\newman-agent-1.0.jar --Jvm=auto --Startup=auto --StartMode=jvm --StartClass=%START_CLASS% --StartMethod=%START_METHOD% --StopMode=jvm --StopClass=%STOP_CLASS% --StopMethod=%STOP_METHOD% ++StopParams=%STOP_PARAMS% --Classpath="%PR_PATH%\%PR_JAR%" --DisplayName="%PR_SERVICE_NAME%" ++JvmOptions=%JVM_OPTIONS1% ++JvmOptions=%JVM_OPTIONS2% ++JvmOptions=%JVM_OPTIONS3% ++JvmOptions=%JVM_OPTIONS4% ++JvmOptions=%JVM_OPTIONS5% ++JvmOptions=%JVM_OPTIONS6% ++JvmOptions=%JVM_OPTIONS7% ++JvmOptions=%JVM_OPTIONS8% --Jvm="C:\Program Files\Java\jdk1.8.0_60\jre\bin\server\jvm.dll" --StdOutput="C:\newman\agent_output.log"
sc failure %PR_SERVICE_NAME% reset= 60 actions= restart/3000
sc failureflag %PR_SERVICE_NAME% 1

