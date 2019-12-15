package io.lumigo.core.network;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.JsonUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import okhttp3.*;
import org.pmw.tinylog.Logger;

public class Reporter {

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
        long time = System.currentTimeMillis();
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
            Response response = client.newCall(request).execute();
            if (response.body() != null) {
                response.body().close();
            }
            long duration = System.currentTimeMillis() - time;
            return duration;
        }
        return 0;
    }
}
