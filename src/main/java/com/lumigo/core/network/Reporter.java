package com.lumigo.core.network;

import com.lumigo.core.configuration.LumigoConfiguration;
import com.lumigo.models.Span;
import okhttp3.*;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.lumigo.core.utils.JsonUtils.getObjectAsJsonString;

public class Reporter {
  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .callTimeout(LumigoConfiguration.getInstance().getLumigoTimeout())
          .build();

  public static void reportSpans(Span span) {
    reportSpans(Collections.singletonList(span));
  }

  public static void reportSpans(List<Span> spans) {
    Logger.info("Reporting the spans: {}", getObjectAsJsonString(spans));

    RequestBody body =
        RequestBody.create(
            MediaType.get("application/json; charset=utf-8"), getObjectAsJsonString(spans));
    Request request =
        new Request.Builder()
            .url(LumigoConfiguration.getInstance().getLumigoEdge())
            .post(body)
            .build();

    try {
      client.newCall(request).execute();
      Logger.debug("{} Spans sent successfully", spans.size());
    } catch (IOException e) {
      Logger.error(e, "Fail in reporting: {}", e.getMessage());
    }

  }
}
