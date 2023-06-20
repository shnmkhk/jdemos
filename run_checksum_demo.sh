#!/bin/sh
$JAVA_HOME/bin/java -cp ./target/classes:./target/dependency/picocli-4.1.4.jar:./target/dependency/picocli-jansi-graalvm-1.1.0.jar picocli.nativeimage.demo.CheckSum "$@"
