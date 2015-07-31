@echo off
set ZipFile=jdk.zip
set BaseDir=.
::wget.exe -e use_proxy=yes -e http_proxy=10.246.5.10:8080 -P . -O jre.zip --referer=http://www.azulsystems.com/products/zulu/downloads http://cdn.azulsystems.com/zulu/2015-04-8.7-bin/zulu1.8.0_45-8.7.0.5-win64.zip
wget.exe -P . -O %ZipFile%  --referer=http://www.azulsystems.com/products/zulu/downloads http://cdn.azulsystems.com/zulu/2015-04-8.7-bin/zulu1.8.0_45-8.7.0.5-win64.zip

unzip.exe -q -o %ZipFile% 
move zulu1.8.0_45-8.7.0.5-win64 java

:: Copy windows login dll
if exist %BaseDir%\java\bin (
    echo "Copy windows login dll %BaseDir%\java\bin"
    copy %BaseDir%\ntlmauth.dll %BaseDir%\java\bin
)

del %ZipFile%
echo "Copy windows login dll %BaseDir%\java\bin"
echo "zulu1.8.0_45-8.7.0.5-win64 JDK downloaded to java folder"