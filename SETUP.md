# Java Tracer - Setup Guide

This guide provides step-by-step instructions for setting up the development environment for the Java Tracer project.

## Prerequisites

This project requires **Java 8** to build and run tests successfully. The project was designed for Java 8 and has compatibility issues with newer Java versions due to:

- JaCoCo version compatibility (requires Java 8)
- Mockito/ByteBuddy compatibility
- Class file version requirements

## Setup Instructions

### 1. Install SDKMAN! (if not already installed)

SDKMAN! is a tool for managing parallel versions of multiple SDKs on Unix-based systems.

```bash
# Install SDKMAN!
curl -s "https://get.sdkman.io" | bash

# Reload shell configuration
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

### 2. Install Java 8

```bash
# List available Java 8 versions
sdk list java | grep -E "(8|1.8)"

# Install Java 8 (Amazon Corretto recommended)
sdk install java 8.0.462-amzn

# Set as default (optional)
sdk default java 8.0.462-amzn
```

### 3. Configure Environment for This Project

**Important:** You must set the Java environment variables before running Maven commands.

```bash
# Set Java 8 environment variables
export JAVA_HOME=$(sdk home java 8.0.462-amzn)
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java version
java -version
# Should show: openjdk version "1.8.0_462"

# Verify Maven is using Java 8
mvn -version
# Should show: Java version: 1.8.0_462
```

### 4. Build and Test

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run full build with all checks
./scripts/checks.sh
```

## Project Structure

```
java-tracer/
├── src/main/java/io/lumigo/     # Main source code
├── src/test/java/io/lumigo/     # Test source code
├── agent/                       # Java agent module
├── scripts/                     # Build and deployment scripts
└── findbugs/                   # FindBugs configuration
```

## Key Features

- **AWS SDK Support**: v1 and v2 instrumentation
- **HTTP Client Instrumentation**: Apache HTTP Client, AWS HTTP Client
- **Event Parsing**: API Gateway, SNS, SQS, DynamoDB, Kinesis
- **Secret Scrubbing**: Automatic sensitive data masking
- **Span Management**: Distributed tracing with spans

## Build Commands

```bash
# Set Java 8 environment first
export JAVA_HOME=$(sdk home java 8.0.462-amzn)
export PATH=$JAVA_HOME/bin:$PATH

# Basic build
mvn clean compile

# Run tests
mvn test

# Full build with checks
mvn clean package

# Build agent module
mvn -f agent/pom.xml clean package

# Run with FindBugs disabled
mvn compile -Dfindbugs.skip=true

# Run with JaCoCo disabled
mvn test -Djacoco.skip=true
```

## Troubleshooting

### Issue: "Unsupported class file major version 67"
**Cause**: Maven is using a newer Java version internally
**Solution**: Ensure JAVA_HOME is set to Java 8 before running Maven

```bash
export JAVA_HOME=$(sdk home java 8.0.462-amzn)
export PATH=$JAVA_HOME/bin:$PATH
mvn -version  # Should show Java 1.8.x
```

### Issue: Lombok compilation errors
**Cause**: Annotation processor not configured
**Solution**: The project includes proper Lombok configuration in pom.xml

### Issue: FindBugs warnings
**Cause**: Static analysis warnings
**Solution**: Warnings are excluded in findbugs/findbugs-exclude.xml

### Issue: Test failures with newer Java versions
**Cause**: Compatibility issues with Java 9+ features
**Solution**: Use Java 8 as specified in setup instructions

## Development

### Adding Dependencies

When adding new dependencies, ensure they are compatible with Java 8:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>library</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Running Specific Tests

```bash
# Run a specific test class
mvn test -Dtest=SpansContainerTest

# Run a specific test method
mvn test -Dtest=SpansContainerTest#testMethodName
```

## CI/CD

The project includes several scripts for CI/CD:

- `scripts/checks.sh` - Full build with all checks
- `scripts/deploy.sh` - Deployment script
- `scripts/ci_deploy.sh` - CI deployment script

## Quick Start Script

You can create a quick start script to set up the environment:

```bash
# Create setup script
cat > setup-env.sh << 'EOF'
#!/bin/bash
export JAVA_HOME=$(sdk home java 8.0.462-amzn)
export PATH=$JAVA_HOME/bin:$PATH
echo "Java environment set up:"
java -version
mvn -version
EOF

# Make it executable
chmod +x setup-env.sh

# Use it
source setup-env.sh
```

## Verification Checklist

Before starting development, ensure:

- [ ] Java 8 is installed via SDKMAN!
- [ ] JAVA_HOME points to Java 8
- [ ] `java -version` shows 1.8.x
- [ ] `mvn -version` shows Java 1.8.x
- [ ] `mvn clean compile` succeeds
- [ ] `mvn test` runs all tests successfully

## Contributing

1. Ensure you're using Java 8
2. Set up the environment as described above
3. Run tests before submitting changes
4. Follow the existing code style and patterns