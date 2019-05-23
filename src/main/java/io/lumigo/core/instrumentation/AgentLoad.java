package io.lumigo.core.instrumentation;

import com.ea.agentloader.AgentLoader;
import io.lumigo.core.configuration.Configuration;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;


import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentLoad {

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {

        final ElementMatcher.Junction<NamedElement> matcher = ElementMatchers.named("io.lumigo.core.configuration.Configuration");

        new AgentBuilder.Default()
                .type(matcher)
                .transform( new AgentBuilder.Transformer.ForAdvice()
                        .include(AgentLoad.class.getClassLoader())
                        .advice(named("getLumigoToken"), AAdvice.class.getName()))
                .installOn(inst);
    }


  public static void main(String[] args) throws Exception{
      AgentLoader.loadAgentClass(AgentLoad.class.getName(), null);
      Configuration.getInstance().getLumigoToken();
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
