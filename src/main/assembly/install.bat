@echo off

:: Remove very old agent if present
if exist C:\NMD\avbagent\pharmacy-agent\bin\pharmacy-agent (
	C:\NMD\avbagent\pharmacy-agent\bin\pharmacy-agent remove
	taskkill /im java.exe /F
	echo Removing very old Pharmacy Agent
)

:: If previous version of java-auto-update and agent is running remove them
if exist bin\java-auto-update (
	bin\java-auto-update remove
	taskkill /im java.exe /F
	echo Removing old Java-Auto-Update
)


:: Download java if java folder is missing
if not exist java (
    call download-java.bat
)

bin\java-auto-update remove & bin\java-auto-update install & bin\java-auto-update start & sc failure java-auto-update reset= 3000  actions= restart/30000/restart/90000/restart/300000
