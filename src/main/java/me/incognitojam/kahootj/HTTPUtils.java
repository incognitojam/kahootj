package me.incognitojam.kahootj;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HTTPUtils {

    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:49.0) Gecko/20100101 Firefox/49.0";
    public static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    public static CloseableHttpClient getClient() {
        return HttpClients.createDefault();
    }

    public static HttpGet GET(String url) {
        HttpGet req = new HttpGet(url);
        req.setHeader("User-Agent", USER_AGENT);
        return req;
    }

    public static HttpPost POST(String url, String rawData) {
        HttpPost req = new HttpPost(url);
        HttpEntity e = new ByteArrayEntity(rawData.getBytes());
        req.setHeader("User-Agent", USER_AGENT);
        req.setHeader("Content-Type", CONTENT_TYPE);
        req.setHeader("Origin", "https://kahoot.it");
        req.setHeader("Accept", "application/json, text/plain, */*");
        req.setEntity(e);
        return req;
    }

}