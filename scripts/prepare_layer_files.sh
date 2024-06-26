#!/usr/bin/env bash

mkdir -p lumigo-java

MVN_DEFAULT_FLAGS="-Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.http.retryHandler.count=10"

mvn "$MVN_DEFAULT_FLAGS" -Dmaven.test.skip=true -Dfindbugs.skip=true -DincludeShade=true clean package --quiet
mvn "$MVN_DEFAULT_FLAGS" -f agent/pom.xml clean package --quiet

cp target/java-tracer-1.0.47.jar lumigo-java/lumigo-tracer.jar
cp agent/target/lumigo-agent-1.0.47.jar lumigo-java/lumigo-agent.jar
