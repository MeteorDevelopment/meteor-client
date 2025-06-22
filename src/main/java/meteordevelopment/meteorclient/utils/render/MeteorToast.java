/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorToast implements Toast {
    private static final int TITLE_COLOR = Color.fromRGBA(145, 61, 226, 255);
    private static final int TEXT_COLOR = Color.fromRGBA(220, 220, 220, 255);
    private static final Identifier TEXTURE = Identifier.of("toast/advancement");
    private static final long DEFAULT_DURATION = 6000;
    private static final SoundInstance DEFAULT_SOUND = PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.2f, 1);

    // Toast fields
    private final @NotNull Text title;
    private final @Nullable Text text;
    private final @Nullable ItemStack icon;
    private final @Nullable SoundInstance customSound;
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
        private final @NotNull Text title;
        private @Nullable Text text;
        private @Nullable ItemStack icon;
        private @Nullable SoundInstance customSound = DEFAULT_SOUND;
        private long duration = DEFAULT_DURATION;

        public Builder(@NotNull String title) {
            this.title = Text.literal(title).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TITLE_COLOR)));
        }

        public Builder text(@Nullable String text) {
            this.text = text != null && !text.trim().isEmpty() ?
                Text.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TEXT_COLOR))) :
                null;
            return this;
        }

        public Builder icon(@Nullable Item item) {
            this.icon = item != null ? item.getDefaultStack() : null;
            return this;
        }

        public Builder sound(@Nullable SoundInstance sound) {
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
    public Visibility getVisibility() {
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
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, getWidth(), getHeight());

        int textX = icon != null ? 28 : 12;
        int titleY = 12;

        if (text != null) {
            context.drawText(textRenderer, text, textX, 18, TEXT_COLOR, false);
            titleY = 7;
        }

        context.drawText(textRenderer, title, textX, titleY, TITLE_COLOR, false);

        if (icon != null) context.drawItem(icon, 8, 8);
    }
}
