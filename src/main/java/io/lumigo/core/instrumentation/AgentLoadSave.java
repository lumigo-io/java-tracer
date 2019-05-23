package io.lumigo.core.instrumentation;

import com.ea.agentloader.AgentLoader;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class AgentLoadSave {

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {

        final ElementMatcher.Junction<NamedElement> matcher = ElementMatchers.named("io.lumigo.core.instrumentation.A");

        new AgentBuilder.Default()
                .type(matcher)
                .transform( new AgentBuilder.Transformer.ForAdvice()
                        .include(AgentLoadSave.class.getClassLoader())
                        .advice(named("a"), AAdvice.class.getName()))
                .installOn(inst);
    }

  public static void main(String[] args) throws Exception{
      AgentLoader.loadAgentClass(AgentLoadSave.class.getName(), null);
      new A().a();
//      HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://www.google.com").openConnection();
//      System.out.println(urlConnection.getRequestMethod());
//      AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
//              .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("https://dynamodb.us-west-2.amazonaws.com", "us-west-2"))
//              .build();
//      ListTablesRequest request = new ListTablesRequest().withLimit(10);
//      ListTablesResult table_list = client.listTables(request);
//      List<String> table_names = table_list.getTableNames();
//    System.out.println(table_names.stream().map(String::valueOf).collect(Collectors.joining()));
  }

    public static class AAdvice {

        @Advice.OnMethodEnter
        public static void enter() {
            System.out.println("Enter Yes!!!!");
        }

        @Advice.OnMethodExit
        public static void exit() {
            System.out.println("Exist Yes!!!!");
        }
    }
}
