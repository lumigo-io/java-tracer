# Java Tracer

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/lumigo-io/java-tracer/tree/master.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/lumigo-io/java-tracer/tree/master)
![Version](https://img.shields.io/badge/version-1.0.45-green.svg)
[![codecov](https://codecov.io/gh/lumigo-io/java-tracer/branch/master/graph/badge.svg?token=D3IZ5hQwaQ)](https://codecov.io/gh/lumigo-io/java-tracer)

Supported Runtimes: Java 8, Java 11, Java 17, Java 21

## Building With Lumigo
Include lumigo java tracer dependency

### Maven
For [Maven](https://maven.apache.org) projects, use:

```xml
<repositories>
    <repository>
        <id>lumigo</id>
        <url>https://raw.githubusercontent.com/lumigo-io/java-tracer/master/local-repository/</url>
    </repository>
</repositories>
```

```xml
<dependency>
  <groupId>io.lumigo</groupId>
  <artifactId>java-tracer</artifactId>
  <version>1.0.45</version>
</dependency>

<dependency>
  <groupId>io.lumigo</groupId>
  <artifactId>lumigo-agent</artifactId>
  <version>1.0.45</version>
</dependency>
```

### Gradle
For [Gradle](https://gradle.org) projects, use:

```groovy
repositories {
    maven {
        url 'https://raw.githubusercontent.com/lumigo-io/java-tracer/master/local-repository/'
    }
}
```

```groovy
dependencies {
    implementation 'io.lumigo:java-tracer:1.0.45'
    implementation 'io.lumigo:lumigo-agent:1.0.45'
}
```

Find the latest version here (the format of the version will be n.n.n):

## Wrapping your Lambda

* Wrap your lambda function by implementing a supplier which contains your code

    ```java
    class MyFunction implements RequestHandler<INPUT, OUTPUT> {

            @Override
            public OUTPUT handleRequest(INPUT event, Context context) {
                Supplier<OUTPUT> supplier = () -> {
                    //Your lambda code
                    //return <result of type OUTPUT>;
                };
                return LumigoRequestExecutor.execute(event, context, supplier);
            }
        }
    ```

* For handler return void use:

    ```java
    class MyFunction implements RequestHandler<INPUT, Void> {

            @Override
            public Void handleRequest(INPUT event, Context context) {
                Supplier<Void> supplier = () -> {
                    //Your lambda code
                    return null;
                };
                return LumigoRequestExecutor.execute(event, context, supplier);
            }
        }
    ```

### Configuration

There are 2 way to pass configuration properties

#### Environment variables

Adding `LUMIGO_TRACER_TOKEN` environment variables

#### Static code initiation

```java
class MyFunction implements RequestHandler<String, String> {

        static{
            LumigoConfiguration.builder().token("xxx").build().init();
        }

        @Override
        public String handleRequest(String event, Context context) {
            Supplier<String> supplier = () -> {
                //Your lambda code
                return "";
            };
            return LumigoRequestExecutor.execute(event, context, supplier);
        }
    }
```

### Support Java 11 and Above

Add the environment variable `JAVA_TOOL_OPTIONS` to your Lambda functions and set it to
`-Djdk.attach.allowAttachSelf=true` in addition to the manual code mentioned above.

### Supported Instrumentation Libraries

- Aws SDK V1
- Aws SDK V2
- Apache HTTP Client
- Apache Kafka

### Secret scrubbing

The tracer will automatically scrub values for keys matching the following regex patterns:
- `.*pass.*`
- `.*key.*`
- `.*secret.*`
- `.*credential.*`
- `.*passphrase.*`
- `SessionToken`
- `x-amz-security-token`
- `Signature`
- `Authorization`

from the payload, at any depth.
This behavior can be overridden by setting the `LUMIGO_SECRET_MASKING_REGEX` environment variable to a JSON array of regex patterns to match, e.g.: `[".+top@secret.+", ".pazzword.+"]`.
* Note - providing a bad regex pattern (e.g., invalid JSON string) will result in an error and fallback to the default patterns.