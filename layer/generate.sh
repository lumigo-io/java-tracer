mkdir -p lumigo

cp ../target/java-tracer-1.0.47.jar lumigo/lumigo-tracer.jar
cp ../agent/target/lumigo-agent-1.0.47.jar lumigo/lumigo-agent.jar

zip -r lumigo-java-layer.zip lumigo

rm -rf lumigo