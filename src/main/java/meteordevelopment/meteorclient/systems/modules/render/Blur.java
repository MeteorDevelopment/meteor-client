/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.WindowResizedEvent;
import meteordevelopment.meteorclient.events.render.RenderAfterWorldEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.Pair;

public class Blur extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScreens = settings.createGroup("Screens");

    // Strength-Levels from https://github.com/jonaburg/picom/blob/a8445684fe18946604848efb73ace9457b29bf80/src/backend/backend_common.c#L372
    private final Pair<Integer, Double>[] strengths = new Pair[]{
        new Pair<>(1, 1.25),   // LVL 1
        new Pair<>(1, 2.25),   // LVL 2
        new Pair<>(2, 2.0),      // LVL 3
        new Pair<>(2, 3.0),      // LVL 4
        new Pair<>(2, 4.25),   // LVL 5
        new Pair<>(3, 2.5),    // LVL 6
        new Pair<>(3, 3.25),   // LVL 7
        new Pair<>(3, 4.25),   // LVL 8
        new Pair<>(3, 5.5),    // LVL 9
        new Pair<>(4, 3.25),   // LVL 10
        new Pair<>(4, 4.0),      // LVL 11
        new Pair<>(4, 5.0),      // LVL 12
        new Pair<>(4, 6.0),      // LVL 13
        new Pair<>(4, 7.25),   // LVL 14
        new Pair<>(4, 8.25),   // LVL 15
        new Pair<>(5, 4.5),    // LVL 16
        new Pair<>(5, 5.25),   // LVL 17
        new Pair<>(5, 6.25),   // LVL 18
        new Pair<>(5, 7.25),   // LVL 19
        new Pair<>(5, 8.5)     // LVL 20
    };

    // General
    private final Setting<Integer> strength = sgGeneral.add(new IntSetting.Builder()
        .name("strength")
        .description("How strong the blur should be.")
        .defaultValue(5)
        .min(1)
        .max(20)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Integer> fadeTime = sgGeneral.add(new IntSetting.Builder()
        .name("fade-time")
        .description("How long the fade will last in milliseconds.")
        .defaultValue(100)
        .min(0)
        .sliderMax(500)
        .build()
    );

    // Screens

    private final Setting<Boolean> meteor = sgScreens.add(new BoolSetting.Builder()
        .name("meteor")
        .description("Applies blur to Meteor screens.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> inventories = sgScreens.add(new BoolSetting.Builder()
        .name("inventories")
        .description("Applies blur to inventory screens.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chat = sgScreens.add(new BoolSetting.Builder()
        .name("chat")
        .description("Applies blur when in chat.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> other = sgScreens.add(new BoolSetting.Builder()
        .name("other")
        .description("Applies blur to all other screen types.")
        .defaultValue(true)
        .build()
    );

    private Shader shaderDown, shaderUp, shaderPassthrough;
    private final Framebuffer[] fbos = new Framebuffer[6];

    private boolean enabled;
    private long fadeEndAt;

    public Blur() {
        super(Categories.Render, "blur", "Blurs background when in GUI screens.");

        // The listeners need to run even when the module is not enabled
        MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(WindowResizedEvent.class, event -> {
            // Resize all fbos
            for (Framebuffer fbo : fbos) {
                if (fbo != null) {
                    fbo.resize();
                }
            }
        }));

        MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(RenderAfterWorldEvent.class, event -> onRenderAfterWorld()));
    }

    private void onRenderAfterWorld() {
        // Enable / disable with fading
        boolean shouldRender = shouldRender();
        long time = System.currentTimeMillis();

        if (enabled) {
            if (!shouldRender) {
                if (fadeEndAt == -1) fadeEndAt = System.currentTimeMillis() + fadeTime.get();

                if (time >= fadeEndAt) {
                    enabled = false;
                    fadeEndAt = -1;
                }
            }
        } else {
            if (shouldRender) {
                enabled = true;
                fadeEndAt = System.currentTimeMillis() + fadeTime.get();
            }
        }

        if (!enabled) return;

        // Initialize shader and framebuffer if running for the first time
        if (shaderDown == null) {
            shaderDown = new Shader("blur.vert", "blur_down.frag");
            shaderUp = new Shader("blur.vert", "blur_up.frag");
            shaderPassthrough = new Shader("passthrough.vert", "passthrough.frag");
            for (int i = 0; i < fbos.length; i++) {
                fbos[i] = new Framebuffer(1 / Math.pow(2, i));
            }
        }

        // Update progress
        double progress = 1;

        if (time < fadeEndAt) {
            if (shouldRender) progress = 1 - (fadeEndAt - time) / fadeTime.get().doubleValue();
            else progress = (fadeEndAt - time) / fadeTime.get().doubleValue();
        } else {
            fadeEndAt = -1;
        }

        // Update strength
        Pair<Integer, Double> strength = strengths[(int) ((this.strength.get() - 1) * progress)];

        // Render the blur
        PostProcessRenderer.beginRender();

        // Initial downsample
        renderToFbo(fbos[0], MinecraftClient.getInstance().getFramebuffer().getColorAttachment(), shaderDown, strength.getRight());

        // Downsample
        for (int i = 0; i < strength.getLeft(); i++) {
            renderToFbo(fbos[i + 1], fbos[i].texture, shaderDown, strength.getRight());
        }

        // Upsample
        for (int i = strength.getLeft(); i >= 1; i--) {
            renderToFbo(fbos[i - 1], fbos[i].texture, shaderUp, strength.getRight());
        }

        // Render output
        MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
        shaderPassthrough.bind();
        GL.bindTexture(fbos[0].texture);
        shaderPassthrough.set("uTexture", 0);
        PostProcessRenderer.render();

        PostProcessRenderer.endRender();
    }

    /**
     * Renders one iteration of the blur.
     *
     * @param targetFbo The framebuffer to render to.
     * @param sourceText The texture to use.
     * @param shader The shader to use.
     */
    private void renderToFbo(Framebuffer targetFbo, int sourceText, Shader shader, double offset) {
        targetFbo.bind();
        targetFbo.setViewport();
        shader.bind();
        GL.bindTexture(sourceText);
        shader.set("uTexture", 0);
        shader.set("uHalfTexelSize", .5 / targetFbo.width, .5 / targetFbo.height);
        shader.set("uOffset", offset);
        PostProcessRenderer.render();
    }

    private boolean shouldRender() {
        if (!isActive()) return false;
        Screen screen = mc.currentScreen;

        if (screen instanceof WidgetScreen) return meteor.get();
        if (screen instanceof HandledScreen) return inventories.get();
        if (screen instanceof ChatScreen) return chat.get();
        if (screen != null) return other.get();

        return false;
    }
}
