package io.lumigo.core.network;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.JsonUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Setter;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.pmw.tinylog.Logger;

public class Reporter {

    @Setter(AccessLevel.PACKAGE)
    private HttpClient client;

    public Reporter() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(
                Long.valueOf(Configuration.getInstance().getLumigoTimeout().toMillis()).intValue());
        requestBuilder.setConnectionRequestTimeout(
                Long.valueOf(Configuration.getInstance().getLumigoTimeout().toMillis()).intValue());
        client = HttpClientBuilder.create().setDefaultRequestConfig(requestBuilder.build()).build();
    }

    public long reportSpans(Object span) throws IOException {
        return reportSpans(Collections.singletonList(span));
    }

    public long reportSpans(List<Object> spans) throws IOException {
        long time = System.nanoTime();
        String spansAsString = JsonUtils.getObjectAsJsonString(spans);
        Logger.debug("Reporting the spans: {}", spansAsString);

        if (Configuration.getInstance().isAwsEnvironment()) {
            HttpPost request = new HttpPost(Configuration.getInstance().getLumigoEdge());
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-Type", "application/json; charset=utf-8");
            request.setEntity(new StringEntity(spansAsString));
            client.execute(request);
            long duration = System.nanoTime() - time;
            Logger.info(
                    "Took: {} milliseconds to send {} Spans to URL: {}",
                    TimeUnit.NANOSECONDS.toMillis(duration),
                    spans.size(),
                    Configuration.getInstance().getLumigoEdge());
            return duration;
        }
        return 0;
    }
}
