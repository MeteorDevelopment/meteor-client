/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.utils.other.JsonDateDeserializer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Http {
    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Date.class, new JsonDateDeserializer())
        .create();

    private enum Method {
        GET,
        POST
    }

    public static class Request {
        private final HttpRequest.Builder builder;
        private Method method;
        private Consumer<Exception> exceptionHandler = Exception::printStackTrace;

        private Request(Method method, String url) {
            try {
                this.builder = HttpRequest.newBuilder().uri(new URI(url)).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
                this.method = method;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public Request header(String name, String value) {
            builder.header(name, value);

            return this;
        }

        public Request bearer(String token) {
            builder.header("Authorization", "Bearer " + token);

            return this;
        }

        public Request bodyString(String string) {
            builder.header("Content-Type", "text/plain");
            builder.method(method.name(), HttpRequest.BodyPublishers.ofString(string));
            method = null;

            return this;
        }

        public Request bodyForm(String string) {
            builder.header("Content-Type", "application/x-www-form-urlencoded");
            builder.method(method.name(), HttpRequest.BodyPublishers.ofString(string));
            method = null;

            return this;
        }

        public Request bodyJson(String string) {
            builder.header("Content-Type", "application/json");
            builder.method(method.name(), HttpRequest.BodyPublishers.ofString(string));
            method = null;

            return this;
        }

        public Request bodyJson(Object object) {
            builder.header("Content-Type", "application/json");
            builder.method(method.name(), HttpRequest.BodyPublishers.ofString(GSON.toJson(object)));
            method = null;

            return this;
        }

        public Request ignoreExceptions() {
            exceptionHandler = e -> {};
            return this;
        }

        public Request exceptionHandler(Consumer<Exception> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        private <T> HttpResponse<T> _sendResponse(String accept, HttpResponse.BodyHandler<T> responseBodyHandler) {
            builder.header("Accept", accept);
            if (method != null) builder.method(method.name(), HttpRequest.BodyPublishers.noBody());

            HttpRequest request = builder.build();

            try {
                return CLIENT.send(request, responseBodyHandler);
            } catch (IOException | InterruptedException e) {
                exceptionHandler.accept(e);
                return new FailedHttpResponse<>(request, e);
            }
        }

        @Nullable
        private <T> T _send(String accept, HttpResponse.BodyHandler<T> responseBodyHandler) {
            HttpResponse<T> res = _sendResponse(accept, responseBodyHandler);
            return res.statusCode() == 200 ? res.body() : null;
        }

        public void send() {
            _send("*/*", HttpResponse.BodyHandlers.discarding());
        }

        public HttpResponse<Void> sendResponse() {
            return _sendResponse("*/*", HttpResponse.BodyHandlers.discarding());
        }

        @Nullable
        public InputStream sendInputStream() {
            return _send("*/*", HttpResponse.BodyHandlers.ofInputStream());
        }

        public HttpResponse<InputStream> sendInputStreamResponse() {
            return _sendResponse("*/*", HttpResponse.BodyHandlers.ofInputStream());
        }

        @Nullable
        public String sendString() {
            return _send("*/*", HttpResponse.BodyHandlers.ofString());
        }

        public HttpResponse<String> sendStringResponse() {
            return _sendResponse("*/*", HttpResponse.BodyHandlers.ofString());
        }

        @Nullable
        public Stream<String> sendLines() {
            return _send("*/*", HttpResponse.BodyHandlers.ofLines());
        }

        public HttpResponse<Stream<String>> sendLinesResponse() {
            return _sendResponse("*/*", HttpResponse.BodyHandlers.ofLines());
        }

        @Nullable
        public <T> T sendJson(Type type) {
            return _send("application/json", JsonBodyHandler.ofJson(GSON, type));
        }

        public <T> HttpResponse<T> sendJsonResponse(Type type) {
            return _sendResponse("*/*", JsonBodyHandler.ofJson(GSON, type));
        }
    }

    public static Request get(String url) {
        return new Request(Method.GET, url);
    }

    public static Request post(String url) {
        return new Request(Method.POST, url);
    }
}
