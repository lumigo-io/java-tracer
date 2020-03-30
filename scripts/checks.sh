#!/usr/bin/env bash
set -eo pipefail

export FOR_LUMIGO_TEST="2"
java -jar libs/google-java-format-1.7-all-deps.jar --set-exit-if-changed -i -a $(find . -type f -name "*.java" | grep ".*/src/.*java")
mvn -f agent/pom.xml clean package
mvn clean package