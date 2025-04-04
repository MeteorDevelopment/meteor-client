/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.addons;

import motordevelopment.motorclient.utils.network.Http;

import javax.annotation.Nullable;

public record GithubRepo(String owner, String name, String branch, @Nullable String accessToken) {
    public GithubRepo(String owner, String name, @Nullable String accessToken) {
        this(owner, name, "master", accessToken);
    }

    public GithubRepo(String owner, String name) {
        this(owner, name, "master", null);
    }

    public String getOwnerName() {
        return owner + "/" + name;
    }

    public void authenticate(Http.Request request) {
        if (this.accessToken != null && !this.accessToken.isBlank()) {
            request.bearer(this.accessToken);
        } else {
            String personalAuthToken = System.getenv("motor.github.authorization");
            if (personalAuthToken != null && !personalAuthToken.isBlank()) {
                request.bearer(personalAuthToken);
            }
        }
    }
}
