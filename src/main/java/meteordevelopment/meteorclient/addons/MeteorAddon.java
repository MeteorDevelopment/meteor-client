/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.addons;

import meteordevelopment.meteorclient.utils.render.color.Color;

import java.io.InputStream;

public abstract class MeteorAddon {
    /** This field is automatically assigned from fabric.mod.json file.
     * @since 1.21.11 */ // todo replace with exact version when released
    public String id;

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
     * Example implementation:
     * <pre>{@code
     *  @Override
     *  public InputStream provideLanguage(String lang) {
     *      return Addon.class.getResourceAsStream("/assets/addon-name/language/" + lang + ".json")
     *  }
     * }
     * </pre><br>
     *
     * Addons should not store their language files in the /assets/xxx/lang/ path as it opens up users to detection
     * by servers via <a href="https://wurst.wiki/sign_translation_vulnerability">the translation exploit</a>.
     * Storing them anywhere else should prevent them from getting picked up via the vanilla resource loader.
     *
     * @param lang  A language code in lowercase
     * @return      An InputStream for the relevant json translation file, or null if the addon doesn't have
     *              a file for that language.
     */
    public InputStream provideLanguage(String lang) {
        return null;
    }
}
