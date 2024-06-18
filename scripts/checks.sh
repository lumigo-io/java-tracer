#!/usr/bin/env bash
set -eo pipefail

java -jar libs/google-java-format-1.7-all-deps.jar --set-exit-if-changed -i -a $(find . -type f -name "*.java" | grep ".*/src/.*java")
mvn -Djava.security.manager=allow -f agent/pom.xml clean package
mvn -Djava.security.manager=allow clean package