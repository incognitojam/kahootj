package me.incognitojam.kahootj;

import okhttp3.*;
import okhttp3.Request.Builder;

import java.io.IOException;

public class HTTPUtils {

    private static final OkHttpClient client = new OkHttpClient();

    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:49.0) Gecko/20100101 Firefox/49.0";
    public static final MediaType JSON = MediaType.parse("application/json; charset=UTF-8");

    public static Call GET(String url) {
        return _GET(url);
    }

    public static Response GET_RESPONSE(String url) {
        try {
            return _GET(url).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Call _GET(String url) {
        Request request = new Request.Builder()
                .addHeader("User-Agent", USER_AGENT)
                .url(url)
                .build();
        return client.newCall(request);
    }

    public static Call POST(String url, String rawData) {
        return _POST(url, rawData, null);
    }

    public static Call POST(String url, String rawData, Headers headers) {
        return _POST(url, rawData, headers);
    }

    public static Call POST(String url, Headers headers) {
        return _POST(url, null, headers);
    }

    private static Call _POST(String url, String rawData, Headers headers) {
        Request.Builder builder = new Builder();
        if (headers != null) builder.headers(headers);
        Request request = builder
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Origin", "https://kahoot.it")
                .addHeader("Accept", "application/json, text/plain, */*")
                .post(rawData != null ? RequestBody.create(JSON, rawData) : RequestBody.create(null, new byte[0])).build();
        return client.newCall(request);
    }

    public static OkHttpClient getClient() {
        return client;
    }

}