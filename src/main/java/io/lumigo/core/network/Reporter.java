package io.lumigo.core.network;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.JsonUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import okhttp3.*;
import org.pmw.tinylog.Logger;

public class Reporter {

    public static final Callback callBack =
            new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Logger.error(e, "Failed to send spans");
                }

                @Override
                public void onResponse(Call call, Response response) {
                    response.body().close();
                }
            };
    private OkHttpClient client;

    public Reporter() {
        client =
                new OkHttpClient.Builder()
                        .callTimeout(Configuration.getInstance().getLumigoTimeout())
                        .build();
    }

    public long reportSpans(Object span) {
        return reportSpans(Collections.singletonList(span));
    }

    public long reportSpans(List<Object> spans) {
        long time = System.nanoTime();
        String spansAsString = JsonUtils.getObjectAsJsonString(spans);
        Logger.debug("Reporting the spans: {}", spansAsString);

        if (Configuration.getInstance().isAwsEnvironment()) {
            RequestBody body =
                    RequestBody.create(
                            MediaType.get("application/json; charset=utf-8"), spansAsString);
            Request request =
                    new Request.Builder()
                            .header("Accept", "application/json")
                            .url(Configuration.getInstance().getLumigoEdge())
                            .post(body)
                            .build();
            client.newCall(request).enqueue(callBack);
            long duration = System.nanoTime() - time;
            return duration;
        }
        return 0;
    }
}
