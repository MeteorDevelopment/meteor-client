/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VulkanDevice.class)
public abstract class VulkanDeviceMixin implements IGpuDevice {
    @Unique
    private int x, y, width, height;

    @Unique
    private boolean set;

    @Override
    public void meteor$pushScissor(int x, int y, int width, int height) {
        if (set) throw new IllegalStateException("Currently there can only be one global scissor pushed");

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        set = true;
    }

    @Override
    public void meteor$popScissor() {
        if (!set) throw new IllegalStateException("No scissor pushed");

        set = false;
    }

    @Deprecated
    @Override
    public void meteor$onCreateRenderPass(RenderPassBackend backend) {
        if (set) {
            backend.enableScissor(x, y, width, height);
        }
    }
}
