package io.lumigo.core.network;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.models.Span;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.pmw.tinylog.Logger;

public class Reporter {
    private final OkHttpClient client;

    public Reporter() {
        client =
                new OkHttpClient.Builder()
                        .callTimeout(Configuration.getInstance().getLumigoTimeout())
                        .build();
    }

    public void reportSpans(Span span) throws IOException {
        reportSpans(Collections.singletonList(span));
    }

    public void reportSpans(List<Object> spans) throws IOException {
        long time = System.nanoTime();
        String spansAsString = JsonUtils.getObjectAsJsonString(spans);
        Logger.debug("Reporting the spans: {}", spansAsString);

        if (Configuration.getInstance().isAwsEnvironment()) {
            RequestBody body =
                    RequestBody.create(
                            MediaType.get("application/json; charset=utf-8"), spansAsString);
            Request request =
                    new Request.Builder()
                            .url(Configuration.getInstance().getLumigoEdge())
                            .post(body)
                            .build();

            client.newCall(request).execute();

            Logger.info(
                    "Took: {} milliseconds to send {} Spans to URL: {}",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time),
                    spans.size(),
                    Configuration.getInstance().getLumigoEdge());
        }
    }
}
