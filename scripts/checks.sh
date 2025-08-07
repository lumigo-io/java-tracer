#!/usr/bin/env bash
set -eo pipefail

echo "Checking for formatting issues..."
if ! java -jar libs/google-java-format-1.7-all-deps.jar --set-exit-if-changed -i -a $(find . -type f -name "*.java" | grep ".*/src/.*java"); then
    echo "some files were formatted, exiting"
    exit 1
fi

mvn -Djava.security.manager=allow -f agent/pom.xml clean package
mvn -Djava.security.manager=allow clean package