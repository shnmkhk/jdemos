#!/bin/sh
mvn compile dependency:copy-dependencies
$JAVA_HOME/bin/java -cp ./target/classes:./target/dependency/jline-3.16.0.jar:./target/dependency/picocli-4.1.4.jar:./target/dependency/picocli-jansi-graalvm-1.1.0.jar com.rabbit.ml.MLMain "$@"
