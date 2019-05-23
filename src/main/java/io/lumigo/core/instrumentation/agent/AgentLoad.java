package io.lumigo.core.instrumentation.agent;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.ea.agentloader.AgentLoader;
import net.bytebuddy.agent.builder.AgentBuilder;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.stream.Collectors;


import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentLoad {

    public static void agentmain(String agentArgs, Instrumentation inst) {

        AgentBuilder builder = new AgentBuilder.Default().disableClassFormatChanges().enableBootstrapInjection(inst,new File("tmp"))
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
        builder.type(ElementMatchers.named("com.amazonaws.AmazonWebServiceClient")
                .and(ElementMatchers.declaresField(ElementMatchers.named("requestHandler2s"))))
                .transform( new AgentBuilder.Transformer.ForAdvice()
                        .include(AgentLoad.class.getClassLoader(), AmazonWebServiceClient.class.getClassLoader())
                        .advice(isConstructor(),AmazonHttpClientAdvice.class.getName()));
        builder.installOn(inst);
    }

    public static class AmazonHttpClientAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void exit(
                @Advice.FieldValue("requestHandler2s") final List<RequestHandler2> handlers,
                @Advice.FieldValue("client") final AmazonHttpClient client
        ) {
            System.out.println("YES !!!!");
            for (final RequestHandler2 handler : handlers) {
                System.out.println(handler.getClass());
            }

        }
    }

    public static void main(String[] args) throws Exception{

        AgentLoader.loadAgentClass(AgentLoad.class.getName(), null);
      AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
              .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("https://dynamodb.us-west-2.amazonaws.com", "us-west-2"))
              .build();
      ListTablesRequest request = new ListTablesRequest().withLimit(10);
      ListTablesResult table_list = client.listTables(request);
      List<String> table_names = table_list.getTableNames();
      System.out.println(table_names.stream().map(String::valueOf).collect(Collectors.joining()));

    }
}
