package com.lumigo.core.network;

import com.lumigo.handlers.LumigoRequestStreamHandler;
import com.lumigo.models.Span;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

import static com.lumigo.core.configuration.LumigoConfiguration.EDGE_DEFAULT_URL;
import static com.lumigo.core.utils.JsonUtils.getObjectAsJsonString;

public class Reporter {
    private static final Logger LOG = LogManager.getLogger(LumigoRequestStreamHandler.class);
    private static final HttpClient client = HttpClientBuilder.create().build();

    public static void reportSpans(Span span) {
        reportSpans(Collections.singletonList(span));
    }

    public static void reportSpans(List<Span> spans) {
        LOG.debug("Sending the spans: " + spans.toString());
        HttpPost post = new HttpPost(EDGE_DEFAULT_URL);
        try {
            StringEntity postingString = new StringEntity(getObjectAsJsonString(spans));
            post.setEntity(postingString);
            post.setHeader("Content-type", "application/json");
            client.execute(post);
            LOG.debug("Span sent successfully");
        } catch (Exception e) {
            LOG.error("Could not report json", e);
        }
    }

}
