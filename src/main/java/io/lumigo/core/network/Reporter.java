package io.lumigo.core.network;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.BaseSpan;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
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

    public long reportSpans(BaseSpan span, int maxSize) throws IOException {
        return reportSpans(Collections.singletonList(span), maxSize);
    }

    public long reportSpans(List<BaseSpan> spans, int maxSize) throws IOException {
        long time = System.currentTimeMillis();
        List<String> spansAsStringList = new LinkedList<>();
        int sizeCount = 0;
        int handledSpans = 0;
        for (Object span : spans) {
            if (sizeCount >= maxSize) {
                Logger.debug("Dropped spans by request size: {}", spans.size() - handledSpans);
                break;
            }
            String spanAsString = JsonUtils.getObjectAsJsonString(span);
            if (spanAsString != null) {
                spansAsStringList.add(spanAsString);
                sizeCount += StringUtils.getBase64Size(spanAsString);
            }
            handledSpans++;
        }

        if (Configuration.getInstance().isAwsEnvironment() && !spansAsStringList.isEmpty()) {
            String spansAsString = "[" + String.join(",", spansAsStringList) + "]";
            Logger.debug("Reporting the spans: {}", spansAsString);
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
            return System.currentTimeMillis() - time;
        }
        return 0;
    }
}
