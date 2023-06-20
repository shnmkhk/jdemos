#!/bin/sh
mvn -DbuildArgs=--no-server clean verify; ./target/jdemos -Dbanner.properties.file=./src/main/resources/banner.properties
