#!/bin/sh
TS=4
echo [1/$TS] Compiling the source files
mvn clean compile
echo [2/$TS] Building native image
sdk install java 22.3.r19-grl
sdk use java 22.3.r19-grl
native-image -cp target/classes -o hw com.rabbit.examples.HelloWorld
echo [3/$TS] Preparing the UPX for compression
brew install upx
echo [4/$TS] Compressing the binary
/opt/homebrew/bin/upx --lzma --best hw -o hw.upx
# echo [5/$TS] Building the docker image
# docker build . -t hw:upx
