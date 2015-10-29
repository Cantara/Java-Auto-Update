@echo off

:: Remove very old agent if present
if exist C:\NMD\avbagent\pharmacy-agent\bin\pharmacy-agent (
	C:\NMD\avbagent\pharmacy-agent\bin\pharmacy-agent remove
	taskkill /im java.exe /F
	echo Removing very old Pharmacy Agent
)

:: If previous version of java-auto-update and agent is running remove them and remove directory
if exist c:\opt\java-auto-update\bin\java-auto-update (
	echo Removing old Java-Auto-Update
	c:\opt\java-auto-update\bin\java-auto-update remove
	taskkill /im java.exe /F

	echo Deleting c:\opt\java-auto-update
	RMDIR /S /Q c:\opt\java-auto-update
)