package com.lumigo.core.network;

import static com.lumigo.core.utils.JsonUtils.getObjectAsJsonString;

import com.lumigo.core.configuration.LumigoConfiguration;
import com.lumigo.models.Span;
import java.util.Collections;
import java.util.List;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.pmw.tinylog.Logger;

public class Reporter {
  private static final HttpClient client = HttpClientBuilder.create().build();

  public static void reportSpans(Span span) {
    reportSpans(Collections.singletonList(span));
  }

  public static void reportSpans(List<Span> spans) {
    Logger.info("Reporting the spans: " + getObjectAsJsonString(spans));
    HttpPost post = new HttpPost(LumigoConfiguration.getInstance().getLumigoEdge());
    try {
      StringEntity postingString = new StringEntity(getObjectAsJsonString(spans));
      post.setEntity(postingString);
      post.setHeader("Content-type", "application/json");
      client.execute(post);
      Logger.debug("{} Spans sent successfully", spans.size());
    } catch (Exception e) {
      Logger.error(e, "Could not report json");
    }
  }
}
