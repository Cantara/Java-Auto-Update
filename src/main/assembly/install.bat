@echo off

:: Download java if java folder is missing
if not exist java (
    call C:\opt\java-auto-update\download-java.bat
)

bin\java-auto-update remove & bin\java-auto-update install & bin\java-auto-update start & sc failure java-auto-update reset= 500000  actions= restart/30000/restart/90000/restart/300000