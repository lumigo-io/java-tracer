![Version](https://img.shields.io/badge/version-1.0.1-green.svg)
[![CircleCI](https://circleci.com/gh/lumigo-io/java-tracer.svg?style=svg&circle-token=f2e3400e6e79bc31daeee1fc614ecc0a149b1905)](https://circleci.com/gh/lumigo-io/java-tracer)
[![codecov](https://codecov.io/gh/lumigo-io/java-tracer/branch/master/graph/badge.svg?token=D3IZ5hQwaQ)](https://codecov.io/gh/lumigo-io/java-tracer)


# Building With Lumigo
## Maven
Include lumigo java tracer dependency, for [Maven](https://maven.apache.org) projects, use:
```xml
<dependency>
  <groupId>io.lumigo</groupId>
  <artifactId>java-tracer</artifactId>
  <version>{version}</version>
</dependency>
```
Find the latest version here (the format of the version will be n.n.n):

# Wrapping your Lambda
* Wrap your lambda function using by extending one of the next classes `LumigoRequestHandler` or `LumigoRequestStreamHandler`
```
class MyFunction extends LumigoRequestHandler<String, String> {

        @Override
        public String doHandleRequest(String input, Context context) {
            //Your Lambda code
            return "Response";
        }
    }
```

## Configuration
There are 2 way to pass configuration properties

### Environment variables
    Adding `LUMIGO_TRACER_TOKEN` environment variables

### Static code initiation
```
class MyFunction extends LumigoRequestHandler<String, String> {

        static{
            LumigoConfiguration.builder().token("xxx").build().init();
        }

        @Override
        public String doHandleRequest(String input, Context context) {
            //Your Lambda code
            return "Response";
        }
    }
```


