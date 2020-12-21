/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.Cancellable;
import minegame159.meteorclient.events.render.DrawSideEvent;
import minegame159.meteorclient.events.render.RenderBlockEntityEvent;
import minegame159.meteorclient.events.world.AmbientOcclusionEvent;
import minegame159.meteorclient.events.world.ChunkOcclusionEvent;
import minegame159.meteorclient.mixin.BlockEntityTypeAccessor;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BlockListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.List;

public class XRay extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks.")
            .defaultValue(new ArrayList<>(0))
            .onChanged(blocks1 -> {
                if (isActive()) mc.worldRenderer.reload();
            })
            .build()
    );

    private boolean fullBrightWasActive = false;

    public XRay() {
        super(Category.Render, "xray", "Only renders specified blocks. Good for mining.");
    }

    @Override
    public void onActivate() {
        FullBright fullBright = ModuleManager.INSTANCE.get(FullBright.class);
        fullBrightWasActive = fullBright.isActive();
        if (!fullBright.isActive()) fullBright.toggle();

        mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        FullBright fullBright = ModuleManager.INSTANCE.get(FullBright.class);
        if (!fullBrightWasActive && fullBright.isActive()) fullBright.toggle();

        if (!MeteorClient.IS_DISCONNECTING) mc.worldRenderer.reload();
    }

    @EventHandler
    private final Listener<RenderBlockEntityEvent> onRenderBlockEntity = new Listener<>(event -> {
        if (!Utils.blockRenderingBlockEntitiesInXray) return;

        for (Block block : ((BlockEntityTypeAccessor) event.blockEntity.getType()).getBlocks()) {
            if (isBlocked(block)) {
                event.cancel();
                break;
            }
        }
    });

    @EventHandler
    private final Listener<DrawSideEvent> onDrawSide = new Listener<>(event -> event.setDraw(!isBlocked(event.state.getBlock())));

    @EventHandler
    private final Listener<ChunkOcclusionEvent> onChunkOcclusion = new Listener<>(Cancellable::cancel);

    @EventHandler
    private final Listener<AmbientOcclusionEvent> onAmbientOcclusion = new Listener<>(event -> event.lightLevel = 1);

    public boolean isBlocked(Block block) {
        return isActive() && !blocks.get().contains(block);
    }
}
