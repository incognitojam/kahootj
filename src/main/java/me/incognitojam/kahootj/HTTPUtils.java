package me.incognitojam.kahootj;

import okhttp3.*;
import okhttp3.Request.Builder;

public class HTTPUtils {

    private static final OkHttpClient client = new OkHttpClient();

    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:49.0) Gecko/20100101 Firefox/49.0";
    public static final MediaType JSON = MediaType.parse("application/json; charset=UTF-8");

    public static void GET(String url, Callback callback) {
        Request request = new Request.Builder()
                .addHeader("User-Agent", USER_AGENT)
                .url(url)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public static void POST(String url, String rawData, Callback callback) {
        POST(url, rawData, new Headers.Builder().build(), callback);
    }

    public static void POST(String url, String rawData, Headers headers, Callback callback) {
        System.out.println(rawData);
        RequestBody body = RequestBody.create(JSON, rawData);
        Request request = new Builder()
                .url(url)
                .headers(headers)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Origin", "https://kahoot.it")
                .addHeader("Accept", "application/json, text/plain, */*").post(body).build();
        client.newCall(request).enqueue(callback);
    }

    public static void POST(String url, Headers headers, Callback callback) {
        Request request = new Builder()
                .url(url)
                .headers(headers)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Origin", "https://kahoot.it")
                .addHeader("Accept", "application/json, text/plain, */*")
                .post(RequestBody.create(null, new byte[0])).build();
        client.newCall(request).enqueue(callback);
    }

    public static OkHttpClient getClient() {
        return client;
    }

}