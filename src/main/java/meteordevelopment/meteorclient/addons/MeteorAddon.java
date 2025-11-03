/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.addons;

import meteordevelopment.meteorclient.utils.render.color.Color;

import java.io.InputStream;

public abstract class MeteorAddon {
    /** This field is automatically assigned from fabric.mod.json file. */
    public String name;

    /** This field is automatically assigned from fabric.mod.json file. */
    public String[] authors;

    /** This field is automatically assigned from the meteor-client:color property in fabric.mod.json file. */
    public final Color color = new Color(255, 255, 255);

    public abstract void onInitialize();

    public void onRegisterCategories() {}

    public abstract String getPackage();

    public String getWebsite() {
        return null;
    }

    public GithubRepo getRepo() {
        return null;
    }

    public String getCommit() {
        return null;
    }

    /**
     * @param lang  A language code in lowercase
     * @return      An InputStream for the relevant json translation file, or null if the addon doesn't have
     *              a file for that language.
     */
    public InputStream provideLanguage(String lang) {
        return null;
    }
}
