package io.lumigo.core.instrumentation;

import com.amazonaws.*;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

public class AmazonHttpClientInstrumentation implements LumigoInstrumentionApi{

    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        return  ElementMatchers.named("com.amazonaws.http.AmazonHttpClient");
    }

    @Override
    public Map<ElementMatcher, String> getTransformers() {
        final Map<ElementMatcher, String> transformers = new HashMap<>();
        transformers.put(ElementMatchers.named("execute"), AmazonHttpClientAdvice.class.getName());
        return transformers;
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
}
