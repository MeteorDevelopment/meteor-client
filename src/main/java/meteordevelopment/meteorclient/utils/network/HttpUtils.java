/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

public class HttpUtils {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    private static InputStream request(String method, String url, String body) {
        try {
            return CLIENT.send(
                HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .method(method, body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", "Meteor Client")
                    .build(),
                HttpResponse.BodyHandlers.ofInputStream()
            ).body();
        } catch (IOException | InterruptedException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static InputStream get(String url) {
        return request("GET", url, null);
    }

    public static InputStream post(String url, String body) {
        return request("POST", url, body);
    }

    public static InputStream post(String url) {
        return post(url, null);
    }

    public static void getLines(String url, Consumer<String> callback) {
        try {
            InputStream in = get(url);
            if (in == null) return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) callback.accept(line);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T get(String url, Type type) {
        try {
            InputStream in = get(url);
            if (in == null) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            T response = GSON.fromJson(reader, type);
            reader.close();

            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
