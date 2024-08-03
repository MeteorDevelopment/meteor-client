/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import javax.annotation.Nullable;
import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

/**
 * Failed {@link HttpResponse} object that is returned from {@link Http} {@code sendRequest} methods when
 * {@link HttpClient#send(HttpRequest, BodyHandler)} throws an exception instead of returning an error response, such
 * as when the connection is interrupted or an I/O error occurs. This object is used to prevent {@code sendRequest}
 * methods from returning {@code null}.
 *
 * @author Crosby
 */
public record FailedHttpResponse<T>(HttpRequest request, Exception exception) implements HttpResponse<T> {
    @Override
    public int statusCode() {
        return Http.BAD_REQUEST;
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        return HttpHeaders.of(Map.of(), (s1, s2) -> true);
    }

    @Override
    public T body() {
        return null;
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        return this.request.uri();
    }

    @Nullable
    @Override
    public HttpClient.Version version() {
        return this.request.version().orElse(null);
    }
}
