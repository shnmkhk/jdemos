#!/bin/sh
mvn -DbuildArgs=--no-server clean verify; ./target/jdemos
