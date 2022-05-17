![Version](https://img.shields.io/badge/version-1.0.38-green.svg)
[![CircleCI](https://circleci.com/gh/lumigo-io/java-tracer.svg?style=svg&circle-token=f2e3400e6e79bc31daeee1fc614ecc0a149b1905)](https://circleci.com/gh/lumigo-io/java-tracer)
[![codecov](https://codecov.io/gh/lumigo-io/java-tracer/branch/master/graph/badge.svg?token=D3IZ5hQwaQ)](https://codecov.io/gh/lumigo-io/java-tracer)

Supported Runtimes: Java 8, Java 11

# Building With Lumigo
## Maven
Include lumigo java tracer dependency, for [Maven](https://maven.apache.org) projects, use:
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
  <version>1.0.38</version>
</dependency>

<dependency>
  <groupId>io.lumigo</groupId>
  <artifactId>lumigo-agent</artifactId>
  <version>1.0.38</version>
</dependency>
```
Find the latest version here (the format of the version will be n.n.n):

# Wrapping your Lambda
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

## Configuration
There are 2 way to pass configuration properties

### Environment variables
    Adding `LUMIGO_TRACER_TOKEN` environment variables

### Static code initiation
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

## Java 11 Support
Add the environment variable `JAVA_TOOL_OPTIONS` to your Lambda functions and set it to
`-Djdk.attach.allowAttachSelf=true` in addition to the manual code mentioned above.

