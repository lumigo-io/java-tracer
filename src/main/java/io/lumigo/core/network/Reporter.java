package io.lumigo.core.network;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.JsonUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Setter;
import okhttp3.*;
import org.pmw.tinylog.Logger;

public class Reporter {
    @Setter(AccessLevel.PACKAGE)
    private OkHttpClient client;

    public Reporter() {
        client =
                new OkHttpClient.Builder()
                        .callTimeout(Configuration.getInstance().getLumigoTimeout())
                        .build();
    }

    public long reportSpans(Object span) throws IOException {
        return reportSpans(Collections.singletonList(span));
    }

    public long reportSpans(List<Object> spans) throws IOException {
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
