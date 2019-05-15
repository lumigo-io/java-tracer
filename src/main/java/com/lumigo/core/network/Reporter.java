package com.lumigo.core.network;

import static com.lumigo.core.utils.JsonUtils.getObjectAsJsonString;

import com.lumigo.core.configuration.LumigoConfiguration;
import com.lumigo.models.Span;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import okhttp3.*;
import org.pmw.tinylog.Logger;

public class Reporter {
    private static final OkHttpClient client =
            new OkHttpClient.Builder()
                    .callTimeout(LumigoConfiguration.getInstance().getLumigoTimeout())
                    .build();

    public static void reportSpans(Span span) {
        reportSpans(Collections.singletonList(span));
    }

    public static void reportSpans(List<Span> spans) {
        try {
            String spansAsString = getObjectAsJsonString(spans);
            Logger.info("Reporting the spans: {}", spansAsString);

            RequestBody body =
                    RequestBody.create(
                            MediaType.get("application/json; charset=utf-8"), spansAsString);
            Request request =
                    new Request.Builder()
                            .url(LumigoConfiguration.getInstance().getLumigoEdge())
                            .post(body)
                            .build();
            client.newCall(request).execute();
            Logger.debug("{} Spans sent successfully", spans.size());
        } catch (Exception e) {
            Logger.error(e, "Fail in reporting: {}", e.getMessage());
        }
    }

    public static void reportSpansAsync(Span span) {
        reportSpansAsync(Collections.singletonList(span));
    }

    public static void reportSpansAsync(List<Span> spans) {
        try {
            String spansAsString = getObjectAsJsonString(spans);
            Logger.info("Reporting the spans async: {}", spansAsString);

            RequestBody body =
                    RequestBody.create(
                            MediaType.get("application/json; charset=utf-8"), spansAsString);
            Request request =
                    new Request.Builder()
                            .url(LumigoConfiguration.getInstance().getLumigoEdge())
                            .post(body)
                            .build();

            client.newCall(request)
                    .enqueue(
                            new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    Logger.error(e, "Fail in reporting: {}", e.getMessage());
                                }

                                @Override
                                public void onResponse(Call call, Response response)
                                        throws IOException {
                                    Logger.debug("{} Spans sent successfully", spans.size());
                                }
                            });
            Logger.debug("{} Spans sent successfully", spans.size());
        } catch (Exception e) {
            Logger.error(e, "Fail in reporting: {}", e.getMessage());
        }
    }
}
