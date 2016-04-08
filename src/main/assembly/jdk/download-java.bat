@ECHO OFF
 
::
:: Default properties. To override these, you can edit download-java-overrides.bat
::

:: Download URL for the "normal" 64-bit JDK
SET DOWNLOAD_URL="http://cdn.azul.com/zulu/bin/zulu8.13.0.5-jdk8.0.72-win_x64.zip"

:: Download URL for the 32-bit JDK that will be downloaded on Windows 2003 servers
SET DOWNLOAD_URL_32_BIT=

:: Root directory in the downloaded zip file.  Only relevant for the 64-bit JDK.
SET ZIP_ROOT_DIR="zulu8.13.0.5-jdk8.0.72-win_x64"

:: Name of the temporary zip file.
SET ZIP_FILE="java.zip"

:: Set this if using an http proxy. Specified as "host:port".
SET HTTP_PROXY=

:: Username and password for HTTP authentication. Only relevant for the 32-bit JDK.
SET HTTP_USER=
SET HTTP_PASSWORD=


CALL download-java-overrides.bat

:: Windows Server 2003 needs 32-bit Java
ver | findstr /i "5\.2\." > nul
IF %ERRORLEVEL% == 0 (
    GOTO use32bit
) ELSE (
    GOTO normal
)
 


:use32bit
echo Downloading Java from %DOWNLOAD_URL_32_BIT%

IF defined HTTP_PROXY (
    wget.exe --user=%HTTP_USER% --password=%HTTP_PASSWORD% -e use_proxy=yes -e http_proxy=%HTTP_PROXY% -O %ZIP_FILE% --content-disposition %DOWNLOAD_URL_32_BIT%
) ELSE (
    wget.exe --user=%HTTP_USER% --password=%HTTP_PASSWORD% -O %ZIP_FILE% --content-disposition %DOWNLOAD_URL_32_BIT%
)

echo Unzipping %ZIP_FILE%
unzip -q -o %ZIP_FILE% -d java
if exist java\jre1.8.0_40 (
    echo Copying java files to java\bin
    move java\jre1.8.0_40\*.* java\
    move java\jre1.8.0_40\bin java\bin
    move java\jre1.8.0_40\lib java\lib
    xcopy /E /O /Y java\bin\client\* java\bin\server\
)
 
GOTO finish
 
 
 
:normal
echo Downloading Java from %DOWNLOAD_URL%

IF defined HTTP_PROXY (
    wget.exe -e use_proxy=yes -e http_proxy=%HTTP_PROXY% -O %ZIP_FILE% %DOWNLOAD_URL%
) ELSE (
    wget.exe -O %ZIP_FILE% %DOWNLOAD_URL%
)
 
echo Unzipping %ZIP_FILE%
unzip.exe -q -o %ZIP_FILE%
move %ZIP_ROOT_DIR% java

GOTO finish
 
 
 
:finish

:: Copy windows ntlmauth dll
if exist java\bin (
    echo Copying ntlmauth.dll to java\bin
    copy ntlmauth.dll java\bin
)

if exist %ZIP_FILE% (
    del %ZIP_FILE%
)
