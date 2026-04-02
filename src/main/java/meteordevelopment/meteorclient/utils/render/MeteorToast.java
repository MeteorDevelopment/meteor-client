/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorToast implements Toast {
    private static final int TITLE_COLOR = Color.fromRGBA(145, 61, 226, 255);
    private static final int TEXT_COLOR = Color.fromRGBA(220, 220, 220, 255);
    private static final Identifier TEXTURE = Identifier.parse("toast/advancement");
    private static final long DEFAULT_DURATION = 6000;
    private static final SimpleSoundInstance DEFAULT_SOUND = SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.2f, 1);

    // Toast fields
    private final @NotNull Component title;
    private final @Nullable Component text;
    private final @Nullable ItemStack icon;
    private final @Nullable SimpleSoundInstance customSound;
    private final long duration;

    // State variables
    private boolean playedSound;
    private long start = -1;
    private Visibility visibility = Visibility.HIDE;

    private MeteorToast(Builder builder) {
        this.title = builder.title;
        this.text = builder.text;
        this.icon = builder.icon;
        this.customSound = builder.customSound;
        this.duration = builder.duration;
    }

    public static class Builder {
        private final @NotNull Component title;
        private @Nullable Component text;
        private @Nullable ItemStack icon;
        private @Nullable SimpleSoundInstance customSound = DEFAULT_SOUND;
        private long duration = DEFAULT_DURATION;

        public Builder(@NotNull String title) {
            this.title = Component.literal(title).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TITLE_COLOR)));
        }

        public Builder text(@Nullable String text) {
            this.text = text != null && !text.trim().isEmpty() ?
                Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TEXT_COLOR))) :
                null;
            return this;
        }

        public Builder icon(@Nullable Item item) {
            this.icon = item != null ? item.getDefaultInstance() : null;
            return this;
        }

        public Builder sound(@Nullable SimpleSoundInstance sound) {
            this.customSound = sound;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = Math.max(0, duration);
            return this;
        }

        public MeteorToast build() {
            return new MeteorToast(this);
        }
    }

    @Override
    public Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(ToastManager manager, long time) {
        if (start == -1) start = time;

        visibility = time - start >= duration ? Visibility.HIDE : Visibility.SHOW;

        if (!playedSound) {
            mc.getSoundManager().play(customSound);
            playedSound = true;
        }
    }

    @Override
    public void render(GuiGraphics context, Font textRenderer, long startTime) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, width(), height());

        int textX = icon != null ? 28 : 12;
        int titleY = 12;

        if (text != null) {
            context.drawString(textRenderer, text, textX, 18, TEXT_COLOR, false);
            titleY = 7;
        }

        context.drawString(textRenderer, title, textX, titleY, TITLE_COLOR, false);

        if (icon != null) context.renderItem(icon, 8, 8);
    }
}
