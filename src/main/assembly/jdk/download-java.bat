@echo off
set DOWNLOAD_URL="http://mvnrepo.cantara.no/service/local/artifact/maven/redirect?r=nmdreleases&g=net.java.jdk8&a=jre&v=LATEST&p=zip"
set TmpDir=.
set ZipFile=jre.zip
set BaseDir=.
wget.exe --user=altran --password=l1nkSys -e use_proxy=yes -e http_proxy=10.246.5.10:8080 -P "%TmpDir%" -O %ZipFile% --content-disposition %DOWNLOAD_URL%
::wget.exe --user=altran --password=l1nkSys -e use_proxy=no -e http_proxy=10.246.5.10:8080 -P "%TmpDir%" -O %ZipFile% --content-disposition %DOWNLOAD_URL%

unzip -q -o %TmpDir%\%ZipFile% -d %BaseDir%\java
if exist %BaseDir%\java\jre1.8.0_40 (
    echo "Copy java files %BaseDir%\java\bin"
    move %BaseDir%\java\jre1.8.0_40\*.* %BaseDir%\java\
	move %BaseDir%\java\jre1.8.0_40\bin %BaseDir%\java\bin
	move %BaseDir%\java\jre1.8.0_40\lib %BaseDir%\java\lib
	copy %BaseDir%\java\bin\client %BaseDir%\java\bin\server
)

:: Copy windows login dll
if exist %BaseDir%\java\bin (
    echo "Copy windows login dll %BaseDir%\java\bin"
    copy %BaseDir%\ntlmauth.dll %BaseDir%\java\bin
)
