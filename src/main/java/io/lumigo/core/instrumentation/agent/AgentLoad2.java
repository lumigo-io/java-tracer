package io.lumigo.core.instrumentation.agent;

import com.amazonaws.Request;
import com.amazonaws.annotation.SdkInternalApi;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.ea.agentloader.AgentLoader;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentLoad2 {

    public static void agentmain(String agentArgs, Instrumentation inst) {

        AgentBuilder builder = new AgentBuilder.Default().disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(not(nameStartsWith("com.amazonaws")));
        builder.type(ElementMatchers.named("com.amazonaws.http.AmazonHttpClient"))
                .transform( new AgentBuilder.Transformer.ForAdvice()
                        .include(AgentLoad2.class.getClassLoader())
                        .advice(ElementMatchers.named("execute").and(isAnnotatedWith(SdkInternalApi.class)),
                                AmazonHttpClientAdvice.class.getName()));
        builder.installOn(inst);
    }

    public static class AmazonHttpClientAdvice {

        @Advice.OnMethodEnter
        public static void executeEnter(@Advice.Argument(0) Request<?> request) {
            System.out.println("Lumigo enter !!!" + request);
        }


        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false, typing = DYNAMIC) Object returned) {
            System.out.println("Lumigo Exist Yes !!!! " + returned);
        }


//        @SdkInternalApi
//        public <T> Response<T> execute(Request<?> request, HttpResponseHandler<AmazonWebServiceResponse<T>> responseHandler, HttpResponseHandler<AmazonServiceException> errorResponseHandler, ExecutionContext executionContext, RequestConfig requestConfig) {
//            HttpResponseHandler<T> adaptedRespHandler = new AwsResponseHandlerAdapter(this.getNonNullResponseHandler(responseHandler), request, executionContext.getAwsRequestMetrics(), this.responseMetadataCache);
//            return this.requestExecutionBuilder().request(request).requestConfig(requestConfig).errorResponseHandler(new AwsErrorResponseHandler(errorResponseHandler, executionContext.getAwsRequestMetrics())).executionContext(executionContext).execute(adaptedRespHandler);
//        }
    }

    public static void main(String[] args) throws Exception{
      AgentLoader.loadAgentClass(AgentLoad2.class.getName(), null);

      AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
              .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("https://dynamodb.us-west-2.amazonaws.com", "us-west-2"))
              .build();
      ListTablesRequest request = new ListTablesRequest().withLimit(10);
      ListTablesResult table_list = client.listTables(request);
      List<String> table_names = table_list.getTableNames();
      System.out.println(table_names.stream().map(String::valueOf).collect(Collectors.joining()));
    }
}
