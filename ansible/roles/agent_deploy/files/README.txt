The steps you should do in order to run our newman-agent-1.0.jar as a windows service. 
My workspace was C:\newman, newman-agent-1.0.jar sould be there.
* I'v worked in Remmina, RDP 192.168.10.7

1. Download "commons-daemon-1.0.15-bin-windows.zip" from this link
http://www.apache.org/dist/commons/daemon/binaries/windows/

2. After extracting the zip, copy prunmgr.exe from the main folder and prunsrv.exe from amd64 folder to your workspace, C:\newman.

3. Run the Install.bat file from command line, as Administrator.
 
Now you have a window service ready with the configorations we need, including recovery options.
The next stage is to change the name of prunmgr.exe file to newman-agent-1.0.exe. Double click the file and click start. 
This should be done after installation and is planned to be done via ansible.  