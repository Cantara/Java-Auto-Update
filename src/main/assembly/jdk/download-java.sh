#!/bin/bash
ZipFile=jdk.zip
#-e use_proxy=yes -e http_proxy=10.246.5.10:8080
wget -P . -O $ZipFile  --referer=http://www.azulsystems.com/products/zulu/downloads http://cdn.azulsystems.com/zulu/2015-04-8.7-bin/zulu1.8.0_45-8.7.0.5-x86lx64.zip

unzip -q -o $ZipFile
mv zulu1.8.0_45-8.7.0.5-x86lx64 java
rm $ZipFile
echo 'zulu1.8.0_45-8.7.0.5-x86lx64 JDK downloaded to java folder'