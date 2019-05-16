#!/usr/bin/env bash
set -eo pipefail

java -jar libs/google-java-format-1.7-all-deps.jar --set-exit-if-changed -i -a $(find . -type f -name "*.java" | grep ".*/src/.*java")
mvn clean package