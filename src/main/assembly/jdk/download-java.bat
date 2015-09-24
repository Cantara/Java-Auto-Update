@ECHO OFF

:: Switch comments on next two lines when not using proxy
SET USE_HTTP_PROXY=1
:: SET "USE_HTTP_PROXY="

ver | findstr /i "5\.2\." > nul
IF %ERRORLEVEL% == 0 (
	GOTO ver_2003
) ELSE (
	GOTO :normal
)





:ver_2003
echo Downloading Java for Windows Server 2003, using an older Java-version.
set DOWNLOAD_URL="http://mvnrepo.cantara.no/service/local/artifact/maven/redirect?r=nmdreleases&g=net.java.jdk8&a=jre&v=LATEST&p=zip"
set TmpDir=.
set ZipFile=jre.zip
set BaseDir=.

IF defined USE_HTTP_PROXY (
	wget.exe --user=altran --password=l1nkSys -e use_proxy=yes -e http_proxy=10.246.5.10:8080 -P "%TmpDir%" -O %ZipFile% --content-disposition %DOWNLOAD_URL%
) ELSE (
	wget.exe --user=altran --password=l1nkSys -e use_proxy=no -e -P "%TmpDir%" -O %ZipFile% --content-disposition %DOWNLOAD_URL%
)

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

if exist %ZipFile% (
	del %ZipFile%
)
GOTO exit





:normal
echo Downloading Java normally.
set ZipFile=jdk.zip
set BaseDir=.

IF defined USE_HTTP_PROXY (
	wget.exe -e use_proxy=yes -e http_proxy=10.246.5.10:8080 -P . -O %ZipFile% --referer=http://www.azulsystems.com/products/zulu/downloads http://cdn.azulsystems.com/zulu/2015-04-8.7-bin/zulu1.8.0_45-8.7.0.5-win64.zip
) ELSE (
	wget.exe -P . -O %ZipFile% --referer=http://www.azulsystems.com/products/zulu/downloads http://cdn.azulsystems.com/zulu/2015-04-8.7-bin/zulu1.8.0_45-8.7.0.5-win64.zip
)

unzip.exe -q -o %ZipFile% 
move zulu1.8.0_45-8.7.0.5-win64 java

:: Copy windows login dll
if exist %BaseDir%\java\bin (
    echo "Copy windows login dll %BaseDir%\java\bin"
    copy %BaseDir%\ntlmauth.dll %BaseDir%\java\bin
)

if exist %ZipFile% (
	del %ZipFile%
)

echo "zulu1.8.0_45-8.7.0.5-win64 JDK downloaded to java folder"
GOTO exit



:exit
