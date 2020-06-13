![Version](https://img.shields.io/badge/version-1.0.31-green.svg)
[![CircleCI](https://circleci.com/gh/lumigo-io/java-tracer.svg?style=svg&circle-token=f2e3400e6e79bc31daeee1fc614ecc0a149b1905)](https://circleci.com/gh/lumigo-io/java-tracer)
[![codecov](https://codecov.io/gh/lumigo-io/java-tracer/branch/master/graph/badge.svg?token=D3IZ5hQwaQ)](https://codecov.io/gh/lumigo-io/java-tracer)

This is the JVM version of Lumigo's tracer library.

Supported Runtimes: Java 8

# Building With Lumigo
## Maven

To install the Lumigo tracer dependency, first update your [Maven](https://maven.apache.org) configuration to add the `nuiton` repository:
```xml
<repositories>
    <repository>
        <id>nuiton</id>
        <url>http://maven.nuiton.org/release/</url>
    </repository>
</repositories>
```

Then, update the dependency section with the following XML. This includes the java tracer and the Lumigo agent:

```xml
<dependency>
  <groupId>io.lumigo</groupId>
  <artifactId>java-tracer</artifactId>
  <version>{version}</version>
</dependency>

<dependency>
  <groupId>io.lumigo</groupId>
  <artifactId>lumigo-agent</artifactId>
  <version>{version}</version>
</dependency>
```

You can find the latest version [here](https://github.com/lumigo-io/java-tracer/releases) - the format of the release number will be `n.n.n`.

# Wrapping your Lambda

To instrument your Lambda function with Lumigo, simply wrap your Lambda function in a supplier class. This supplier class contains your function's code. The following example implements a request handler for a function that accepts an INPUT object, and returns an OUTPUT object:

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

If you are instead returning `Void` from your function, use the following modified code:

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

There are two different ways to pass configuration properties to the Lumigo library - through environment variables, and through static code instantiation.

The list of supported tokens is as follows:

* `LUMIGO_DEBUG=TRUE` - Enables debug logging
* `LUMIGO_SECRET_MASKING_REGEX=["regex1", "regex2"]` - Prevents Lumigo from sending keys that match the supplied regular expressions. All regular expressions are case-insensitive. By default, Lumigo applies the following regular expressions: `[".*pass.*", ".*key.*", ".*secret.*", ".*credential.*", ".*passphrase.*"]`. 
* `LUMIGO_DOMAINS_SCRUBBER=[".*secret.*"]` - Prevents Lumigo from collecting both request and response details from a list of domains. This accepts a comma-separated list of regular expressions that is JSON-formatted. By default, the tracer uses `["secretsmanager\..*\.amazonaws\.com", "ssm\..*\.amazonaws\.com", "kms\..*\.amazonaws\.com"]`. **Note** - These defaults are overridden when you define a different list of regular expressions.
* `LUMIGO_SWITCH_OFF=TRUE` - In the event a critical issue arises, this turns off all actions that Lumigo takes in response to your code. This happens without a deployment, and is picked up on the next function run once the environment variable is present.

### Environment variables

Environment variables that control Lumigo's operation are available to the function's execution environment. Simply define one of the above tokens in an environment variable for your Lambda function.

### Static code instantiation

To assign values to static token objects at run-time, use the following code:

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

This code creates the appropriate tokens in the LumigoConfiguration object, based on the value that you provide for `xxx`.
