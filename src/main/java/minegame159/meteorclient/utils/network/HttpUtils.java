/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.network;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class HttpUtils {
    private static final Gson GSON = new Gson();

    private static InputStream request(String method, String url, String body) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);
            conn.setRequestProperty("User-Agent", "Meteor Client");

            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", Integer.toString(bytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(bytes);
            }

            return conn.getInputStream();
        } catch (SocketTimeoutException ignored) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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
