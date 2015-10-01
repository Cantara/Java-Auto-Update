@echo off

:: Download java if java folder is missing
if not exist java (
    call C:\opt\java-auto-update\download-java.bat
)

bin\java-auto-update remove & bin\java-auto-update install & bin\java-auto-update start