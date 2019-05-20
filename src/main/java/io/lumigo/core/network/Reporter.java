package io.lumigo.core.network;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.models.Span;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
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

    public void reportSpans(List<Span> spans) throws IOException {
        String spansAsString = JsonUtils.getObjectAsJsonString(spans);
        Logger.info("Reporting the spans: {}", spansAsString);

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

            Logger.debug("{} Spans sent successfully", spans.size());
        }
    }
}
