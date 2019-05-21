package io.lumigo.core.hooks;

import io.lumigo.core.SpansContainer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.pmw.tinylog.Logger;

import java.net.URI;

public class Hooker {
    public static boolean hookRequests() {
        try {
            ByteBuddyAgent.install();

            new ByteBuddy()
                    .redefine(CloseableHttpClient.class)
                    .method(ElementMatchers.named("determineTarget"))
                    .intercept(MethodDelegation.to(RequestHooker.class))
                    .make()
                    .load(CloseableHttpClient.class.getClassLoader(),
                            ClassReloadingStrategy.fromInstalledAgent())
                    .getLoaded();

            return true;
        } catch (Exception e) {
            Logger.error(e, "Exception while hooking the CloseableHttpClient class");
            return false;
        }
    }

    public static class RequestHooker {
        public static HttpHost determineTarget(HttpUriRequest request) throws ClientProtocolException {
            try {
                SpansContainer.getInstance().addHttpSpan(request.getURI(), request.getAllHeaders());
            } catch (Exception e) {
                Logger.error(e, "Exception while adding an http span");
            }
            return originalDetermineTarget(request);
        }

        /**
         * This is the original code of CloseableHttpClient#determineTarget.
         *
         * FIND A WAY TO DO THAT BETTER!
         */
        private static HttpHost originalDetermineTarget(HttpUriRequest request) throws ClientProtocolException {
            HttpHost target = null;
            URI requestURI = request.getURI();
            if (requestURI.isAbsolute()) {
                target = URIUtils.extractHost(requestURI);
                if (target == null) {
                    throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI);
                }
            }
            return target;
        }
    }

}
