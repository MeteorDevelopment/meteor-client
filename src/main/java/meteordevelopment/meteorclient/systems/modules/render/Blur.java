/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.IntFloatImmutablePair;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ResolutionChangedEvent;
import meteordevelopment.meteorclient.events.render.RenderAfterWorldEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixininterface.IGpuTexture;
import meteordevelopment.meteorclient.renderer.FixedUniformStorage;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.gl.DynamicUniformStorage;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.nio.ByteBuffer;

public class Blur extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScreens = settings.createGroup("Screens");

    // Strength-Levels from https://github.com/jonaburg/picom/blob/a8445684fe18946604848efb73ace9457b29bf80/src/backend/backend_common.c#L372
    private final IntFloatImmutablePair[] strengths = new IntFloatImmutablePair[]{
        IntFloatImmutablePair.of(1, 1.25f), // LVL 1
        IntFloatImmutablePair.of(1, 2.25f), // LVL 2
        IntFloatImmutablePair.of(2, 2.0f),  // LVL 3
        IntFloatImmutablePair.of(2, 3.0f),  // LVL 4
        IntFloatImmutablePair.of(2, 4.25f), // LVL 5
        IntFloatImmutablePair.of(3, 2.5f),  // LVL 6
        IntFloatImmutablePair.of(3, 3.25f), // LVL 7
        IntFloatImmutablePair.of(3, 4.25f), // LVL 8
        IntFloatImmutablePair.of(3, 5.5f),  // LVL 9
        IntFloatImmutablePair.of(4, 3.25f), // LVL 10
        IntFloatImmutablePair.of(4, 4.0f),  // LVL 11
        IntFloatImmutablePair.of(4, 5.0f),  // LVL 12
        IntFloatImmutablePair.of(4, 6.0f),  // LVL 13
        IntFloatImmutablePair.of(4, 7.25f), // LVL 14
        IntFloatImmutablePair.of(4, 8.25f), // LVL 15
        IntFloatImmutablePair.of(5, 4.5f),  // LVL 16
        IntFloatImmutablePair.of(5, 5.25f), // LVL 17
        IntFloatImmutablePair.of(5, 6.25f), // LVL 18
        IntFloatImmutablePair.of(5, 7.25f), // LVL 19
        IntFloatImmutablePair.of(5, 8.5f)   // LVL 20
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

    private final GpuTextureView[] fbos = new GpuTextureView[6];
    private GpuBufferSlice[] ubos;

    private boolean enabled;
    private long fadeEndAt;
    private float previousOffset = -1;

    public Blur() {
        super(Categories.Render, "blur", "Blurs background when in GUI screens.");

        // Initialize fbos for the first time
        for (int i = 0; i < fbos.length; i++) {
            fbos[i] = createFbo(i);
        }

        // The listeners need to run even when the module is not enabled
        MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(ResolutionChangedEvent.class, event -> {
            // Resize all fbos
            for (int i = 0; i < fbos.length; i++) {
                if (fbos[i] != null) {
                    fbos[i].close();
                }

                fbos[i] = createFbo(i);
            }

            // Invalidate ubos
            previousOffset = -1;
        }));

        MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(RenderAfterWorldEvent.class, event -> onRenderAfterWorld()));
    }

    private GpuTextureView createFbo(int i) {
        double scale = 1 / Math.pow(2, i);

        int width = (int) (mc.getWindow().getFramebufferWidth() * scale);
        int height = (int) (mc.getWindow().getFramebufferHeight() * scale);

        return RenderSystem.getDevice().createTextureView(RenderSystem.getDevice().createTexture("Blur - " + i, 15,  TextureFormat.RGBA8, width, height, 1, 1));
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

        // Update progress
        double progress = 1;

        if (time < fadeEndAt) {
            if (shouldRender) progress = 1 - (fadeEndAt - time) / fadeTime.get().doubleValue();
            else progress = (fadeEndAt - time) / fadeTime.get().doubleValue();
        } else {
            fadeEndAt = -1;
        }

        // Update strength
        IntFloatImmutablePair strength = strengths[(int) ((this.strength.get() - 1) * progress)];
        int iterations = strength.leftInt();
        float offset = strength.rightFloat();

        // Update uniforms
        if (previousOffset != offset) {
            updateUniforms(offset);
            previousOffset = offset;
        }

        // Initial downsample
        renderToFbo(fbos[0], mc.getFramebuffer().getColorAttachmentView(), MeteorRenderPipelines.BLUR_DOWN, ubos[0]);

        // Downsample
        for (int i = 0; i < iterations; i++) {
            renderToFbo(fbos[i + 1], fbos[i], MeteorRenderPipelines.BLUR_DOWN, ubos[i + 1]);
        }

        // Upsample
        for (int i = iterations; i >= 1; i--) {
            renderToFbo(fbos[i - 1], fbos[i], MeteorRenderPipelines.BLUR_UP, ubos[i - 1]);
        }

        // Render output
        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(MeteorRenderPipelines.BLUR_PASSTHROUGH)
            .fullscreen()
            .sampler("u_Texture", fbos[0])
            .end();
    }

    private void renderToFbo(GpuTextureView targetFbo, GpuTextureView sourceTexture, RenderPipeline pipeline, GpuBufferSlice ubo) {
        AddressMode prevAddressModeU = ((IGpuTexture) sourceTexture.texture()).meteor$getAddressModeU();
        AddressMode prevAddressModeV = ((IGpuTexture) sourceTexture.texture()).meteor$getAddressModeV();

        sourceTexture.texture().setAddressMode(AddressMode.CLAMP_TO_EDGE);

        MeshRenderer.begin()
            .attachments(targetFbo, null)
            .pipeline(pipeline)
            .fullscreen()
            .uniform("BlurData", ubo)
            .sampler("u_Texture", sourceTexture)
            .end();

        sourceTexture.texture().setAddressMode(prevAddressModeU, prevAddressModeV);
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

    // Uniforms

    private void updateUniforms(float offset) {
        UNIFORM_STORAGE.clear();

        BlurUniformData[] uboData = new BlurUniformData[6];
        for (int i = 0; i < uboData.length; i++) {
            GpuTextureView fbo = fbos[i];
            uboData[i] = new BlurUniformData(
                0.5f / fbo.getWidth(0), 0.5f / fbo.getHeight(0),
                offset
            );
        }

        ubos = UNIFORM_STORAGE.writeAll(uboData);
    }

    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec2()
        .putFloat()
        .get();

    private static final FixedUniformStorage<BlurUniformData> UNIFORM_STORAGE = new FixedUniformStorage<>("Meteor - Blur UBO", UNIFORM_SIZE, 6);

    private record BlurUniformData(float halfTexelSizeX, float halfTexelSizeY, float offset) implements DynamicUniformStorage.Uploadable {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(halfTexelSizeX, halfTexelSizeY)
                .putFloat(offset);
        }
    }
}
