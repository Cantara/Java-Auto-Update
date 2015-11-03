@echo off

:: Download java if java folder is missing
if not exist java (
    call download-java.bat
)

bin\java-auto-update remove & bin\java-auto-update install & bin\java-auto-update start & sc failure java-auto-update reset= 60  actions= restart/10000/restart/10000/restart/10000
