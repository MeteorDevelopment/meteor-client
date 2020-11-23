/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BlockListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
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
        super(Category.Render, "xray", "See only specified blocks.");
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

    public boolean isBlocked(Block block) {
        return isActive() && !blocks.get().contains(block);
    }
}
