TS=5
echo [1/$TS] Compiling the source files
mvn clean compile
echo [2/$TS] Building native image
sdk install java 22.3.r19-grl
sdk use java 22.3.r19-grl
native-image -cp target/classes --static -o hw com.rabbit.examples.HelloWorld
echo [3/$TS] Preparing the UPX for compression
brew install upx-ucl -y
echo [4/$TS] Compressing the binary
/usr/bin/upx --lzma --best hw -o hw.upx
echo [5/$TS] Building the docker image
docker build . -t hw:upx
