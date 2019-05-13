package com.lumigo.core.network;

import com.lumigo.models.Span;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.List;

public class Reporter {
    private static final HttpClient client = HttpClientBuilder.create().build();

    public static void reportSpans(Span span) {
    }

    public static void reportSpans(List<Span> span) {
    }
}
