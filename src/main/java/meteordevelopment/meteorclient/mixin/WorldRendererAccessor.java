/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor
    void setEntityOutlinesFramebuffer(Framebuffer framebuffer);

    @Accessor("blockBreakingInfos")
    Int2ObjectMap<BlockBreakingInfo> getBlockBreakingInfos();
}
