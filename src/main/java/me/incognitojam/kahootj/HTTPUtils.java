package me.incognitojam.kahootj;

import okhttp3.*;
import okhttp3.Request.Builder;

import java.io.IOException;

public class HTTPUtils {

    private static final OkHttpClient client = new OkHttpClient();

    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:49.0) Gecko/20100101 Firefox/49.0";
    public static final MediaType JSON = MediaType.parse("application/json; charset=UTF-8");

    public static Response GET(String url) {
        Request request = new Request.Builder()
                .addHeader("User-Agent", USER_AGENT)
                .url(url)
                .build();
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Response POST(String url, String rawData) {
        return POST(url, rawData, new Headers.Builder().build());
    }

    public static Response POST(String url, String rawData, Headers headers) {
        System.out.println(rawData);
        RequestBody body = RequestBody.create(JSON, rawData);
        Request request = new Builder()
                .url(url)
                .headers(headers)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Origin", "https://kahoot.it")
                .addHeader("Accept", "application/json, text/plain, */*").post(body).build();
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Response POST(String url, Headers headers) {
        Request request = new Builder()
                .url(url)
                .headers(headers)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Origin", "https://kahoot.it")
                .addHeader("Accept", "application/json, text/plain, */*")
                .post(RequestBody.create(null, new byte[0])).build();
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static OkHttpClient getClient() {
        return client;
    }

}