/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.addons;

public record GithubRepo(String owner, String name, String branch) {
    public GithubRepo(String owner, String name) {
        this(owner, name, "master");
    }

    public String getOwnerName() {
        return owner + "/" + name;
    }
}
