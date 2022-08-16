/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorToast implements Toast {
    public static final int TITLE_COLOR = Color.fromRGBA(145, 61, 226, 255);
    public static final int TEXT_COLOR = Color.fromRGBA(220, 220, 220, 255);

    private ItemStack icon;
    private Text title, text;
    private boolean justUpdated = true, playedSound;
    private long start, duration;

    public MeteorToast(@Nullable Item item, @NotNull String title, @Nullable String text, long duration) {
        this.icon = item != null ? item.getDefaultStack() : null;
        this.title = Text.literal(title).setStyle(Style.EMPTY.withColor(new TextColor(TITLE_COLOR)));
        this.text = text != null ? Text.literal(text).setStyle(Style.EMPTY.withColor(new TextColor(TEXT_COLOR))) : null;
        this.duration = duration;
    }

    public MeteorToast(@Nullable Item item, @NotNull String title, @Nullable String text) {
        this.icon = item != null ? item.getDefaultStack() : null;
        this.title = Text.literal(title).setStyle(Style.EMPTY.withColor(new TextColor(TITLE_COLOR)));
        this.text = text != null ? Text.literal(text).setStyle(Style.EMPTY.withColor(new TextColor(TEXT_COLOR))) : null;
        this.duration = 6000;
    }

    public Visibility draw(MatrixStack matrices, ToastManager toastManager, long currentTime) {
        if (justUpdated) {
            start = currentTime;
            justUpdated = false;
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        toastManager.drawTexture(matrices, 0, 0, 0, 0, getWidth(), getHeight());

        int x = icon != null ? 28 : 12;
        int titleY = 12;

        if (text != null) {
            mc.textRenderer.draw(matrices, text, x, 18, TEXT_COLOR);
            titleY = 7;
        }

        mc.textRenderer.draw(matrices, title, x, titleY, TITLE_COLOR);

        if (icon != null) mc.getItemRenderer().renderInGui(icon, 8, 8);

        if (!playedSound) {
            mc.getSoundManager().play(getSound());
            playedSound = true;
        }

        return currentTime - start >= duration ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    public void setIcon(@Nullable Item item) {
        this.icon = item != null ? item.getDefaultStack() : null;
        justUpdated = true;
    }

    public void setTitle(@NotNull String title) {
        this.title = Text.literal(title).setStyle(Style.EMPTY.withColor(new TextColor(TITLE_COLOR)));
        justUpdated = true;
    }

    public void setText(@Nullable String text) {
        this.text = text != null ? Text.literal(text).setStyle(Style.EMPTY.withColor(new TextColor(TEXT_COLOR))) : null;
        justUpdated = true;
    }

    public void setDuration(long duration) {
        this.duration = duration;
        justUpdated = true;
    }

    // Addons: @Override this method in a subclass if you want a different sound
    public SoundInstance getSound() {
        return PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_CHIME, 1.2f, 1);
    }
}
