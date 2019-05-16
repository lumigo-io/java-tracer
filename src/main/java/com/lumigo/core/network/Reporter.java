package com.lumigo.core.network;

import static com.lumigo.core.utils.JsonUtils.getObjectAsJsonString;

import com.lumigo.core.configuration.Configuration;
import com.lumigo.models.Span;
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
        String spansAsString = getObjectAsJsonString(spans);
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

    //    public void reportSpansAsync(Span span) throws JsonProcessingException {
    //        reportSpansAsync(Collections.singletonList(span));
    //    }
    //
    //    public void reportSpansAsync(List<Span> spans) throws JsonProcessingException {
    //        String spansAsString = getObjectAsJsonString(spans);
    //        Logger.info("Reporting the spans async: {}", spansAsString);
    //
    //        if (Configuration.getInstance().isAwsEnvironment()) {
    //            RequestBody body =
    //                    RequestBody.create(
    //                            MediaType.get("application/json; charset=utf-8"), spansAsString);
    //            Request request =
    //                    new Request.Builder()
    //                            .url(Configuration.getInstance().getLumigoEdge())
    //                            .post(body)
    //                            .build();
    //
    //            client.newCall(request).enqueue(new AsyncHandler(spans.size()));
    //            Logger.info("{} Spans sent successfully", spans.size());
    //        }
    //    }

    private static class AsyncHandler implements Callback {
        private int numberOfSpans;

        public AsyncHandler(int numberOfSpans) {
            this.numberOfSpans = numberOfSpans;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            Logger.error(e, "Fail in reporting: {}", e.getMessage());
        }

        @Override
        public void onResponse(Call call, Response response) {
            Logger.debug("{} Spans sent successfully", numberOfSpans);
        }
    }
}
