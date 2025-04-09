/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import com.mojang.authlib.yggdrasil.ProfileResult;
import meteordevelopment.meteorclient.mixin.PlayerListEntryAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;

import java.util.UUID;
import java.util.function.Supplier;

public class NameProtect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> nameProtect = sgGeneral.add(new BoolSetting.Builder()
        .name("name-protect")
        .description("Hides your name client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("Name to be replaced with.")
        .defaultValue("seasnail")
        .visible(nameProtect::get)
        .build()
    );

    private final Setting<SkinProtect> skinProtect = sgGeneral.add(new EnumSetting.Builder<SkinProtect>()
        .name("skin-protect")
        .description("Modify your currently applied skin.")
        .defaultValue(SkinProtect.DefaultSkin)
        .build()
    );

    @SuppressWarnings("unused")
    private final Setting<String> customSkinUuid = sgGeneral.add(new StringSetting.Builder()
        .name("custom-skin-url")
        .description("The UUID to use as the source of the skin.")
        .defaultValue("3aa4fb57-2ef7-4304-b4d7-bd2187f48e1f")
        .visible(() -> skinProtect.get() == SkinProtect.CustomSkin)
        .onChanged(this::updateSkin)
        .build()
    );

    private String username = "If you see this, something is wrong.";
    private Supplier<SkinTextures> skinTexture = () -> DefaultSkinHelper.getSkinTextures(MinecraftClient.getInstance().getGameProfile());

    public NameProtect() {
        super(Categories.Player, "name-protect", "Hide player names and skins.");
    }

    @Override
    public void onActivate() {
        username = mc.getSession().getUsername();
    }

    private void updateSkin(String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            ProfileResult result = MinecraftClient.getInstance().getSessionService().fetchProfile(uuid, false);
            if (result != null) {
                skinTexture = PlayerListEntryAccessor.meteor$texturesSupplier(result.profile());
            }
        } catch (IllegalArgumentException ignored) {}
    }

    public String replaceName(String string) {
        if (string != null && isActive()) {
            return string.replace(username, name.get());
        }

        return string;
    }

    public String getName(String original) {
        if (!name.get().isEmpty() && isActive()) {
            return name.get();
        }

        return original;
    }

    public boolean modifySkin() {
        return isActive() && skinProtect.get() != SkinProtect.Disabled;
    }

    public SkinTextures getSkin() {
        return skinProtect.get() == SkinProtect.DefaultSkin ? DefaultSkinHelper.getSkinTextures(MinecraftClient.getInstance().getGameProfile()) : skinTexture.get();
    }

    public enum SkinProtect {
        Disabled,
        DefaultSkin,
        CustomSkin
    }
}
