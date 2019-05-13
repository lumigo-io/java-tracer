package com.lumigo.core.network;

import com.lumigo.core.SpansContainer;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class Reporter {
    private static final HttpClient client = HttpClientBuilder.create().build();

    public static void reportSpans(SpansContainer instance) {
    }
}
