#!/usr/bin/env bash

mkdir -p lumigo

mvn -Dfindbugs.skip=true -DskipTests=true -DincludeShade=true clean package
pushd agent
  mvn -Dfindbugs.skip=true -DskipTests=true clean package
popd

cp target/java-tracer-1.0.47.jar lumigo/lumigo-tracer.jar
cp agent/target/lumigo-agent-1.0.47.jar lumigo/lumigo-agent.jar

zip -r lumigo-java-layer.zip lumigo

rm -rf lumigo